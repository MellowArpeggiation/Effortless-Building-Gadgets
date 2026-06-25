package net.mellow.effortless.buildmode.modes;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class Extended extends BaseBuildMode {

    @Override
    public boolean click(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        return true;
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos = BlockPos.fromRaycastReplaceable(world, mop);
        if (pos == null) return null;
        return new ConstructionSet(pos);
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, float partialTicks) {
        Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
    }

    @Override public boolean clear(ItemStack stack) { return false; }
    @Override public boolean isPlacing(ItemStack stack) { return false; }
    
}
