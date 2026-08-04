package com.cahcap.neoforge.client.screen;

import com.cahcap.OusCommon;
import com.cahcap.common.inventory.CosmosChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Cosmos Chest. The texture is wider than a vanilla container, so the blit has to
 * pass explicit texture dimensions rather than relying on the 256x256 default.
 */
public class CosmosChestScreen extends AbstractContainerScreen<CosmosChestMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            OusCommon.MOD_ID, "textures/gui/container/cosmos_chest.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 258;

    public CosmosChestScreen(CosmosChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = CosmosChestMenu.WIDTH;
        this.imageHeight = CosmosChestMenu.HEIGHT;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight,
                TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
