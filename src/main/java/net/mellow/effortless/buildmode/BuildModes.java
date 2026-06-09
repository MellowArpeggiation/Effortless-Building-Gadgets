package net.mellow.effortless.buildmode;

import net.mellow.effortless.blocks.Vec3;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BuildModes {

    public static MovingObjectPosition getMop(EntityPlayer player, int reach) {
        Vec3 look = getPlayerLookVec(player);
        Vec3 start = getPlayerPos(player);
        Vec3 end = start.add(look.scale(reach));

        return player.worldObj.func_147447_a(start.toLegacy(), end.toLegacy(), false, false, true);
    }

    //Find coordinates on a line bound by a plane
    public static Vec3 findXBound(double x, Vec3 start, Vec3 look) {
        //then y and z are
        double y = (x - start.x) / look.x * look.y + start.y;
        double z = (x - start.x) / look.x * look.z + start.z;

        return new Vec3(x, y, z);
    }

    public static Vec3 findYBound(double y, Vec3 start, Vec3 look) {
        //then x and z are
        double x = (y - start.y) / look.y * look.x + start.x;
        double z = (y - start.y) / look.y * look.z + start.z;

        return new Vec3(x, y, z);
    }

    public static Vec3 findZBound(double z, Vec3 start, Vec3 look) {
        //then x and y are
        double x = (z - start.z) / look.z * look.x + start.x;
        double y = (z - start.z) / look.z * look.y + start.y;

        return new Vec3(x, y, z);
    }

    //Use this instead of player.getLookVec() in any buildmodes code
    public static Vec3 getPlayerLookVec(EntityPlayer player) {
        Vec3 lookVec = new Vec3(player.getLookVec());
        double x = lookVec.x;
        double y = lookVec.y;
        double z = lookVec.z;

        //Further calculations (findXBound etc) don't like any component being 0 or 1 (e.g. dividing by 0)
        //isCriteriaValid below will take up to 2 minutes to raytrace blocks towards infinity if that is the case
        //So make sure they are close to but never exactly 0 or 1
        if (Math.abs(x) < 0.0001) x = 0.0001;
        if (Math.abs(x - 1.0) < 0.0001) x = 0.9999;
        if (Math.abs(x + 1.0) < 0.0001) x = -0.9999;

        if (Math.abs(y) < 0.0001) y = 0.0001;
        if (Math.abs(y - 1.0) < 0.0001) y = 0.9999;
        if (Math.abs(y + 1.0) < 0.0001) y = -0.9999;

        if (Math.abs(z) < 0.0001) z = 0.0001;
        if (Math.abs(z - 1.0) < 0.0001) z = 0.9999;
        if (Math.abs(z + 1.0) < 0.0001) z = -0.9999;

        return new Vec3(x, y, z);
    }

    // what the fuck... who do I even blame for this absolute fucking spaghetti ass bullshit, mojang or forge??
    public static Vec3 getPlayerPos(EntityPlayer player) {
        float defaultEyeHeight = player.worldObj.isRemote ? player.getDefaultEyeHeight() : 0;
        return new Vec3(player.posX, player.posY + (player.getEyeHeight() - defaultEyeHeight), player.posZ);
    }

    public static boolean isCriteriaValid(Vec3 start, Vec3 look, int reach, EntityPlayer player, boolean skipRaytrace, Vec3 lineBound, Vec3 planeBound, double distToPlayerSq) {
        boolean intersects = false;
        if (!skipRaytrace) {
            //collision within a 1 block radius to selected is fine
            MovingObjectPosition rayTraceResult = player.worldObj.rayTraceBlocks(start.toLegacy(), lineBound.toLegacy());
            intersects = rayTraceResult != null && rayTraceResult.typeOfHit == MovingObjectType.BLOCK &&
                planeBound.distanceToSqr(new Vec3(rayTraceResult.hitVec)) > 4;
        }

        return planeBound.subtract(start).dot(look) > 0 &&
            distToPlayerSq > 2 && distToPlayerSq < reach * reach &&
            !intersects;
    }

}
