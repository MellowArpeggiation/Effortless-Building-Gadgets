package net.mellow.effortless.buildmode.modes;

import java.util.ArrayList;
import java.util.List;

import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.Vec3;
import net.mellow.effortless.buildmode.BaseBuildMode;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class Cube extends BaseBuildMode {

    @Override
    public int add(ItemStack stack, BlockMeta selected, World world, EntityPlayer player, MovingObjectPosition mop) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));

        if (pos0 == null) {
            pos0 = BlockPos.fromRaycastSide(mop);
            if (pos0 == null) return 0;

            int placedMeta = getFinalPlacedMeta(selected, world, player, pos0.x, pos0.y, pos0.z, mop.sideHit, new Vec3(mop.hitVec));

            stack.stackTagCompound.setInteger("placedMeta", placedMeta);
            stack.stackTagCompound.setTag("pos0", pos0.save());
        } else if (pos1 == null) {
            pos1 = Floor.findFloor(player, pos0, true);
            if (pos1 == null) return 0;

            stack.stackTagCompound.setTag("pos1", pos1.save());
        } else {
            BlockPos pos2 = findHeight(player, pos1, true);
            if (pos2 == null) return 0;

            int placedMeta = stack.stackTagCompound.getInteger("placedMeta");

            clear(stack);

            BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.CUBE_FILL);

            if (pos0.x != pos2.x && pos0.y != pos2.y && pos0.z != pos2.z) {
                if (fillMode == BuildingAction.CUBE_SKELETON) {
                    return buildSkeletonCube(world, player, selected, placedMeta, pos0, pos2, false);
                } else if (fillMode == BuildingAction.CUBE_HOLLOW) {
                    return buildHollowCube(world, player, selected, placedMeta, pos0, pos2, false);
                }
            }
            
            return buildBox(world, player, selected, placedMeta, pos0, pos2, false);
        }

        return 0;
    }

    @Override
    public void clear(ItemStack stack) {
        stack.stackTagCompound.removeTag("pos0");
        stack.stackTagCompound.removeTag("pos1");
    }

    @Override
    public void render(ItemStack stack, World world, EntityPlayer player, float partialTicks) {
        BlockPos pos0 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos0"));
        BlockPos pos1 = BlockPos.load(stack.stackTagCompound.getCompoundTag("pos1"));

        if (pos0 == null) {
            MovingObjectPosition mop = BuildModes.getMop(player, reach(stack));
            if (mop == null) return;

            Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
        } else if (pos1 == null) {
            pos1 = Floor.findFloor(player, pos0, true);
            if (pos1 == null) return;

            renderBox(player, partialTicks, pos0, pos1, true);
        } else {
            BlockPos pos2 = findHeight(player, pos1, true);
            if (pos2 == null) return;

            renderBox(player, partialTicks, pos0, pos2, true);

            BlockPos min = BlockPos.min(pos0, pos2);
            BlockPos max = BlockPos.max(pos0, pos2);
            
            BuildingAction fillMode = ItemBuildingGadget.getAction(stack, BuildingOption.CUBE_FILL);
            if (fillMode != BuildingAction.CUBE_FULL && (max.x - min.x > 1 && max.y - min.y > 1 && max.z - min.z > 1)) {
                renderBox(player, partialTicks, min.add(1, 1, 1), max.add(-1, -1, -1));

                if (fillMode == BuildingAction.CUBE_SKELETON) {
                    drawInnerFaces(player, min, max, partialTicks);
                }
            }
        }
    }

    private static void drawInnerFaces(EntityPlayer player, BlockPos min, BlockPos max, float partialTicks) {
        double minX = min.x + 0.075;
        double maxX = max.x + 0.925;
        double minY = min.y + 0.075;
        double maxY = max.y + 0.925;
        double minZ = min.z + 0.075;
        double maxZ = max.z + 0.925;

        Tessellator tess = Tessellator.instance;
        startLineDraw(tess, player, partialTicks);
        
        // top
        tess.addVertex(minX + 1, maxY, minZ + 1);
        tess.addVertex(minX + 1, maxY, maxZ - 1);
        
        tess.addVertex(minX + 1, maxY, maxZ - 1);
        tess.addVertex(maxX - 1, maxY, maxZ - 1);
        
        tess.addVertex(maxX - 1, maxY, maxZ - 1);
        tess.addVertex(maxX - 1, maxY, minZ + 1);

        tess.addVertex(maxX - 1, maxY, minZ + 1);
        tess.addVertex(minX + 1, maxY, minZ + 1);
        
        // bottom
        tess.addVertex(minX + 1, minY, minZ + 1);
        tess.addVertex(minX + 1, minY, maxZ - 1);
        
        tess.addVertex(minX + 1, minY, maxZ - 1);
        tess.addVertex(maxX - 1, minY, maxZ - 1);
        
        tess.addVertex(maxX - 1, minY, maxZ - 1);
        tess.addVertex(maxX - 1, minY, minZ + 1);

        tess.addVertex(maxX - 1, minY, minZ + 1);
        tess.addVertex(minX + 1, minY, minZ + 1);

        // -z
        tess.addVertex(minX + 1, minY + 1, minZ);
        tess.addVertex(minX + 1, maxY - 1, minZ);

        tess.addVertex(minX + 1, maxY - 1, minZ);
        tess.addVertex(maxX - 1, maxY - 1, minZ);

        tess.addVertex(maxX - 1, maxY - 1, minZ);
        tess.addVertex(maxX - 1, minY + 1, minZ);

        tess.addVertex(maxX - 1, minY + 1, minZ);
        tess.addVertex(minX + 1, minY + 1, minZ);

        // +z
        tess.addVertex(minX + 1, minY + 1, maxZ);
        tess.addVertex(minX + 1, maxY - 1, maxZ);

        tess.addVertex(minX + 1, maxY - 1, maxZ);
        tess.addVertex(maxX - 1, maxY - 1, maxZ);

        tess.addVertex(maxX - 1, maxY - 1, maxZ);
        tess.addVertex(maxX - 1, minY + 1, maxZ);

        tess.addVertex(maxX - 1, minY + 1, maxZ);
        tess.addVertex(minX + 1, minY + 1, maxZ);

        // -x
        tess.addVertex(minX, minY + 1, minZ + 1);
        tess.addVertex(minX, maxY - 1, minZ + 1);

        tess.addVertex(minX, maxY - 1, minZ + 1);
        tess.addVertex(minX, maxY - 1, maxZ - 1);

        tess.addVertex(minX, maxY - 1, maxZ - 1);
        tess.addVertex(minX, minY + 1, maxZ - 1);

        tess.addVertex(minX, minY + 1, maxZ - 1);
        tess.addVertex(minX, minY + 1, minZ + 1);

        // +x
        tess.addVertex(maxX, minY + 1, minZ + 1);
        tess.addVertex(maxX, maxY - 1, minZ + 1);

        tess.addVertex(maxX, maxY - 1, minZ + 1);
        tess.addVertex(maxX, maxY - 1, maxZ - 1);

        tess.addVertex(maxX, maxY - 1, maxZ - 1);
        tess.addVertex(maxX, minY + 1, maxZ - 1);

        tess.addVertex(maxX, minY + 1, maxZ - 1);
        tess.addVertex(maxX, minY + 1, minZ + 1);
        
        // yeah, that was stupid

        endLineDraw(tess);
    }

    public static BlockPos findHeight(EntityPlayer player, BlockPos secondPos, boolean skipRaytrace) {
        Vec3 look = BuildModes.getPlayerLookVec(player);
        Vec3 start = BuildModes.getPlayerPos(player);

        List<HeightCriteria> criteriaList = new ArrayList<>(3);

        //X
        Vec3 xBound = BuildModes.findXBound(secondPos.x, start, look);
        criteriaList.add(new HeightCriteria(xBound, secondPos, start));

        //Z
        Vec3 zBound = BuildModes.findZBound(secondPos.z, start, look);
        criteriaList.add(new HeightCriteria(zBound, secondPos, start));

        //Remove invalid criteria
        // int reach = CapabilityHandler.getBuildModeReach(player);
        int reach = 32;
        criteriaList.removeIf(criteria -> !criteria.isValid(start, look, reach, player, skipRaytrace));

        //If none are valid, return empty list of blocks
        if (criteriaList.isEmpty()) return null;

        //If only 1 is valid, choose that one
        HeightCriteria selected = criteriaList.get(0);

        //If multiple are valid, choose based on criteria
        if (criteriaList.size() > 1) {
            //Select the one that is closest (from wall position to its line counterpart)
            for (int i = 1; i < criteriaList.size(); i++) {
                HeightCriteria criteria = criteriaList.get(i);
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

    static class HeightCriteria {
        Vec3 planeBound;
        Vec3 lineBound;
        double distToLineSq;
        double distToPlayerSq;

        HeightCriteria(Vec3 planeBound, BlockPos secondPos, Vec3 start) {
            this.planeBound = planeBound;
            this.lineBound = toLongestLine(this.planeBound, secondPos);
            this.distToLineSq = this.lineBound.distanceToSqr(this.planeBound);
            this.distToPlayerSq = this.planeBound.distanceToSqr(start);
        }

        //Make it from a plane into a line, on y axis only
        private Vec3 toLongestLine(Vec3 boundVec, BlockPos secondPos) {
            BlockPos bound = BlockPos.containing(boundVec);
            return new Vec3(secondPos.x, bound.y, secondPos.z);
        }

        //check if its not behind the player and its not too close and not too far
        //also check if raytrace from player to block does not intersect blocks
        public boolean isValid(Vec3 start, Vec3 look, int reach, EntityPlayer player, boolean skipRaytrace) {
            return BuildModes.isCriteriaValid(start, look, reach, player, skipRaytrace, lineBound, planeBound, distToPlayerSq);
        }
    }
    
}
