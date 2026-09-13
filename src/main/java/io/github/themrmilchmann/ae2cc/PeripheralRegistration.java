package io.github.themrmilchmann.ae2cc;

import dan200.computercraft.api.ForgeComputerCraftAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = AE2CCBridgeForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PeripheralRegistration {
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ForgeComputerCraftAPI.registerPeripheralProvider((Level level, BlockPos pos, Direction side) -> {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof AE2CCAdapterBlockEntity adapter) {
                    return LazyOptional.of(adapter::asPeripheral);
                }
                return LazyOptional.empty();
            });
        });
    }
}