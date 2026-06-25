package net.mellow.effortless.buildmode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.blocks.IConsumableStack;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.blocks.BlockPos.Dimension;
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

    public static enum Operation {
        PLACE,
        BREAK,
    }

    public abstract int add(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop);
    public abstract boolean clear(ItemStack stack);
    public abstract boolean isPlacing(ItemStack stack);

    // TODO: abstract these when finished, and kill the above crap!!
    // on true, attempt to get placed blocks and commit them to the world
    public boolean click(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        return false;
    }

    // TODO: same here!!!!!
    // return null if getblocks failed, should ignore clicks and draw nothing
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player) {
        return null;
    }

    // put the placeable somewhere safe, will retrieve it upon finishing
    public void savePlaceable(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        if (isPlacing(stack)) return; // only on first click

        BlockPos from = BlockPos.fromRaycastReplaceable(world, mop);
        PlaceableStack place = PlaceableStack.getPlaceableStack(selected, world, player, from.x, from.y, from.z, mop.sideHit, new Vec3(mop.hitVec));

        stack.stackTagCompound.setTag("place", place.save());
    }

    public PlaceableStack getPlaceable(ItemStack stack) {
        return PlaceableStack.load(stack.stackTagCompound.getCompoundTag("place"));
    }

    public void renderNew(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        
    }

    // public int remove(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
    //     return 0;
    // }

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
    
    // // break is a reserved keyword dum dum
    // public static int destroy(World world, EntityPlayer player, List<BlockPos> positions) {
    //     if (world.isRemote) return 0;
    //     if (positions == null || positions.isEmpty()) return 0;

    //     boolean isSurvival = !player.capabilities.isCreativeMode;

    //     List<HistoryBlock> previousState = new ArrayList<>();

    //     // List<IConsumableStack> depletedStacks = new ArrayList<>();
    //     // IConsumableStack toDeplete = null;

    //     Map<BlockPos, PlaceableStack> placed = History.getPlaceableMap(player);

    //     // if (isSurvival) {
    //     //     toDeplete = IConsumableStack.getMatchingStack(player, selected, positions.size());
    //     //     if (toDeplete == null) return 0;

    //     //     depletedStacks.add(toDeplete);
    //     // }

    //     int blocksBroken = 0;

    //     for (BlockPos pos : positions) {
    //         Block block = world.getBlock(pos.x, pos.y, pos.z);
    //         if (block.isAir(world, pos.x, pos.y, pos.z)) continue; // skip double breaking

    //         int meta = world.getBlockMetadata(pos.x, pos.y, pos.z);
    //         if (!PlaceableStack.isPlaceable(block, meta)) continue;

    //         if (isSurvival) {

    //         }
            
    //         world.setBlockToAir(pos.x, pos.y, pos.z);
    //         blocksBroken++;
    //     }

    //     return blocksBroken;
    // }

    // public static int destroy(World world, EntityPlayer player, BlockPos position) {
    //     List<BlockPos> list = new ArrayList<>();
    //     list.add(position);
    //     return destroy(world, player, list);
    // }

    public static BlockPos getFinalPos(EntityPlayer player, BlockPos from, Vec3 pos) {
        return getFinalPos(player, from, pos, false, false, false);
    }

    public static BlockPos getFinalPos(EntityPlayer player, BlockPos from, Vec3 pos, Dimension skip) {
        return getFinalPos(player, from, pos, skip == Dimension.X, skip == Dimension.Y, skip == Dimension.Z);
    }

    public static BlockPos getFinalPos(EntityPlayer player, BlockPos from, Vec3 pos, boolean skipX, boolean skipY, boolean skipZ) {
        if (player.isSneaking()) {
            BlockPos to = BlockPos.containing(pos);
            BlockPos offset = to.subtract(from);

            int absX = Math.abs(offset.x);
            int absY = Math.abs(offset.y);
            int absZ = Math.abs(offset.z);
            int signX = offset.x < 0 ? -1 : 1;
            int signY = offset.y < 0 ? -1 : 1;
            int signZ = offset.z < 0 ? -1 : 1;

            int max = Math.max(Math.max(absX, absY), absZ);

            return from.add(skipX ? offset.x : max * signX, skipY ? offset.y : max * signY, skipZ ? offset.z : max * signZ);
        }

        return BlockPos.containing(pos);
    }

    public abstract void render(ItemStack stack, World world, EntityPlayer player, float partialTicks);

    public static void updateHighlight(BlockPos from, BlockPos to, int count) {
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        List<String> values = new ArrayList<>();
        if (min.x != max.x) values.add("" + (max.x - min.x + 1));
        if (min.y != max.y) values.add("" + (max.y - min.y + 1));
        if (min.z != max.z) values.add("" + (max.z - min.z + 1));

        highlightTitle = count + " (" + (!values.isEmpty() ? String.join("x", values) : "1") + ")";
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
