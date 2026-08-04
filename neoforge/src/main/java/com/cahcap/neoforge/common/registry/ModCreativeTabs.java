package com.cahcap.neoforge.common.registry;

import com.cahcap.OusCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    
    public static final DeferredRegister<CreativeModeTab> TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OusCommon.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OUS_TAB = TABS.register("ous_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + OusCommon.MOD_ID))
                    .icon(() -> new ItemStack(ModItems.FLOWWEAVE_RING.get()))
                    .displayItems((parameters, output) -> {
                        // Herb Products
                        output.accept(ModItems.SCALEPLATE.get());
                        output.accept(ModItems.DEWPETAL.get());
                        output.accept(ModItems.ZEPHYR_BLOSSOM.get());
                        output.accept(ModItems.CRYST_SPINE.get());
                        output.accept(ModItems.PYRO_NODE.get());
                        output.accept(ModItems.STELLAR_MOTE.get());
                        
                        // Herb Seeds
                        output.accept(ModItems.SCLERIS_SEED.get());
                        output.accept(ModItems.DORELLA_SEED.get());
                        output.accept(ModItems.SEPHREL_SEED.get());
                        output.accept(ModItems.CRYSEL_SEED.get());
                        output.accept(ModItems.PYRAZE_SEED.get());
                        output.accept(ModItems.STELLIA_SEED.get());
                        
                        // Crafting Materials
                        output.accept(ModItems.LEATHER_PLATE.get());
                        output.accept(ModItems.VELVET_PLATE.get());
                        output.accept(ModItems.SILK_PLATE.get());
                        output.accept(ModItems.ARCANE_ALLOY_DUST.get());
                        output.accept(ModItems.ARCANE_ALLOY_INGOT.get());
                        output.accept(ModItems.BRILLIANT_GEM_DUST.get());
                        output.accept(ModItems.BRILLIANT_GEM.get());
                        
                        // Red Cherry Items
                        output.accept(ModItems.RED_CHERRY_STICK.get());
                        output.accept(ModItems.RED_CHERRY.get());
                        
                        // Herb Flowers
                        output.accept(ModBlocks.SCLERIS.get());
                        output.accept(ModBlocks.DORELLA.get());
                        output.accept(ModBlocks.SEPHREL.get());
                        output.accept(ModBlocks.CRYSEL.get());
                        output.accept(ModBlocks.PYRAZE.get());
                        output.accept(ModBlocks.STELLIA.get());
                        
                        // Red Cherry Blocks
                        output.accept(ModBlocks.RED_CHERRY_LOG.get());
                        output.accept(ModBlocks.STRIPPED_RED_CHERRY_LOG.get());
                        output.accept(ModBlocks.RED_CHERRY_WOOD.get());
                        output.accept(ModBlocks.STRIPPED_RED_CHERRY_WOOD.get());
                        output.accept(ModBlocks.RED_CHERRY_PLANKS.get());
                        output.accept(ModBlocks.RED_CHERRY_STAIRS.get());
                        output.accept(ModBlocks.RED_CHERRY_SLAB.get());
                        output.accept(ModBlocks.RED_CHERRY_FENCE.get());
                        output.accept(ModBlocks.RED_CHERRY_FENCE_GATE.get());
                        output.accept(ModBlocks.RED_CHERRY_BUTTON.get());
                        output.accept(ModBlocks.RED_CHERRY_PRESSURE_PLATE.get());
                        output.accept(ModBlocks.RED_CHERRY_LEAVES.get());
                        output.accept(ModBlocks.RED_CHERRY_SAPLING.get());
                        
                        // Lumistone Blocks
                        output.accept(ModBlocks.LUMISTONE.get());
                        output.accept(ModBlocks.LUMISTONE_SLAB.get());
                        output.accept(ModBlocks.LUMISTONE_STAIRS.get());
                        output.accept(ModBlocks.LUMISTONE_WALL.get());
                        output.accept(ModBlocks.LUMISTONE_PRESSURE_PLATE.get());
                        output.accept(ModBlocks.LUMISTONE_BUTTON.get());
                        output.accept(ModBlocks.LUMISTONE_BRICKS.get());
                        output.accept(ModBlocks.RUNE_STONE_BRICKS.get());
                        output.accept(ModBlocks.LUMISTONE_BRICK_SLAB.get());
                        output.accept(ModBlocks.LUMISTONE_BRICK_STAIRS.get());
                        output.accept(ModBlocks.LUMISTONE_BRICK_WALL.get());
                        output.accept(ModBlocks.ARCANE_ALLOY_BLOCK.get());
                        
                        // Leafweave Armor
                        output.accept(ModItems.LEAFWEAVE_HELMET.get());
                        output.accept(ModItems.LEAFWEAVE_CHESTPLATE.get());
                        output.accept(ModItems.LEAFWEAVE_LEGGINGS.get());
                        output.accept(ModItems.LEAFWEAVE_BOOTS.get());
                        
                        // Lumistone Tools
                        output.accept(ModItems.LUMISTONE_SWORD.get());
                        output.accept(ModItems.LUMISTONE_PICKAXE.get());
                        output.accept(ModItems.LUMISTONE_AXE.get());
                        output.accept(ModItems.LUMISTONE_SHOVEL.get());
                        output.accept(ModItems.LUMISTONE_HOE.get());
                        
                        // Red Cherry Crossbow
                        output.accept(ModItems.RED_CHERRY_CROSSBOW.get());
                        output.accept(ModItems.RED_CHERRY_BOLT_MAGAZINE.get());
                        
                        // Special Items
                        output.accept(ModItems.FLOWWEAVE_RING.get());
                        output.accept(ModItems.HERB_BOX.get());
                        output.accept(ModItems.POT.get());
                        // Note: CAULDRON and HERB_CABINET are technical items for display only (multiblock structures)
                        output.accept(ModItems.HERB_BASKET.get());
                        output.accept(ModItems.SHELF.get());
                        output.accept(ModItems.WORKBENCH.get());
                        output.accept(ModItems.HERB_POT.get());
                        // No recipe exists yet, so hand out a Speed-bound lantern for testing.
                        // Replace with a plain accept() once the crafting recipe binds the effect.
                        output.accept(createBoundLantern());
                        output.accept(ModItems.ARCANE_ALLOY_ANVIL.get());
                        output.accept(ModItems.ARCANE_ALLOY_HOPPER.get());
                        output.accept(ModItems.COSMOS_CHEST.get());
                        output.accept(ModItems.INCENSE_BURNER.get());
                        
                        output.accept(ModBlocks.ARCANE_ALLOY_BLOCK.get());

                        // Incense Powders
                        output.accept(ModItems.WITHER_SKELETON_POWDER.get());
                        
                        // Crystal Plants
                        output.accept(ModItems.IRON_CRYST_PLANT.get());
                        
                        // Workbench Tools
                        output.accept(ModItems.CUTTING_KNIFE.get());
                        output.accept(ModItems.FEATHER_QUILL.get());
                        output.accept(ModItems.WOVEN_ROPE.get());
                        output.accept(ModItems.FORGE_HAMMER.get());
                    })
                    .build());

    /**
     * Creative-tab Crystal Lantern, pre-bound to Speed. Without a recipe there is no other way
     * to get a working lantern, and an unbound one neither lights up nor applies anything.
     */
    private static net.minecraft.world.item.ItemStack createBoundLantern() {
        net.minecraft.world.item.ItemStack stack =
                new net.minecraft.world.item.ItemStack(ModItems.CRYSTAL_LANTERN.get());
        com.cahcap.common.item.CrystalLanternItem.setBinding(stack,
                java.util.List.of("minecraft:speed"),
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED.value().getColor());
        return stack;
    }
}

