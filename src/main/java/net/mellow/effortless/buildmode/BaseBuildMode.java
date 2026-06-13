package net.mellow.effortless.buildmode;

import java.util.ArrayList;
import java.util.List;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEItemStack;
import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.blocks.BlockPos;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.buildmode.History.HistoryBlock;
import net.mellow.effortless.compat.CompatAE2;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public abstract class BaseBuildMode {

    public abstract int add(ItemStack stack, ItemStack selected, World world, EntityPlayer player, MovingObjectPosition mop);
    public abstract void clear(ItemStack stack);

    public int reach(ItemStack stack) {
        return 32;
    }

    // selected - the block selected by the tool
    // toPlace  - the transformed block to be placed into the world
    public static int build(World world, EntityPlayer player, PlaceableStack selected, List<BlockPos> positions, boolean replaceAny) {
        if (world.isRemote) return 0;
        if (positions == null || positions.isEmpty()) return 0;

        boolean useItems = !player.capabilities.isCreativeMode;

        List<HistoryBlock> previousState = new ArrayList<>();
        ItemStack toDeplete = null;
        if (useItems) {
            toDeplete = getMatchingStack(player, selected);
            if (toDeplete == null) return 0;
        }

        int blocksPlaced = 0;

        for (BlockPos pos : positions) {
            Block block = world.getBlock(pos.x, pos.y, pos.z);
            if (!replaceAny && !block.isReplaceable(world, pos.x, pos.y, pos.z)) continue;

            int meta = world.getBlockMetadata(pos.x, pos.y, pos.z);
            if (selected.place.block == block && selected.place.meta == meta) continue; // skip double placing
            if (block.hasTileEntity(meta)) continue;

            AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(pos.x, pos.y, pos.z, pos.x + 1, pos.y + 1, pos.z + 1);

            if (!world.checkNoEntityCollision(bb, player)) continue;

            if (useItems) {
                if (toDeplete == null || toDeplete.stackSize <= 0) {
                    toDeplete = getMatchingStack(player, selected);
                    if (toDeplete == null) {
                        break;
                    }
                    if (CompatAE2.hasAE2){
                        CompatAE2.removeFromNetwork(player, selected, toDeplete.stackSize);
                        IItemList<IAEItemStack> itemList = CompatAE2.getItemList(player);
                        if (itemList == null) break;
                        if (itemList.isEmpty()) break;
                    }
                }

                toDeplete.stackSize--;
            }

            previousState.add(new HistoryBlock(new BlockMeta(block, meta), selected.place, new BlockPos(pos.x, pos.y, pos.z)));
            world.setBlock(pos.x, pos.y, pos.z, selected.place.block, selected.place.meta, 3);

            blocksPlaced++;
        }
        History.addUndo(player, previousState, selected);

        cleanInventory(player);

        world.playSoundEffect(player.posX, player.posY, player.posZ, selected.place.block.stepSound.func_150496_b(), (selected.place.block.stepSound.getVolume() + 1.0F) / 2.0F, selected.place.block.stepSound.getPitch() * 0.8F);

        return blocksPlaced;
    }

    public static int build(World world, EntityPlayer player, PlaceableStack selected, BlockPos position, boolean replaceAny) {
        List<BlockPos> list = new ArrayList<>();
        list.add(position);
        return build(world, player, selected, list, replaceAny);
    }

    public static ItemStack getMatchingStack(EntityPlayer player, PlaceableStack selected) {
        for (int i = player.inventory.mainInventory.length - 1; i >= 0; i--) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (PlaceableStack.stackMatches(stack, selected.stack)) {
                return stack;
            }
        }
        if (CompatAE2.hasAE2){
            return CompatAE2.findItemInNetwork(selected, player);
        }
        return null;
    }

    // Vanilla doesn't handle empty stacks automatically, this is a (shit) solution to that
    public static void cleanInventory(EntityPlayer player) {
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack != null && stack.stackSize <= 0) player.inventory.mainInventory[i] = null;
        }

        player.inventoryContainer.detectAndSendChanges();
    }

    public abstract void render(ItemStack stack, World world, EntityPlayer player, float partialTicks);

    public static void updateHighlight(BlockPos from, BlockPos to) {
        BlockPos min = BlockPos.min(from, to);
        BlockPos max = BlockPos.max(from, to);

        List<String> values = new ArrayList<>();
        if (min.x != max.x) values.add("" + (max.x - min.x + 1));
        if (min.y != max.y) values.add("" + (max.y - min.y + 1));
        if (min.z != max.z) values.add("" + (max.z - min.z + 1));

        highlightTitle = !values.isEmpty() ? String.join("x", values) : "1";
        Minecraft.getMinecraft().ingameGUI.remainingHighlightTicks = 40;
    }

    public static String highlightTitle;

    public String getItemHighlight(ItemStack stack) {
        if (highlightTitle == null) return null;

        String title = highlightTitle;
        highlightTitle = null;
        return title;
    }

}
