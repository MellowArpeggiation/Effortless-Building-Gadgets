package net.mellow.effortless.buildmode;

import java.util.ArrayList;
import java.util.List;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.IConsumableStack;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.buildmode.History.HistoryBlock;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public abstract class BaseBuildMode {

    public abstract int add(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop);
    public abstract void clear(ItemStack stack);

    public int reach(ItemStack stack) {
        return 32;
    }

    // selected - the block selected by the tool
    // toPlace  - the transformed block to be placed into the world
    public static int build(World world, EntityPlayer player, PlaceableStack selected, List<BlockPos> positions, boolean replaceAny) {
        if (world.isRemote) return 0;
        if (positions == null || positions.isEmpty()) return 0;

        boolean useItems = !player.capabilities.isCreativeMode;

        List<HistoryBlock> previousState = new ArrayList<>();

        List<IConsumableStack> depletedStacks = new ArrayList<>();
        IConsumableStack toDeplete = null;

        if (useItems) {
            toDeplete = IConsumableStack.getMatchingStack(player, selected, positions.size());
            if (toDeplete == null) return 0;

            depletedStacks.add(toDeplete);
        }

        int blocksPlaced = 0;

        for (BlockPos pos : positions) {
            Block block = world.getBlock(pos.x, pos.y, pos.z);
            if (!replaceAny && !block.isReplaceable(world, pos.x, pos.y, pos.z)) continue;

            int meta = world.getBlockMetadata(pos.x, pos.y, pos.z);
            if (selected.place.block == block && selected.place.meta == meta) continue; // skip double placing
            if (block.hasTileEntity(meta)) continue;

            AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(pos.x, pos.y, pos.z, pos.x + 1, pos.y + 1, pos.z + 1);

            if (!world.checkNoEntityCollision(bb, player)) continue;

            if (useItems) {
                if (toDeplete == null) {
                    toDeplete = IConsumableStack.getMatchingStack(player, selected, positions.size() - blocksPlaced);
                    if (toDeplete == null) break;

                    depletedStacks.add(toDeplete);
                }

                // Eat a block, returning true indicates the consumable is finished
                if (toDeplete.consumeOne()) {
                    toDeplete = null;
                }
            }

            previousState.add(new HistoryBlock(new BlockMeta(block, meta), selected.place, new BlockPos(pos.x, pos.y, pos.z)));
            world.setBlock(pos.x, pos.y, pos.z, selected.place.block, selected.place.meta, 1);
            if (selected.nbt != null) {
                TileEntity tile = world.getTileEntity(pos.x, pos.y, pos.z);
                selected.nbt.setInteger("x", pos.x);
                selected.nbt.setInteger("y", pos.y);
                selected.nbt.setInteger("z", pos.z);
                tile.readFromNBT(selected.nbt);
                tile.markDirty();
            }
            world.markBlockForUpdate(pos.x, pos.y, pos.z);

            blocksPlaced++;
        }
        History.addUndo(player, previousState, selected);

        if (useItems) {
            IConsumableStack.cleanInventory(player, depletedStacks);
        }

        world.playSoundEffect(player.posX, player.posY, player.posZ, selected.place.block.stepSound.func_150496_b(), (selected.place.block.stepSound.getVolume() + 1.0F) / 2.0F, selected.place.block.stepSound.getPitch() * 0.8F);

        return blocksPlaced;
    }

    public static int build(World world, EntityPlayer player, PlaceableStack selected, BlockPos position, boolean replaceAny) {
        List<BlockPos> list = new ArrayList<>();
        list.add(position);
        return build(world, player, selected, list, replaceAny);
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
