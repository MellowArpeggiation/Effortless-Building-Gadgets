package net.mellow.effortless.items;

import java.util.List;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.modes.Line;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class ItemBuildingGadget extends Item implements IItemRenderPreview {

    public static enum BuildingMode {
        EXTENDED, // greater reach
        LINE, // lines
        WALL, // walls
        FLOOR, // floors
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean bool) {
        BlockMeta selected = getSelected(stack);
        BuildingMode mode = getMode(stack);
        BlockPos from = getFromPosition(stack);

        list.add("mode:  " + mode);

        list.add("block: " + selected.block.getUnlocalizedName());
        list.add("meta:  " + selected.meta);

        if (from != null) list.add("from:  " + from);
    }

    // IF WE PUT ROCKS IN THE SHAPE OF A RUNWAY GOD WILL GIVE US HIGH-FRUCTOSE CORN SYRUP
    @Override
    public int getMaxItemUseDuration(ItemStack stack) {
        return 1;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        int reach = 32;
        Vec3 look = BuildModes.getPlayerLookVec(player);
        Vec3 start = Vec3.createVectorHelper(player.posX, player.posY + player.getEyeHeight(), player.posZ);
        Vec3 end = start.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);
        MovingObjectPosition mop = world.rayTraceBlocks(start, end);

        if (player.isSneaking()) {
            if (!world.isRemote) {
                if (mop == null || mop.typeOfHit != MovingObjectType.BLOCK) return stack;

                int x = mop.blockX;
                int y = mop.blockY;
                int z = mop.blockZ;

                BlockMeta target = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
                setSelected(stack, target);
            }
        } else {
            BlockPos from = getFromPosition(stack);

            if (from == null) {
                if (mop == null || mop.typeOfHit != MovingObjectType.BLOCK) return stack;

                int x = mop.blockX;
                int y = mop.blockY;
                int z = mop.blockZ;
                int side = mop.sideHit;

                ForgeDirection dir = ForgeDirection.getOrientation(side);
                BlockPos pos = new BlockPos(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);

                setFromPosition(stack, pos);
            } else {
                BlockPos to = Line.findLine(player, from, true);

                build(world, getSelected(stack), from, to);
                clearFromPosition(stack);
            }
        }

        return stack;
    }

    public static void build(World world, BlockMeta selected, BlockPos from, BlockPos to) {
        for (int x = Math.min(from.x, to.x); x <= Math.max(from.x, to.x); x++)
        for (int y = Math.min(from.y, to.y); y <= Math.max(from.y, to.y); y++)
        for (int z = Math.min(from.z, to.z); z <= Math.max(from.z, to.z); z++) {
            world.setBlock(x, y, z, selected.block, selected.meta, 3);
        }
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

    public static BuildingMode getMode(ItemStack stack) {
        if (stack.stackTagCompound == null) return BuildingMode.LINE;
        return BuildingMode.LINE;
    }

    public static BlockPos getFromPosition(ItemStack stack) {
        if (stack.stackTagCompound == null || !stack.stackTagCompound.hasKey("x") || !stack.stackTagCompound.hasKey("y") || !stack.stackTagCompound.hasKey("z"))
            return null;

        int x = stack.stackTagCompound.getInteger("x");
        int y = stack.stackTagCompound.getInteger("y");
        int z = stack.stackTagCompound.getInteger("z");

        return new BlockPos(x, y, z);
    }

    public static void setFromPosition(ItemStack stack, BlockPos pos) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

        stack.stackTagCompound.setInteger("x", pos.x);
        stack.stackTagCompound.setInteger("y", pos.y);
        stack.stackTagCompound.setInteger("z", pos.z);
    }

    public static void clearFromPosition(ItemStack stack) {
        stack.stackTagCompound.removeTag("x");
        stack.stackTagCompound.removeTag("y");
        stack.stackTagCompound.removeTag("z");
    }

    @Override
    public void render(World world, EntityPlayer player, ItemStack stack, float partialTicks) {
        BlockPos from = getFromPosition(stack);

        if (from != null) {
            BlockPos to = Line.findLine(player, from, true);

            if (to == null) return;

            // MovingObjectPosition mop = Minecraft.getMinecraft().objectMouseOver;
            // ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
            // BlockPos to = new BlockPos(mop.blockX + dir.offsetX, mop.blockY + dir.offsetY, mop.blockZ + dir.offsetZ);
            
            double dx = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
            double dy = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
            double dz = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

            double minX = Math.min(from.x, to.x) + 0.125;
            double maxX = Math.max(from.x, to.x) + 0.875;
            double minY = Math.min(from.y, to.y) + 0.125;
            double maxY = Math.max(from.y, to.y) + 0.875;
            double minZ = Math.min(from.z, to.z) + 0.125;
            double maxZ = Math.max(from.z, to.z) + 0.875;
            
            GL11.glPushMatrix();
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glColor3f(1F, 1F, 1F);
            
            Tessellator tess = Tessellator.instance;
            tess.setTranslation(-dx, -dy, -dz);
            tess.startDrawing(GL11.GL_LINES);
            tess.setBrightness(240);
            tess.setColorRGBA_F(1F, 1F, 1F, 1F);
            
            // top
            tess.addVertex(minX, maxY, minZ);
            tess.addVertex(minX, maxY, maxZ);
            
            tess.addVertex(minX, maxY, maxZ);
            tess.addVertex(maxX, maxY, maxZ);
            
            tess.addVertex(maxX, maxY, maxZ);
            tess.addVertex(maxX, maxY, minZ);

            tess.addVertex(maxX, maxY, minZ);
            tess.addVertex(minX, maxY, minZ);
            
            // bottom
            tess.addVertex(minX, minY, minZ);
            tess.addVertex(minX, minY, maxZ);
            
            tess.addVertex(minX, minY, maxZ);
            tess.addVertex(maxX, minY, maxZ);
            
            tess.addVertex(maxX, minY, maxZ);
            tess.addVertex(maxX, minY, minZ);

            tess.addVertex(maxX, minY, minZ);
            tess.addVertex(minX, minY, minZ);

            // sides
            tess.addVertex(minX, minY, minZ);
            tess.addVertex(minX, maxY, minZ);

            tess.addVertex(maxX, minY, minZ);
            tess.addVertex(maxX, maxY, minZ);

            tess.addVertex(maxX, minY, maxZ);
            tess.addVertex(maxX, maxY, maxZ);

            tess.addVertex(minX, minY, maxZ);
            tess.addVertex(minX, maxY, maxZ);
            
            tess.draw();
            tess.setTranslation(0, 0, 0);
            
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glPopMatrix();
        }
    }

}
