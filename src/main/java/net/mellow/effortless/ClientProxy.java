package net.mellow.effortless;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.mellow.effortless.items.IItemRenderPreview;

public class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.

    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);

        IItemRenderPreview.init();

        Keybinds.register();
    }

}
