package net.mellow.effortless.buildmode.modes;

import java.util.HashSet;
import java.util.Set;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.buildmode.ThreeClicksBuildMode;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class SlopeFloor extends ThreeClicksBuildMode {

    @Override
    public BlockPos getMid(ItemStack stack, World world, EntityPlayer player, BlockPos pos0) {
        return Floor.findFloor(player, pos0, true);
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos pos0) {
        BlockPos pos1 = getMid(stack, world, player, pos0);
        if (pos1 == null) return null;

        return new ConstructionSet(Floor.getFloorBlocks(pos0, pos1, true), pos0);
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos pos0, BlockPos pos1) {
        BlockPos pos2 = Cube.findHeight(player, pos0, pos1, true);
        if (pos2 == null) return null;

        BuildingAction edge = ItemBuildingGadget.getAction(stack, BuildingOption.RAISED_EDGE);
        return new ConstructionSet(getSlopeFloorBlocks(pos0, pos1, pos2, edge == BuildingAction.SHORT_EDGE), pos0);
    }

    //Add slope floor from first to second
    public static Set<BlockPos> getSlopeFloorBlocks(BlockPos from, BlockPos mid, BlockPos to, boolean shortEdge) {
        Set<BlockPos> set = new HashSet<>();

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
            Set<BlockPos> diagonalLineBlocks = DiagonalLine.getDiagonalLineBlocks(from, new BlockPos(mid.x, to.y, from.z), 1);

            //Limit amount of blocks we can place
            int lowest = Math.min(from.z, mid.z);
            int highest = Math.max(from.z, mid.z);

            //Copy diagonal line on x axis
            for (BlockPos blockPos : diagonalLineBlocks) {
                for (int z = lowest; z <= highest; z++) {
                    set.add(new BlockPos(blockPos.x, blockPos.y, z));
                }
            }

        } else {
            //Along Z goes up

            //Get diagonal line blocks
            Set<BlockPos> diagonalLineBlocks = DiagonalLine.getDiagonalLineBlocks(from, new BlockPos(from.x, to.y, mid.z), 1f);

            //Limit amount of blocks we can place
            int lowest = Math.min(from.x, mid.x);
            int highest = Math.max(from.x, mid.x);

            //Copy diagonal line on x axis
            for (BlockPos blockPos : diagonalLineBlocks) {
                for (int x = lowest; x <= highest; x++) {
                    set.add(new BlockPos(x, blockPos.y, blockPos.z));
                }
            }
        }

        return set;
    }
    
}
