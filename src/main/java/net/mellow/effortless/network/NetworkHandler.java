package net.mellow.effortless.network;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.mellow.effortless.Effortless;

public class NetworkHandler {

    public static final SimpleNetworkWrapper instance = NetworkRegistry.INSTANCE.newSimpleChannel(Effortless.MODID);

    public static void init() {
        int i = 0;

        instance.registerMessage(NBTControlPacket.HandlerServer.class, NBTControlPacket.class, i++, Side.SERVER);
    }

}
