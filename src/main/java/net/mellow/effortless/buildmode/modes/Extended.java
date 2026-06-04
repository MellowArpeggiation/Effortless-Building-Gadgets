package net.mellow.effortless.buildmode.modes;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.mellow.effortless.buildmode.BuildModes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class Extended extends BaseBuildMode {

    @Override
    public void add(ItemStack stack, BlockMeta selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos = BlockPos.fromRaycastSide(mop);
        if (pos == null) return;
        BaseBuildMode.buildBox(world, player, selected, pos, pos, false);
    }

    @Override
    public void clear(ItemStack stack) {
        
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        BlockPos pos = BlockPos.fromRaycastSide(BuildModes.getMop(player, reach(stack)));
        if (pos == null) return;
        renderBox(player, partialTicks, pos, pos);
    }
    
}
