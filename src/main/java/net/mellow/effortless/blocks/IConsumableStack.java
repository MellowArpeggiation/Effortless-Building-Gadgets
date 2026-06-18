package net.mellow.effortless.blocks;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public interface IConsumableStack {
    
    public boolean consumeOne();
    public void flush();

    public static IConsumableStack getMatchingStack(EntityPlayer player, PlaceableStack selected) {
        for (int i = player.inventory.mainInventory.length - 1; i >= 0; i--) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (PlaceableStack.stackMatches(stack, selected.stack)) {
                return new ConsumableStack(stack, player.inventory, i);
            }
        }

        return null;
    }

    // Not as shit as it was before yaaaay
    public static void cleanInventory(EntityPlayer player, List<IConsumableStack> toFlush) {
        for (IConsumableStack consumable : toFlush) {
            consumable.flush();
        }

        player.inventoryContainer.detectAndSendChanges();
    }

}
