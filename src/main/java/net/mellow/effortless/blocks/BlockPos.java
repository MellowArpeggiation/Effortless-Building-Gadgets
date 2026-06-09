package net.mellow.effortless.blocks;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.common.util.ForgeDirection;

public class BlockPos implements Comparable<BlockPos> {

    public int x;
    public int y;
    public int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static BlockPos load(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey("x") || !tag.hasKey("y") || !tag.hasKey("z")) return null;
        int x = tag.getInteger("x");
        int y = tag.getInteger("y");
        int z = tag.getInteger("z");
        return new BlockPos(x, y, z);
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        return tag;
    }

    public static BlockPos fromRaycast(MovingObjectPosition mop) {
        if (mop == null || mop.typeOfHit != MovingObjectType.BLOCK) return null;
        return new BlockPos(mop.blockX, mop.blockY, mop.blockZ);
    }

    public static BlockPos fromRaycastSide(MovingObjectPosition mop) {
        if (mop == null || mop.typeOfHit != MovingObjectType.BLOCK) return null;
        ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
        return new BlockPos(mop.blockX + dir.offsetX, mop.blockY + dir.offsetY, mop.blockZ + dir.offsetZ);
    }

    public static BlockPos containing(Vec3 vec) {
        return new BlockPos(MathHelper.floor_double(vec.x), MathHelper.floor_double(vec.y), MathHelper.floor_double(vec.z));
    }

    public static BlockPos min(BlockPos one, BlockPos two) {
        return new BlockPos(Math.min(one.x, two.x), Math.min(one.y, two.y), Math.min(one.z, two.z));
    }

    public static BlockPos max(BlockPos one, BlockPos two) {
        return new BlockPos(Math.max(one.x, two.x), Math.max(one.y, two.y), Math.max(one.z, two.z));
    }

    public BlockPos add(BlockPos pos) {
        return new BlockPos(x + pos.x, y + pos.y, z + pos.z);
    }

    public BlockPos add(int x, int y, int z) {
        return new BlockPos(this.x + x, this.y + y, this.z + z);
    }

    public BlockPos subtract(BlockPos pos) {
        return new BlockPos(x - pos.x, y - pos.y, z - pos.z);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + x;
        result = prime * result + y;
        result = prime * result + z;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BlockPos other = (BlockPos) obj;
        if (x != other.x)
            return false;
        if (y != other.y)
            return false;
        if (z != other.z)
            return false;
        return true;
    }

    @Override
    public int compareTo(BlockPos o) {
        return equals(o) ? 0 : 1;
    }

    @Override
    public String toString() {
        return x + "x" + y + "x" + z;
    }

}
