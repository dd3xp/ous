package com.cahcap.common.inventory;

import com.cahcap.common.registry.ModRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the Cosmos Chest: 96 slots laid out 12 wide by 8 tall.
 * <p>
 * Vanilla's {@code ChestMenu} is fixed to 9 columns, so the grid is built here instead. The
 * coordinates match the generated GUI texture.
 */
public class CosmosChestMenu extends AbstractContainerMenu {

    public static final int COLUMNS = 12;
    public static final int ROWS = 8;
    public static final int SIZE = COLUMNS * ROWS;

    /** Panel size the GUI texture was drawn for. */
    public static final int WIDTH = 238;
    public static final int HEIGHT = 258;

    private static final int GRID_X = 12;
    private static final int GRID_Y = 18;
    private static final int SLOT = 18;
    /** Player inventory is narrower than the chest grid, so it sits centred. */
    private static final int INV_X = (WIDTH - 162) / 2 + 1;

    private final Container container;

    /** Client-side constructor: the container is a throwaway of the right size. */
    public CosmosChestMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SIZE));
    }

    public CosmosChestMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModRegistries.COSMOS_CHEST_MENU.get(), containerId);
        checkContainerSize(container, SIZE);
        this.container = container;
        container.startOpen(playerInventory.player);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                addSlot(new Slot(container, col + row * COLUMNS,
                        GRID_X + col * SLOT, GRID_Y + row * SLOT));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        INV_X + col * SLOT, HEIGHT - (4 - row) * SLOT - 10));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, INV_X + col * SLOT, HEIGHT - 24));
        }
    }

    /** The backing chest; the opener counter uses this to spot its own viewers. */
    public Container getContainer() {
        return container;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SIZE) {
            if (!moveItemStackTo(stack, SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, SIZE, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
