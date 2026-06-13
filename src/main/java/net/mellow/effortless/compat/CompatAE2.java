package net.mellow.effortless.compat;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.features.ILocatable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collection;

public class CompatAE2 {
    public static boolean hasAE2 = Loader.isModLoaded("Applied Energistics 2");
    @Optional.Method(modid = Compat.MODID_AE2)
    public static IStorageGrid getNetwork(EntityPlayer player) {
        ItemStack stack = player.getHeldItem();
        if (stack.getItem() instanceof ItemBuildingGadget gadget){
            String encryptionKey = gadget.getEncryptionKey(stack);
            ILocatable obj = null;
            try {
                if (encryptionKey != null) {
                    final long encKey = Long.parseLong(encryptionKey);
                    obj = AEApi.instance().registries().locatable().getLocatableBy(encKey);
                }
            } catch (final NumberFormatException err) {
                FMLLog.warning("Invalid encryption key: " + encryptionKey);
                return null;
            }

            if (obj instanceof IGridHost) {
                final IGridNode n = ((IGridHost) obj).getGridNode(ForgeDirection.UNKNOWN);
                if (n == null) return null;
                IGrid targetGrid;
                targetGrid = n.getGrid();
                if (targetGrid == null) return null;
                IStorageGrid sg;
                sg = targetGrid.getCache(IStorageGrid.class);
                return sg;
            }else if (obj == null) FMLLog.warning("Object is null");
            return null;
        }
        return null;
    }

    @Optional.Method(modid = Compat.MODID_AE2)
    public static ItemStack findItemInNetwork(PlaceableStack selected, EntityPlayer player) {
        IAEItemStack targetStack = toAEItemStack(selected.stack);
        if (targetStack == null) return null;
        IItemList<IAEItemStack> itemList = getItemList(player);
        if (itemList == null) return null;
        Collection<IAEItemStack> matchingStack = itemList.findFuzzy(targetStack, FuzzyMode.IGNORE_ALL);
        if (matchingStack == null || matchingStack.isEmpty()) return null;
        IAEItemStack AE2foundItem = matchingStack.iterator().next();
        ItemStack foundStack = toItemStack(AE2foundItem);
        if (!PlaceableStack.stackMatches(foundStack, selected.stack)) return null;
        return foundStack;
    }
    //let us celebrate the fact this [method] for some reason works
    @Optional.Method(modid = Compat.MODID_AE2)
    public static void removeFromNetwork(EntityPlayer player, PlaceableStack blockNeeded, long itemsToUse) {
        if (!canOperate(player, blockNeeded, itemsToUse)) return;
        IMEMonitor<IAEItemStack> inv = getInventory(player);
        if (inv == null) return;
        BaseActionSource src = new PlayerSource(player, null);
        inv.extractItems(toAEItemStack(blockNeeded.stack).setStackSize(itemsToUse), Actionable.MODULATE, src);
    }
    @Optional.Method(modid = Compat.MODID_AE2)
    private static boolean canOperate(EntityPlayer player, PlaceableStack blockNeeded, long itemsToUse){
        BaseActionSource src = new PlayerSource(player, null);
        IMEMonitor<IAEItemStack> inv = getInventory(player);
        if (inv == null) return false;
        IAEItemStack itemsNeeded = inv.extractItems(toAEItemStack(blockNeeded.stack).setStackSize(itemsToUse), Actionable.SIMULATE, src);
        IItemList<IAEItemStack> itemList = getItemList(player);
        if (itemList == null || itemList.isEmpty()) return false;
        return itemsNeeded.getStackSize() >= itemList.size();
    }
    @Optional.Method(modid = Compat.MODID_AE2)
    private static IAEItemStack toAEItemStack(ItemStack stack) {
        return AEApi.instance().storage().createItemStack(stack);
    }
    @Optional.Method(modid = Compat.MODID_AE2)
    private static ItemStack toItemStack(IAEItemStack itemStack) {
        return itemStack.getItemStack();
    }
    private static IMEMonitor<IAEItemStack> getInventory(EntityPlayer player){
        IStorageGrid sg = getNetwork(player);
        if (sg == null) return null;
        return sg.getItemInventory();
    }
    @Optional.Method(modid = Compat.MODID_AE2)
    public static IItemList<IAEItemStack> getItemList(EntityPlayer player){
        IMEMonitor<IAEItemStack> inventory = getInventory(player);
        if (inventory != null) return inventory.getStorageList();
        return null;
    }
}
