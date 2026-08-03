package com.cahcap.neoforge.client.handler;

import com.cahcap.OusCommon;
import com.cahcap.common.block.CrystalLanternBlock;
import com.cahcap.common.blockentity.CrystalLanternBlockEntity;
import com.cahcap.common.registry.ModRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Client-side handler for the Crystal Lantern HUD tooltip.
 * Shows the remaining fuel count below the crosshair.
 */
@EventBusSubscriber(modid = OusCommon.MOD_ID, value = Dist.CLIENT)
public class CrystalLanternTooltipHandler extends TooltipHandler {

    private static final CrystalLanternTooltipHandler INSTANCE = new CrystalLanternTooltipHandler();

    // Per-frame state
    private CrystalLanternBlockEntity lantern;

    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        INSTANCE.handleEvent(event);
    }

    @Override
    protected boolean isTargetBlock(BlockState state) {
        return state.getBlock() instanceof CrystalLanternBlock;
    }

    @Override
    protected boolean isValidEntity(BlockEntity entity) {
        if (entity instanceof CrystalLanternBlockEntity l) {
            lantern = l;
            return true;
        }
        return false;
    }

    @Override
    protected void renderContent(GuiGraphics guiGraphics, Minecraft mc,
                                 BlockEntity entity, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        int x = centerX - 8;
        int y = centerY + 10;

        // Fuel count only — the bound effect is surfaced by the item tooltip and the lamp tint.
        ItemStack fuel = lantern.getFuelStack();
        guiGraphics.renderItem(fuel.isEmpty() ? new ItemStack(ModRegistries.DEWPETAL.get()) : fuel, x, y);
        String amountText = lantern.getFuelCount() + "/" + CrystalLanternBlockEntity.MAX_FUEL;
        guiGraphics.drawString(mc.font, amountText, x + 18, y + 4, 0xFFFFFF, true);
    }
}
