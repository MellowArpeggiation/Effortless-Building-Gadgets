package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.List;

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

public class Line extends BaseBuildMode {

    public int add(ItemStack stack, BlockMeta selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));

        if (from == null) {
            from = BlockPos.fromRaycastSide(mop);
            if (from == null) return 0;

            int placedMeta = getFinalPlacedMeta(selected, world, player, from.x, from.y, from.z, mop.sideHit, new Vec3(mop.hitVec));

            stack.stackTagCompound.setTag("pos0", from.save());
            stack.stackTagCompound.setInteger("placedMeta", placedMeta);
        } else {
            BlockPos to = findLine(player, from, true);
            if (to == null) return 0;

            int placedMeta = stack.stackTagCompound.getInteger("placedMeta");

            clear(stack);
            
            return buildBox(world, player, selected, placedMeta, from, to, false);
        }

        return 0;
    }

    public void clear(ItemStack stack) {
        stack.stackTagCompound.removeTag("pos0");
    }

    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (from == null) {
            MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
            if (mop != null) {
                Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
            }
        } else {
            BlockPos to = findLine(player, from, true);
            if (to == null) return;
    
            renderBox(player, partialTicks, from, to, true);
        }
    }


    public static BlockPos findLine(EntityPlayer player, BlockPos firstPos, boolean skipRaytrace) {
        Vec3 look = BuildModes.getPlayerLookVec(player);
        Vec3 start = BuildModes.getPlayerPos(player);

        List<Criteria> criteriaList = new ArrayList<>(3);

        //X
        Vec3 xBound = BuildModes.findXBound(firstPos.x, start, look);
        criteriaList.add(new Criteria(xBound, firstPos, start));

        //Y
        Vec3 yBound = BuildModes.findYBound(firstPos.y, start, look);
        criteriaList.add(new Criteria(yBound, firstPos, start));

        //Z
        Vec3 zBound = BuildModes.findZBound(firstPos.z, start, look);
        criteriaList.add(new Criteria(zBound, firstPos, start));

        //Remove invalid criteria
        // int reach = CapabilityHandler.getBuildModeReach(player);
        int reach = 32;
        criteriaList.removeIf(criteria -> !criteria.isValid(start, look, reach, player, skipRaytrace));

        //If none are valid, return empty list of blocks
        if (criteriaList.isEmpty()) return null;

        //If only 1 is valid, choose that one
        Criteria selected = criteriaList.get(0);

        //If multiple are valid, choose based on criteria
        if (criteriaList.size() > 1) {
            //Select the one that is closest (from wall position to its line counterpart)
            for (int i = 1; i < criteriaList.size(); i++) {
                Criteria criteria = criteriaList.get(i);
                if (criteria.distToLineSq < 2.0 && selected.distToLineSq < 2.0) {
                    //Both very close to line, choose closest to player
                    if (criteria.distToPlayerSq < selected.distToPlayerSq)
                        selected = criteria;
                } else {
                    //Pick closest to line
                    if (criteria.distToLineSq < selected.distToLineSq)
                        selected = criteria;
                }
            }

        }

        return BlockPos.containing(selected.lineBound);
    }

    static class Criteria {
        Vec3 planeBound;
        Vec3 lineBound;
        double distToLineSq;
        double distToPlayerSq;

        Criteria(Vec3 planeBound, BlockPos firstPos, Vec3 start) {
            this.planeBound = planeBound;
            this.lineBound = toLongestLine(this.planeBound, firstPos);
            this.distToLineSq = this.lineBound.distanceToSqr(this.planeBound);
            this.distToPlayerSq = this.planeBound.distanceToSqr(start);
        }

        //Make it from a plane into a line
        //Select the axis that is longest
        private Vec3 toLongestLine(Vec3 boundVec, BlockPos firstPos) {
            BlockPos bound = BlockPos.containing(boundVec);

            BlockPos firstToSecond = firstPos.subtract(bound);
            firstToSecond = new BlockPos(Math.abs(firstToSecond.x), Math.abs(firstToSecond.y), Math.abs(firstToSecond.z));
            int longest = Math.max(firstToSecond.x, Math.max(firstToSecond.y, firstToSecond.z));
            if (longest == firstToSecond.x) {
                return new Vec3(bound.x, firstPos.y, firstPos.z);
            }
            if (longest == firstToSecond.y) {
                return new Vec3(firstPos.x, bound.y, firstPos.z);
            }
            if (longest == firstToSecond.z) {
                return new Vec3(firstPos.x, firstPos.y, bound.z);
            }
            return null;
        }

        //check if its not behind the player and its not too close and not too far
        //also check if raytrace from player to block does not intersect blocks
        public boolean isValid(Vec3 start, Vec3 look, int reach, EntityPlayer player, boolean skipRaytrace) {
            return BuildModes.isCriteriaValid(start, look, reach, player, skipRaytrace, lineBound, planeBound, distToPlayerSq);
        }

    }

}
