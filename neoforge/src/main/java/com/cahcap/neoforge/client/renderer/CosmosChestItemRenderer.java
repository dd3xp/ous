package com.cahcap.neoforge.client.renderer;

import com.cahcap.common.blockentity.CosmosChestBlockEntity;
import com.cahcap.neoforge.common.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Renders the Cosmos Chest in item form.
 * <p>
 * The block has no static model — it is drawn by its block entity renderer — so the item would
 * otherwise show up untextured. This draws the same chest through a throwaway block entity, the
 * way vanilla handles its own chest items.
 */
public class CosmosChestItemRenderer extends BlockEntityWithoutLevelRenderer {

    private CosmosChestBlockEntity chest;

    public CosmosChestItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (chest == null) {
            chest = new CosmosChestBlockEntity(BlockPos.ZERO,
                    ModBlocks.COSMOS_CHEST.get().defaultBlockState());
        }
        Minecraft.getInstance().getBlockEntityRenderDispatcher()
                .renderItem(chest, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
