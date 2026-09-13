package io.github.themrmilchmann.ae2cc;

import io.github.themrmilchmann.ae2cc.registry.ModBlockEntities;
import io.github.themrmilchmann.ae2cc.registry.ModBlocks;
import io.github.themrmilchmann.ae2cc.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(AE2CCBridgeForge.MOD_ID)
public class AE2CCBridgeForge {
    public static final String MOD_ID = "ae2cc";
    private static final Logger LOGGER = LogManager.getLogger();

    public AE2CCBridgeForge() {
        var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        modEventBus.addListener(this::addCreativeTabItems);

        LOGGER.info("AE2CC Bridge Forge loaded!");
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(new ItemStack(ModItems.ADAPTER.get()));
        }
    }
}