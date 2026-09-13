package io.github.themrmilchmann.ae2cc.registry;

import io.github.themrmilchmann.ae2cc.AE2CCBridgeForge;
import io.github.themrmilchmann.ae2cc.AE2CCAdapterBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AE2CCBridgeForge.MOD_ID);

    public static final RegistryObject<BlockEntityType<AE2CCAdapterBlockEntity>> ADAPTER = 
        BLOCK_ENTITIES.register("adapter",
            () -> BlockEntityType.Builder.of(AE2CCAdapterBlockEntity::new, 
                ModBlocks.ADAPTER.get()).build(null));
}