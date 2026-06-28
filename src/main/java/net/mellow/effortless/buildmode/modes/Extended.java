package net.mellow.effortless.buildmode.modes;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class Extended extends BaseBuildMode {

    @Override
    public boolean click(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, Operation operation) {
        return true;
    }

    @Override
    public ConstructionSet getBlocks(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop, Operation operation) {
        BlockPos pos = BlockPos.fromRaycastInteraction(world, mop, operation);
        if (pos == null) return null;
        return new ConstructionSet(pos);
    }

    @Override
    public BuildingAction repeatSpeed(ItemStack stack) {
        return ItemBuildingGadget.getAction(stack, BuildingOption.SPEED);
    }

    @Override public boolean clear(ItemStack stack) { return false; }
    @Override public boolean isPlacing(ItemStack stack) { return false; }
    @Override public boolean shouldRender(ItemStack stack) { return false; }
    @Override public boolean showHighlight(ItemStack stack) { return false; }
    
}
