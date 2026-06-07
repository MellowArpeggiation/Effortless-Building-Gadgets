package net.mellow.effortless;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.compat.CompatBaublesExpanded;
import net.mellow.effortless.items.IItemGuiProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class Keybinds {

    public static final String category = "effortless.key";

    public static KeyBinding uiKey = new KeyBinding(category + ".uiKey", Keyboard.KEY_LMENU, category);

    public static void register() {
        ClientRegistry.registerKeyBinding(uiKey);

        FMLCommonHandler.instance().bus().register(new Keybinds());
    }

    // some mods are a bit funky with regular keybind handling when you require the user to hold a key...
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void postClientTick(ClientTickEvent event) {
        if (event.phase != Phase.END) return;

        if (uiKey.getIsKeyPressed()) {
            EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;

            ItemStack held = player.getHeldItem();
            if (held != null && held.getItem() instanceof IItemGuiProvider) {
                ((IItemGuiProvider) held.getItem()).provideGui(held, player, held);
            } else if (held == null || held.getItem() instanceof ItemBlock) {
                BlockMeta selected = BlockMeta.fromStack(held);
                ItemStack gadget = CompatBaublesExpanded.getGadgetFromBaubles(player);
                if (gadget != null && (selected == null || !selected.block.hasTileEntity(selected.meta))) {
                    ((IItemGuiProvider) gadget.getItem()).provideGui(gadget, player, held);
                }
            }
        }
    }

}
