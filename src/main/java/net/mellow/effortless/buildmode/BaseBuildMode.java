package net.mellow.effortless.buildmode;

import java.util.ArrayList;
import java.util.List;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.buildmode.History.HistoryBlock;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
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

    // selected - the block selected by the tool
    // toPlace  - the transformed block to be placed into the world
    // TODO refactor `selected`/`toPlace`/`placedMeta` to be more well contained, easier to grok, right now placedMeta is a bit of a hack
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

        History.addUndo(player, previousState, selected);

        cleanInventory(player);

        world.playSoundEffect(player.posX, player.posY, player.posZ, selected.block.stepSound.func_150496_b(), (selected.block.stepSound.getVolume() + 1.0F) / 2.0F, selected.block.stepSound.getPitch() * 0.8F);

        return blocksPlaced;
    }

    public static int build(World world, EntityPlayer player, BlockMeta selected, int placedMeta, BlockPos position, boolean replaceAny) {
        List<BlockPos> list = new ArrayList<>();
        list.add(position);
        return build(world, player, selected, placedMeta, list, replaceAny);
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

    public static void updateHighlight(BlockPos from, BlockPos to) {
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        List<String> values = new ArrayList<>();
        if (min.x != max.x) values.add("" + (max.x - min.x + 1));
        if (min.y != max.y) values.add("" + (max.y - min.y + 1));
        if (min.z != max.z) values.add("" + (max.z - min.z + 1));

        highlightTitle = !values.isEmpty() ? String.join("x", values) : "1";
        Minecraft.getMinecraft().ingameGUI.remainingHighlightTicks = 40;
    }

    public static String highlightTitle;
    
    public String getItemHighlight(ItemStack stack) {
        if (highlightTitle == null) return null;

        String title = highlightTitle;
        highlightTitle = null;
        return title;
    }

}
