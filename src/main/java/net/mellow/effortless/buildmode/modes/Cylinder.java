package net.mellow.effortless.buildmode.modes;

import java.util.HashSet;
import java.util.Set;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.BlockPos.Dimension;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.buildmode.ThreeClicksBuildMode;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class Cylinder extends ThreeClicksBuildMode {

    @Override
    public BlockPos getMid(ItemStack stack, World world, EntityPlayer player, BlockPos pos0) {
        BuildingAction tilt = ItemBuildingGadget.getAction(stack, BuildingOption.CIRCLE_TILT);

        return tilt == BuildingAction.CIRCLE_VERTICAL
            ? Floor.findFloor(player, pos0, true)
            : Wall.findWall(player, pos0, true);
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos pos0) {
        BlockPos pos1 = getMid(stack, world, player, pos0);
        if (pos1 == null) return null;

        BuildingAction start = ItemBuildingGadget.getAction(stack, BuildingOption.CIRCLE_START);
        BuildingAction fill = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
        return new ConstructionSet(Circle.getCircleBlocks(pos0, pos1, start == BuildingAction.CIRCLE_START_CORNER, fill == BuildingAction.FULL), pos0);
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos pos0, BlockPos pos1) {
        BuildingAction tilt = ItemBuildingGadget.getAction(stack, BuildingOption.CIRCLE_TILT);

        BlockPos pos2 = tilt == BuildingAction.CIRCLE_VERTICAL
            ? Cube.findHeight(player, pos0, pos1, true)
            : Cube.findLength(player, pos0, pos1, pos0.x == pos1.x ? Dimension.X : Dimension.Z, true);
        if (pos2 == null) return null;

        BuildingAction start = ItemBuildingGadget.getAction(stack, BuildingOption.CIRCLE_START);
        BuildingAction fill = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
        return new ConstructionSet(getCylinderBlocks(pos0, pos1, pos2, start == BuildingAction.CIRCLE_START_CORNER, fill == BuildingAction.FULL), pos0);
    }

    public static Set<BlockPos> getCylinderBlocks(BlockPos from, BlockPos mid, BlockPos to, boolean fromCorner, boolean fill) {
        Set<BlockPos> set = new HashSet<>();

        //Get circle blocks (using CIRCLE_START and FILL options built-in)
        Set<BlockPos> circleBlocks = Circle.getCircleBlocks(from, mid, fromCorner, fill);

        if (from.y == mid.y) {
            int lowest = Math.min(from.y, to.y);
            int highest = Math.max(from.y, to.y);
    
            //Copy circle on y axis
            for (BlockPos blockPos : circleBlocks) {
                for (int y = lowest; y <= highest; y++) {
                    set.add(new BlockPos(blockPos.x, y, blockPos.z));
                }
            }
        } else if (from.x == mid.x) {
            int lowest = Math.min(from.x, to.x);
            int highest = Math.max(from.x, to.x);
    
            //Copy circle on x axis
            for (BlockPos blockPos : circleBlocks) {
                for (int x = lowest; x <= highest; x++) {
                    set.add(new BlockPos(x, blockPos.y, blockPos.z));
                }
            }
        } else {
            int lowest = Math.min(from.z, to.z);
            int highest = Math.max(from.z, to.z);
    
            //Copy circle on z axis
            for (BlockPos blockPos : circleBlocks) {
                for (int z = lowest; z <= highest; z++) {
                    set.add(new BlockPos(blockPos.x, blockPos.y, z));
                }
            }
        }
    
        return set;
    }
    
}
