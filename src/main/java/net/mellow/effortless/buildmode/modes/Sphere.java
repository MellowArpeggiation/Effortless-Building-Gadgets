package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.List;

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

public class Sphere extends ThreeClicksBuildMode {

    @Override
    public BlockPos getMid(ItemStack stack, World world, EntityPlayer player, BlockPos pos0) {
        return Floor.findFloor(player, pos0, true);
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos pos0) {
        BlockPos pos1 = getMid(stack, world, player, pos0);
        if (pos1 == null) return null;

        BuildingAction start = ItemBuildingGadget.getAction(stack, BuildingOption.CIRCLE_START);
        BuildingAction fill = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
        List<BlockPos> blocks = Circle.getCircleBlocks(pos0, pos1, start == BuildingAction.CIRCLE_START_CORNER, fill == BuildingAction.FULL);
        return new ConstructionSet(blocks, pos0, pos1);
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos pos0, BlockPos pos1) {
        BlockPos pos2 = Cube.findHeight(player, pos0, pos1, true);
        if (pos2 == null) return null;

        BuildingAction start = ItemBuildingGadget.getAction(stack, BuildingOption.CIRCLE_START);
        BuildingAction fill = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
        List<BlockPos> blocks = getSphereBlocks(pos0, pos1, pos2, start == BuildingAction.CIRCLE_START_CORNER, fill == BuildingAction.FULL);
        return new ConstructionSet(blocks, pos0, pos2);
    }

    public static List<BlockPos> getSphereBlocks(BlockPos from, BlockPos mid, BlockPos to, boolean fromCorner, boolean fill) {
        List<BlockPos> list = new ArrayList<>();

        double centerX = from.x;
        double centerY = from.y;
        double centerZ = from.z;

        //Adjust for CIRCLE_START
        if (fromCorner) {
            centerX = from.x + (mid.x - from.x) / 2f;
            centerY = from.y + (to.y - from.y) / 2f;
            centerZ = from.z + (mid.z - from.z) / 2f;
        } else {
            from.x = (int) (centerX - (mid.x - centerX));
            from.y = (int) (centerY - (to.y - centerY));
            from.z = (int) (centerZ - (mid.z - centerZ));
        }

        double radiusX = Math.abs(mid.x - centerX);
        double radiusY = Math.abs(to.y - centerY);
        double radiusZ = Math.abs(mid.z - centerZ);

        if (fill) {
            addSphereBlocks(list, from.x, from.y, from.z, to.x, to.y, to.z, centerX, centerY, centerZ, radiusX, radiusY, radiusZ);
        } else {
            addHollowSphereBlocks(list, from.x, from.y, from.z, to.x, to.y, to.z, centerX, centerY, centerZ, radiusX, radiusY, radiusZ);
        }

        return list;
    }

    public static void addSphereBlocks(List<BlockPos> list, int x1, int y1, int z1, int x2, int y2, int z2,
                                       double centerX, double centerY, double centerZ, double radiusX, double radiusY, double radiusZ) {
        for (int l = x1; x1 < x2 ? l <= x2 : l >= x2; l += x1 < x2 ? 1 : -1) {
            for (int n = z1; z1 < z2 ? n <= z2 : n >= z2; n += z1 < z2 ? 1 : -1) {
                for (int m = y1; y1 < y2 ? m <= y2 : m >= y2; m += y1 < y2 ? 1 : -1) {
                    double distance = distance(l, m, n, centerX, centerY, centerZ);
                    double radius = calculateSpheroidRadius(centerX, centerY, centerZ, radiusX, radiusY, radiusZ, l, m, n);
                    if (distance < radius + 0.4f)
                        list.add(new BlockPos(l, m, n));
                }
            }
        }
    }

    public static void addHollowSphereBlocks(List<BlockPos> list, int x1, int y1, int z1, int x2, int y2, int z2,
                                             double centerX, double centerY, double centerZ, double radiusX, double radiusY, double radiusZ) {
        for (int l = x1; x1 < x2 ? l <= x2 : l >= x2; l += x1 < x2 ? 1 : -1) {
            for (int n = z1; z1 < z2 ? n <= z2 : n >= z2; n += z1 < z2 ? 1 : -1) {
                for (int m = y1; y1 < y2 ? m <= y2 : m >= y2; m += y1 < y2 ? 1 : -1) {
                    double distance = distance(l, m, n, centerX, centerY, centerZ);
                    double radius = calculateSpheroidRadius(centerX, centerY, centerZ, radiusX, radiusY, radiusZ, l, m, n);
                    if (distance < radius + 0.4f && distance > radius - 0.6f)
                        list.add(new BlockPos(l, m, n));
                }
            }
        }
    }

    private static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
    }

    public static double calculateSpheroidRadius(double centerX, double centerY, double centerZ, double radiusX, double radiusY, double radiusZ, int x, int y, int z) {
        //Twice ellipse radius
        double radiusXZ = Circle.calculateEllipseRadius(centerX, centerZ, radiusX, radiusZ, x, z);

        //TODO project x to plane
        return Circle.calculateEllipseRadius(centerX, centerY, radiusXZ, radiusY, x, y);
    }
    
}
