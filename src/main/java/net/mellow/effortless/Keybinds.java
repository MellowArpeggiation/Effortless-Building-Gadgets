package net.mellow.effortless;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.effortless.items.IItemGuiProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;

public class Keybinds {

    public static final String category = "effortless.key";

    public static KeyBinding uiKey = new KeyBinding(category + ".uiKey", Keyboard.KEY_LMENU, category);

    public static void register() {
        ClientRegistry.registerKeyBinding(uiKey);

        FMLCommonHandler.instance().bus().register(new Keybinds());
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyDown(KeyInputEvent event) {
        if (uiKey.getIsKeyPressed()) {
            EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
            ItemStack held = player.getHeldItem();
            if (held != null && held.getItem() instanceof IItemGuiProvider) {
                ((IItemGuiProvider) held.getItem()).provideGui(held, player);
            }
        }
    }
    
}
