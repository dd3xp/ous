package com.cahcap.common.inventory;

import com.cahcap.common.block.ArcaneAlloyAnvilBlock;
import com.cahcap.common.registry.ModRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Anvil menu for the Arcane Alloy Anvil.
 * <p>
 * Rather than reimplementing vanilla's ~150 line cost formula (which would then have to be kept in
 * sync with Minecraft), this runs vanilla's own calculation and adjusts the result:
 * <ul>
 *     <li>20% off, rounded down.</li>
 *     <li>The prior work penalty stops compounding, so repeated use costs the same.</li>
 *     <li>No "Too Expensive!" cut-off, so enchantments can be stacked without limit.</li>
 * </ul>
 * The last one is achieved by flipping the player's creative flag for the duration of the vanilla
 * call: both of vanilla's level-40 cut-offs are already waived for creative players.
 */
public class ArcaneAlloyAnvilMenu extends AnvilMenu {

    /** Vanilla's cut-off. Costs at or above this are what we deliberately allow through. */
    private static final int VANILLA_COST_LIMIT = 40;

    public ArcaneAlloyAnvilMenu(int containerId, Inventory playerInventory) {
        super(containerId, playerInventory);
    }

    public ArcaneAlloyAnvilMenu(int containerId, Inventory playerInventory,
                               ContainerLevelAccess access) {
        super(containerId, playerInventory, access);
    }

    @Override
    public void createResult() {
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);

        withCreativeFlag(this.player, super::createResult);

        ItemStack result = this.resultSlots.getItem(0);
        if (result.isEmpty()) {
            return;
        }

        // Undo the prior work penalty: vanilla stores calculateIncreasedRepairCost(previous),
        // which doubles every time and is what makes repeated use spiral.
        int carried = Math.max(left.getOrDefault(DataComponents.REPAIR_COST, 0),
                right.getOrDefault(DataComponents.REPAIR_COST, 0));
        result.set(DataComponents.REPAIR_COST, carried);

        // Integer division floors, which is the rounding we want.
        this.cost.set(Math.max(1, this.cost.get() * 4 / 5));
        this.broadcastChanges();
    }

    /**
     * Vanilla would call {@code AnvilBlock.damage}, which does not know our blocks and returns
     * {@code null}, making it delete the anvil outright. Suppress that branch and run the same
     * 12% roll over our own damage chain.
     */
    @Override
    protected void onTake(Player player, ItemStack stack) {
        boolean shouldDamage = !player.hasInfiniteMaterials()
                && player.getRandom().nextFloat() < 0.12F;

        withCreativeFlag(player, () -> super.onTake(player, stack));

        // The creative flag also waived the XP charge, so settle it here instead.
        if (!player.getAbilities().instabuild) {
            player.giveExperienceLevels(-this.cost.get());
        }
        this.cost.set(0);

        this.access.execute((level, pos) -> {
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModRegistries.ARCANE_ALLOY_ANVIL.get())
                    && !state.is(ModRegistries.CHIPPED_ARCANE_ALLOY_ANVIL.get())
                    && !state.is(ModRegistries.DAMAGED_ARCANE_ALLOY_ANVIL.get())) {
                return;
            }
            if (!shouldDamage) {
                level.levelEvent(1030, pos, 0);
                return;
            }
            BlockState damaged = ArcaneAlloyAnvilBlock.damage(state);
            if (damaged == null) {
                level.removeBlock(pos, false);
                level.levelEvent(1029, pos, 0);
            } else {
                level.setBlock(pos, damaged, 2);
                level.levelEvent(1030, pos, 0);
            }
        });
    }

    /** Accepts our three anvils; vanilla checks {@code BlockTags.ANVIL}, which they also carry. */
    @Override
    protected boolean isValidBlock(BlockState state) {
        return super.isValidBlock(state)
                || state.is(ModRegistries.ARCANE_ALLOY_ANVIL.get())
                || state.is(ModRegistries.CHIPPED_ARCANE_ALLOY_ANVIL.get())
                || state.is(ModRegistries.DAMAGED_ARCANE_ALLOY_ANVIL.get());
    }

    /** Runs {@code body} with the player marked creative, restoring the flag afterwards. */
    private static void withCreativeFlag(Player player, Runnable body) {
        Abilities abilities = player.getAbilities();
        boolean previous = abilities.instabuild;
        abilities.instabuild = true;
        try {
            body.run();
        } finally {
            abilities.instabuild = previous;
        }
    }
}
