package net.mellow.effortless.buildmode;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.buildmode.History.HistoryBlock;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public abstract class BaseBuildMode {
    
    public abstract void add(ItemStack stack, BlockMeta selected, World world, EntityPlayer player, MovingObjectPosition mop);
    public abstract void clear(ItemStack stack);

    public int reach(ItemStack stack) {
        return 32;
    }

    public static void buildBox(World world, EntityPlayer player, BlockMeta selected, BlockPos from, BlockPos to, boolean replaceAny) {
        if (from == null || to == null) return;
        if (world.isRemote) return;

        boolean useItems = !player.capabilities.isCreativeMode;

        List<HistoryBlock> previousState = new ArrayList<>();
        ItemStack toDeplete = null;
        if (useItems) {
            toDeplete = getMatchingStack(player, selected);
            if (toDeplete == null) return;
        }

        outer:
        for (int x = Math.min(from.x, to.x); x <= Math.max(from.x, to.x); x++)
        for (int y = Math.min(from.y, to.y); y <= Math.max(from.y, to.y); y++)
        for (int z = Math.min(from.z, to.z); z <= Math.max(from.z, to.z); z++) {
            Block block = world.getBlock(x, y, z);
            if (!replaceAny && !block.isReplaceable(world, x, y, z)) continue;

            int meta = world.getBlockMetadata(x, y, z);
            if (block.hasTileEntity(meta)) continue;

            if (useItems) {
                if (toDeplete == null || toDeplete.stackSize <= 0) {
                    toDeplete = getMatchingStack(player, selected);

                    if (toDeplete == null) {
                        break outer;
                    }
                }

                toDeplete.stackSize--;
            }

            previousState.add(new HistoryBlock(new BlockMeta(block, meta), selected, new BlockPos(x, y, z)));
            world.setBlock(x, y, z, selected.block, selected.meta, 3);
        }

        History.addUndo(player, previousState);

        cleanInventory(player);
    }

    public static ItemStack getMatchingStack(EntityPlayer player, BlockMeta selected) {
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];

            if (stack == null) continue;
            if (stack.stackSize <= 0) continue;
            if (!(stack.getItem() instanceof ItemBlock)) continue;

            BlockMeta block = new BlockMeta(((ItemBlock) stack.getItem()).field_150939_a, stack.getItem().getMetadata(stack.getItemDamage()));

            if (block.equals(selected)) return stack;
        }

        return null;
    }

    // Vanilla doesn't handle empty stacks automatically, this is a (shit) solution to that
    public static void cleanInventory(EntityPlayer player) {
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack != null && stack.stackSize <= 0) player.inventory.mainInventory[i] = null;
        }
    }

    public abstract void render(ItemStack stack, World world, EntityPlayer player, float partialTicks);

    public static void renderBox(EntityPlayer player, float partialTicks, BlockPos from, BlockPos to) {
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
