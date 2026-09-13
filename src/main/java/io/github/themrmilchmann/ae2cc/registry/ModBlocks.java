package io.github.themrmilchmann.ae2cc.registry;

import io.github.themrmilchmann.ae2cc.AE2CCBridgeForge;
import io.github.themrmilchmann.ae2cc.AE2CCAdapterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = 
        DeferredRegister.create(ForgeRegistries.BLOCKS, AE2CCBridgeForge.MOD_ID);

    public static final RegistryObject<Block> ADAPTER = BLOCKS.register("adapter",
        () -> new AE2CCAdapterBlock(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));
}