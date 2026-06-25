package net.mellow.effortless.buildmode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.IConsumableStack;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.util.FixedStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class History {

    private static Map<UUID, FixedStack<History>> undoStacks = new HashMap<>();
    private static Map<UUID, FixedStack<History>> redoStacks = new HashMap<>();

    private static Map<UUID, Map<BlockPos, PlaceableStack>> placedBlocks = new HashMap<>();

    public static void clear(EntityPlayer player) {
        undoStacks.remove(player.getUniqueID());
        redoStacks.remove(player.getUniqueID());
    }

    public static void clear() {
        undoStacks.clear();
        redoStacks.clear();
    }

    public static void addUndo(EntityPlayer player, List<HistoryBlock> blocks, PlaceableStack placed) {
        History history = new History(blocks, placed);

        if (!undoStacks.containsKey(player.getUniqueID())) {
            undoStacks.put(player.getUniqueID(), new FixedStack<>(new History[64]));
        }

        undoStacks.get(player.getUniqueID()).push(history);

        if (!placedBlocks.containsKey(player.getUniqueID())) {
            placedBlocks.put(player.getUniqueID(), new HashMap<>(blocks.size()));
        }

        Map<BlockPos, PlaceableStack> playerPlaced = placedBlocks.get(player.getUniqueID());
        for (HistoryBlock block : blocks) {
            playerPlaced.put(block.pos, placed);
        }
    }

    public static boolean undo(World world, EntityPlayer player) {
        if (!undoStacks.containsKey(player.getUniqueID())) return false;

        FixedStack<History> undoStack = undoStacks.get(player.getUniqueID());
		if (undoStack.isEmpty()) return false;
        History blockSet = undoStack.pop();
        if (blockSet == null || blockSet.state.length == 0) return false;

        int blocksReturned = 0;
        List<HistoryBlock> redoBlocks = new ArrayList<>();

        for (HistoryBlock step : blockSet.state) {
            int x = step.pos.x;
            int y = step.pos.y;
            int z = step.pos.z;

            BlockMeta current = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));

            if (!current.equals(step.isNow)) continue; // only undo blocks that haven't changed
            if (current.equals(step.type)) continue; // only place blocks that aren't already the current type

            redoBlocks.add(new HistoryBlock(current, step.type, step.pos));
            world.setBlock(x, y, z, step.type.block, step.type.meta, 3);
            blocksReturned++;
        }

        if (!player.capabilities.isCreativeMode) {
            while (blocksReturned > 0) {
                // TODO: separate itemstacks by blocks if we add block randomisation
                ItemStack toReturn = blockSet.placed.stack.copy();
                toReturn.stackSize = Math.min(blocksReturned, toReturn.getMaxStackSize());
                blocksReturned -= toReturn.stackSize;
                player.inventory.addItemStackToInventory(toReturn);
            }
        }

        addRedo(player, redoBlocks, blockSet.placed);

        return true;
    }

    public static void addRedo(EntityPlayer player, List<HistoryBlock> blocks, PlaceableStack placed) {
        History history = new History(blocks, placed);

        if (!redoStacks.containsKey(player.getUniqueID())) {
            redoStacks.put(player.getUniqueID(), new FixedStack<>(new History[64]));
        }

        redoStacks.get(player.getUniqueID()).push(history);
    }

    public static boolean redo(World world, EntityPlayer player) {
        if (!redoStacks.containsKey(player.getUniqueID())) return false;

        FixedStack<History> redoStack = redoStacks.get(player.getUniqueID());
		if (redoStacks.isEmpty()) return false;
        History blockSet = redoStack.pop();
        if (blockSet == null || blockSet.state.length == 0) return false;

        boolean useItems = !player.capabilities.isCreativeMode;

        List<IConsumableStack> depletedStacks = new ArrayList<>();
        IConsumableStack toDeplete = null;

        if (useItems) {
            toDeplete = IConsumableStack.getMatchingStack(player, blockSet.placed, blockSet.state.length);
            if (toDeplete == null) return false;

            depletedStacks.add(toDeplete);
        }

        List<HistoryBlock> undoBlocks = new ArrayList<>();

        int blocksPlaced = 0;

        for (HistoryBlock step : blockSet.state) {
            int x = step.pos.x;
            int y = step.pos.y;
            int z = step.pos.z;

            BlockMeta current = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));

            if (!current.equals(step.isNow)) continue; // only redo blocks that haven't changed
            if (current.equals(step.type)) continue; // only place blocks that aren't already the current type

            if (useItems) {
                if (toDeplete == null) {
                    toDeplete = IConsumableStack.getMatchingStack(player, blockSet.placed, blockSet.state.length - blocksPlaced);
                    if (toDeplete == null) break;

                    depletedStacks.add(toDeplete);
                }

                // Eat a block, returning true indicates the consumable is finished
                if (toDeplete.consumeOne()) {
                    toDeplete = null;
                }
            }

            undoBlocks.add(new HistoryBlock(current, step.type, step.pos));
            world.setBlock(x, y, z, step.type.block, step.type.meta, 1);
            if (blockSet.placed.nbt != null) {
                TileEntity tile = world.getTileEntity(x, y, z);
                blockSet.placed.nbt.setInteger("x", x);
                blockSet.placed.nbt.setInteger("y", y);
                blockSet.placed.nbt.setInteger("z", z);
                tile.readFromNBT(blockSet.placed.nbt);
                tile.markDirty();
            }
            world.markBlockForUpdate(x, y, z);

            blocksPlaced++;
        }

        addUndo(player, undoBlocks, blockSet.placed);

        if (useItems) {
            IConsumableStack.cleanInventory(player, depletedStacks);
        }

        return true;
    }

    public static Map<BlockPos, PlaceableStack> getPlaceableMap(EntityPlayer player) {
        return placedBlocks.get(player.getUniqueID());
    }

    public History(List<HistoryBlock> blocks, PlaceableStack placed) {
        this.state = blocks.toArray(new HistoryBlock[blocks.size()]);
        this.placed = placed;
    }

    // gonna try to be somewhat efficient with memory usage here (haha)
    public final HistoryBlock[] state;
    public final PlaceableStack placed;
    
    public static final class HistoryBlock {

        public final BlockMeta type;
        public final BlockMeta isNow; // if the current block doesn't match "isNow", it has been modified and should be excluded
        public final BlockPos pos;

        public HistoryBlock(BlockMeta type, BlockMeta isNow, BlockPos pos) {
            this.type = type;
            this.isNow = isNow;
            this.pos = pos;
        }

    }

}
