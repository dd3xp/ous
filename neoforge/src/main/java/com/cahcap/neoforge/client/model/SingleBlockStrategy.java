package com.cahcap.neoforge.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;

import java.util.function.Function;

/**
 * Cleanup-only strategy for custom-shape blocks whose geometry fits inside a single cell.
 * <p>
 * Strips {@code rotation:{angle:0}} (the AO-breaking Blockbench export artifact) and hands
 * the cleaned elements to the vanilla baker. No clipping, no state dispatch.
 */
public final class SingleBlockStrategy implements BakeStrategy {

    public static final SingleBlockStrategy INSTANCE = new SingleBlockStrategy();

    private SingleBlockStrategy() {}

    @Override
    public BakedModel bake(JsonObject rawModel,
                           IGeometryBakingContext ctx,
                           ModelBaker baker,
                           Function<Material, TextureAtlasSprite> spriteGetter,
                           ModelState modelState,
                           ItemOverrides overrides) {
        JsonArray elements = ElementProcessing.stripZeroRotation(rawModel.getAsJsonArray("elements"));
        JsonObject textures = rawModel.has("textures") && rawModel.get("textures").isJsonObject()
                ? rawModel.getAsJsonObject("textures").deepCopy() : new JsonObject();
        JsonElement textureSize = rawModel.get("texture_size");

        boolean useAO = !rawModel.has("ambientocclusion")
                || rawModel.get("ambientocclusion").getAsBoolean();

        BakedModel baked = ElementProcessing.bakeSubset(elements, textures, textureSize,
                baker, spriteGetter, modelState, useAO);

        // bakeSubset's synthetic model declares minecraft:block/block as its parent but nothing
        // ever resolves it, so the baked result's transforms collapse to identity — rotation and
        // scale are lost in every item display context. Re-attach the real model's transforms
        // the same way MultiblockStrategy does.
        return new TransformOverride(baked, ctx.getTransforms());
    }

    /** Delegates everything but the item display transforms to the vanilla-baked model. */
    private static final class TransformOverride extends BakedModelWrapper<BakedModel> {

        private final ItemTransforms transforms;

        TransformOverride(BakedModel original, ItemTransforms transforms) {
            super(original);
            this.transforms = transforms;
        }

        @Override
        public ItemTransforms getTransforms() {
            return transforms;
        }

        /**
         * Must be overridden alongside {@link #getTransforms()}: the item renderer applies
         * transforms through this method, and {@code BakedModelWrapper} forwards it straight to
         * the wrapped model, which would put the identity transforms back.
         */
        @Override
        public BakedModel applyTransform(ItemDisplayContext displayContext, PoseStack poseStack,
                                         boolean applyLeftHandTransform) {
            transforms.getTransform(displayContext).apply(applyLeftHandTransform, poseStack);
            return this;
        }
    }
}
