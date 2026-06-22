package net.mellow.effortless.buildmode.modes;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.mellow.effortless.buildmode.BuildModes;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class Extended extends BaseBuildMode {

    @Override
    public int add(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        if (world.isRemote) return 0;

        BlockPos pos = BlockPos.fromRaycastReplaceable(world, mop);
        if (pos == null) return 0;
        
        PlaceableStack place = PlaceableStack.getPlaceableStack(selected, world, player, pos.x, pos.y, pos.z, mop.sideHit, new Vec3(mop.hitVec));
        return build(world, player, place, pos, false);
    }

    @Override public boolean clear(ItemStack stack) { return false; }
    @Override public boolean isPlacing(ItemStack stack) { return false; }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
        if (mop != null) {
            Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
        }
    }
    
}
