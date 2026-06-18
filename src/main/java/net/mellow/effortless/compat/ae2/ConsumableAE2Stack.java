package net.mellow.effortless.compat.ae2;

import net.mellow.effortless.blocks.IConsumableStack;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.compat.CompatAE2;
import net.minecraft.entity.player.EntityPlayer;
import appeng.api.config.Actionable;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.security.PlayerSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.util.item.AEItemStack;

public class ConsumableAE2Stack implements IConsumableStack {

    // my kingdom for a `yield` that works like C#
    // because otherwise I have to do either THIS stupid shit
    // or another - more complicated - yet still stupid shit
    public static boolean hasChecked = false;

    private final IAEItemStack stack;
    private long amountConsumed = 0;

    // Store the request and the inventory to MODULATE this request upon flushing
    private final IMEMonitor<IAEItemStack> inventory;
    private final IAEItemStack request;
    private final BaseActionSource source;

    public ConsumableAE2Stack(IAEItemStack stack, IMEMonitor<IAEItemStack> inventory, IAEItemStack request, BaseActionSource source) {
        this.stack = stack;
        this.inventory = inventory;
        this.request = request;
        this.source = source;
    }

    @Override
    public boolean consumeOne() {
        amountConsumed++;
        return amountConsumed >= stack.getStackSize();
    }

    @Override
    public void flush() {
        request.setStackSize(amountConsumed);
        inventory.extractItems(request, Actionable.MODULATE, source);
    }

    public static IConsumableStack getStack(EntityPlayer player, PlaceableStack selected, int maximumToPlace) {
        WirelessTerminalGuiObject terminal = CompatAE2.getTerminalGuiObject(player);
        if (terminal == null || !terminal.rangeCheck()) return null;
        if (!CompatAE2.hasRequiredPermission(player, terminal.getGrid(), SecurityPermissions.EXTRACT)) return null;
        
        IMEMonitor<IAEItemStack> inventory = terminal.getItemInventory();
        if (inventory == null) return null;
        
        IAEItemStack request = AEItemStack.create(selected.stack).setStackSize(maximumToPlace);
        BaseActionSource source = new PlayerSource(player, terminal);
        IAEItemStack stack = inventory.extractItems(request, Actionable.SIMULATE, source);
        if (stack == null) return null;

        return new ConsumableAE2Stack(stack, inventory, request, source);
    }
    
}
