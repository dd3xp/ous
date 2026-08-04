package com.cahcap.client.renderer;

import com.cahcap.common.block.CosmosChestBlock;
import com.cahcap.common.blockentity.CosmosChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws the Cosmos Chest.
 * <p>
 * The block is not a vanilla chest, so vanilla's chest renderer — which looks its texture up
 * through {@code Sheets.chooseMaterial} and expects the double-chest combiner — cannot be reused.
 * This walks the same {@link ModelLayers#CHEST} parts by hand.
 */
public class CosmosChestRenderer implements BlockEntityRenderer<CosmosChestBlockEntity> {

    /** Placeholder skin: the vanilla chest until the Cosmos Chest gets its own artwork. */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/chest/normal.png");

    private final ModelPart lid;
    private final ModelPart bottom;
    private final ModelPart lock;

    public CosmosChestRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart root = context.bakeLayer(ModelLayers.CHEST);
        this.bottom = root.getChild("bottom");
        this.lid = root.getChild("lid");
        this.lock = root.getChild("lock");
    }

    @Override
    public void render(CosmosChestBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // In item form there is no level and the throwaway block entity keeps the default
        // facing, which would show the chest from behind. Vanilla faces those south instead.
        BlockState state = blockEntity.getBlockState();
        Direction facing = blockEntity.getLevel() != null && state.hasProperty(CosmosChestBlock.FACING)
                ? state.getValue(CosmosChestBlock.FACING)
                : Direction.SOUTH;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        // Same easing vanilla uses, so the lid moves identically.
        float openness = blockEntity.getOpenNess(partialTick);
        openness = 1.0F - openness;
        openness = 1.0F - openness * openness * openness;

        float angle = -openness * ((float) Math.PI / 2.0F);
        lid.xRot = angle;
        lock.xRot = angle;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(TEXTURE));
        lid.render(poseStack, consumer, packedLight, packedOverlay);
        lock.render(poseStack, consumer, packedLight, packedOverlay);
        bottom.render(poseStack, consumer, packedLight, packedOverlay);

        poseStack.popPose();
    }
}
