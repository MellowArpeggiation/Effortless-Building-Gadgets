package net.mellow.effortless.buildmode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.util.FixedStack;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class History {

    private static Map<UUID, FixedStack<History>> undoStacks = new HashMap<>();
    private static Map<UUID, FixedStack<History>> redoStacks = new HashMap<>();

    public static void clear(EntityPlayer player) {
        undoStacks.remove(player.getUniqueID());
        redoStacks.remove(player.getUniqueID());
    }

    public static void clear() {
        undoStacks.clear();
        redoStacks.clear();
    }

    public static void addUndo(EntityPlayer player, List<HistoryBlock> blocks, BlockMeta blockItem) {
        History history = new History(blocks, blockItem);

        if (!undoStacks.containsKey(player.getUniqueID())) {
            undoStacks.put(player.getUniqueID(), new FixedStack<>(new History[64]));
        }

        undoStacks.get(player.getUniqueID()).push(history);
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

            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);

            if (!new BlockMeta(block, meta).equals(step.isNow)) continue;

            redoBlocks.add(new HistoryBlock(new BlockMeta(block, meta), step.type, step.pos));
            world.setBlock(x, y, z, step.type.block, step.type.meta, 3);
            blocksReturned++;
        }

        if (!player.capabilities.isCreativeMode) {
            while (blocksReturned > 0) {
                // TODO: separate itemstacks by blocks if we add block randomisation
                int size = Math.min(blocksReturned, 64);
                ItemStack toReturn = new ItemStack(blockSet.blockItem.block, size, blockSet.blockItem.meta);
                player.inventory.addItemStackToInventory(toReturn);
                blocksReturned -= size;
            }
        }

        addRedo(player, redoBlocks, blockSet.blockItem);

        return true;
    }

    public static void addRedo(EntityPlayer player, List<HistoryBlock> blocks, BlockMeta blockItem) {
        History history = new History(blocks, blockItem);

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
        
        ItemStack toDeplete = null;
        if (useItems) {
            toDeplete = BaseBuildMode.getMatchingStack(player, blockSet.blockItem);
            if (toDeplete == null) return false;
        }

        List<HistoryBlock> undoBlocks = new ArrayList<>();

        for (HistoryBlock step : blockSet.state) {
            int x = step.pos.x;
            int y = step.pos.y;
            int z = step.pos.z;

            Block block = world.getBlock(x, y, z);
            int meta = world.getBlockMetadata(x, y, z);

            if (!new BlockMeta(block, meta).equals(step.isNow)) continue;
            
            if (useItems) {
                if (toDeplete == null || toDeplete.stackSize <= 0) {
                    toDeplete = BaseBuildMode.getMatchingStack(player, blockSet.blockItem);

                    if (toDeplete == null) {
                        break;
                    }
                }

                toDeplete.stackSize--;
            }

            undoBlocks.add(new HistoryBlock(new BlockMeta(block, meta), step.type, step.pos));
            world.setBlock(x, y, z, step.type.block, step.type.meta, 3);
        }

        addUndo(player, undoBlocks, blockSet.blockItem);

        BaseBuildMode.cleanInventory(player);

        return true;
    }

    public History(List<HistoryBlock> blocks, BlockMeta blockItem) {
        this.state = blocks.toArray(new HistoryBlock[blocks.size()]);
        this.blockItem = blockItem;
    }

    // gonna try to be somewhat efficient with memory usage here
    public final HistoryBlock[] state;
    public final BlockMeta blockItem;
    
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
