package com.cahcap.neoforge.common.handler;

import com.cahcap.common.blockentity.CrystalLanternBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Exposes the Crystal Lantern's single fuel slot to hoppers and pipes.
 * <p>
 * Extraction is deliberately not supported: the lantern consumes what it is given, and letting
 * automation pull the flowers back out would let a hopper loop starve it.
 */
public class CrystalLanternItemHandler implements IItemHandler {

    private final CrystalLanternBlockEntity lantern;

    public CrystalLanternItemHandler(CrystalLanternBlockEntity lantern) {
        this.lantern = lantern;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return lantern.getFuelStack();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != 0 || stack.isEmpty() || !isItemValid(slot, stack)) {
            return stack;
        }
        int space = CrystalLanternBlockEntity.MAX_FUEL - lantern.getFuelCount();
        if (space <= 0) {
            return stack;
        }
        int accepted = Math.min(space, stack.getCount());
        if (!simulate) {
            ItemStack toInsert = stack.copyWithCount(accepted);
            lantern.insertFuel(toInsert);
        }
        return accepted >= stack.getCount() ? ItemStack.EMPTY
                : stack.copyWithCount(stack.getCount() - accepted);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return CrystalLanternBlockEntity.MAX_FUEL;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return lantern.canAccept(stack);
    }
}
