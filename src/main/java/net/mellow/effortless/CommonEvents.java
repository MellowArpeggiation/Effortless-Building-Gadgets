package net.mellow.effortless;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.mellow.effortless.buildmode.History;

public class CommonEvents {
    
    public static void init() {
        CommonEvents handler = new CommonEvents();
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

}
