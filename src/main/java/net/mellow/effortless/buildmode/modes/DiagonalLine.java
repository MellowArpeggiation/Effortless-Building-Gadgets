package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.List;

import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.VoxelRenderer;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class DiagonalLine extends BaseBuildMode {

    @Override
    public int add(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        BuildingAction type = ItemBuildingGadget.getAction(stack, BuildingOption.LINE_DRAW);

        if (type == BuildingAction.LINE_CONSTRUCT) return addConstruct(stack, selected, world, player, mop);
        return addPointToPoint(stack, selected, world, player, mop);
    }

    private int addConstruct(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));

        if (pos0 == null) {
            pos0 = BlockPos.fromRaycastReplaceable(world, mop);
            if (pos0 == null) return 0;

            PlaceableStack place = PlaceableStack.getPlaceableStack(selected, world, player, pos0.x, pos0.y, pos0.z, mop.sideHit, new Vec3(mop.hitVec));

            stack.stackTagCompound.setTag("pos0", pos0.save());
            stack.stackTagCompound.setTag("place", place.save());
        } else if (pos1 == null) {
            pos1 = Floor.findFloor(player, pos0, true);
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

            BlockPos pos2 = Cube.findHeight(player, pos0, pos1, true);
            if (pos2 == null) return 0;

            int built = build(world, player, place, getDiagonalLineBlocks(pos0, pos2, 10), false);

            if (built <= 0) return 0;

            clear(stack);
            
            return built;
        }

        return 0;
    }

    private int addPointToPoint(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));

        if (pos0 == null) {
            pos0 = BlockPos.fromRaycastReplaceable(world, mop);
            if (pos0 == null) return 0;

            PlaceableStack place = PlaceableStack.getPlaceableStack(selected, world, player, pos0.x, pos0.y, pos0.z, mop.sideHit, new Vec3(mop.hitVec));

            stack.stackTagCompound.setTag("pos0", pos0.save());
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

            BlockPos pos2 = BlockPos.fromRaycastReplaceable(world, mop);
            if (pos2 == null) return 0;

            int built = build(world, player, place, getDiagonalLineBlocks(pos0, pos2, 10), false);

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
    public boolean isPlacing(ItemStack stack) {
        return stack.stackTagCompound.hasKey("pos0");
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        BuildingAction type = ItemBuildingGadget.getAction(stack, BuildingOption.LINE_DRAW);

        if (type == BuildingAction.LINE_CONSTRUCT) {
            renderConstruct(stack, world, player, partialTicks);
        } else {
            renderPointToPoint(stack, world, player, partialTicks);
        }
    }

    public void renderConstruct(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));

        if (pos0 == null) {
            MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
            if (mop == null) return;

            Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
        } else if (pos1 == null) {
            pos1 = Floor.findFloor(player, pos0, true);
            if (pos1 == null) return;

            List<BlockPos> blocks = getDiagonalLineBlocks(pos0, pos1, 10);
            VoxelRenderer.renderBlocks(blocks, player, partialTicks);

            updateHighlight(pos0, pos1, blocks.size());
        } else {
            BlockPos pos2 = Cube.findHeight(player, pos0, pos1, true);
            if (pos2 == null) return;

            List<BlockPos> blocks = getDiagonalLineBlocks(pos0, pos2, 10);
            VoxelRenderer.renderBlocks(blocks, player, partialTicks);

            updateHighlight(pos0, pos2, blocks.size());
        }
    }

    public void renderPointToPoint(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));

        if (pos0 == null) {
            MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
            if (mop == null) return;

            Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
        } else {
            MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
            BlockPos pos1 = BlockPos.fromRaycastReplaceable(world, mop);
            if (pos1 == null) return;

            List<BlockPos> blocks = getDiagonalLineBlocks(pos0, pos1, 10);
            VoxelRenderer.renderBlocks(blocks, player, partialTicks);

            updateHighlight(pos0, pos1, blocks.size());
        }
    }

    //Add diagonal line from first to second
    public static List<BlockPos> getDiagonalLineBlocks(BlockPos from, BlockPos to, float sampleMultiplier) {
        List<BlockPos> list = new ArrayList<>();

        Vec3 first = Vec3.atCenterOf(from);
        Vec3 second = Vec3.atCenterOf(to);

        int iterations = (int) Math.ceil(first.distanceTo(second) * sampleMultiplier);
        for (double t = 0; t <= 1.0; t += 1.0 / iterations) {
            Vec3 lerp = first.add(second.subtract(first).scale(t));
            BlockPos candidate = BlockPos.containing(lerp);
            //Only add if not equal to the last in the list
            if (list.isEmpty() || !list.get(list.size() - 1).equals(candidate))
                list.add(candidate);
        }

        return list;
    }
    
}
