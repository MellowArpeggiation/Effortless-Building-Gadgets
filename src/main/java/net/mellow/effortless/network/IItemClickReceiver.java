package net.mellow.effortless.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface IItemClickReceiver {
    
    public void receiveClick(EntityPlayer player, ItemStack stack, MouseClickPacket packet);

}
