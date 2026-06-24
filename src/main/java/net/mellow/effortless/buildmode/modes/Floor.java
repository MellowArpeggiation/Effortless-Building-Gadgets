package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.List;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.blocks.BlockPos.Dimension;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.TwoClicksBuildMode;
import net.mellow.effortless.buildmode.VoxelRenderer;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class Floor extends TwoClicksBuildMode {

    @Override
    public int add(ItemStack stack, PlaceableStack selected, World world, EntityPlayer player, BlockPos from) {
        BlockPos to = findFloor(player, from, true);
        if (to == null) return 0;
        
        BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
        List<BlockPos> blocks = getFloorBlocks(from, to, fillMode == BuildingAction.FULL);
        return build(world, player, selected, blocks, false);
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, BlockPos from, float partialTicks) {
        BlockPos to = findFloor(player, from, true);
        if (to == null) return;
        
        BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
        List<BlockPos> blocks = getFloorBlocks(from, to, fillMode == BuildingAction.FULL);
        VoxelRenderer.renderBlocks(blocks, player, partialTicks);

        updateHighlight(from, to);
    }

    public static BlockPos findFloor(EntityPlayer player, BlockPos firstPos, boolean skipRaytrace) {
        Vec3 look = BuildModes.getPlayerLookVec(player);
        Vec3 start = BuildModes.getPlayerPos(player);

        List<Criteria> criteriaList = new ArrayList<>(3);

        //Y
        Vec3 yBound = BuildModes.findYBound(firstPos.y, start, look);
        criteriaList.add(new Criteria(yBound, start));

        //Remove invalid criteria
        // int reach = CapabilityHandler.getBuildModeReach(player);
        int reach = 32;
        criteriaList.removeIf(criteria -> !criteria.isValid(start, look, reach, player, skipRaytrace));

        //If none are valid, return empty list of blocks
        if (criteriaList.isEmpty()) return null;

        //Then only 1 can be valid, return that one
        Criteria selected = criteriaList.get(0);

        return getFinalPos(player, firstPos, selected.planeBound, Dimension.Y);
    }

    public static List<BlockPos> getFloorBlocks(BlockPos from, BlockPos to, boolean fill) {
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        List<BlockPos> list = new ArrayList<>();

        if (fill) {
            addFloorBlocks(list, min.x, max.x, min.y, min.z, max.z);
        } else {
            addHollowFloorBlocks(list, min.x, max.x, min.y, min.z, max.z);
        }

        return list;
    }

    public static void addFloorBlocks(List<BlockPos> list, int x1, int x2, int y, int z1, int z2) {
        for (int x = x1; x <= x2; x++) {
        for (int z = z1; z <= z2; z++)
            list.add(new BlockPos(x, y, z));
        }
    }

    public static void addHollowFloorBlocks(List<BlockPos> list, int x1, int x2, int y, int z1, int z2) {
        Line.addXLineBlocks(list, x1, x2, y, z1);
        Line.addXLineBlocks(list, x1, x2, y, z2);
        Line.addZLineBlocks(list, z1, z2, x1, y);
        Line.addZLineBlocks(list, z1, z2, x2, y);
    }

    static class Criteria {
        Vec3 planeBound;
        double distToPlayerSq;

        Criteria(Vec3 planeBound, Vec3 start) {
            this.planeBound = planeBound;
            this.distToPlayerSq = this.planeBound.distanceToSqr(start);
        }

        //check if its not behind the player and its not too close and not too far
        //also check if raytrace from player to block does not intersect blocks
        public boolean isValid(Vec3 start, Vec3 look, int reach, EntityPlayer player, boolean skipRaytrace) {
            return BuildModes.isCriteriaValid(start, look, reach, player, skipRaytrace, planeBound, planeBound, distToPlayerSq);
        }
    }
}
