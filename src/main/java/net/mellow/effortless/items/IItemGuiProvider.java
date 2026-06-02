package net.mellow.effortless.items;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface IItemGuiProvider {

    // for items that show a GUI when LALT is pressed
    public void provideGui(ItemStack stack, EntityPlayer player);

}
