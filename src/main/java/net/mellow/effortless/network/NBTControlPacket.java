package net.mellow.effortless.network;
import java.io.IOException;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public class NBTControlPacket implements IMessage {

    PacketBuffer buffer;

    public NBTControlPacket() {}

    public NBTControlPacket(NBTTagCompound nbt) {
        this.buffer = new PacketBuffer(Unpooled.buffer());

        try {
            buffer.writeNBTTagCompoundToBuffer(nbt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buffer == null) buffer = new PacketBuffer(Unpooled.buffer());

        buffer.writeBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (buffer == null) buffer = new PacketBuffer(Unpooled.buffer());

        buf.writeBytes(buffer);
    }

    public static class HandlerServer implements IMessageHandler<NBTControlPacket, IMessage> {

        @Override
        public IMessage onMessage(NBTControlPacket message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;

            ItemStack held = player.getHeldItem();
            if (held == null || !(held.getItem() instanceof IItemControlReceiver)) return null;

            try {
                NBTTagCompound nbt = message.buffer.readNBTTagCompoundFromBuffer();

                if (nbt != null) {
                    ((IItemControlReceiver) held.getItem()).receiveControl(held, nbt);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                message.buffer.release();
            }

            return null;
        }

    }

}
