package net.mellow.effortless.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.mellow.effortless.buildmode.BaseBuildMode.Operation;
import net.mellow.effortless.compat.CompatBaublesExpanded;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class MouseClickPacket implements IMessage {

    // Pretty much just a reimplementation of `C08PacketPlayerBlockPlacement` because
    //  a) forge events don't fucking give us `sub` hits
    //  b) we need to fully cancel everything in order to avoid the EVIL static mutable state hacks!
    //  c) baubles can't cancel events on client without cancelling ALL serverside interactions
    //  d) regular events have funky interaction max distance handling, and we're going to let the player click things from MUCH further away
    //
    // Ultimately, this solution is just... much cleaner

    public Operation operation;

    public int blockX;
    public int blockY;
    public int blockZ;
    public int face;
    public float subX;
    public float subY;
    public float subZ;

    public MouseClickPacket() {}

    public MouseClickPacket(Operation operation, int blockX, int blockY, int blockZ, int face, float subX, float subY, float subZ) {
        this.operation = operation;
        this.blockX = blockX;
        this.blockY = blockY;
        this.blockZ = blockZ;
        this.face = face;
        this.subX = subX;
        this.subY = subY;
        this.subZ = subZ;
    }

    // We do the same truncation that vanilla does, this is pretty much identical to the vanilla packet
    @Override
    public void fromBytes(ByteBuf buf) {
        this.operation = buf.readBoolean() ? Operation.PLACE : Operation.BREAK;
        this.blockX = buf.readInt();
        this.blockY = buf.readUnsignedByte();
        this.blockZ = buf.readInt();
        this.face = buf.readUnsignedByte();
        this.subX = (float)buf.readUnsignedByte() / 16.0F;
        this.subY = (float)buf.readUnsignedByte() / 16.0F;
        this.subZ = (float)buf.readUnsignedByte() / 16.0F;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(operation == Operation.PLACE);
        buf.writeInt(blockX);
        buf.writeByte(blockY);
        buf.writeInt(blockZ);
        buf.writeByte(face);
        buf.writeByte((int)(subX * 16.0F));
        buf.writeByte((int)(subY * 16.0F));
        buf.writeByte((int)(subZ * 16.0F));
    }

    public static class HandlerServer implements IMessageHandler<MouseClickPacket, IMessage> {

        @Override
        public IMessage onMessage(MouseClickPacket message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;

            ItemStack held = player.getHeldItem();
            if (held == null || !(held.getItem() instanceof IItemClickReceiver)) {
                held = CompatBaublesExpanded.getGadgetFromBaubles(player);
                if (held == null) return null;
            }

            ((IItemClickReceiver) held.getItem()).receiveClick(player, held, message);

            return null;
        }

    }
    
}
