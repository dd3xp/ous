package com.cahcap.neoforge.common.registry;

import com.cahcap.OusCommon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModMenuTypes {
    
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
            DeferredRegister.create(BuiltInRegistries.MENU, OusCommon.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<com.cahcap.common.inventory.ArcaneAlloyAnvilMenu>> ARCANE_ALLOY_ANVIL =
            MENU_TYPES.register("arcane_alloy_anvil",
                    () -> new MenuType<>(com.cahcap.common.inventory.ArcaneAlloyAnvilMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredHolder<MenuType<?>, MenuType<com.cahcap.common.inventory.CosmosChestMenu>> COSMOS_CHEST =
            MENU_TYPES.register("cosmos_chest",
                    () -> new MenuType<>(com.cahcap.common.inventory.CosmosChestMenu::new, FeatureFlags.VANILLA_SET));
}

