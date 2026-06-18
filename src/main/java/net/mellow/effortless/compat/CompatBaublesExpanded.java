package net.mellow.effortless.compat;

import baubles.api.BaublesApi;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.common.event.EventHandlerNetwork;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class CompatBaublesExpanded {
    
    public static boolean loaded;
    public static int[] gadgetSlotIds;

    public static void preInit() {
        BaubleExpandedSlots.tryAssignSlotsUpToMinimum(BaubleExpandedSlots.charmType, 1);
    }

    public static void postInit() {
        gadgetSlotIds = BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType(BaubleExpandedSlots.charmType);
        loaded = true;
    }

    public static ItemStack getGadgetFromBaubles(EntityPlayer player) {
        if (!loaded) return null;
        
        for (int slotIndex : gadgetSlotIds) {
            ItemStack gadget = BaublesApi.getBaubles(player).getStackInSlot(slotIndex);
            if (gadget != null && gadget.getItem() instanceof ItemBuildingGadget) {
                return gadget;
            }
        }
        return null;
    }

    public static void syncBaubles(EntityPlayer player) {
        if (!loaded) return;
        EventHandlerNetwork.syncBaubles(player);
    }

}
