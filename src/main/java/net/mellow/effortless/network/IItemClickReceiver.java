package net.mellow.effortless.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public interface IItemClickReceiver {
    
    public void receiveClick(EntityPlayerMP player, ItemStack stack, MouseClickPacket packet);

}
