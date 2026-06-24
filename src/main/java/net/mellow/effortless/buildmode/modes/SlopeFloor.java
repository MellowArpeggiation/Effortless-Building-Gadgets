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

public class SlopeFloor extends ThreeClicksBuildMode {

    @Override
    public BlockPos addMid(ItemStack stack, World world, EntityPlayer player, BlockPos pos0) {
        return Floor.findFloor(player, pos0, true);
    }

    @Override
    public int add(ItemStack stack, PlaceableStack selected, World world, EntityPlayer player, BlockPos pos0, BlockPos pos1) {
        BlockPos pos2 = Cube.findHeight(player, pos0, pos1, true);
        if (pos2 == null) return 0;

        BuildingAction edge = ItemBuildingGadget.getAction(stack, BuildingOption.RAISED_EDGE);
        List<BlockPos> blocks = getSlopeFloorBlocks(pos0, pos1, pos2, edge == BuildingAction.SHORT_EDGE);

        return build(world, player, selected, blocks, false);
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, BlockPos pos0, float partialTicks) {
        BlockPos pos1 = Floor.findFloor(player, pos0, true);
        if (pos1 == null) return;

        List<BlockPos> blocks = Floor.getFloorBlocks(pos0, pos1, true);
        VoxelRenderer.renderBlocks(blocks, player, partialTicks);

        updateHighlight(pos0, pos1, blocks.size());
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, BlockPos pos0, BlockPos pos1, float partialTicks) {
        BlockPos pos2 = Cube.findHeight(player, pos0, pos1, true);
        if (pos2 == null) return;

        BuildingAction edge = ItemBuildingGadget.getAction(stack, BuildingOption.RAISED_EDGE);
        List<BlockPos> blocks = getSlopeFloorBlocks(pos0, pos1, pos2, edge == BuildingAction.SHORT_EDGE);
        VoxelRenderer.renderBlocks(blocks, player, partialTicks);

        updateHighlight(pos0, pos2, blocks.size());
    }

    //Add slope floor from first to second
    public static List<BlockPos> getSlopeFloorBlocks(BlockPos from, BlockPos mid, BlockPos to, boolean shortEdge) {
        List<BlockPos> list = new ArrayList<>();

        //Determine whether to use x or z axis to slope up
        boolean onXAxis = true;

        int xLength = Math.abs(mid.x - from.x);
        int zLength = Math.abs(mid.z - from.z);

        if (shortEdge) {
            //Slope along short edge
            if (zLength > xLength) onXAxis = false;
        } else {
            //Slope along long edge
            if (zLength <= xLength) onXAxis = false;
        }

        if (onXAxis) {
            //Along X goes up

            //Get diagonal line blocks
            List<BlockPos> diagonalLineBlocks = DiagonalLine.getDiagonalLineBlocks(from, new BlockPos(mid.x, to.y, from.z), 1);

            //Limit amount of blocks we can place
            int lowest = Math.min(from.z, mid.z);
            int highest = Math.max(from.z, mid.z);

            //Copy diagonal line on x axis
            for (int z = lowest; z <= highest; z++) {
                for (BlockPos blockPos : diagonalLineBlocks) {
                    list.add(new BlockPos(blockPos.x, blockPos.y, z));
                }
            }

        } else {
            //Along Z goes up

            //Get diagonal line blocks
            List<BlockPos> diagonalLineBlocks = DiagonalLine.getDiagonalLineBlocks(from, new BlockPos(from.x, to.y, mid.z), 1f);

            //Limit amount of blocks we can place
            int lowest = Math.min(from.x, mid.x);
            int highest = Math.max(from.x, mid.x);

            //Copy diagonal line on x axis
            for (int x = lowest; x <= highest; x++) {
                for (BlockPos blockPos : diagonalLineBlocks) {
                    list.add(new BlockPos(x, blockPos.y, blockPos.z));
                }
            }
        }

        return list;
    }
    
}
