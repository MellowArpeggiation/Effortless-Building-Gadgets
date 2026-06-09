package net.mellow.effortless.buildmode.modes;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
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
    public int add(ItemStack stack, BlockMeta selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos = BlockPos.fromRaycastSide(mop);
        if (pos == null) return 0;
        
        int placedMeta = getFinalPlacedMeta(selected, world, player, pos.x, pos.y, pos.z, mop.sideHit, new Vec3(mop.hitVec));
        return buildBox(world, player, selected, placedMeta, pos, pos, false);
    }

    @Override
    public void clear(ItemStack stack) {
        
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
        if (mop != null) {
            Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
        }
    }
    
}
