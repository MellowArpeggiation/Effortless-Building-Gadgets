package net.mellow.effortless.events;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.compat.CompatBaublesExpanded;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;

public class ClientEvents {
    
    public static void init() {
        ClientEvents handler = new ClientEvents();
        MinecraftForge.EVENT_BUS.register(handler);
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

}
