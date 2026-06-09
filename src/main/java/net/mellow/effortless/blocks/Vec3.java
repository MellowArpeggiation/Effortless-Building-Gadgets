package net.mellow.effortless.blocks;

public class Vec3 {
    
    // unfucking ancient minecraft bullshit since 2023

    public static final Vec3 ZERO = new Vec3(0, 0, 0);
    
    public final double x;
    public final double y;
    public final double z;
    
    public static Vec3 atLowerCornerOf(BlockPos pos) {
        return new Vec3((double)pos.x, (double)pos.y, (double)pos.z);
    }
    
    public static Vec3 atLowerCornerWithOffset(BlockPos pos, double x, double y, double z) {
        return new Vec3((double)pos.x + x, (double)pos.y + y, (double)pos.z + z);
    }
    
    public static Vec3 atCenterOf(BlockPos pos) {
        return atLowerCornerWithOffset(pos, 0.5D, 0.5D, 0.5D);
    }
    
    public static Vec3 atBottomCenterOf(BlockPos pos) {
        return atLowerCornerWithOffset(pos, 0.5D, 0.0D, 0.5D);
    }
    
    public static Vec3 upFromBottomCenterOf(BlockPos pos, double y) {
        return atLowerCornerWithOffset(pos, 0.5D, y, 0.5D);
    }
    
    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3(net.minecraft.util.Vec3 ancientBullshit) {
        this.x = ancientBullshit.xCoord;
        this.y = ancientBullshit.yCoord;
        this.z = ancientBullshit.zCoord;
    }

    public net.minecraft.util.Vec3 toLegacy() {
        return net.minecraft.util.Vec3.createVectorHelper(x, y, z);
    }
    
    public Vec3 vectorTo(Vec3 vec) {
        return new Vec3(vec.x - this.x, vec.y - this.y, vec.z - this.z);
    }
    
    public Vec3 normalize() {
        double d0 = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        return d0 < 1.0E-4D ? ZERO : new Vec3(this.x / d0, this.y / d0, this.z / d0);
    }
    
    public double dot(Vec3 vec) {
        return this.x * vec.x + this.y * vec.y + this.z * vec.z;
    }
    
    public Vec3 cross(Vec3 vec) {
        return new Vec3(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
    }
    
    public Vec3 subtract(Vec3 vec) {
        return this.subtract(vec.x, vec.y, vec.z);
    }
    
    public Vec3 subtract(double x, double y, double z) {
        return this.add(-x, -y, -z);
    }
    
    public Vec3 add(Vec3 vec) {
        return this.add(vec.x, vec.y, vec.z);
    }
    
    public Vec3 add(double x, double y, double z) {
        return new Vec3(this.x + x, this.y + y, this.z + z);
    }
    
    public double distanceTo(Vec3 vec) {
        double d0 = vec.x - this.x;
        double d1 = vec.y - this.y;
        double d2 = vec.z - this.z;
        return Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }
    
    public double distanceToSqr(Vec3 vec) {
        double d0 = vec.x - this.x;
        double d1 = vec.y - this.y;
        double d2 = vec.z - this.z;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }
    
    public double distanceToSqr(double x, double y, double z) {
        double d0 = x - this.x;
        double d1 = y - this.y;
        double d2 = z - this.z;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }
    
    public Vec3 scale(double scale) {
        return this.multiply(scale, scale, scale);
    }
    
    public Vec3 reverse() {
        return this.scale(-1.0D);
    }
    
    public Vec3 multiply(Vec3 vec) {
        return this.multiply(vec.x, vec.y, vec.z);
    }
    
    public Vec3 multiply(double x, double y, double z) {
        return new Vec3(this.x * x, this.y * y, this.z * z);
    }
    
    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }
    
    public double lengthSqr() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }
    
    public double horizontalDistance() {
        return Math.sqrt(this.x * this.x + this.z * this.z);
    }
    
    public double horizontalDistanceSqr() {
        return this.x * this.x + this.z * this.z;
    }
    
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Vec3)) {
            return false;
        } else {
            Vec3 vec3 = (Vec3)obj;
            if (Double.compare(vec3.x, this.x) != 0) {
                return false;
            } else if (Double.compare(vec3.y, this.y) != 0) {
                return false;
            } else {
                return Double.compare(vec3.z, this.z) == 0;
            }
        }
    }
    
    public int hashCode() {
        long j = Double.doubleToLongBits(this.x);
        int i = (int)(j ^ j >>> 32);
        j = Double.doubleToLongBits(this.y);
        i = 31 * i + (int)(j ^ j >>> 32);
        j = Double.doubleToLongBits(this.z);
        return 31 * i + (int)(j ^ j >>> 32);
    }
    
    public String toString() {
        return "(" + this.x + ", " + this.y + ", " + this.z + ")";
    }
    
    public Vec3 xRot(double angle) {
        double f = Math.cos(angle);
        double f1 = Math.sin(angle);
        double d0 = this.x;
        double d1 = this.y * f + this.z * f1;
        double d2 = this.z * f - this.y * f1;
        return new Vec3(d0, d1, d2);
    }
    
    public Vec3 yRot(double angle) {
        double f = Math.cos(angle);
        double f1 = Math.sin(angle);
        double d0 = this.x * f + this.z * f1;
        double d1 = this.y;
        double d2 = this.z * f - this.x * f1;
        return new Vec3(d0, d1, d2);
    }
    
    public Vec3 zRot(double angle) {
        double f = Math.cos(angle);
        double f1 = Math.sin(angle);
        double d0 = this.x * f + this.y * f1;
        double d1 = this.y * f - this.x * f1;
        double d2 = this.z;
        return new Vec3(d0, d1, d2);
    }
    
    public static Vec3 directionFromRotation(double pitch, double yaw) {
        double x = Math.sin(-yaw * (Math.PI / 180F) - (float)Math.PI);
        double z = Math.cos(-yaw * (Math.PI / 180F) - Math.PI);
        double p = -Math.cos(-pitch * (Math.PI / 180F));
        double y = Math.sin(-pitch * (Math.PI / 180F));
        return new Vec3(x * p, y, z * p);
    }
    
}
