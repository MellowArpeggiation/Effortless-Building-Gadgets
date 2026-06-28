package net.mellow.effortless.buildmode;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.BlockPos.Dimension;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.blocks.Vec3;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public abstract class BaseBuildMode {

    public static enum Operation {
        PLACE,
        BREAK,
    }

    public abstract boolean clear(ItemStack stack);
    public abstract boolean isPlacing(ItemStack stack);

    // on true, attempt to get placed blocks and commit them to the world
    public abstract boolean click(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, Operation operation);

    // return null if getblocks failed, should ignore clicks and draw nothing
    public abstract ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, Operation operation);

    // put the placeable somewhere safe, will retrieve it upon finishing
    public void savePlaceable(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        if (isPlacing(stack)) return; // only on first click

        BlockPos from = BlockPos.fromRaycastReplaceable(world, mop);
        if (from == null) return;
        PlaceableStack place = PlaceableStack.getPlaceableStack(selected, world, player, from.x, from.y, from.z, mop.sideHit, new Vec3(mop.hitVec));

        stack.stackTagCompound.setTag("place", place.save());
    }

    public PlaceableStack getPlaceable(ItemStack stack) {
        return PlaceableStack.load(stack.stackTagCompound.getCompoundTag("place"));
    }

    public boolean shouldRender(ItemStack stack) {
        return true;
    }

    public boolean showHighlight(ItemStack stack) {
        return true;
    }

    public int reach(ItemStack stack) {
        return 32;
    }

    // return null if this action should not auto-repeat
    public BuildingAction repeatSpeed(ItemStack stack) {
        return null;
    }

    public static BlockPos getFinalPos(EntityPlayer player, BlockPos from, Vec3 pos) {
        return getFinalPos(player, from, pos, false, false, false);
    }

    public static BlockPos getFinalPos(EntityPlayer player, BlockPos from, Vec3 pos, Dimension skip) {
        return getFinalPos(player, from, pos, skip == Dimension.X, skip == Dimension.Y, skip == Dimension.Z);
    }

    public static BlockPos getFinalPos(EntityPlayer player, BlockPos from, Vec3 pos, boolean skipX, boolean skipY, boolean skipZ) {
        if (player.isSneaking()) {
            BlockPos to = BlockPos.containing(pos);
            BlockPos offset = to.subtract(from);

            int absX = Math.abs(offset.x);
            int absY = Math.abs(offset.y);
            int absZ = Math.abs(offset.z);
            int signX = offset.x < 0 ? -1 : 1;
            int signY = offset.y < 0 ? -1 : 1;
            int signZ = offset.z < 0 ? -1 : 1;

            int max = Math.max(Math.max(absX, absY), absZ);

            return from.add(skipX ? offset.x : max * signX, skipY ? offset.y : max * signY, skipZ ? offset.z : max * signZ);
        }

        return BlockPos.containing(pos);
    }

}
