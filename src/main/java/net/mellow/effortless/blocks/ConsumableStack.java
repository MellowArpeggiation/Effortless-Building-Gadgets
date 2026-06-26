package net.mellow.effortless.blocks;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class ConsumableStack implements IConsumableStack {

    private final ItemStack stack;
    private final IInventory inventory;
    private final int index;

    public ConsumableStack(ItemStack stack, IInventory inventory, int index) {
        this.stack = stack;
        this.inventory = inventory;
        this.index = index;
    }

    @Override
    public boolean consumeOne() {
        stack.stackSize--;
        return stack.stackSize <= 0;
    }

    @Override
    public void flush() {
        if (stack.stackSize <= 0) inventory.setInventorySlotContents(index, null);
    }

    @Override
    public ItemStack getStack() {
        return stack;
    }
    
}
