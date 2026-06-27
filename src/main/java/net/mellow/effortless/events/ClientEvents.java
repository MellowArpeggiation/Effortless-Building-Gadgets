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
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

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
            if (useGadget(mc, player, Operation.BREAK)) event.setCanceled(true);
        }
        if (button == mc.gameSettings.keyBindUseItem.getKeyCode()) {
            if (useGadget(mc, player, Operation.PLACE)) event.setCanceled(true);
        }
    }

    // Return true to cancel mouse event entirely!
    private boolean useGadget(Minecraft mc, EntityPlayer player, Operation operation) {
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null) return false;

        ItemStack held = player.getHeldItem();

        if (held == null || !(held.getItem() instanceof ItemBuildingGadget gadget)) return false;

        int x = mop.blockX;
        int y = mop.blockY;
        int z = mop.blockZ;
        int side = mop.sideHit;
        float subX = (float)mop.hitVec.xCoord - (float)mop.blockX;
        float subY = (float)mop.hitVec.yCoord - (float)mop.blockY;
        float subZ = (float)mop.hitVec.zCoord - (float)mop.blockZ;

        // First, check regular client-side interactions
        if (operation == Operation.PLACE) {
            // Check for client side interaction cancels!
            if (ForgeEventFactory.onPlayerInteract(player, Action.RIGHT_CLICK_BLOCK, x, y, z, side, player.worldObj).isCanceled())
                return true;

            if (held.getItem() != null && held.getItem().onItemUseFirst(held, player, player.worldObj, x, y, z, side, subX, subY, subZ)) {
                return true;
            }

            if (!player.isSneaking() || player.getHeldItem() == null || player.getHeldItem().getItem().doesSneakBypassUse(player.worldObj, x, y, z, player)) {
                // If the client receives a block activation, perform only vanilla behaviour
                if (player.worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ).onBlockActivated(player.worldObj, x, y, z, player, side, subX, subY, subZ)) {
                    mc.playerController.netClientHandler.addToSendQueue(new C08PacketPlayerBlockPlacement(x, y, z, side, player.inventory.getCurrentItem(), subX, subY, subZ));
                    return true;
                }
            }
        }
        
        if (gadget.onItemClick(held, player.worldObj, player, operation)) {
            NetworkHandler.instance.sendToServer(new MouseClickPacket(operation, x, y, z, side, subX, subY, subZ));
            return true;
        }

        return false;
    }

}
