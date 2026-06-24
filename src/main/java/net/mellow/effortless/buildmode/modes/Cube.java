package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.List;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.blocks.BlockPos.Dimension;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.ThreeClicksBuildMode;
import net.mellow.effortless.buildmode.VoxelRenderer;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class Cube extends ThreeClicksBuildMode {

    @Override
    public BlockPos addMid(ItemStack stack, World world, EntityPlayer player, BlockPos pos0) {
        return Floor.findFloor(player, pos0, true);
    }

    @Override
    public int add(ItemStack stack, PlaceableStack selected, World world, EntityPlayer player, BlockPos pos0, BlockPos pos1) {
        BlockPos pos2 = findHeight(player, pos0, pos1, true);
        if (pos2 == null) return 0;

        BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.CUBE_FILL);
        List<BlockPos> blocks = getCubeBlocks(pos0, pos2, fillMode);
        return build(world, player, selected, blocks, false);
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, BlockPos pos0, float partialTicks) {
        BlockPos pos1 = Floor.findFloor(player, pos0, true);
        if (pos1 == null) return;

        BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.CUBE_FILL);
        List<BlockPos> blocks = getFloorBlocksUsingCubeFill(pos0, pos1, fillMode);
        VoxelRenderer.renderBlocks(blocks, player, partialTicks);

        updateHighlight(pos0, pos1, blocks.size());
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, BlockPos pos0, BlockPos pos1, float partialTicks) {
        BlockPos pos2 = findHeight(player, pos0, pos1, true);
        if (pos2 == null) return;

        BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.CUBE_FILL);
        List<BlockPos> blocks = getCubeBlocks(pos0, pos2, fillMode);
        VoxelRenderer.renderBlocks(blocks, player, partialTicks);

        updateHighlight(pos0, pos2, blocks.size());
    }

    public static BlockPos findHeight(EntityPlayer player, BlockPos firstPos, BlockPos secondPos, boolean skipRaytrace) {
        return findLength(player, firstPos, secondPos, Dimension.Y, skipRaytrace);
    }

    public static BlockPos findLength(EntityPlayer player, BlockPos firstPos, BlockPos secondPos, Dimension dimension, boolean skipRaytrace) {
        Vec3 look = BuildModes.getPlayerLookVec(player);
        Vec3 start = BuildModes.getPlayerPos(player);

        List<LengthCriteria> criteriaList = new ArrayList<>(3);

        //X
        if (dimension != Dimension.X) { 
            Vec3 xBound = BuildModes.findXBound(secondPos.x, start, look);
            criteriaList.add(new LengthCriteria(xBound, secondPos, start, dimension));
        }

        //Y
        if (dimension != Dimension.Y) {
            Vec3 yBound = BuildModes.findXBound(secondPos.y, start, look);
            criteriaList.add(new LengthCriteria(yBound, secondPos, start, dimension));
        }

        //Z
        if (dimension != Dimension.Z) { 
            Vec3 zBound = BuildModes.findZBound(secondPos.z, start, look);
            criteriaList.add(new LengthCriteria(zBound, secondPos, start, dimension));
        }

        //Remove invalid criteria
        // int reach = CapabilityHandler.getBuildModeReach(player);
        int reach = 32;
        criteriaList.removeIf(criteria -> !criteria.isValid(start, look, reach, player, skipRaytrace));

        //If none are valid, return empty list of blocks
        if (criteriaList.isEmpty()) return null;

        //If only 1 is valid, choose that one
        LengthCriteria selected = criteriaList.get(0);

        //If multiple are valid, choose based on criteria
        if (criteriaList.size() > 1) {
            //Select the one that is closest (from wall position to its line counterpart)
            for (int i = 1; i < criteriaList.size(); i++) {
                LengthCriteria criteria = criteriaList.get(i);
                if (criteria.distToLineSq < 2.0 && selected.distToLineSq < 2.0) {
                    //Both very close to line, choose closest to player
                    if (criteria.distToPlayerSq < selected.distToPlayerSq)
                        selected = criteria;
                } else {
                    //Pick closest to line
                    if (criteria.distToLineSq < selected.distToLineSq)
                        selected = criteria;
                }
            }
        }

        return getFinalPos(player, firstPos, selected.lineBound, dimension != Dimension.X, dimension != Dimension.Y, dimension != Dimension.Z);
    }

    public static List<BlockPos> getFloorBlocksUsingCubeFill(BlockPos from, BlockPos to, BuildingAction fill) {
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        List<BlockPos> list = new ArrayList<>();

        if (fill == BuildingAction.CUBE_SKELETON) {
            Floor.addHollowFloorBlocks(list, min.x, max.x, min.y, min.z, max.z);
        } else {
            Floor.addFloorBlocks(list, min.x, max.x, min.y, min.z, max.z);
        }

        return list;
    }

    public static List<BlockPos> getCubeBlocks(BlockPos from, BlockPos to, BuildingAction fill) {
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        List<BlockPos> list = new ArrayList<>();

        switch (fill) {
            case CUBE_FULL:
                addCubeBlocks(list, min.x, max.x, min.y, max.y, min.z, max.z);
                break;
            case CUBE_HOLLOW:
                addHollowCubeBlocks(list, min.x, max.x, min.y, max.y, min.z, max.z);
                break;
            case CUBE_SKELETON:
                addSkeletonCubeBlocks(list, min.x, max.x, min.y, max.y, min.z, max.z);
                break;
            default: break;
        }

        return list;
    }

    public static void addCubeBlocks(List<BlockPos> list, int x1, int x2, int y1, int y2, int z1, int z2) {
        for (int x = x1; x <= x2; x++)
        for (int y = y1; y <= y2; y++)
        for (int z = z1; z <= z2; z++) {
            list.add(new BlockPos(x, y, z));
        }
    }

    public static void addHollowCubeBlocks(List<BlockPos> list, int x1, int x2, int y1, int y2, int z1, int z2) {
        Wall.addXWallBlocks(list, x1, y1, y2, z1, z2);
        Wall.addXWallBlocks(list, x2, y1, y2, z1, z2);

        Wall.addZWallBlocks(list, x1, x2, y1, y2, z1);
        Wall.addZWallBlocks(list, x1, x2, y1, y2, z2);

        Floor.addFloorBlocks(list, x1, x2, y1, z1, z2);
        Floor.addFloorBlocks(list, x1, x2, y2, z1, z2);
    }

    public static void addSkeletonCubeBlocks(List<BlockPos> list, int x1, int x2, int y1, int y2, int z1, int z2) {
        Line.addXLineBlocks(list, x1, x2, y1, z1);
        Line.addXLineBlocks(list, x1, x2, y1, z2);
        Line.addXLineBlocks(list, x1, x2, y2, z1);
        Line.addXLineBlocks(list, x1, x2, y2, z2);

        Line.addYLineBlocks(list, y1, y2, x1, z1);
        Line.addYLineBlocks(list, y1, y2, x1, z2);
        Line.addYLineBlocks(list, y1, y2, x2, z1);
        Line.addYLineBlocks(list, y1, y2, x2, z2);

        Line.addZLineBlocks(list, z1, z2, x1, y1);
        Line.addZLineBlocks(list, z1, z2, x1, y2);
        Line.addZLineBlocks(list, z1, z2, x2, y1);
        Line.addZLineBlocks(list, z1, z2, x2, y2);
    }

    private static class LengthCriteria {
        Vec3 planeBound;
        Vec3 lineBound;
        double distToLineSq;
        double distToPlayerSq;

        public LengthCriteria(Vec3 planeBound, BlockPos secondPos, Vec3 start, Dimension dimension) {
            this.planeBound = planeBound;
            this.lineBound = toLongestLine(this.planeBound, secondPos, dimension);
            this.distToLineSq = this.lineBound.distanceToSqr(this.planeBound);
            this.distToPlayerSq = this.planeBound.distanceToSqr(start);
        }

        //Make it from a plane into a line, on selected axis only
        private Vec3 toLongestLine(Vec3 boundVec, BlockPos secondPos, Dimension dimension) {
            BlockPos bound = BlockPos.containing(boundVec);
            switch (dimension) {
                case X: return new Vec3(bound.x, secondPos.y, secondPos.z);
                case Z: return new Vec3(secondPos.x, secondPos.y, bound.z);
                default: return new Vec3(secondPos.x, bound.y, secondPos.z);
            }
        }

        //check if its not behind the player and its not too close and not too far
        //also check if raytrace from player to block does not intersect blocks
        public boolean isValid(Vec3 start, Vec3 look, int reach, EntityPlayer player, boolean skipRaytrace) {
            return BuildModes.isCriteriaValid(start, look, reach, player, skipRaytrace, lineBound, planeBound, distToPlayerSq);
        }
    }
    
}
