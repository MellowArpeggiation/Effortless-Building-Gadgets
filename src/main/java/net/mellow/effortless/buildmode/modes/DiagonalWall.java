package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.List;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.buildmode.ThreeClicksBuildMode;
import net.mellow.effortless.buildmode.VoxelRenderer;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class DiagonalWall extends ThreeClicksBuildMode {

    @Override
    public BlockPos addMid(ItemStack stack, World world, EntityPlayer player, BlockPos pos0) {
        return Floor.findFloor(player, pos0, true);
    }

    @Override
    public int add(ItemStack stack, PlaceableStack selected, World world, EntityPlayer player, BlockPos pos0, BlockPos pos1) {
        BlockPos pos2 = Cube.findHeight(player, pos0, pos1, true);
        if (pos2 == null) return 0;

        BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
        List<BlockPos> blocks = getDiagonalWallBlocks(pos0, pos1, pos2, fillMode == BuildingAction.FULL);

        return build(world, player, selected, blocks, false);
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, BlockPos pos0, float partialTicks) {
        BlockPos pos1 = Floor.findFloor(player, pos0, true);
        if (pos1 == null) return;

        List<BlockPos> blocks = DiagonalLine.getDiagonalLineBlocks(pos0, pos1, 1);
        VoxelRenderer.renderBlocks(blocks, player, partialTicks);

        updateHighlight(pos0, pos1, blocks.size());
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, BlockPos pos0, BlockPos pos1, float partialTicks) {
        BlockPos pos2 = Cube.findHeight(player, pos0, pos1, true);
        if (pos2 == null) return;

        BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
        List<BlockPos> blocks = getDiagonalWallBlocks(pos0, pos1, pos2, fillMode == BuildingAction.FULL);
        VoxelRenderer.renderBlocks(blocks, player, partialTicks);

        updateHighlight(pos0, pos2, blocks.size());
    }

    //Add diagonal wall from first to second
    public static List<BlockPos> getDiagonalWallBlocks(BlockPos from, BlockPos mid, BlockPos to, boolean fill) {
        List<BlockPos> list = new ArrayList<>();

        //Get diagonal line blocks
        List<BlockPos> diagonalLineBlocks = DiagonalLine.getDiagonalLineBlocks(from, mid, 1);

        int lowest = Math.min(from.y, to.y);
        int highest = Math.max(from.y, to.y);

        if (fill) {
            //Copy diagonal line on y axis
            for (int y = lowest; y <= highest; y++) {
                for (BlockPos blockPos : diagonalLineBlocks) {
                    list.add(new BlockPos(blockPos.x, y, blockPos.z));
                }
            }
        } else {
            // Place bottom and top
            for (BlockPos blockPos : diagonalLineBlocks) {
                list.add(new BlockPos(blockPos.x, lowest, blockPos.z));
                list.add(new BlockPos(blockPos.x, highest, blockPos.z));
            }

            // Place caps
            for (int y = lowest; y <= highest; y++) {
                list.add(new BlockPos(from.x, y, from.z));
                list.add(new BlockPos(to.x, y, to.z));
            }
        }

        return list;
    }
    
}
