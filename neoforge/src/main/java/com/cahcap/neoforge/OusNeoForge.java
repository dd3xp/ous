package com.cahcap.neoforge;

import com.cahcap.OusCommon;
import com.cahcap.neoforge.common.handler.StrippingHandler;
import com.cahcap.neoforge.common.registry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(OusCommon.MOD_ID)
public class OusNeoForge {
    
    public static final Logger LOGGER = LoggerFactory.getLogger(OusCommon.MOD_ID);

    public OusNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Once Upon a Season NeoForge is loading...");
        
        modEventBus.addListener(this::commonSetup);
        
        // Register armor materials BEFORE items (items depend on armor materials)
        ModArmorMaterials.register(modEventBus);
        
        // Register all deferred registries
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        modEventBus.addListener(OusNeoForge::addHopperBlockEntityBlocks);
        ModSounds.SOUNDS.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        
        // Register recipe types and serializers
        ModRecipeTypes.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        
        // Initialize common registries
        RegistryInit.init();
        
        // Initialize common module
        OusCommon.init();
        
        LOGGER.info("Once Upon a Season NeoForge registration complete");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialize stripping handler (Red Cherry log -> Stripped Red Cherry log, etc.)
            StrippingHandler.init();
            
            // Register flower pot contents (must be done in enqueueWork for thread safety)
            ModBlocks.registerFlowerPots();
        });
        
        // Initialize platform item transfer helper using NeoForge capabilities
        com.cahcap.common.util.ItemTransferHelper.INSTANCE =
                new com.cahcap.neoforge.common.handler.NeoForgeItemTransferHelper();

        OusCommon.commonSetup();
        LOGGER.info("Once Upon a Season NeoForge common setup complete");
    }

    /**
     * The Arcane Alloy Hopper reuses vanilla's HopperBlockEntity, whose only constructor hardcodes
     * BlockEntityType.HOPPER. Register the block against that type or creating the block entity
     * fails validation.
     */
    private static void addHopperBlockEntityBlocks(
            net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent event) {
        event.modify(net.minecraft.world.level.block.entity.BlockEntityType.HOPPER,
                ModBlocks.ARCANE_ALLOY_HOPPER.get());
    }
}

