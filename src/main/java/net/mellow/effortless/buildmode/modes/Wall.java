package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.List;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class Wall extends BaseBuildMode {

    @Override
    public int add(ItemStack stack, BlockMeta selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));

        if (from == null) {
            from = BlockPos.fromRaycastSide(mop);
            if (from == null) return 0;

            int placedMeta = getFinalPlacedMeta(selected, world, player, from.x, from.y, from.z, mop.sideHit, mop.hitVec);

            stack.stackTagCompound.setTag("pos0", from.save());
            stack.stackTagCompound.setInteger("placedMeta", placedMeta);
        } else {
            BlockPos to = findWall(player, from, true);
            if (to == null) return 0;

            int placedMeta = stack.stackTagCompound.getInteger("placedMeta");

            clear(stack);
            
            BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
            
            if (fillMode == BuildingAction.HOLLOW) {
                if (from.x != to.x) {
                    return buildHollowWallZ(world, player, selected, placedMeta, from, to, false);
                } else if (from.z != to.z) {
                    return buildHollowWallX(world, player, selected, placedMeta, from, to, false);
                }
            }
            
            return buildBox(world, player, selected, placedMeta, from, to, false);
        }

        return 0;
    }

    @Override
    public void clear(ItemStack stack) {
        stack.stackTagCompound.removeTag("pos0");
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        BlockPos from = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        if (from == null) {
            MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
            if (mop != null) {
                Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
            }
        } else {
            BlockPos to = findWall(player, from, true);;
            if (to == null) return;
            
            renderBox(player, partialTicks, from, to, true);
            
            BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.FILL);
            if (fillMode == BuildingAction.HOLLOW && Math.abs(from.y - to.y) > 1) {
                BlockPos min = BlockPos.min(from, to);
                BlockPos max = BlockPos.max(from, to);

                if (max.x - min.x > 1) {
                    renderBox(player, partialTicks, min.add(1, 1, 0), max.add(-1, -1, 0));
                } else if (max.z - min.z > 1) {
                    renderBox(player, partialTicks, min.add(0, 1, 1), max.add(0, -1, -1));
                }
            }
        }
    }
    

    public static BlockPos findWall(EntityPlayer player, BlockPos firstPos, boolean skipRaytrace) {
        Vec3 look = BuildModes.getPlayerLookVec(player);
        Vec3 start = BuildModes.getPlayerPos(player);

        List<Criteria> criteriaList = new ArrayList<>(3);

        //X
        Vec3 xBound = BuildModes.findXBound(firstPos.x, start, look);
        criteriaList.add(new Criteria(xBound, firstPos, start, look));

        //Z
        Vec3 zBound = BuildModes.findZBound(firstPos.z, start, look);
        criteriaList.add(new Criteria(zBound, firstPos, start, look));

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
            //Select the one that is closest
            //Limit the angle to not be too extreme
            for (int i = 1; i < criteriaList.size(); i++) {
                Criteria criteria = criteriaList.get(i);
                if (criteria.distToPlayerSq < selected.distToPlayerSq && Math.abs(criteria.angle) - Math.abs(selected.angle) < 3)
                    selected = criteria;
            }
        }

        return BlockPos.containing(selected.planeBound);
    }

    static class Criteria {
        Vec3 planeBound;
        double distToPlayerSq;
        double angle;

        Criteria(Vec3 planeBound, BlockPos firstPos, Vec3 start, Vec3 look) {
            this.planeBound = planeBound;
            this.distToPlayerSq = this.planeBound.squareDistanceTo(start);
            Vec3 wall = this.planeBound.subtract(Vec3.createVectorHelper(firstPos.x, firstPos.y, firstPos.z));
            this.angle = wall.xCoord * look.xCoord + wall.zCoord * look.zCoord; //dot product ignoring y (looking up/down should not affect this angle)
        }

        //check if its not behind the player and its not too close and not too far
        //also check if raytrace from player to block does not intersect blocks
        public boolean isValid(Vec3 start, Vec3 look, int reach, EntityPlayer player, boolean skipRaytrace) {
            return BuildModes.isCriteriaValid(start, look, reach, player, skipRaytrace, planeBound, planeBound, distToPlayerSq);
        }
    }
    
}
