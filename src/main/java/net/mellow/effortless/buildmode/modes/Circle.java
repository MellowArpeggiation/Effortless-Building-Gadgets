package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.List;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.BlockPos.Dimension;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.buildmode.TwoClicksBuildMode;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class Circle extends TwoClicksBuildMode {
    
    // TODO: highlight text fix!!
    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos from) {
        BuildingAction tilt = ItemBuildingGadget.getAction(stack, BuildingOption.CIRCLE_TILT);

        BlockPos to = tilt == BuildingAction.CIRCLE_VERTICAL
            ? Floor.findFloor(player, from, true)
            : Wall.findWall(player, from, true);
        if (to == null) return null;

        BuildingAction start = ItemBuildingGadget.getAction(stack, BuildingOption.CIRCLE_START);
        BuildingAction fill = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);

        return new ConstructionSet(getCircleBlocks(from, to, start == BuildingAction.CIRCLE_START_CORNER, fill == BuildingAction.FULL), from, to);
    }

    public static List<BlockPos> getCircleBlocks(BlockPos from, BlockPos to, boolean fromCorner, boolean fill) {
        // if line, just do line
        if (from.x == to.x && from.y == to.y) return Line.getLineBlocks(from, to);
        if (from.x == to.x && from.z == to.z) return Line.getLineBlocks(from, to);
        if (from.y == to.y && from.z == to.z) return Line.getLineBlocks(from, to);

        // swizzle inputs based on circle dimensions
        if (from.y == to.y) return getCircleSwizzled(from, to, fromCorner, fill, Dimension.Y);

        if (from.x == to.x) {
            BlockPos swizzleFrom = new BlockPos(from.y, from.x, from.z);
            BlockPos swizzleTo = new BlockPos(to.y, to.x, to.z);

            return getCircleSwizzled(swizzleFrom, swizzleTo, fromCorner, fill, Dimension.X);
        }

        if (from.z == to.z) {
            BlockPos swizzleFrom = new BlockPos(from.x, from.z, from.y);
            BlockPos swizzleTo = new BlockPos(to.x, to.z, to.y);

            return getCircleSwizzled(swizzleFrom, swizzleTo, fromCorner, fill, Dimension.Z);
        }

        // Invalid circle, just do Y
        return getCircleSwizzled(from, to, fromCorner, fill, Dimension.Y);
    }

    public static List<BlockPos> getCircleSwizzled(BlockPos from, BlockPos to, boolean fromCorner, boolean fill, Dimension swizzle) {
        List<BlockPos> list = new ArrayList<>();

        double centerX = from.x;
        double centerZ = from.z;

        //Adjust for CIRCLE_START
        if (fromCorner) {
            centerX = from.x + (to.x - from.x) / 2f;
            centerZ = from.z + (to.z - from.z) / 2f;
        } else {
            from = new BlockPos((int) (centerX - (to.x - centerX)), from.y, (int) (centerZ - (to.z - centerZ)));
        }

        double radiusX = Math.abs(to.x - centerX);
        double radiusZ = Math.abs(to.z - centerZ);

        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        if (fill)
            addCircleBlocks(list, min, max, centerX, centerZ, radiusX, radiusZ, swizzle);
        else
            addHollowCircleBlocks(list, min, max, centerX, centerZ, radiusX, radiusZ, swizzle);

        return list;
    }

    public static void addCircleBlocks(List<BlockPos> list, BlockPos min, BlockPos max, double centerX, double centerZ, double radiusX, double radiusZ, Dimension swizzle) {
        for (int x = min.x; x <= max.x; x++) {
            for (int z = min.z; z <= max.z; z++) {
                double distance = distance(x, z, centerX, centerZ);
                double radius = calculateEllipseRadius(centerX, centerZ, radiusX, radiusZ, x, z);
                if (distance < radius + 0.4f)
                    addToListSwizzled(list, x, min.y, z, swizzle);
            }
        }
    }

    public static void addHollowCircleBlocks(List<BlockPos> list, BlockPos min, BlockPos max, double centerX, double centerZ, double radiusX, double radiusZ, Dimension swizzle) {
        if (radiusX < 1.25 || radiusZ < 1.25) {
            addCircleBlocks(list, min, max, centerX, centerZ, radiusX, radiusZ, swizzle);
            return;
        }

        for (int x = min.x; x <= max.x; x++) {
            for (int z = min.z; z <= max.z; z++) {
                double distance = distance(x, z, centerX, centerZ);
                double radiusOuter = calculateEllipseRadius(centerX, centerZ, radiusX, radiusZ, x, z);
                double radiusInner = calculateEllipseRadius(centerX, centerZ, radiusX - 1, radiusZ - 1, x, z);
                radiusInner = Math.min(radiusInner, radiusOuter - 1);
                if (distance < radiusOuter + 0.4f && distance > radiusInner + 0.4f)
                    addToListSwizzled(list, x, min.y, z, swizzle);
            }
        }
    }

    private static void addToListSwizzled(List<BlockPos> list, int x, int y, int z, Dimension swizzle) {
        switch (swizzle) {
            case Y: list.add(new BlockPos(x, y, z)); break;
            case X: list.add(new BlockPos(y, x, z)); break;
            case Z: list.add(new BlockPos(x, z, y)); break;
        }
    }

    private static double distance(double x1, double z1, double x2, double z2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
    }

    public static double calculateEllipseRadius(double centerX, double centerZ, double radiusX, double radiusZ, int x, int z) {
        double theta = Math.atan2(z - centerZ, x - centerX);
        return calculateEllipseRadius(centerX, centerZ, radiusX, radiusZ, theta);
    }

    public static double calculateEllipseRadius(double centerX, double centerZ, double radiusX, double radiusZ, double theta) {
        //https://math.stackexchange.com/questions/432902/how-to-get-the-radius-of-an-ellipse-at-a-specific-angle-by-knowing-its-semi-majo
        double part1 = radiusX * radiusX * Math.sin(theta) * Math.sin(theta);
        double part2 = radiusZ * radiusZ * Math.cos(theta) * Math.cos(theta);
        return radiusX * radiusZ / Math.sqrt(part1 + part2);
    }
    
}
