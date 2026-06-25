package net.mellow.effortless.buildmode;

import java.util.List;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.blocks.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public abstract class TwoClicksBuildMode extends BaseBuildMode {

    public abstract int add(ItemStack stack, PlaceableStack selected, World world, EntityPlayer player, BlockPos from);
    public abstract void render(ItemStack stack, World world, EntityPlayer player, BlockPos from, float partialTicks);

    // // TODO: another to abstract
    // public BlockPos getSecondPos(ItemStack stack, World world, EntityPlayer player, BlockPos from) {
    //     return null;
    // }

    @Override
    public boolean click(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));

        if (from != null) return true;

        // if (from == null) {
            from = BlockPos.fromRaycastReplaceable(world, mop);
            if (from == null) return false;

            stack.stackTagCompound.setTag("pos0", from.save());
        // } else {
        //     BlockPos to = getSecondPos(stack, world, player, from);
        //     if (to == null) return false;

        //     stack.stackTagCompound.setTag("pos1", to.save());

        //     return true;
        // }

        return false;
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (from == null) return null;

        return getBlocks(stack, world, player, from);
    }

    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, BlockPos from) {
        return null;
    }

    @Override
    public void renderNew(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));

        if (from == null) {
            MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
            if (mop != null) {
                Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
            }
        } else {
            ConstructionSet set = getBlocks(stack, world, player, from);
            if (set != null) {
                set.render(player, partialTicks);
            }
        }
    }

    @Override
    public int add(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));

        if (from == null) {
            from = BlockPos.fromRaycastReplaceable(world, mop);
            if (from == null) return 0;

            PlaceableStack place = PlaceableStack.getPlaceableStack(selected, world, player, from.x, from.y, from.z, mop.sideHit, new Vec3(mop.hitVec));

            stack.stackTagCompound.setTag("pos0", from.save());
            stack.stackTagCompound.setTag("place", place.save());
        } else {
            if (world.isRemote) {
                clear(stack);
                return 0;
            }

            PlaceableStack place = PlaceableStack.load(stack.stackTagCompound.getCompoundTag("place"));
            if (place == null) {
                clear(stack);
                return 0;
            }

            int built = add(stack, place, world, player, from);

            if (built <= 0) return 0;

            clear(stack);
            
            return built;
        }

        return 0;
    }

    @Override
    public boolean clear(ItemStack stack) {
        boolean didClear = stack.stackTagCompound.hasKey("pos0");
        stack.stackTagCompound.removeTag("pos0");
        return didClear;
    }

    @Override
    public boolean isPlacing(ItemStack stack) {
        return stack.stackTagCompound.hasKey("pos0");
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));

        if (from == null) {
            MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
            if (mop != null) {
                Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
            }
        } else {
            render(stack, world, player, from, partialTicks);
        }
    }
    
}
