package net.mellow.effortless.buildmode;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.blocks.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public abstract class ThreeClicksBuildMode extends BaseBuildMode {
    
    public abstract BlockPos addMid(ItemStack stack, World world, EntityPlayer player, BlockPos pos0);
    public abstract int add(ItemStack stack, PlaceableStack selected, World world, EntityPlayer player, BlockPos pos0, BlockPos pos1);
    public abstract void render(ItemStack stack, World world, EntityPlayer player, BlockPos pos0, float partialTicks);
    public abstract void render(ItemStack stack, World world, EntityPlayer player, BlockPos pos0, BlockPos pos1, float partialTicks);

    @Override
    public int add(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));

        if (pos0 == null) {
            pos0 = BlockPos.fromRaycastReplaceable(world, mop);
            if (pos0 == null) return 0;

            PlaceableStack place = PlaceableStack.getPlaceableStack(selected, world, player, pos0.x, pos0.y, pos0.z, mop.sideHit, new Vec3(mop.hitVec));

            stack.stackTagCompound.setTag("pos0", pos0.save());
            stack.stackTagCompound.setTag("place", place.save());
        } else if (pos1 == null) {
            pos1 = addMid(stack, world, player, pos0);
            if (pos1 == null) return 0;

            stack.stackTagCompound.setTag("pos1", pos1.save());
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

            int built = add(stack, place, world, player, pos0, pos1);

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
        stack.stackTagCompound.removeTag("pos1");
        return didClear;
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));

        if (pos0 == null) {
            MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
            if (mop == null) return;

            Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
        } else if (pos1 == null) {
            render(stack, world, player, pos0, partialTicks);
        } else {
            render(stack, world, player, pos0, pos1, partialTicks);
        }
    }
    
}
