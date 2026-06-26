package net.mellow.effortless.blocks;

import java.util.List;

import cpw.mods.fml.common.Optional;
import net.mellow.effortless.compat.Compat;
import net.mellow.effortless.compat.CompatAE2;
import net.mellow.effortless.compat.ae2.ConsumableAE2Stack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public interface IConsumableStack {
    
    public boolean consumeOne();
    public void flush();
    public ItemStack getStack();

    // maximumToPlace may not be the final amount placed, just an upper bound!!
    public static IConsumableStack getMatchingStack(EntityPlayer player, PlaceableStack selected, int maximumToPlace) {
        // Regular inventory first
        for (int i = player.inventory.mainInventory.length - 1; i >= InventoryPlayer.getHotbarSize(); i--) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (PlaceableStack.stackMatches(stack, selected.stack)) {
                return new ConsumableStack(stack, player.inventory, i);
            }
        }
        
        // any other wacky inventories next
        if (CompatAE2.loaded && !ConsumableAE2Stack.hasChecked) {
            ConsumableAE2Stack.hasChecked = true;
            IConsumableStack ae2Stack = getAE2Stack(player, selected, maximumToPlace);
            if (ae2Stack != null) return ae2Stack;
        }
        
        // Hotbar last
        for (int i = InventoryPlayer.getHotbarSize() - 1; i >= 0; i--) {
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

        ConsumableAE2Stack.hasChecked = false;

        player.inventoryContainer.detectAndSendChanges();
    }

    @Optional.Method(modid = Compat.MODID_AE2)
    public static IConsumableStack getAE2Stack(EntityPlayer player, PlaceableStack selected, int maximumToPlace) {
        // we just passing this shit on
        return ConsumableAE2Stack.getStack(player, selected.stack, maximumToPlace);
    }

}
