package com.cahcap.common.item;

import com.cahcap.common.util.PotionHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Item form of the Crystal Lantern. Carries the potion effect bound at craft time so the
 * binding survives breaking and replacing the block.
 * <p>
 * Two lanterns bound to different effects are otherwise indistinguishable, so the effect is
 * surfaced both here (tooltip) and on the block itself (lamp body tint, look-at tooltip).
 */
public class CrystalLanternItem extends BlockItem {

    private static final String TAG_BOUND_EFFECTS = "BoundEffects";
    private static final String TAG_POTION_COLOR = "PotionColor";
    private static final int DEFAULT_COLOR = 0xC671FD;

    public CrystalLanternItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static List<String> getBoundEffects(ItemStack stack) {
        List<String> out = new ArrayList<>();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return out;
        }
        ListTag list = customData.copyTag().getList(TAG_BOUND_EFFECTS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    public static int getPotionColor(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return DEFAULT_COLOR;
        }
        CompoundTag tag = customData.copyTag();
        return tag.contains(TAG_POTION_COLOR) ? tag.getInt(TAG_POTION_COLOR) : DEFAULT_COLOR;
    }

    public static void setBinding(ItemStack stack, List<String> effects, int color) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData == null ? new CompoundTag() : customData.copyTag();
        ListTag list = new ListTag();
        for (String id : effects) {
            list.add(StringTag.valueOf(id));
        }
        tag.put(TAG_BOUND_EFFECTS, list);
        tag.putInt(TAG_POTION_COLOR, color);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        List<String> effects = getBoundEffects(stack);
        if (effects.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.ous.crystal_lantern.unbound")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        for (String id : effects) {
            Holder<MobEffect> effect = PotionHelper.getEffectForType(id);
            if (effect == null) {
                continue;
            }
            tooltip.add(Component.translatable("tooltip.ous.crystal_lantern.bound",
                            Component.translatable(effect.value().getDescriptionId()))
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
