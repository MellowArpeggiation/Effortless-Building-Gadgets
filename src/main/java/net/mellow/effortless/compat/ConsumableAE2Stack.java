package net.mellow.effortless.compat;

import net.mellow.effortless.blocks.IConsumableStack;

import appeng.api.storage.data.IAEItemStack;

public class ConsumableAE2Stack implements IConsumableStack {

    IAEItemStack stack;

    @Override
    public boolean consumeOne() {
        stack.decStackSize(1);
        return true;
    }

    @Override
    public void flush() {
        
    }
    
}
