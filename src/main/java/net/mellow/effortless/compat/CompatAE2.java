package net.mellow.effortless.compat;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.features.IWirelessTermRegistry;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.helpers.WirelessTerminalGuiObject;
import baubles.api.BaublesApi;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class CompatAE2 {

    public static boolean loaded = Loader.isModLoaded(Compat.MODID_AE2);
    
    @Optional.Method(modid = Compat.MODID_AE2)
    public static WirelessTerminalGuiObject getTerminalGuiObject(EntityPlayer player) {
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            WirelessTerminalGuiObject terminal = getTerminalGuiObject(player.inventory.getStackInSlot(i), player, i, 0);
            if (terminal == null) continue;

            return terminal;
        }

        if (Loader.isModLoaded(Compat.MODID_BAUBLES)) {
            return readBaubles(player);
        }

        return null;
    }

    @Optional.Method(modid = Compat.MODID_AE2)
    public static WirelessTerminalGuiObject getTerminalGuiObject(ItemStack item, EntityPlayer player, int inventorySlot, int mode) {
        if (item == null) return null;
        if (!(item.getItem() instanceof IWirelessTermHandler terminal)) return null;
        if (!terminal.canHandle(item)) return null;

        IWirelessTermRegistry registry = AEApi.instance().registries().wireless();
        if (!registry.isWirelessTerminal(item)) return null;

        IWirelessTermHandler handler = registry.getWirelessTerminalHandler(item);
        if (!handler.hasPower(player, 1000F, item)) return null;

        // x = inventorySlot
        // y = mode
        // z = ... fuck knows
        return new WirelessTerminalGuiObject(terminal, item, player, player.worldObj, inventorySlot, mode, Integer.MIN_VALUE);
    }

    @Optional.Method(modid = Compat.MODID_BAUBLES)
    public static WirelessTerminalGuiObject readBaubles(EntityPlayer player) {
        for (int i = 0; i < BaublesApi.getBaubles(player).getSizeInventory(); i++) {
            WirelessTerminalGuiObject terminal = getTerminalGuiObject(BaublesApi.getBaubles(player).getStackInSlot(i), player, i, 0);
            if (terminal == null) continue;

            return terminal;
        }
        
        return null;
    }    

    @Optional.Method(modid = Compat.MODID_AE2)
    public static boolean hasRequiredPermission(EntityPlayer player, WirelessTerminalGuiObject terminal, SecurityPermissions requiredPermission) {
        IGridNode gridNode = terminal.getActionableNode();
        if (gridNode == null) return false;
        ISecurityGrid grid = gridNode.getGrid().getCache(ISecurityGrid.class);
        return grid.hasPermission(player, requiredPermission);
    }

}
