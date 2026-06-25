package net.mellow.effortless.blocks;

import net.mellow.effortless.api.BlockRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Block.SoundType;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class PlaceableStack {

    // Stores an item stack _and_ how it should be placed into the world
    
    public final ItemStack stack;
    public final BlockMeta place;
    public final NBTTagCompound nbt;

    public PlaceableStack(ItemStack stack, BlockMeta place, NBTTagCompound nbt) {
        this.stack = stack;
        this.place = place;
        this.nbt = nbt;
    }

    public PlaceableStack(ItemStack stack, Block block, int meta, NBTTagCompound nbt) {
        this(stack, new BlockMeta(block, meta), nbt);
    }

    // Checks if a picked item is actually usable by any gadget
    public static boolean isPlaceable(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0 || !(stack.getItem() instanceof ItemBlock)) return false;

        Block block = ((ItemBlock) stack.getItem()).field_150939_a;
        int meta = stack.getItemDamage();

        return isPlaceable(block, meta);
    }

    public static boolean isPlaceable(Block block, int meta) {
        // Compat fixes
        if (block instanceof BlockBed) return false; // EFR makes its own "ItemBLOCKBed" placement class which doesn't conform to the vanilla expectation of it not being a non-ItemBlock Item, guh

        // TE exceptions
        if (BlockRegistry.isWhitelisted(block)) return true;

        // No TEs (with exceptions)
        if (block.hasTileEntity(meta)) return false;

        return true;
    }

    public static PlaceableStack load(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey("stack") || !tag.hasKey("block") || !tag.hasKey("meta")) return null;
        ItemStack stack = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("stack"));
        BlockMeta place = new BlockMeta(tag.getInteger("block"), tag.getInteger("meta"));
        NBTTagCompound nbt = tag.hasKey("nbt") ? tag.getCompoundTag("nbt") : null;
        return new PlaceableStack(stack, place, nbt);
    }

    public NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound stackTag = new NBTTagCompound();
        stack.writeToNBT(stackTag);
        tag.setTag("stack", stackTag);
        tag.setInteger("block", Block.getIdFromBlock(place.block));
        tag.setInteger("meta", place.meta);
        if (nbt != null) tag.setTag("nbt", nbt);
        return tag;
    }

    private static final SoundType SILENT_BLOCK = new Block.SoundType("stone", -1.0F, 1.0F);

    // will place and immediately remove the block once it finds the final meta
    // x y z is the final block position, not the block it is placed on
    public static PlaceableStack getPlaceableStack(ItemStack selected, World world, EntityPlayer player, int x, int y, int z, int side, Vec3 hitVector) {
        float subX = (float)hitVector.x - (float)x;
        float subY = (float)hitVector.y - (float)y;
        float subZ = (float)hitVector.z - (float)z;

        ItemBlock placingItem = (ItemBlock) selected.getItem();

        BlockMeta was = new BlockMeta(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
        int wasSize = selected.stackSize;
        SoundType wasSound = placingItem.field_150939_a.stepSound;

        // Fake placement by real player to correctly fetch all block data
        placingItem.field_150939_a.stepSound = SILENT_BLOCK;
        placingItem.onItemUse(selected, player, world, x, y, z, side, subX, subY, subZ);

        // get meta (and TE for blocks with extended data)
        int meta = world.getBlockMetadata(x, y, z);
        NBTTagCompound nbt = null;
        if (placingItem.field_150939_a.hasTileEntity(meta)) {
            TileEntity tile = world.getTileEntity(x, y, z);

            if (tile != null) {
                nbt = new NBTTagCompound();
                tile.writeToNBT(nbt);
                nbt.removeTag("x");
                nbt.removeTag("y");
                nbt.removeTag("z");
            }
        }

        // Reset back to whatever the state was before placement
        selected.stackSize = wasSize;
        placingItem.field_150939_a.stepSound = wasSound;
        world.setBlock(x, y, z, was.block, was.meta, 2);

        PlaceableStack stack = new PlaceableStack(selected, placingItem.field_150939_a, meta, nbt);

        return stack;
    }

    public static boolean stackMatches(ItemStack stack1, ItemStack stack2) {
        return stack1 != null && stack1.stackSize > 0 && stack2 != null && stack2.stackSize > 0
            && (stack1.getItem() == stack2.getItem()
            && (!stack2.getHasSubtypes() || stack2.getItemDamage() == stack1.getItemDamage())
            && ItemStack.areItemStackTagsEqual(stack2, stack1));
    }

}
