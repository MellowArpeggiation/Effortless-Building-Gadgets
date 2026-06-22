package net.mellow.effortless.buildmode.modes;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.VoxelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class Air extends BaseBuildMode {

    @Override
    public int reach(ItemStack stack) {
        return 6;
    }

    @Override
    public int add(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        if (world.isRemote) return 0;

        if (mop == null) return 0;
        if (mop.typeOfHit == MovingObjectType.MISS) {
            BlockPos pos = new BlockPos(mop.blockX, mop.blockY, mop.blockZ);

            // we gotta figure out which "side" is closest to facing the player
            ForgeDirection facing = ForgeDirection.UNKNOWN;
            if (player.rotationPitch > 45) {
                facing = ForgeDirection.DOWN;
            } else if (player.rotationPitch < 45) {
                facing = ForgeDirection.UP;
            } else {
                int rot = MathHelper.floor_double((double)(player.rotationYaw * 4.0F / 360.0F) + 0.5D) & 3;

                if (rot == 0) facing = ForgeDirection.NORTH;
                if (rot == 1) facing = ForgeDirection.EAST;
                if (rot == 2) facing = ForgeDirection.SOUTH;
                if (rot == 3) facing = ForgeDirection.WEST;
            }

            PlaceableStack place = PlaceableStack.getPlaceableStack(selected, world, player, pos.x, pos.y, pos.z, facing.ordinal(), new Vec3(mop.hitVec));
            
            return build(world, player, place, pos, false);
        } else {
            BlockPos pos = BlockPos.fromRaycastSide(mop);
            if (pos == null) return 0;

            PlaceableStack place = PlaceableStack.getPlaceableStack(selected, world, player, pos.x, pos.y, pos.z, mop.sideHit, new Vec3(mop.hitVec));

            return build(world, player, place, pos, false);
        }
    }

    @Override
    public boolean clear(ItemStack stack) {
        return false;
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
        if (mop == null) return;

        if (mop.typeOfHit == MovingObjectType.MISS) {
            BlockPos pos = new BlockPos(mop.blockX, mop.blockY, mop.blockZ);
            VoxelRenderer.renderBlock(pos, player, partialTicks);
        } else {
            BlockPos pos = BlockPos.fromRaycastSide(mop);
            if (pos == null) return;
            VoxelRenderer.renderBlock(pos, player, partialTicks);
        }
    }
    
}
