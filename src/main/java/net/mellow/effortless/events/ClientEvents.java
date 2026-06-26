package net.mellow.effortless.events;

import org.lwjgl.input.Mouse;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.buildmode.BaseBuildMode.Operation;
import net.mellow.effortless.compat.CompatBaublesExpanded;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.mellow.effortless.network.MouseClickPacket;
import net.mellow.effortless.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;

public class ClientEvents {
    
    public static void init() {
        ClientEvents handler = new ClientEvents();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    // Instead of rendering the tool name, render the current tool info,
    // while still preserving the regular tool name in the inventory
    @SubscribeEvent
    public void onOverlayRenderPre(RenderGameOverlayEvent.Pre event) {
        if (event.type != ElementType.ALL) return;
        ItemBuildingGadget.isRenderingOverlay = true;

        if (ConstructionSet.highlightTitle == null) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        ItemStack gadget = CompatBaublesExpanded.getGadgetFromBaubles(player);
        if (gadget == null) return;

        if (!PlaceableStack.isPlaceable(player.getHeldItem())) return;

        mc.ingameGUI.highlightingItemStack = gadget;
    }

    @SubscribeEvent
    public void onOverlayRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.ALL) return;
        ItemBuildingGadget.isRenderingOverlay = false;
    }

    // OF COURSE THERE IS NO EQUIVALENT FOR CANCELLING KEYBOARD EVENTS THAT WOULD BE TOO USEFUL AND CONSISTENT
    // FUCKING HELL FORGE STOP FINDING ME IN THE ALPS
    // okay fine, when I implement the regular behaviour, I'll just find a workaround that works for both keyboard and mouse
    // will probably need to use AT and drink the fluids from my spine
    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;

        if (player == null) return;

        int button = Mouse.getEventButton() - 100; // MC mouse button "keyCode"
        if (!Mouse.getEventButtonState()) return; // only care about mouse down

        if (button == mc.gameSettings.keyBindAttack.getKeyCode()) {
            if (useGadget(player, Operation.BREAK)) event.setCanceled(true);
        }
        if (button == mc.gameSettings.keyBindUseItem.getKeyCode()) {
            if (useGadget(player, Operation.PLACE)) event.setCanceled(true);
        }
    }

    // Return true to cancel mouse event entirely!
    public boolean useGadget(EntityPlayer player, Operation operation) {
        ItemStack held = player.getHeldItem();

        if (held == null || !(held.getItem() instanceof ItemBuildingGadget gadget)) return false;
        
        if (gadget.onItemClick(held, player.worldObj, player, operation)) {
            NetworkHandler.instance.sendToServer(new MouseClickPacket(operation, 0, 0, 0, 0, 0, 0, 0));
            return true;
        }

        return false;
    }

}
