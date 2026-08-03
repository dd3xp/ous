package com.cahcap.common.recipe;

import com.cahcap.common.registry.ModRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Defines what the Crystal Lantern can burn. Produces nothing — like the kiln catalyst, this is
 * a pure data definition:
 * <ul>
 *     <li>fuel: what item may be put into the lantern</li>
 *     <li>burnTime: how many ticks one of them lasts</li>
 * </ul>
 * Slot capacity and the effect refresh timings stay in code: they belong to the lantern rather
 * than to any one fuel, and the refresh interval is what makes the effect permanent.
 */
public class CrystalLanternFuelRecipe implements Recipe<SingleRecipeInput> {

    private final Ingredient fuel;
    private final int burnTime;

    public CrystalLanternFuelRecipe(Ingredient fuel, int burnTime) {
        this.fuel = fuel;
        this.burnTime = burnTime;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return fuel.test(input.getItem(0));
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    public Ingredient getFuel() {
        return fuel;
    }

    public int getBurnTime() {
        return burnTime;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return ModRegistries.CRYSTAL_LANTERN_FUEL_RECIPE_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CrystalLanternFuelRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<CrystalLanternFuelRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC_NONEMPTY.fieldOf("fuel").forGetter(r -> r.fuel),
                        Codec.INT.fieldOf("burn_time").forGetter(r -> r.burnTime)
                ).apply(instance, CrystalLanternFuelRecipe::new)
        );

        private static final StreamCodec<RegistryFriendlyByteBuf, CrystalLanternFuelRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        private static CrystalLanternFuelRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
            Ingredient fuel = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            int burnTime = buf.readVarInt();
            return new CrystalLanternFuelRecipe(fuel, burnTime);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buf, CrystalLanternFuelRecipe recipe) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.fuel);
            buf.writeVarInt(recipe.burnTime);
        }

        @Override
        public MapCodec<CrystalLanternFuelRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CrystalLanternFuelRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
