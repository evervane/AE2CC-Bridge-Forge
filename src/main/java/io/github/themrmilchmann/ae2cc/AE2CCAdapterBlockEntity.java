package io.github.themrmilchmann.ae2cc;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.google.common.collect.ImmutableSet;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import io.github.themrmilchmann.ae2cc.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AE2CCAdapterBlockEntity extends AENetworkBlockEntity implements ICraftingRequester, IGridTickable {
    private static final AtomicBoolean INTERNAL_ASSUMPTION_FAILED = new AtomicBoolean(false);
    private static final Logger LOGGER = LogManager.getLogger();

    private final ReentrantLock pendingJobLock = new ReentrantLock();
    private final List<PendingCraftingJob> pendingJobs = new ArrayList<>();

    private final ReentrantLock craftingJobLock = new ReentrantLock();
    private final List<CraftingJob> craftingJobs = new ArrayList<>();

    private record PendingCraftingJob(UUID id, Future<ICraftingPlan> futureCraftingPlan, @Nullable String cpu) {}
    private record CraftingJob(UUID id, ICraftingLink link) {}

    private final AdapterPeripheral peripheral = new AdapterPeripheral();

    public AE2CCAdapterBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.ADAPTER.get(), blockPos, blockState);

        this.getMainNode().setIdlePowerUsage(5);
        this.getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.getMainNode().addService(ICraftingRequester.class, this);
        this.getMainNode().addService(IGridTickable.class, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AE2CCAdapterBlockEntity blockEntity) {
        blockEntity.tickInternal();
    }

    private void tickInternal() {
        IGridNode node = this.getGridNode();
        if (node == null || node.getGrid() == null) return;

        this.pendingJobLock.lock();
        try {
            ICraftingService craftingService = node.getGrid().getCraftingService();
            Iterator<PendingCraftingJob> pendingJobIterator = this.pendingJobs.iterator();

            while (pendingJobIterator.hasNext()) {
                PendingCraftingJob pendingJob = pendingJobIterator.next();
                Future<ICraftingPlan> futureCraftingPlan = pendingJob.futureCraftingPlan();

                if (futureCraftingPlan.isCancelled()) {
                    pendingJobIterator.remove();
                    this.peripheral.notify("ae2cc:crafting_cancelled", pendingJob.id().toString(), "CANCELLED");
                    continue;
                }

                if (!futureCraftingPlan.isDone()) continue;
                pendingJobIterator.remove();

                ICraftingPlan craftingPlan;
                try {
                    craftingPlan = futureCraftingPlan.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }

                ICraftingCPU craftingCPU = null;
                if (pendingJob.cpu() != null) {
                    craftingCPU = craftingService.getCpus().stream().filter(it -> {
                        Component cpuName = it.getName();
                        return cpuName != null && pendingJob.cpu().equals(cpuName.getString());
                    }).findAny().orElse(null);

                    if (craftingCPU == null) {
                        this.peripheral.notify("ae2cc:crafting_cancelled", pendingJob.id().toString(), "CPU_NOT_FOUND");
                        continue;
                    }
                }

                IActionSource actionSource = IActionSource.ofMachine(this);
                ICraftingSubmitResult craftingSubmitResult = node.getGrid().getCraftingService().submitJob(craftingPlan, this, craftingCPU, false, actionSource);
                if (!craftingSubmitResult.successful()) {
                    String reason = switch (Objects.requireNonNull(craftingSubmitResult.errorCode())) {
                        case INCOMPLETE_PLAN -> "INCOMPLETE_PLAN";
                        case NO_CPU_FOUND -> "NO_CPU_FOUND";
                        case NO_SUITABLE_CPU_FOUND -> "NO_SUITABLE_CPU_FOUND";
                        case CPU_BUSY -> "CPU_BUSY";
                        case CPU_OFFLINE -> "CPU_OFFLINE";
                        case CPU_TOO_SMALL -> "CPU_TOO_SMALL";
                        case MISSING_INGREDIENT -> "MISSING_INGREDIENT";
                    };

                    this.peripheral.notify("ae2cc:crafting_cancelled", pendingJob.id().toString(), reason);
                    continue;
                }

                ICraftingLink craftingLink = craftingSubmitResult.link();
                assert craftingLink != null;

                CraftingJob craftingJob = new CraftingJob(pendingJob.id(), craftingLink);
                craftingJobLock.lock();
                try {
                    craftingJobs.add(craftingJob);
                    this.peripheral.notify("ae2cc:crafting_started", craftingJob.id().toString());
                } finally {
                    craftingJobLock.unlock();
                }
            }
        } finally {
            this.pendingJobLock.unlock();
        }
    }

    public IPeripheral asPeripheral() {
        return this.peripheral;
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.craftingJobs.stream().map(CraftingJob::link).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        return 0;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        this.craftingJobLock.lock();
        try {
            this.craftingJobs.removeIf(job -> {
                if (job.link() == link) {
                    this.peripheral.notify("ae2cc:crafting_done", job.id().toString());
                    return true;
                }
                return false;
            });
        } finally {
            this.craftingJobLock.unlock();
        }
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return this.getGridNode();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int tickCount) {
        tickInternal();
        return TickRateModulation.SAME;
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        ListTag jobsTag = new ListTag();
        craftingJobLock.lock();
        try {
            for (CraftingJob job : craftingJobs) {
                CompoundTag jobTag = new CompoundTag();
                jobTag.putUUID("id", job.id());
                // Note: ICraftingLink may not be directly serializable in Forge
                // This is a simplified version
                jobsTag.add(jobTag);
            }
        } finally {
            craftingJobLock.unlock();
        }
        data.put("jobs", jobsTag);
    }

    // load() is final in AEBaseBlockEntity, cannot override

    public final class AdapterPeripheral implements IPeripheral {
        private final ReentrantLock attachedComputerLock = new ReentrantLock();
        private final List<IComputerAccess> attachedComputers = new ArrayList<>();

        @Nonnull
        @Override
        public String getType() {
            return "ae2cc_adapter";
        }

        @Override
        public boolean equals(@Nullable IPeripheral obj) {
            if (!(obj instanceof AdapterPeripheral other)) return false;
            return AE2CCAdapterBlockEntity.this == other.getBlockEntity();
        }

        public AE2CCAdapterBlockEntity getBlockEntity() {
            return AE2CCAdapterBlockEntity.this;
        }

        @Override
        public void attach(@Nonnull IComputerAccess computer) {
            attachedComputerLock.lock();
            try {
                attachedComputers.add(computer);
            } finally {
                attachedComputerLock.unlock();
            }
        }

        @Override
        public void detach(@Nonnull IComputerAccess computer) {
            attachedComputerLock.lock();
            try {
                attachedComputers.remove(computer);
            } finally {
                attachedComputerLock.unlock();
            }
        }

        private void notify(String event, Object... data) {
            attachedComputerLock.lock();
            try {
                for (IComputerAccess attachedComputer : this.attachedComputers) {
                    attachedComputer.queueEvent(event, data);
                }
            } finally {
                attachedComputerLock.unlock();
            }
        }

        private IGrid getGrid() throws LuaException {
            IGridNode node = AE2CCAdapterBlockEntity.this.getGridNode();
            if (node == null || node.getGrid() == null) {
                throw new LuaException("Cannot connect to AE2 Network");
            }
            return node.getGrid();
        }

        @LuaFunction(mainThread = true)
        public final Map<String, Object> getStatus() {
            Map<String, Object> status = new HashMap<>();
            IGridNode node = AE2CCAdapterBlockEntity.this.getGridNode();
            status.put("hasNode", node != null);
            if (node != null) {
                status.put("hasGrid", node.getGrid() != null);
                status.put("active", node.isActive());
                status.put("gridReady", node.getGrid() != null);
                if (node.getGrid() != null) {
                    int nodeCount = 0;
                    for (IGridNode n : node.getGrid().getNodes()) nodeCount++;
                    status.put("gridNodes", nodeCount);
                }
            }
            return status;
        }

        @LuaFunction(mainThread = true)
        public final List<Map<String, Object>> getAvailableObjects() throws LuaException {
            IGrid grid = getGrid();
            MEStorage inventory = grid.getStorageService().getInventory();
            KeyCounter keyCounter = inventory.getAvailableStacks();

            return StreamSupport.stream(keyCounter.spliterator(), false)
                .map(it -> {
                    AEKey key = it.getKey();
                    long value = it.getLongValue();

                    Map<String, Object> data = new HashMap<>();
                    data.put("id", key.getId().toString());
                    data.put("displayName", key.getDisplayName().getString());

                    if (key instanceof AEFluidKey) {
                        data.put("type", "fluid");
                        data.put("amount", value / 81);
                    } else if (key instanceof AEItemKey) {
                        data.put("type", "item");
                        data.put("amount", value);
                    } else {
                        data.put("type", "unknown");
                        data.put("amount", value);
                    }

                    return Map.copyOf(data);
                })
                .filter(Objects::nonNull)
                .toList();
        }

        @LuaFunction(mainThread = true)
        public final List<Map<String, Object>> getCraftableObjects() throws LuaException {
            IGrid grid = getGrid();
            ICraftingService craftingService = grid.getCraftingService();
            return craftingService.getCraftables(it -> it instanceof AEFluidKey || it instanceof AEItemKey)
                .stream()
                .map(this::deriveLuaRepresentation)
                .toList();
        }

        @LuaFunction
        public final List<Map<String, Object>> getCraftingCPUs() throws LuaException {
            IGrid grid = getGrid();
            Set<ICraftingCPU> cpus = grid.getCraftingService().getCpus();

            return cpus.stream()
                .map(cpu -> {
                    String selectionMode = switch (cpu.getSelectionMode()) {
                        case ANY -> "ANY";
                        case MACHINE_ONLY -> "MACHINE_ONLY";
                        case PLAYER_ONLY -> "PLAYER_ONLY";
                    };

                    HashMap<String, Object> data = new HashMap<>();
                    data.put("availableCoProcessors", cpu.getCoProcessors());
                    data.put("availableStorage", cpu.getAvailableStorage());
                    data.put("selectionMode", selectionMode);

                    Component name = cpu.getName();
                    if (name != null) {
                        data.put("name", name.getString());
                    }

                    CraftingJobStatus jobStatus = cpu.getJobStatus();
                    if (jobStatus != null) {
                        Map<String, Object> jobData = new HashMap<>();
                        jobData.put("totalObjects", jobStatus.totalItems());
                        jobData.put("craftedObjects", jobStatus.progress());
                        jobData.put("elapsedNanos", jobStatus.elapsedTimeNanos());

                        if (cpu instanceof CraftingCPUCluster cluster) {
                            ICraftingLink link = cluster.craftingLogic.getLastLink();
                            if (link != null) {
                                jobData.put("systemID", link.getCraftingID());
                            }
                        } else {
                            if (!INTERNAL_ASSUMPTION_FAILED.getAndSet(true)) {
                                LOGGER.error("Incorrect assumption about AE2 internals: ICraftingCPU implementation is not a CraftingCPUCluster: {}", cpu.getClass().getName());
                            }
                        }

                        GenericStack stack = jobStatus.crafting();
                        Map<String, Object> stackData = new HashMap<>();
                        stackData.put("amount", stack.amount());
                        stackData.putAll(deriveLuaRepresentation(stack.what()));
                        jobData.put("output", Map.copyOf(stackData));
                        data.put("jobStatus", Map.copyOf(jobData));
                    }

                    return Map.copyOf(data);
                })
                .toList();
        }

        @LuaFunction
        public final List<Map<String, Object>> getIssuedCraftingJobs() {
            pendingJobLock.lock();
            try {
                craftingJobLock.lock();
                try {
                    return Stream.concat(
                        pendingJobs.stream().map(pendingJob -> Map.<String, Object>of(
                            "state", "SCHEDULED",
                            "jobID", pendingJob.id().toString()
                        )),
                        craftingJobs.stream().map(craftingJob -> Map.<String, Object>of(
                            "state", "STARTED",
                            "jobID", craftingJob.id().toString(),
                            "systemID", craftingJob.link().getCraftingID()
                        ))
                    ).toList();
                } finally {
                    craftingJobLock.unlock();
                }
            } finally {
                pendingJobLock.unlock();
            }
        }

        @LuaFunction
        public final List<Map<String, Object>> getAllCraftingRequests() throws LuaException {
            IGrid grid = getGrid();
            try {
                return grid.getCraftingService().getCpus().stream()
                    .flatMap(cpu -> {
                        CraftingJobStatus jobStatus = cpu.getJobStatus();
                        if (jobStatus == null) return Stream.empty();
                        GenericStack stack = jobStatus.crafting();
                        if (stack == null) return Stream.empty();
                        return Stream.of(Map.<String, Object>of(
                            "systemID", stack.what().getId().toString(),
                            "displayName", stack.what().getDisplayName().getString(),
                            "amount", stack.amount()
                        ));
                    })
                    .toList();
            } catch (Exception e) {
                throw new LuaException(e.getMessage());
            }
        }

        @LuaFunction
        public final String scheduleCrafting(String type, String id, long amount) throws LuaException {
            IGrid grid = getGrid();
            ResourceLocation resourceLocation = ResourceLocation.tryParse(id);
            if (resourceLocation == null) throw new LuaException("Invalid ID: '" + id + "'");

            AEKey key;
            switch (type) {
                case "fluid" -> {
                    var fluid = ForgeRegistries.FLUIDS.getValue(resourceLocation);
                    if (fluid == null) throw new LuaException("Fluid does not exist: " + resourceLocation);
                    key = AEFluidKey.of(fluid);
                }
                case "item" -> {
                    var item = ForgeRegistries.ITEMS.getValue(resourceLocation);
                    if (item == null) throw new LuaException("Item does not exist: " + resourceLocation);
                    key = AEItemKey.of(item);
                }
                default -> throw new LuaException("Invalid type: '" + type + "' (Valid types are 'fluid' and 'item')");
            }

            ICraftingService craftingService = grid.getCraftingService();
            IActionSource actionSource = IActionSource.ofMachine(AE2CCAdapterBlockEntity.this);

            IGridNode node = AE2CCAdapterBlockEntity.this.getGridNode();
            if (node == null) throw new LuaException("Cannot connect to AE2 Network");

            Future<ICraftingPlan> futureCraftingPlan = craftingService.beginCraftingCalculation(
                AE2CCAdapterBlockEntity.this.level,
                () -> actionSource,
                key,
                amount,
                CalculationStrategy.CRAFT_LESS
            );

            pendingJobLock.lock();
            try {
                UUID jobID = UUID.randomUUID();
                PendingCraftingJob pendingCraftingJob = new PendingCraftingJob(jobID, futureCraftingPlan, null);
                pendingJobs.add(pendingCraftingJob);
                return jobID.toString();
            } finally {
                pendingJobLock.unlock();
            }
        }

        private Map<String, Object> deriveLuaRepresentation(AEKey key) {
            String type;
            if (key instanceof AEFluidKey) {
                type = "fluid";
            } else if (key instanceof AEItemKey) {
                type = "item";
            } else {
                type = "unknown";
            }

            return Map.of(
                "type", type,
                "id", key.getId().toString(),
                "displayName", key.getDisplayName().getString()
            );
        }
    }
}