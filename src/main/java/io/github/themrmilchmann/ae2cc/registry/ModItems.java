package io.github.themrmilchmann.ae2cc.registry;

import io.github.themrmilchmann.ae2cc.AE2CCBridgeForge;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = 
        DeferredRegister.create(ForgeRegistries.ITEMS, AE2CCBridgeForge.MOD_ID);

    public static final RegistryObject<Item> ADAPTER = ITEMS.register("adapter",
        () -> new BlockItem(ModBlocks.ADAPTER.get(), new Item.Properties()));
}