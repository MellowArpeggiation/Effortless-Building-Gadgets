package net.mellow.effortless.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.mellow.effortless.buildmode.BaseBuildMode.Operation;
import net.mellow.effortless.buildmode.History;
import net.mellow.effortless.buildmode.History.HistoryBlock;
import net.mellow.effortless.buildmode.VoxelRenderer;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public class ConstructionSet {
    
    public final List<BlockPos> positions;
    public final BlockPos from;
    public final BlockPos to;

    public ConstructionSet(Set<BlockPos> positions) {
        this.positions = new ArrayList<>(positions);

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : this.positions) {
            if (pos.x < minX) minX = pos.x;
            if (pos.y < minY) minY = pos.y;
            if (pos.z < minZ) minZ = pos.z;
            if (pos.x > maxX) maxX = pos.x;
            if (pos.y > maxY) maxY = pos.y;
            if (pos.z > maxZ) maxZ = pos.z;
        }

        this.from = new BlockPos(minX, minY, minZ);
        this.to = new BlockPos(maxX, maxY, maxZ);
    }

    public ConstructionSet(BlockPos pos) {
        this.positions = new ArrayList<>();
        this.positions.add(pos);
        this.from = pos;
        this.to = pos;
    }

    public int build(World world, EntityPlayer player, PlaceableStack selected, boolean replaceAny) {
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

    public int destroy(World world, EntityPlayer player) {
        if (world.isRemote) return 0;
        if (positions == null || positions.isEmpty()) return 0;

        Block.SoundType stepSound = null;

        int blocksBroken = 0;

        for (BlockPos pos : positions) {
            Block block = world.getBlock(pos.x, pos.y, pos.z);
            int meta = world.getBlockMetadata(pos.x, pos.y, pos.z);

            if (!PlaceableStack.isPlaceable(block, meta)) continue; // only break blocks we're allowed to

            if (stepSound == null) stepSound = block.stepSound;
            world.setBlockToAir(pos.x, pos.y, pos.z);

            blocksBroken++;
        }

        if (stepSound != null) {
            world.playSoundEffect(player.posX, player.posY, player.posZ, stepSound.getBreakSound(), (stepSound.getVolume() + 1.0F) / 2.0F, stepSound.getPitch() * 0.8F);
        }

        return blocksBroken;
    }

    public void render(EntityPlayer player, float partialTicks, Operation operation, boolean showHighlight) {
        VoxelRenderer.renderBlocks(positions, player, operation, partialTicks);
        if (showHighlight) updateHighlight(from, to, positions.size());
    }

    private static void updateHighlight(BlockPos from, BlockPos to, int count) {
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

    public static String getItemHighlight(ItemStack stack) {
        if (highlightTitle == null) return null;

        String title = highlightTitle;
        highlightTitle = null;
        return title;
    }
    
}
