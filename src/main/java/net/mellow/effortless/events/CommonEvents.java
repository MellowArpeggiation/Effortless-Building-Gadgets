package net.mellow.effortless.events;

import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.History;
import net.mellow.effortless.buildmode.ModeOptions.BuildingMode;
import net.mellow.effortless.compat.CompatBaublesExpanded;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

public class CommonEvents {
    
    public static void init() {
        CommonEvents handler = new CommonEvents();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    // Leaving ends the session
    @SubscribeEvent
    public void onPlayerLeave(PlayerLoggedOutEvent event) {
        History.clear(event.player);
    }

    // Joining also clears, necessary for singleplayer worlds to clear sessions
    @SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent event) {
        History.clear(event.player);
    }

    // Changing dimension clears history, sessions are per dimension
    @SubscribeEvent
    public void onPlayerChangeDimension(PlayerChangedDimensionEvent event) {
        History.clear(event.player);
    }

    // Intercept ItemBlock usage for creative mode + baubles
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack gadget = CompatBaublesExpanded.getGadgetFromBaubles(event.entityPlayer);
        if (gadget == null) return;
        
        ItemStack held = event.entityPlayer.getHeldItem();
        if (!PlaceableStack.isPlaceable(held)) return;

        BuildingMode mode = ItemBuildingGadget.getMode(gadget);
        if (mode.handler == null) return;

        ItemBuildingGadget gadgetItem = (ItemBuildingGadget) gadget.getItem();

        if (event.action == Action.LEFT_CLICK_BLOCK) {
            // This occurs first, and if it clears, then don't attempt to do the funky player tick canceling below
            if (gadgetItem.onEntitySwing(event.entityLiving, gadget)) {

                event.useBlock = Result.DENY;
                event.useItem = Result.DENY;

                if (!event.world.isRemote) event.setCanceled(true);
            }
        } else if (event.action == Action.RIGHT_CLICK_AIR) {
            gadgetItem.onItemRightClick(gadget, event.world, event.entityPlayer, held.copy());

            event.useBlock = Result.DENY;
            event.useItem = Result.DENY;

            if (!event.world.isRemote) event.setCanceled(true);
        } else {
            // recreate the block interaction and then cancel the regular block interaction!
            boolean result = false;
            
            if (!mode.handler.isPlacing(gadget)) {
                Block block = event.world.getBlock(event.x, event.y, event.z);
                boolean useBlock = !event.entityPlayer.isSneaking() || event.entityPlayer.getHeldItem() == null;
                if (!useBlock) useBlock = event.entityPlayer.getHeldItem().getItem().doesSneakBypassUse(event.world, event.x, event.y, event.z, event.entityPlayer);
    
                // Recreate the block interaction, unfortunately we have to fire _yet another raytrace_ because the dumbass forge event doesn't pass the exact hit location
                if (useBlock) {
                    MovingObjectPosition mop = BuildModes.getMop(event.entityPlayer, 16);
                    float subX = (float)mop.hitVec.xCoord - (float)event.x;
                    float subY = (float)mop.hitVec.yCoord - (float)event.y;
                    float subZ = (float)mop.hitVec.zCoord - (float)event.z;
                    result = block.onBlockActivated(event.world, event.x, event.y, event.z, event.entityPlayer, event.face, subX, subY, subZ);
                }
            }

            if (!result) {
                gadgetItem.onItemRightClick(gadget, event.world, event.entityPlayer, held.copy());
            }

            event.useBlock = Result.DENY;
            event.useItem = Result.DENY;

            if (!event.world.isRemote) event.setCanceled(true);
        }

        if (!event.world.isRemote) {
            lastServerAction.put(event.entityPlayer, event.action);
        }

        CompatBaublesExpanded.syncBaubles(event.entityPlayer);
    }

    private static Map<EntityPlayer, Action> lastServerAction = new HashMap<>();

    // This is kinda fucking horrendous, yes there is static mutable state involved
    // the above event can't see air clicks, ONLY items can see those
    // so we handle the air click tool cancelling here, block breaking cancelling is done above
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        // LEFT_CLICK_BLOCK only occurs on the server so mirror that here
        if (event.player.worldObj.isRemote) return;

        if (event.phase == Phase.END) {
            lastServerAction.remove(event.player);
            return;
        }

        // Fortunately, whenever the player swings, both the client and server set swing progress to -1 for exactly one tick!
        if (event.player.swingProgressInt != -1) return;

        // Ignore any handled events
        if (lastServerAction.get(event.player) != null) return;

        ItemStack gadget = CompatBaublesExpanded.getGadgetFromBaubles(event.player);
        if (gadget == null) return;
        
        ItemStack held = event.player.getHeldItem();
        if (!PlaceableStack.isPlaceable(held)) return;

        if (ItemBuildingGadget.getMode(gadget).handler == null) return;

        gadget.getItem().onEntitySwing(event.player, gadget);

        CompatBaublesExpanded.syncBaubles(event.player);
    }

}
