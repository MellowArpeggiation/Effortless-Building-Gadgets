package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.BlockPos.Dimension;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.buildmode.TwoClicksBuildMode;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class Wall extends TwoClicksBuildMode {

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos from) {
        BlockPos to = findWall(player, from, true);
        if (to == null) return null;

        BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
        return new ConstructionSet(getWallBlocks(from, to, fillMode == BuildingAction.FULL), from);
    }

    public static BlockPos findWall(EntityPlayer player, BlockPos firstPos, boolean skipRaytrace) {
        Vec3 look = BuildModes.getPlayerLookVec(player);
        Vec3 start = BuildModes.getPlayerPos(player);

        List<Criteria> criteriaList = new ArrayList<>(3);

        //X
        Vec3 xBound = BuildModes.findXBound(firstPos.x, start, look);
        criteriaList.add(new Criteria(xBound, firstPos, start, look, Dimension.X));

        //Z
        Vec3 zBound = BuildModes.findZBound(firstPos.z, start, look);
        criteriaList.add(new Criteria(zBound, firstPos, start, look, Dimension.Z));

        //Remove invalid criteria
        // int reach = CapabilityHandler.getBuildModeReach(player);
        int reach = 32;
        criteriaList.removeIf(criteria -> !criteria.isValid(start, look, reach, player, skipRaytrace));

        //If none are valid, return empty list of blocks
        if (criteriaList.isEmpty()) return null;

        //If only 1 is valid, choose that one
        Criteria selected = criteriaList.get(0);

        //If multiple are valid, choose based on criteria
        if (criteriaList.size() > 1) {
            //Select the one that is closest
            //Limit the angle to not be too extreme
            for (int i = 1; i < criteriaList.size(); i++) {
                Criteria criteria = criteriaList.get(i);
                if (criteria.distToPlayerSq < selected.distToPlayerSq && Math.abs(criteria.angle) - Math.abs(selected.angle) < 3)
                    selected = criteria;
            }
        }

        return getFinalPos(player, firstPos, selected.planeBound, selected.extendsIn);
    }

    public static Set<BlockPos> getWallBlocks(BlockPos from, BlockPos to, boolean fill) {
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        Set<BlockPos> set = new HashSet<>();

        if (min.x == max.x) {
            if (fill) {
                addXWallBlocks(set, min.x, min.y, max.y, min.z, max.z);
            } else {
                addXHollowWallBlocks(set, min.x, min.y, max.y, min.z, max.z);
            }
        } else {
            if (fill) {
                addZWallBlocks(set, min.x, max.x, min.y, max.y, min.z);
            } else {
                addZHollowWallBlocks(set, min.x, max.x, min.y, max.y, min.z);
            }
        }

        return set;
    }

    public static void addXWallBlocks(Set<BlockPos> list, int x, int y1, int y2, int z1, int z2) {
        for (int z = z1; z <= z2; z++) {
        for (int y = y1; y <= y2; y++)
            list.add(new BlockPos(x, y, z));
        }
    }

    public static void addZWallBlocks(Set<BlockPos> list, int x1, int x2, int y1, int y2, int z) {
        for (int x = x1; x <= x2; x++) {
        for (int y = y1; y <= y2; y++)
            list.add(new BlockPos(x, y, z));
        }
    }

    public static void addXHollowWallBlocks(Set<BlockPos> list, int x, int y1, int y2, int z1, int z2) {
        Line.addZLineBlocks(list, z1, z2, x, y1);
        Line.addZLineBlocks(list, z1, z2, x, y2);
        Line.addYLineBlocks(list, y1, y2, x, z1);
        Line.addYLineBlocks(list, y1, y2, x, z2);
    }

    public static void addZHollowWallBlocks(Set<BlockPos> list, int x1, int x2, int y1, int y2, int z) {
        Line.addXLineBlocks(list, x1, x2, y1, z);
        Line.addXLineBlocks(list, x1, x2, y2, z);
        Line.addYLineBlocks(list, y1, y2, x1, z);
        Line.addYLineBlocks(list, y1, y2, x2, z);
    }

    static class Criteria {
        Vec3 planeBound;
        double distToPlayerSq;
        double angle;
        Dimension extendsIn;

        Criteria(Vec3 planeBound, BlockPos firstPos, Vec3 start, Vec3 look, Dimension extendsIn) {
            this.planeBound = planeBound;
            this.distToPlayerSq = this.planeBound.distanceToSqr(start);
            Vec3 wall = this.planeBound.subtract(Vec3.atLowerCornerOf(firstPos));
            this.angle = wall.x * look.x + wall.z * look.z; //dot product ignoring y (looking up/down should not affect this angle)
            this.extendsIn = extendsIn;
        }

        //check if its not behind the player and its not too close and not too far
        //also check if raytrace from player to block does not intersect blocks
        public boolean isValid(Vec3 start, Vec3 look, int reach, EntityPlayer player, boolean skipRaytrace) {
            return BuildModes.isCriteriaValid(start, look, reach, player, skipRaytrace, planeBound, planeBound, distToPlayerSq);
        }
    }
    
}
