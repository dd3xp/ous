package com.cahcap.client.renderer;

import com.cahcap.OusCommon;
import com.cahcap.common.blockentity.CrystalLanternBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Draws the Crystal Lantern's lamp body, which is kept out of the block model so its colour can
 * follow the block entity.
 * <p>
 * A block colour handler cannot do this: it is evaluated while baking the chunk mesh, so the tint
 * only refreshes when the section happens to be re-meshed. Drawing the body here re-reads the
 * block entity every frame instead, the same way the Herb Pot and Incense Burner renderers work.
 */
public class CrystalLanternRenderer implements BlockEntityRenderer<CrystalLanternBlockEntity> {

    private static final ModelResourceLocation BODY_MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(OusCommon.MOD_ID, "block/crystal_lantern_body"),
            "standalone");

    /** Idle lamp body colour, matching the amethyst the model was originally painted. */
    private static final int AMETHYST_COLOR = 0xC671FD;

    private final BlockRenderDispatcher blockRenderer;

    public CrystalLanternRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CrystalLanternBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(BODY_MODEL);
        if (model == null) {
            return;
        }

        int color = blockEntity.isWorking() ? blockEntity.getPotionColor() : AMETHYST_COLOR;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                bufferSource.getBuffer(RenderType.solid()),
                blockEntity.getBlockState(),
                model,
                r, g, b,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
    }
}
