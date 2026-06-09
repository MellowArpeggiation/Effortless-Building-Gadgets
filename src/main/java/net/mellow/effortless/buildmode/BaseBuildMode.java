package net.mellow.effortless.buildmode;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.buildmode.History.HistoryBlock;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
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

    // will place and immediately remove the block once it finds the final meta
    // x y z is the final block position, not the block it is placed on
    public static int getFinalPlacedMeta(BlockMeta selected, World world, EntityPlayer player, int x, int y, int z, int side, Vec3 hitVector) {

        float subX = (float)hitVector.x - (float)x;
        float subY = (float)hitVector.y - (float)y;
        float subZ = (float)hitVector.z - (float)z;

        BlockMeta was = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));

        ItemStack stack = new ItemStack(selected.block, 1, selected.meta);
        int meta = stack.getItem().getMetadata(stack.getItemDamage());
        meta = selected.block.onBlockPlaced(world, x, y, z, side, subX, subY, subZ, meta);

        if (!world.setBlock(x, y, z, selected.block, meta, 0)) {
            return meta;
        }

        if (world.getBlock(x, y, z) == selected.block) {
            selected.block.onBlockPlacedBy(world, x, y, z, player, stack);
            selected.block.onPostBlockPlaced(world, x, y, z, meta);
        }

        meta = world.getBlockMetadata(x, y, z);

        world.setBlock(x, y, z, was.block, was.meta, 2);

        return meta;
    }

    public static int buildBox(World world, EntityPlayer player, BlockMeta selected, int placedMeta, BlockPos from, BlockPos to, boolean replaceAny) {
        if (world.isRemote) return 0;
        if (from == null || to == null) return 0;

        List<BlockPos> positions = new ArrayList<>();
        addBox(positions, from, to);
        
        return build(world, player, selected, placedMeta, positions, replaceAny);
    }

    private static void addBox(List<BlockPos> positions, BlockPos from, BlockPos to) {
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        for (int x = min.x; x <= max.x; x++)
        for (int y = min.y; y <= max.y; y++)
        for (int z = min.z; z <= max.z; z++) {
            positions.add(new BlockPos(x, y, z));
        }
    }

    public static int buildHollowFloor(World world, EntityPlayer player, BlockMeta selected, int placedMeta, BlockPos from, BlockPos to, boolean replaceAny) {
        if (world.isRemote) return 0;
        if (from == null || to == null) return 0;

        List<BlockPos> positions = new ArrayList<>();
        addHollowFloor(positions, from, to);

        return build(world, player, selected, placedMeta, positions, replaceAny);
    }

    private static void addHollowFloor(List<BlockPos> positions, BlockPos from, BlockPos to) {
        for (int x = Math.min(from.x, to.x); x <= Math.max(from.x, to.x); x++) {
            positions.add(new BlockPos(x, from.y, from.z));
            if (from.z != to.z) positions.add(new BlockPos(x, from.y, to.z));
        }

        for (int z = Math.min(from.z, to.z); z <= Math.max(from.z, to.z); z++) {
            positions.add(new BlockPos(from.x, from.y, z));
            if (from.x != to.x) positions.add(new BlockPos(to.x, from.y, z));
        }
    }

    public static int buildHollowWallX(World world, EntityPlayer player, BlockMeta selected, int placedMeta, BlockPos from, BlockPos to, boolean replaceAny) {
        if (world.isRemote) return 0;
        if (from == null || to == null) return 0;

        List<BlockPos> positions = new ArrayList<>();

        for (int z = Math.min(from.z, to.z); z <= Math.max(from.z, to.z); z++) {
            positions.add(new BlockPos(from.x, from.y, z));
            if (from.y != to.y) positions.add(new BlockPos(from.x, to.y, z));
        }

        for (int y = Math.min(from.y, to.y); y <= Math.max(from.y, to.y); y++) {
            positions.add(new BlockPos(from.x, y, from.z));
            if (from.z != to.z) positions.add(new BlockPos(from.x, y, to.z));
        }

        return build(world, player, selected, placedMeta, positions, replaceAny);
    }

    public static int buildHollowWallZ(World world, EntityPlayer player, BlockMeta selected, int placedMeta, BlockPos from, BlockPos to, boolean replaceAny) {
        if (world.isRemote) return 0;
        if (from == null || to == null) return 0;

        List<BlockPos> positions = new ArrayList<>();

        for (int x = Math.min(from.x, to.x); x <= Math.max(from.x, to.x); x++) {
            positions.add(new BlockPos(x, from.y, from.z));
            if (from.y != to.y) positions.add(new BlockPos(x, to.y, from.z));
        }

        for (int y = Math.min(from.y, to.y); y <= Math.max(from.y, to.y); y++) {
            positions.add(new BlockPos(from.x, y, from.z));
            if (from.x != to.x) positions.add(new BlockPos(to.x, y, from.z));
        }

        return build(world, player, selected, placedMeta, positions, replaceAny);
    }

    public static int buildHollowCube(World world, EntityPlayer player, BlockMeta selected, int placedMeta, BlockPos from, BlockPos to, boolean replaceAny) {
        if (world.isRemote) return 0;
        if (from == null || to == null) return 0;

        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        List<BlockPos> positions = new ArrayList<>();
        addBox(positions, min, new BlockPos(max.x, min.y, max.z)); // bottom
        if (min.y != max.y) {
            addBox(positions, new BlockPos(min.x, max.y, min.z), max); // top
        }

        if (max.y - min.y > 1) {
            addBox(positions, new BlockPos(min.x, min.y + 1, min.z), new BlockPos(min.x, max.y - 1, max.z)); // -X wall
            
            if (min.x != max.x) {
                addBox(positions, new BlockPos(max.x, min.y + 1, min.z), new BlockPos(max.x, max.y - 1, max.z)); // +X wall
            }

            if (max.x - min.x > 1) {
                addBox(positions, new BlockPos(min.x + 1, min.y + 1, min.z), new BlockPos(max.x - 1, max.y - 1, min.z)); // -Z wall

                if (min.z != max.z) {
                    addBox(positions, new BlockPos(min.x + 1, min.y + 1, max.z), new BlockPos(max.x - 1, max.y - 1, max.z)); // +Z wall
                }
            }
        }

        return build(world, player, selected, placedMeta, positions, replaceAny);
    }

    public static int buildSkeletonCube(World world, EntityPlayer player, BlockMeta selected, int placedMeta, BlockPos from, BlockPos to, boolean replaceAny) {
        if (world.isRemote) return 0;
        if (from == null || to == null) return 0;

        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        List<BlockPos> positions = new ArrayList<>();
        addHollowFloor(positions, min, new BlockPos(max.x, min.y, max.z)); // bottom
        if (min.y != max.y) {
            addHollowFloor(positions, new BlockPos(min.x, max.y, min.z), max); // top

            if (max.y - min.y > 1) {
                addBox(positions, new BlockPos(min.x, min.y + 1, min.z), new BlockPos(min.x, max.y - 1, min.z));
                addBox(positions, new BlockPos(min.x, min.y + 1, max.z), new BlockPos(min.x, max.y - 1, max.z));
                addBox(positions, new BlockPos(max.x, min.y + 1, min.z), new BlockPos(max.x, max.y - 1, min.z));
                addBox(positions, new BlockPos(max.x, min.y + 1, max.z), new BlockPos(max.x, max.y - 1, max.z));
            }
        }

        return build(world, player, selected, placedMeta, positions, replaceAny);
    }

    // selected - the block selected by the tool
    // toPlace  - the transformed block to be placed into the world
    public static int build(World world, EntityPlayer player, BlockMeta selected, int placedMeta, List<BlockPos> positions, boolean replaceAny) {
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

        BlockMeta toPlace = new BlockMeta(selected.block, placedMeta);

        for (BlockPos pos : positions) {
            Block block = world.getBlock(pos.x, pos.y, pos.z);
            if (!replaceAny && !block.isReplaceable(world, pos.x, pos.y, pos.z)) continue;

            int meta = world.getBlockMetadata(pos.x, pos.y, pos.z);
            if (toPlace.block == block && toPlace.meta == meta) continue; // skip double placing
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

            previousState.add(new HistoryBlock(new BlockMeta(block, meta), toPlace, new BlockPos(pos.x, pos.y, pos.z)));
            world.setBlock(pos.x, pos.y, pos.z, toPlace.block, toPlace.meta, 3);

            blocksPlaced++;
        }

        History.addUndo(player, previousState);

        cleanInventory(player);

        world.playSoundEffect(player.posX, player.posY, player.posZ, selected.block.stepSound.func_150496_b(), (selected.block.stepSound.getVolume() + 1.0F) / 2.0F, selected.block.stepSound.getPitch() * 0.8F);

        return blocksPlaced;
    }

    public static ItemStack getMatchingStack(EntityPlayer player, BlockMeta selected) {
        for (int i = player.inventory.mainInventory.length - 1; i >= 0; i--) {
            ItemStack stack = player.inventory.mainInventory[i];

            BlockMeta block = BlockMeta.fromStack(stack);
            if (block == null) continue;

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

        player.inventoryContainer.detectAndSendChanges();
    }

    public abstract void render(ItemStack stack, World world, EntityPlayer player, float partialTicks);

    public static void renderBox(EntityPlayer player, float partialTicks, BlockPos from, BlockPos to) {
        renderBox(player, partialTicks, from, to, false);
    }

    public static void renderBox(EntityPlayer player, float partialTicks, BlockPos from, BlockPos to, boolean addToHighlight) {
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        if (addToHighlight) {
            List<String> values = new ArrayList<>();
            if (min.x != max.x) values.add("" + (max.x - min.x + 1));
            if (min.y != max.y) values.add("" + (max.y - min.y + 1));
            if (min.z != max.z) values.add("" + (max.z - min.z + 1));

            highlightTitle = !values.isEmpty() ? String.join("x", values) : "1";
            Minecraft.getMinecraft().ingameGUI.remainingHighlightTicks = 40;
        }

        double minX = min.x + 0.075;
        double maxX = max.x + 0.925;
        double minY = min.y + 0.075;
        double maxY = max.y + 0.925;
        double minZ = min.z + 0.075;
        double maxZ = max.z + 0.925;

        Tessellator tess = Tessellator.instance;
        startLineDraw(tess, player, partialTicks);
        
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

        endLineDraw(tess);
    }

    public static void startLineDraw(Tessellator tess, EntityPlayer player, float partialTicks) {
        double dx = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double dy = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double dz = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor3f(1F, 1F, 1F);
        
        tess.setTranslation(-dx, -dy, -dz);
        tess.startDrawing(GL11.GL_LINES);
        tess.setBrightness(240);
        tess.setColorRGBA_F(1F, 1F, 1F, 1F);
    }

    public static void endLineDraw(Tessellator tess) {
        tess.draw();
        tess.setTranslation(0, 0, 0);
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glPopMatrix();
    }

    public static String highlightTitle;
    
    public String getItemHighlight(ItemStack stack) {
        if (highlightTitle == null) return null;

        String title = highlightTitle;
        highlightTitle = null;
        return title;
    }

}
