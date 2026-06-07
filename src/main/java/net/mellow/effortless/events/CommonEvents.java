package net.mellow.effortless.events;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.buildmode.History;
import net.mellow.effortless.compat.CompatBaublesExpanded;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.item.ItemStack;
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
        if (event.world.isRemote) return;

        ItemStack gadget = CompatBaublesExpanded.getGadgetFromBaubles(event.entityPlayer);
        if (gadget == null) return;
        
        ItemStack held = event.entityPlayer.getHeldItem();
        BlockMeta selected = BlockMeta.fromStack(held);
        if (selected == null || selected.block.hasTileEntity(selected.meta)) return;

        if (ItemBuildingGadget.getMode(gadget).handler == null) return;

        ItemBuildingGadget gadgetItem = (ItemBuildingGadget) gadget.getItem();

        if (event.action == Action.LEFT_CLICK_BLOCK) {
            gadgetItem.onEntitySwing(event.entityLiving, gadget);
        } else {
            gadgetItem.onItemRightClick(gadget, event.world, event.entityPlayer, BlockMeta.fromStack(held));

            event.useBlock = Result.DENY;
            event.useItem = Result.DENY;
        }

        CompatBaublesExpanded.syncBaubles(event.entityPlayer);
    }

}
