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

public class DiagonalWall extends ThreeClicksBuildMode {

    @Override
    public BlockPos getMid(ItemStack stack, World world, EntityPlayer player, BlockPos pos0) {
        return Floor.findFloor(player, pos0, true);
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos pos0) {
        BlockPos pos1 = getMid(stack, world, player, pos0);
        if (pos1 == null) return null;

        return new ConstructionSet(DiagonalLine.getDiagonalLineBlocks(pos0, pos1, 1));
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos pos0, BlockPos pos1) {
        BlockPos pos2 = Cube.findHeight(player, pos0, pos1, true);
        if (pos2 == null) return null;

        BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
        return new ConstructionSet(getDiagonalWallBlocks(pos0, pos1, pos2, fillMode == BuildingAction.FULL));
    }

    //Add diagonal wall from first to second
    public static Set<BlockPos> getDiagonalWallBlocks(BlockPos from, BlockPos mid, BlockPos to, boolean fill) {
        Set<BlockPos> set = new HashSet<>();

        //Get diagonal line blocks
        Set<BlockPos> diagonalLineBlocks = DiagonalLine.getDiagonalLineBlocks(from, mid, 1);

        int lowest = Math.min(from.y, to.y);
        int highest = Math.max(from.y, to.y);

        if (fill) {
            //Copy diagonal line on y axis
            for (BlockPos blockPos : diagonalLineBlocks) {
                for (int y = lowest; y <= highest; y++) {
                    set.add(new BlockPos(blockPos.x, y, blockPos.z));
                }
            }
        } else {
            // Place bottom and top
            for (BlockPos blockPos : diagonalLineBlocks) {
                set.add(new BlockPos(blockPos.x, lowest, blockPos.z));
                set.add(new BlockPos(blockPos.x, highest, blockPos.z));
            }

            // Place caps
            for (int y = lowest; y <= highest; y++) {
                set.add(new BlockPos(from.x, y, from.z));
                set.add(new BlockPos(to.x, y, to.z));
            }
        }

        return set;
    }
    
}
