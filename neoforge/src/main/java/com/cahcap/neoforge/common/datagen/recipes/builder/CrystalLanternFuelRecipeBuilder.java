package com.cahcap.neoforge.common.datagen.recipes.builder;

import com.cahcap.common.recipe.CrystalLanternFuelRecipe;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

/**
 * Builder for Crystal Lantern fuel definitions.
 */
public class CrystalLanternFuelRecipeBuilder {

    private Ingredient fuel = Ingredient.EMPTY;
    /** One minute, matching the design's "one flower per minute". */
    private int burnTime = 1200;

    private CrystalLanternFuelRecipeBuilder() {}

    public static CrystalLanternFuelRecipeBuilder builder() {
        return new CrystalLanternFuelRecipeBuilder();
    }

    public CrystalLanternFuelRecipeBuilder fuel(ItemLike item) {
        this.fuel = Ingredient.of(item);
        return this;
    }

    public CrystalLanternFuelRecipeBuilder fuel(TagKey<Item> tag) {
        this.fuel = Ingredient.of(tag);
        return this;
    }

    public CrystalLanternFuelRecipeBuilder fuel(Ingredient ingredient) {
        this.fuel = ingredient;
        return this;
    }

    /** How many ticks one item burns for. */
    public CrystalLanternFuelRecipeBuilder burnTime(int ticks) {
        this.burnTime = ticks;
        return this;
    }

    public void build(RecipeOutput output, String name) {
        if (fuel.isEmpty()) {
            throw new IllegalStateException("Fuel is required for crystal lantern fuel recipe: " + name);
        }
        if (burnTime <= 0) {
            throw new IllegalStateException("Burn time must be positive for crystal lantern fuel recipe: " + name);
        }

        CrystalLanternFuelRecipe recipe = new CrystalLanternFuelRecipe(fuel, burnTime);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("ous", "crystal_lantern_fuel/" + name);
        output.accept(id, recipe, null);
    }
}
