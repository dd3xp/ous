package com.cahcap.common.item;

import com.cahcap.common.block.StarryCakeBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Item form of the Starry Cake. A part-eaten cake keeps its progress on the stack, which is
 * otherwise invisible, so it is spelled out in the tooltip.
 */
public class StarryCakeItem extends BlockItem {

    public StarryCakeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int remaining = StarryCakeBlock.MAX_BITES + 1 - StarryCakeBlock.readBites(stack);
        Component line = Component.translatable("tooltip.ous.starry_cake.slices",
                        remaining, StarryCakeBlock.MAX_BITES + 1)
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        // Directly under the item name. The creative inventory tab still shows a mod-name line
        // above it, which is how Jade and WTHIT behave for every mod's items, not just ours.
        tooltip.add(Math.min(1, tooltip.size()), line);
    }
}
