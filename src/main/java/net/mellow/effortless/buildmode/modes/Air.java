package net.mellow.effortless.buildmode.modes;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.mellow.effortless.buildmode.BuildModes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.world.World;

public class Air extends BaseBuildMode {

    @Override
    public int reach(ItemStack stack) {
        return 6;
    }

    @Override
    public void add(ItemStack stack, BlockMeta selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        if (mop == null) return;
        if (mop.typeOfHit == MovingObjectType.MISS) {
            BlockPos pos = new BlockPos(mop.blockX, mop.blockY, mop.blockZ);
            BaseBuildMode.buildBox(world, player, selected, pos, pos, false);
        } else {
            BlockPos pos = BlockPos.fromRaycastSide(mop);
            if (pos == null) return;
            BaseBuildMode.buildBox(world, player, selected, pos, pos, false);
        }
    }

    @Override
    public void clear(ItemStack stack) {
        
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
        if (mop == null) return;

        if (mop.typeOfHit == MovingObjectType.MISS) {
            BlockPos pos = new BlockPos(mop.blockX, mop.blockY, mop.blockZ);
            renderBox(player, partialTicks, pos, pos);
        } else {
            BlockPos pos = BlockPos.fromRaycastSide(mop);
            if (pos == null) return;
            renderBox(player, partialTicks, pos, pos);
        }
    }
    
}
