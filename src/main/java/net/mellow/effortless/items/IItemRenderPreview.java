package net.mellow.effortless.items;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.compat.CompatBaublesExpanded;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

public interface IItemRenderPreview {

    public void render(World world, EntityPlayer player, ItemStack stack, float partialTicks);

    public static void init() {
        EventHandler handler = new EventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
    }

    public static class EventHandler {

        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public void onRenderWorldLastEvent(RenderWorldLastEvent event) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer player = mc.thePlayer;
            World world = mc.theWorld;

            ItemStack held = player.getHeldItem();

            if (held != null) {
                if (held.getItem() instanceof IItemRenderPreview) {
                    ((IItemRenderPreview) held.getItem()).render(world, player, held, event.partialTicks);
                } else if (held.getItem() instanceof ItemBlock) {
                    BlockMeta selected = BlockMeta.fromStack(held);
                    ItemStack gadget = CompatBaublesExpanded.getGadgetFromBaubles(player);
                    if (gadget != null && selected != null && !selected.block.hasTileEntity(selected.meta)) {
                        ((IItemRenderPreview) gadget.getItem()).render(world, player, gadget, event.partialTicks);
                    }
                }
            }
        }

    }

}
