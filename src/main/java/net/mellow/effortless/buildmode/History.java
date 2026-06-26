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
import net.mellow.effortless.buildmode.BaseBuildMode.Operation;
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

    public static void addUndo(EntityPlayer player, List<HistoryBlock> blocks, PlaceableStack placed, Operation operation) {
        History history = new History(blocks, operation);

        if (!undoStacks.containsKey(player.getUniqueID())) {
            undoStacks.put(player.getUniqueID(), new FixedStack<>(new History[64]));
        }

        undoStacks.get(player.getUniqueID()).push(history);

        if (placed != null) {
            if (!placedBlocks.containsKey(player.getUniqueID())) {
                placedBlocks.put(player.getUniqueID(), new HashMap<>(blocks.size()));
            }
    
            Map<BlockPos, PlaceableStack> playerPlaced = placedBlocks.get(player.getUniqueID());
            for (HistoryBlock block : blocks) {
                playerPlaced.put(block.pos, placed);
            }
        }
    }

    public static boolean undo(World world, EntityPlayer player) {
        if (!undoStacks.containsKey(player.getUniqueID())) return false;

        FixedStack<History> undoStack = undoStacks.get(player.getUniqueID());
		if (undoStack.isEmpty()) return false;
        History blockSet = undoStack.pop();
        if (blockSet == null || blockSet.state.length == 0) return false;

        List<HistoryBlock> redoBlocks = blockSet.operation == Operation.PLACE
            ? destroy(world, player, blockSet)
            : build(world, player, blockSet);
        if (redoBlocks == null) return false;

        addRedo(player, redoBlocks, blockSet.operation);

        return true;
    }
    
    // TODO: refactor to remove code duplicated from ConstructionSet
    private static List<HistoryBlock> destroy(World world, EntityPlayer player, History blockSet) {
        // yeah you get it
        boolean useItems = !player.capabilities.isCreativeMode;
        Map<BlockPos, PlaceableStack> placeMap = History.getPlaceableMap(player);
        if (useItems && placeMap == null) return null;
        ItemStack toReturn = null;

        List<HistoryBlock> history = new ArrayList<>();

        for (HistoryBlock step : blockSet.state) {
            int x = step.pos.x;
            int y = step.pos.y;
            int z = step.pos.z;

            BlockMeta current = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));

            if (!current.equals(step.isNow)) continue; // only undo blocks that haven't changed
            if (current.equals(step.type)) continue; // only place blocks that aren't already the current type

            if (useItems) {
                PlaceableStack placed = placeMap.get(step.pos);
                if (placed == null) continue;

                if (toReturn != null && (toReturn.stackSize >= 64 || !PlaceableStack.stackMatches(toReturn, placed.stack))) {
                    player.inventory.addItemStackToInventory(toReturn);
                    toReturn = null;
                }

                if (toReturn == null) {
                    toReturn = placed.stack.copy();
                    toReturn.stackSize = 0;
                }
                
                toReturn.stackSize++;
            }

            history.add(new HistoryBlock(current, step.type, step.pos, step.tile, step.placed));
            world.setBlock(x, y, z, step.type.block, step.type.meta, 3);
        }

        if (toReturn != null) {
            player.inventory.addItemStackToInventory(toReturn);
        }

        return history;
    }

    public static void addRedo(EntityPlayer player, List<HistoryBlock> blocks, Operation operation) {
        History history = new History(blocks, operation);

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

        List<HistoryBlock> undoBlocks = blockSet.operation == Operation.PLACE
            ? build(world, player, blockSet)
            : destroy(world, player, blockSet);
        if (undoBlocks == null) return false;
        
        addUndo(player, undoBlocks, null, blockSet.operation);
        
        return true;
    }

    // TODO: yeah same here gotta remove code duplicated from ConstructionSet
    private static List<HistoryBlock> build(World world, EntityPlayer player, History blockSet) {
        List<HistoryBlock> history = new ArrayList<>();

        boolean useItems = !player.capabilities.isCreativeMode;

        List<IConsumableStack> depletedStacks = new ArrayList<>();
        IConsumableStack toDeplete = null;

        int blocksPlaced = 0;

        for (HistoryBlock step : blockSet.state) {
            int x = step.pos.x;
            int y = step.pos.y;
            int z = step.pos.z;

            BlockMeta current = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));

            if (!current.equals(step.isNow)) continue; // only redo blocks that haven't changed
            if (current.equals(step.type)) continue; // only place blocks that aren't already the current type

            if (useItems) {
                if (toDeplete == null || !PlaceableStack.stackMatches(toDeplete.getStack(), step.placed.stack)) {
                    toDeplete = IConsumableStack.getMatchingStack(player, step.placed, blockSet.state.length - blocksPlaced);
                    if (toDeplete == null) break;

                    depletedStacks.add(toDeplete);
                }

                // Eat a block, returning true indicates the consumable is finished
                if (toDeplete.consumeOne()) {
                    toDeplete = null;
                }
            }

            history.add(new HistoryBlock(current, step.type, step.pos, step.tile, step.placed));
            world.setBlock(x, y, z, step.type.block, step.type.meta, 1);
            if (step.tile != null) {
                world.setTileEntity(x, y, z, step.tile);
            }
            world.markBlockForUpdate(x, y, z);

            blocksPlaced++;
        }

        if (useItems) {
            IConsumableStack.cleanInventory(player, depletedStacks);
        }

        return history;
    }

    public static Map<BlockPos, PlaceableStack> getPlaceableMap(EntityPlayer player) {
        if (!placedBlocks.containsKey(player.getUniqueID())) {
            placedBlocks.put(player.getUniqueID(), new HashMap<>());
        }

        return placedBlocks.get(player.getUniqueID());
    }

    public History(List<HistoryBlock> blocks, Operation operation) {
        this.state = blocks.toArray(new HistoryBlock[blocks.size()]);
        this.operation = operation;
    }

    // gonna try to be somewhat efficient with memory usage here (hahaahhahahahahahahahaha fuck)
    public final HistoryBlock[] state;
    public final Operation operation;
    
    public static final class HistoryBlock {

        public final BlockMeta type;
        public final BlockMeta isNow;
        public final BlockPos pos;

        // okay this is a slight bit horrific now
        // tile for broken blocks being undone (putting them back)
        // placed for placed blocks being undone (to put back into the inventory)
        public final TileEntity tile;
        public final PlaceableStack placed;

        public HistoryBlock(BlockMeta type, BlockMeta isNow, BlockPos pos, TileEntity tile, PlaceableStack placed) {
            this.type = type;
            this.isNow = isNow;
            this.pos = pos;
            this.tile = tile;
            this.placed = placed;
        }

    }

}
