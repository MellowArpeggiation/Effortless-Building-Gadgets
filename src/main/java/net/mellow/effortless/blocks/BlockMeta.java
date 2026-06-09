package net.mellow.effortless.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

public class BlockMeta {

    public final Block block;
    public final int meta;

    public BlockMeta(Block block, int meta) {
        if (block == null) block = Blocks.air;

        this.block = block;
        this.meta = meta;
    }

    public BlockMeta(String name, int meta) {
        Block block = Block.getBlockFromName(name);
        if (block == null) block = Blocks.air;

        this.block = block;
        this.meta = meta;
    }

    public BlockMeta(int id, int meta) {
        Block block = Block.getBlockById(id);
        if (block == null) block = Blocks.air;

        this.block = block;
        this.meta = meta;
    }

    public static BlockMeta fromStack(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) return null;
        BlockMeta selected = new BlockMeta(((ItemBlock) stack.getItem()).field_150939_a, stack.getItemDamage()); // don't transform yet
        if (selected.block instanceof BlockBed) return null; // EFR makes its own "ItemBLOCKBed" placement class which doesn't conform to the vanilla expectation of it not being a non-ItemBlock Item, guh
        return selected;
    }

    @Override
    public int hashCode() {
        final int prime = 27644437;
        int result = 1;
        result = prime * result + ((block == null) ? 0 : block.hashCode());
        result = prime * result + meta;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        BlockMeta other = (BlockMeta) obj;
        if (block == null) {
            if (other.block != null) return false;
        } else if (!block.equals(other.block)) {
            return false;
        }

        return meta == other.meta;
    }

}
