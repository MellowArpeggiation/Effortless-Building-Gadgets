package net.mellow.effortless.events;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent.KeyInputEvent;
import cpw.mods.fml.common.gameevent.InputEvent.MouseInputEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import net.mellow.effortless.api.BlockRegistry;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.buildmode.BaseBuildMode.Operation;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingMode;
import net.mellow.effortless.compat.CompatBaublesExpanded;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.mellow.effortless.network.MouseClickPacket;
import net.mellow.effortless.network.NetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
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

        ItemStack held = player.getHeldItem();
        if (held != null && !PlaceableStack.isPlaceable(held)) return;

        mc.ingameGUI.highlightingItemStack = gadget;
    }

    @SubscribeEvent
    public void onOverlayRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.type != ElementType.ALL) return;
        ItemBuildingGadget.isRenderingOverlay = false;
    }

    @SubscribeEvent
    public void onMouseInput(MouseInputEvent event) {
        handleKeybind(Mouse.getEventButton() - 100, Mouse.getEventButtonState());
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        handleKeybind(Keyboard.getEventKey(), Keyboard.getEventKeyState());
    }

    // i made an okay-ish workaround
    // sorry forge ily you can drink my final spluids
    private void handleKeybind(int keyCode, boolean pressed) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;

        if (player == null) return;

        if (keyCode == mc.gameSettings.keyBindAttack.getKeyCode()) {
            if (pressed) {
                if (useGadget(mc, player, Operation.BREAK)) {
                    mc.gameSettings.keyBindAttack.pressTime = 0;
                    mc.gameSettings.keyBindAttack.pressed = false;
                }
            } else {
                keyBindAttackTicks = 0;
            }
        }

        if (keyCode == mc.gameSettings.keyBindUseItem.getKeyCode()) {
            if (pressed) {
                if (useGadget(mc, player, Operation.PLACE)) {
                    mc.gameSettings.keyBindUseItem.pressTime = 0;
                    mc.gameSettings.keyBindUseItem.pressed = false;
                }
            } else {
                keyBindUseItemTicks = 0;
            }
        }
    }

    // static mutable state is fine if we're purely client sided :)
    private static int keyBindAttackTicks = 0;
    private static int keyBindUseItemTicks = 0;

    // For repeating clicks
    // sometimes I worry about the return early pattern I (over-)use, is it good code or bad
    // I think my nose is too close to this shit
    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) return;

        if (handleClickRepeat()) {
            keyBindAttackTicks = 0;
            keyBindUseItemTicks = 0;
        }
    }

    // Returns true to cancel click repeating
    private boolean handleClickRepeat() {
        if (keyBindAttackTicks == 0 && keyBindUseItemTicks == 0) return false;
        if (keyBindAttackTicks != 0 && keyBindUseItemTicks != 0) return true;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        MovingObjectPosition mop = mc.objectMouseOver;
        if (player == null || mop == null || mc.currentScreen != null) return true;

        ItemStack gadgetStack = ItemBuildingGadget.getGadgetStack(player);
        if (gadgetStack == null) return true;

        ItemStack heldStack = player.getHeldItem();
        if (heldStack != gadgetStack && !PlaceableStack.isPlaceable(heldStack)) return true;

        BuildingMode mode = ItemBuildingGadget.getMode(gadgetStack);
        if (mode.handler == null) return true;

        BuildingAction speed = mode.handler.repeatSpeed(gadgetStack);
        if (speed == null) return true;

        ItemBuildingGadget gadget = (ItemBuildingGadget) gadgetStack.getItem();
        Operation operation = keyBindAttackTicks > 0 ? Operation.BREAK : Operation.PLACE;
        int ticks = keyBindAttackTicks > 0 ? keyBindAttackTicks : keyBindUseItemTicks;
        boolean shouldRepeat = speed == BuildingAction.SPEED_FAST ? ticks % 2 == 0 : ticks % 4 == 0;
        
        if (shouldRepeat && gadget.onItemClick(gadgetStack, heldStack, player.worldObj, player, operation)) {
            float subX = (float)mop.hitVec.xCoord - (float)mop.blockX;
            float subY = (float)mop.hitVec.yCoord - (float)mop.blockY;
            float subZ = (float)mop.hitVec.zCoord - (float)mop.blockZ;
            NetworkHandler.instance.sendToServer(new MouseClickPacket(operation, mop.blockX, mop.blockY, mop.blockZ, mop.sideHit, subX, subY, subZ));
            player.swingItem();
        }

        if (keyBindAttackTicks > 0) {
            keyBindAttackTicks++;
        } else if (keyBindUseItemTicks > 0) {
            keyBindUseItemTicks++;
        }

        return false;
    }

    // Return true to cancel mouse event entirely!
    private boolean useGadget(Minecraft mc, EntityPlayer player, Operation operation) {
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit == MovingObjectType.ENTITY) return false;

        ItemStack gadgetStack = ItemBuildingGadget.getGadgetStack(player);
        if (gadgetStack == null) return false;

        BuildingMode mode = ItemBuildingGadget.getMode(gadgetStack);
        if (mode.handler == null) return false;

        ItemStack heldStack = player.getHeldItem();
        if (heldStack != gadgetStack && !PlaceableStack.isPlaceable(heldStack) && heldStack != null) return false;

        ItemBuildingGadget gadget = (ItemBuildingGadget) gadgetStack.getItem();

        int x = mop.blockX;
        int y = mop.blockY;
        int z = mop.blockZ;
        int side = mop.sideHit;
        float subX = (float)mop.hitVec.xCoord - (float)mop.blockX;
        float subY = (float)mop.hitVec.yCoord - (float)mop.blockY;
        float subZ = (float)mop.hitVec.zCoord - (float)mop.blockZ;

        mc.playerController.syncCurrentPlayItem();

        // First, check regular client-side interactions, unless we're in the middle of a place
        if (!mode.handler.isPlacing(gadgetStack)) {
            if (operation == Operation.PLACE) {
                if (!player.worldObj.isAirBlock(x, y, z)) {
                    // Check for client side interaction cancels!
                    if (ForgeEventFactory.onPlayerInteract(player, Action.RIGHT_CLICK_BLOCK, x, y, z, side, player.worldObj).isCanceled()) {
                        return true;
                    }
    
                    if (heldStack != null && heldStack.getItem() != null && heldStack.getItem().onItemUseFirst(heldStack, player, player.worldObj, x, y, z, side, subX, subY, subZ)) {
                        return true;
                    }
    
                    if (!player.isSneaking() || player.getHeldItem() == null || player.getHeldItem().getItem().doesSneakBypassUse(player.worldObj, x, y, z, player)) {
                        // If the client receives a block activation, perform only vanilla behaviour
                        if (player.worldObj.getBlock(mop.blockX, mop.blockY, mop.blockZ).onBlockActivated(player.worldObj, x, y, z, player, side, subX, subY, subZ)) {
                            mc.playerController.netClientHandler.addToSendQueue(new C08PacketPlayerBlockPlacement(x, y, z, side, player.inventory.getCurrentItem(), subX, subY, subZ));
                            return true;
                        }
                    }
                } else {
                    if (ForgeEventFactory.onPlayerInteract(player, Action.RIGHT_CLICK_AIR, 0, 0, 0, -1, player.worldObj).isCanceled()) {
                        return true;
                    }
                }
            } else {
                Block block = player.worldObj.getBlock(x, y, z);
                if (BlockRegistry.isLeftClickBlacklisted(block)) {
                    return false;
                }

                // client side doesn't fire `LEFT_CLICK_BLOCK` (have you figured out yet that forge events are a bit uh...)
            }
        }
        
        if (gadget.onItemClick(gadgetStack, heldStack, player.worldObj, player, operation)) {
            NetworkHandler.instance.sendToServer(new MouseClickPacket(operation, x, y, z, side, subX, subY, subZ));
            player.swingItem();
            if (operation == Operation.PLACE) {
                keyBindUseItemTicks = 1;
            } else {
                keyBindAttackTicks = 1;
            }
            return true;
        }

        return false;
    }

}
