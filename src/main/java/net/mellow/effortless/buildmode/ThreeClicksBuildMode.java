package net.mellow.effortless.buildmode;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.ConstructionSet;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public abstract class ThreeClicksBuildMode extends BaseBuildMode {

    public abstract BlockPos getMid(ItemStack stack, World world, EntityPlayer player, BlockPos pos0);
    public abstract ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos pos0);
    public abstract ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, BlockPos pos0, BlockPos pos1);

    @Override
    public boolean click(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));
        if (pos1 != null) return true;

        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (pos0 != null) {
            pos1 = getMid(stack, world, player, pos0);
            if (pos1 == null) return false;
            stack.stackTagCompound.setTag("pos1", pos1.save());
        }
        
        stack.stackTagCompound.setTag("pos0", BlockPos.fromRaycastReplaceable(world, mop).save());

        return false;
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (pos0 == null) return null;

        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));
        if (pos1 == null) return getBlocks(stack, world, player, mop, pos0);

        return getBlocks(stack, world, player, mop, pos0, pos1);
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, float partialTicks) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));

        if (pos0 == null) {
            Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
        } else if (pos1 == null) {
            ConstructionSet set = getBlocks(stack, world, player, mop, pos0);
            if (set != null) set.render(player, partialTicks);
        } else {
            ConstructionSet set = getBlocks(stack, world, player, mop, pos0, pos1);
            if (set != null) set.render(player, partialTicks);
        }
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
    
}
