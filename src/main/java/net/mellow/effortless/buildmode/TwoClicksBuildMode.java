package net.mellow.effortless.buildmode;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.ConstructionSet;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public abstract class TwoClicksBuildMode extends BaseBuildMode {

    public abstract ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos from);

    @Override
    public boolean click(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, Operation operation) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (from != null) return true;

        from = BlockPos.fromRaycastInteraction(world, mop, operation);
        if (from == null) return false;
        stack.stackTagCompound.setTag("pos0", from.save());

        return false;
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, Operation operation) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (from == null) return null;

        return getBlocks(stack, world, player, mop, from);
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
    
}
