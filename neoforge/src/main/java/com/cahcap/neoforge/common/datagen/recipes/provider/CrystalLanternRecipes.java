package com.cahcap.neoforge.common.datagen.recipes.provider;

import com.cahcap.neoforge.common.datagen.recipes.builder.CrystalLanternFuelRecipeBuilder;
import com.cahcap.neoforge.common.registry.ModItems;
import net.minecraft.data.recipes.RecipeOutput;

/**
 * Crystal Lantern fuel definitions.
 */
public class CrystalLanternRecipes {

    public void build(RecipeOutput output) {
        // One Dewpetal per minute, per the design.
        CrystalLanternFuelRecipeBuilder.builder()
                .fuel(ModItems.DEWPETAL.get())
                .burnTime(1200)
                .build(output, "dewpetal");
    }
}
