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
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public abstract class BaseBuildMode {
    
    public abstract int add(ItemStack stack, BlockMeta selected, World world, EntityPlayer player, MovingObjectPosition mop);
    public abstract void clear(ItemStack stack);

    public int reach(ItemStack stack) {
        return 32;
    }

    public static int buildBox(World world, EntityPlayer player, BlockMeta selected, BlockPos from, BlockPos to, boolean replaceAny) {
        if (world.isRemote) return 0;
        if (from == null || to == null) return 0;

        List<BlockPos> positions = new ArrayList<>();

        for (int x = Math.min(from.x, to.x); x <= Math.max(from.x, to.x); x++)
        for (int y = Math.min(from.y, to.y); y <= Math.max(from.y, to.y); y++)
        for (int z = Math.min(from.z, to.z); z <= Math.max(from.z, to.z); z++) {
            positions.add(new BlockPos(x, y, z));
        }
        
        return build(world, player, selected, positions, replaceAny);
    }

    public static int build(World world, EntityPlayer player, BlockMeta selected, List<BlockPos> positions, boolean replaceAny) {
        if (world.isRemote) return 0;
        if (positions == null || positions.isEmpty()) return 0;

        boolean useItems = !player.capabilities.isCreativeMode;

        List<HistoryBlock> previousState = new ArrayList<>();
        ItemStack toDeplete = null;
        if (useItems) {
            toDeplete = getMatchingStack(player, selected);
            if (toDeplete == null) return 0;
        }

        int blocksPlaced = 0;

        for (BlockPos pos : positions) {
            Block block = world.getBlock(pos.x, pos.y, pos.z);
            if (!replaceAny && !block.isReplaceable(world, pos.x, pos.y, pos.z)) continue;

            int meta = world.getBlockMetadata(pos.x, pos.y, pos.z);
            if (block.hasTileEntity(meta)) continue;

            AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(pos.x, pos.y, pos.z, pos.x + 1, pos.y + 1, pos.z + 1);

            if (!world.checkNoEntityCollision(bb, player)) continue;

            if (useItems) {
                if (toDeplete == null || toDeplete.stackSize <= 0) {
                    toDeplete = getMatchingStack(player, selected);

                    if (toDeplete == null) {
                        break;
                    }
                }

                toDeplete.stackSize--;
            }

            previousState.add(new HistoryBlock(new BlockMeta(block, meta), selected, new BlockPos(pos.x, pos.y, pos.z)));
            world.setBlock(pos.x, pos.y, pos.z, selected.block, selected.meta, 3);

            blocksPlaced++;
        }

        History.addUndo(player, previousState);

        cleanInventory(player);

        return blocksPlaced;
    }

    public static ItemStack getMatchingStack(EntityPlayer player, BlockMeta selected) {
        for (int i = player.inventory.mainInventory.length - 1; i >= 0; i--) {
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
