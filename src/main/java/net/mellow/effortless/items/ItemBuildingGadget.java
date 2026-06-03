package net.mellow.effortless.items;

import java.util.List;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.modes.*;
import net.mellow.effortless.gui.GuiBuildingGadget;
import net.mellow.effortless.network.IItemControlReceiver;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class ItemBuildingGadget extends Item implements IItemRenderPreview, IItemGuiProvider, IItemControlReceiver {

    public static enum BuildingMode {
        EXTENDED(new Extended(), 16, 16), // greater reach
        AIR(new Air(), 16, 16), // air placement
        LINE(new Line(), 32, 16), // lines
        WALL(new Wall(), 48, 16), // walls
        FLOOR(new Floor(), 64, 16); // floors

        public BaseBuildMode handler;
        public int iconX;
        public int iconY;

        private BuildingMode(BaseBuildMode handler, int iconX, int iconY) {
            this.handler = handler;
            this.iconX = iconX;
            this.iconY = iconY;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean bool) {
        BlockMeta selected = getSelected(stack);
        BuildingMode mode = getMode(stack);

        list.add("mode:  " + mode);

        list.add("block: " + selected.block.getUnlocalizedName());
        list.add("meta:  " + selected.meta);
    }

    // IF WE PUT ROCKS IN THE SHAPE OF A RUNWAY GOD WILL GIVE US HIGH-FRUCTOSE CORN SYRUP
    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 1;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        BuildingMode mode = getMode(stack);

        MovingObjectPosition mop = BuildModes.getMop(player, mode.handler.reach(stack));

        mode.handler.add(stack, getSelected(stack), world, player, mop);

        return stack;
    }

    @Override
    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        getMode(stack).handler.clear(stack);
        return false;
    }


    public static BlockMeta getSelected(ItemStack stack) {
        if (stack.stackTagCompound == null) return new BlockMeta(Blocks.stone, 0);
        return new BlockMeta(stack.stackTagCompound.getInteger("block"), stack.stackTagCompound.getByte("meta"));
    }

    public static void setSelected(ItemStack stack, BlockMeta select) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setInteger("block", Block.getIdFromBlock(select.block));
        stack.stackTagCompound.setByte("meta", (byte)select.meta);
    }


    // mode is stored as a string so inserting new modes won't fuck up existing tools
    public static BuildingMode getMode(ItemStack stack) {
        if (stack.stackTagCompound == null || !stack.stackTagCompound.hasKey("mode")) return BuildingMode.EXTENDED;
        try {
            return BuildingMode.valueOf(stack.stackTagCompound.getString("mode"));
        } catch (IllegalArgumentException ex) {
            return BuildingMode.LINE;
        }
    }

    public static void setMode(ItemStack stack, BuildingMode mode) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setString("mode", mode.name());
    }


    @Override
    public void render(World world, EntityPlayer player, ItemStack stack, float partialTicks) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        getMode(stack).handler.render(stack, world, player, partialTicks);
    }

    @Override
    public void provideGui(ItemStack stack, EntityPlayer player) {
        FMLCommonHandler.instance().showGuiScreen(new GuiBuildingGadget(stack));
    }

    @Override
    public void receiveControl(ItemStack stack, NBTTagCompound nbt) {
        if (nbt.hasKey("mode")) stack.stackTagCompound.setString("mode", nbt.getString("mode"));
        if (nbt.hasKey("block")) stack.stackTagCompound.setInteger("block", nbt.getInteger("block"));
        if (nbt.hasKey("meta")) stack.stackTagCompound.setByte("meta", nbt.getByte("meta"));
    }

}
