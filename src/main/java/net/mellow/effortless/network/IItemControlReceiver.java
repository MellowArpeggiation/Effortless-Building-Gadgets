package net.mellow.effortless.network;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public interface IItemControlReceiver {
    
    public void receiveControl(ItemStack stack, NBTTagCompound nbt);

}
