package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.List;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class DiagonalLine extends BaseBuildMode {

    @Override
    public boolean click(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        BuildingAction type = ItemBuildingGadget.getAction(stack, BuildingOption.LINE_DRAW);
        if (type == BuildingAction.LINE_CONSTRUCT) return clickConstruct(stack, world, player, mop);
        return clickPointToPoint(stack, world, player, mop);
    }

    private boolean clickConstruct(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));
        if (pos1 != null) return true;

        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (pos0 != null) {
            pos1 = Floor.findFloor(player, pos0, true);
            if (pos1 == null) return false;

            stack.stackTagCompound.setTag("pos1", pos1.save());
            
            return false;
        }

        pos0 = BlockPos.fromRaycastReplaceable(world, mop);
        if (pos0 == null) return false;        
        stack.stackTagCompound.setTag("pos0", pos0.save());

        return false;
    }

    private boolean clickPointToPoint(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (from != null) return true;

        from = BlockPos.fromRaycastReplaceable(world, mop);
        if (from == null) return false;
        stack.stackTagCompound.setTag("pos0", from.save());

        return false;
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        BuildingAction type = ItemBuildingGadget.getAction(stack, BuildingOption.LINE_DRAW);
        if (type == BuildingAction.LINE_CONSTRUCT) return getBlocksConstruct(stack, world, player, mop);
        return getBlocksPointToPoint(stack, world, player, mop);
    }

    private ConstructionSet getBlocksConstruct(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (pos0 == null) return null;

        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));
        if (pos1 == null) {
            pos1 = Floor.findFloor(player, pos0, true);
            if (pos1 == null) return null;

            return new ConstructionSet(DiagonalLine.getDiagonalLineBlocks(pos0, pos1, 10), pos0, pos1);
        }

        BlockPos pos2 = Cube.findHeight(player, pos0, pos1, true);
        if (pos2 == null) return null;

        return new ConstructionSet(DiagonalLine.getDiagonalLineBlocks(pos0, pos2, 10), pos0, pos2);
    }

    private ConstructionSet getBlocksPointToPoint(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (pos0 == null) return null;

        BlockPos pos2 = BlockPos.fromRaycastReplaceable(world, mop);
        if (pos2 == null) return null;
        return new ConstructionSet(DiagonalLine.getDiagonalLineBlocks(pos0, pos2, 10), pos0, pos2);
    }

    @Override
    public boolean clear(ItemStack stack) {
        boolean didClear = stack.stackTagCompound.hasKey("pos0");
        stack.stackTagCompound.removeTag("pos0");
        stack.stackTagCompound.removeTag("pos1");
        return didClear;
    }

    @Override
    public boolean isPlacing(ItemStack stack) {
        return stack.stackTagCompound.hasKey("pos0");
    }

    //Add diagonal line from first to second
    public static List<BlockPos> getDiagonalLineBlocks(BlockPos from, BlockPos to, float sampleMultiplier) {
        List<BlockPos> list = new ArrayList<>();

        Vec3 first = Vec3.atCenterOf(from);
        Vec3 second = Vec3.atCenterOf(to);

        int iterations = (int) Math.ceil(first.distanceTo(second) * sampleMultiplier);
        for (double t = 0; t <= 1.0; t += 1.0 / iterations) {
            Vec3 lerp = first.add(second.subtract(first).scale(t));
            BlockPos candidate = BlockPos.containing(lerp);
            //Only add if not equal to the last in the list
            if (list.isEmpty() || !list.get(list.size() - 1).equals(candidate))
                list.add(candidate);
        }

        return list;
    }
    
}
