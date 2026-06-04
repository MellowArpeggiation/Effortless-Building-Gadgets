package net.mellow.effortless.buildmode;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BuildModes {

    public static MovingObjectPosition getMop(EntityPlayer player, int reach) {
        Vec3 look = getPlayerLookVec(player);
        Vec3 start = getPlayerPos(player);
        Vec3 end = start.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        return player.worldObj.func_147447_a(start, end, false, false, true);
    }

    //Find coordinates on a line bound by a plane
    public static Vec3 findXBound(double x, Vec3 start, Vec3 look) {
        //then y and z are
        double y = (x - start.xCoord) / look.xCoord * look.yCoord + start.yCoord;
        double z = (x - start.xCoord) / look.xCoord * look.zCoord + start.zCoord;

        return Vec3.createVectorHelper(x, y, z);
    }

    public static Vec3 findYBound(double y, Vec3 start, Vec3 look) {
        //then x and z are
        double x = (y - start.yCoord) / look.yCoord * look.xCoord + start.xCoord;
        double z = (y - start.yCoord) / look.yCoord * look.zCoord + start.zCoord;

        return Vec3.createVectorHelper(x, y, z);
    }

    public static Vec3 findZBound(double z, Vec3 start, Vec3 look) {
        //then x and y are
        double x = (z - start.zCoord) / look.zCoord * look.xCoord + start.xCoord;
        double y = (z - start.zCoord) / look.zCoord * look.yCoord + start.yCoord;

        return Vec3.createVectorHelper(x, y, z);
    }

    //Use this instead of player.getLookVec() in any buildmodes code
    public static Vec3 getPlayerLookVec(EntityPlayer player) {
        Vec3 lookVec = player.getLookVec();
        double x = lookVec.xCoord;
        double y = lookVec.yCoord;
        double z = lookVec.zCoord;

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

        return Vec3.createVectorHelper(x, y, z);
    }

    // what the fuck... who do I even blame for this absolute fucking spaghetti ass bullshit, mojang or forge??
    public static Vec3 getPlayerPos(EntityPlayer player) {
        float defaultEyeHeight = player.worldObj.isRemote ? player.getDefaultEyeHeight() : 0;
        return Vec3.createVectorHelper(player.posX, player.posY + (player.getEyeHeight() - defaultEyeHeight), player.posZ);
    }

    public static boolean isCriteriaValid(Vec3 start, Vec3 look, int reach, EntityPlayer player, boolean skipRaytrace, Vec3 lineBound, Vec3 planeBound, double distToPlayerSq) {
        boolean intersects = false;
        if (!skipRaytrace) {
            //collision within a 1 block radius to selected is fine
            MovingObjectPosition rayTraceResult = player.worldObj.rayTraceBlocks(start, lineBound);
            intersects = rayTraceResult != null && rayTraceResult.typeOfHit == MovingObjectType.BLOCK &&
                planeBound.squareDistanceTo(rayTraceResult.hitVec) > 4;
        }

        return start.subtract(planeBound).dotProduct(look) > 0 &&
            distToPlayerSq > 2 && distToPlayerSq < reach * reach &&
            !intersects;
    }

}
