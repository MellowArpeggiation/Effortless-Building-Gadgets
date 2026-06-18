package net.mellow.effortless.compat;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.features.ILocatable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.features.IWirelessTermRegistry;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.misc.TileSecurity;
import appeng.util.Platform;
import baubles.api.BaublesApi;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import net.mellow.effortless.blocks.PlaceableStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.Collection;

public class CompatAE2 {
    
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

    public static void removeFromNetwork(EntityPlayer player, PlaceableStack blockNeeded) {
        int itemsToUse = 1;
        if (!canOperate(player, blockNeeded, itemsToUse)) return;

        IMEMonitor<IAEItemStack> inv = getInventory(player);
        if (inv == null) return;
        
        BaseActionSource src = new PlayerSource(player, null);
        inv.extractItems(toAEItemStack(blockNeeded.stack).setStackSize(itemsToUse), Actionable.MODULATE, src);
    }

    public static boolean canOperate(EntityPlayer player, PlaceableStack blockNeeded, long itemsToUse){
        BaseActionSource src = new PlayerSource(player, null);
        IMEMonitor<IAEItemStack> inv = getInventory(player);
        if (inv == null) return false;

        IAEItemStack itemsNeeded = inv.extractItems(toAEItemStack(blockNeeded.stack).setStackSize(itemsToUse), Actionable.SIMULATE, src);
        IItemList<IAEItemStack> itemList = getItemList(player);
        if (itemList == null || itemList.isEmpty()) return false;

        ItemStack itemStack = findItemInNetwork(blockNeeded, player);
        if (itemStack == null) return false;

        return itemsNeeded.getStackSize() <= itemStack.stackSize;
    }

    private static IStorageGrid getStorageFromTerminal(EntityPlayer player) {
        WirelessTerminalGuiObject wt = getTerminalGuiObject(player);
        if (wt == null) return null;

        final IGridNode node = wt.getGridNode(ForgeDirection.UNKNOWN);
        if (node == null) return null;

        IGrid targetGrid;
        targetGrid = node.getGrid();
        if (targetGrid == null) return null;

        IStorageGrid sg;
        sg = targetGrid.getCache(IStorageGrid.class);
        return sg;
    }
    
    @Optional.Method(modid = Compat.MODID_AE2)
    public static WirelessTerminalGuiObject getTerminalGuiObject(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack item = player.inventory.getStackInSlot(i);

            if (item == null) continue;
            if (!(item.getItem() instanceof IWirelessTermHandler t)) continue;
            if (!t.canHandle(item)) continue;

            return getTerminalGuiObject(item, player, i, 0);
        }

        if (Loader.isModLoaded(Compat.MODID_BAUBLES)) {
            return readBaubles(player);
        }

        return null;
    }

    @Optional.Method(modid = Compat.MODID_AE2)
    public static WirelessTerminalGuiObject getTerminalGuiObject(ItemStack item, EntityPlayer player, int x, int y) {
        if (Platform.isClient() || item == null) return null;
        if (!(item.getItem() instanceof IWirelessTermHandler wt)) return null;
        if (!wt.canHandle(item)) return null;

        IWirelessTermRegistry registry = AEApi.instance().registries().wireless();
        if (!registry.isWirelessTerminal(item)) return null;

        IWirelessTermHandler handler = registry.getWirelessTerminalHandler(item);
        String unparsedKey = handler.getEncryptionKey(item);
        if (unparsedKey.isEmpty()) return null;

        long parsedKey = Long.parseLong(unparsedKey);
        ILocatable securityStation = AEApi.instance().registries().locatable().getLocatableBy(parsedKey);
        if (!(securityStation instanceof TileSecurity)) return null;
        if (!handler.hasPower(player, 1000F, item)) return null;

        return new WirelessTerminalGuiObject(wt, item, player, player.worldObj, x, y, Integer.MIN_VALUE);
    }

    @Optional.Method(modid = Compat.MODID_BAUBLES)
    public static WirelessTerminalGuiObject readBaubles(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaubles(player).getSizeInventory(); i++) {
            ItemStack item = BaublesApi.getBaubles(player).getStackInSlot(i);
            if (item == null) continue;

            if (item.getItem() instanceof IWirelessTermHandler t) {
                if (t.canHandle(item)) {
                    return getTerminalGuiObject(item, player, i, 1);
                }
            }
        }
        
        return null;
    }

    private static IAEItemStack toAEItemStack(ItemStack stack) {
        return AEApi.instance().storage().createItemStack(stack);
    }

    private static ItemStack toItemStack(IAEItemStack itemStack) {
        return itemStack.getItemStack();
    }

    private static IMEMonitor<IAEItemStack> getInventory(EntityPlayer player){
        IStorageGrid sg = getStorageFromTerminal(player);
        if (sg == null) return null;
        return sg.getItemInventory();
    }
    
    public static IItemList<IAEItemStack> getItemList(EntityPlayer player){
        IMEMonitor<IAEItemStack> inventory = getInventory(player);
        if (inventory != null) return inventory.getStorageList();
        return null;
    }

}
