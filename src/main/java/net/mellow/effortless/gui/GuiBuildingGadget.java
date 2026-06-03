package net.mellow.effortless.gui;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import net.mellow.effortless.Keybinds;
import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.mellow.effortless.items.ItemBuildingGadget.BuildingMode;
import net.mellow.effortless.network.NBTControlPacket;
import net.mellow.effortless.network.NetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GuiBuildingGadget extends GuiScreen {

    private ItemStack gadget;
    private List<BlockMeta> usableBlocks = new ArrayList<>();

    private BuildingMode switchToMode = null;
    private BlockMeta switchToBlock = null;

    public GuiBuildingGadget(ItemStack stack) {
        this.gadget = stack;
    }

    @Override
    public void initGui() {
        EntityPlayer player = this.mc.thePlayer;
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);

            if (stack == null) continue;
            if (!(stack.getItem() instanceof ItemBlock)) continue;

            usableBlocks.add(new BlockMeta(((ItemBlock) stack.getItem()).field_150939_a, stack.getItem().getMetadata(stack.getItemDamage())));
        }
    }

    @Override
    protected void actionPerformed(GuiButton btn) {
        
    }

    @Override
    public void handleInput() {
        super.handleInput();

        if (!Keyboard.isKeyDown(Keybinds.uiKey.getKeyCode())) {
            this.mc.displayGuiScreen(null);
            this.mc.setIngameFocus();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        super.drawScreen(mouseX, mouseY, partialTicks);

        Tessellator tessellator = Tessellator.instance;

        int modeCount = ItemBuildingGadget.BuildingMode.values().length;
        int blockCount = usableBlocks.size();

        double midX = width / 2;
        double midY = height / 2;
        double mx = mouseX - midX;
        double my = mouseY - midY;

        // reset mouseover selection
        switchToMode = null;
        switchToBlock = null;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4d(1, 1, 1, 1);

        // Draw radial menu
        double mr = Math.atan2(my, mx);

        double qtrCircle = Math.PI * 0.5;
        if (mr < -qtrCircle) {
            mr += Math.PI * 2;
        }

        double ringInner = 30;
        double ringOuter = 65;
        double gapInner = Math.PI * 0.005;
        double gapOuter = Math.PI * 0.0025;
        double radiansPer = 2 * Math.PI / modeCount;

        tessellator.startDrawingQuads();

        for (int i = 0; i < modeCount; i++) {
            double begin = i * radiansPer - qtrCircle;
            double end = (i + 1) * radiansPer - qtrCircle;

            double x1m1 = Math.cos(begin + gapInner) * ringInner;
            double x2m1 = Math.cos(end - gapInner) * ringInner;
            double y1m1 = Math.sin(begin + gapInner) * ringInner;
            double y2m1 = Math.sin(end - gapInner) * ringInner;

            double x1m2 = Math.cos(begin + gapOuter) * ringOuter;
            double x2m2 = Math.cos(end - gapOuter) * ringOuter;
            double y1m2 = Math.sin(begin + gapOuter) * ringOuter;
            double y2m2 = Math.sin(end - gapOuter) * ringOuter;

            boolean isMouseInQuad = inTriangle(x1m1, y1m1, x2m2, y2m2, x2m1, y2m1, mx, my)
                || inTriangle(x1m1, y1m1, x1m2, y1m2, x2m2, y2m2, mx, my);
            boolean isHighlighted = begin <= mr && mr <= end && isMouseInQuad;

            if (isHighlighted) {
                switchToMode = BuildingMode.values()[i];
            }

            tessellator.setColorRGBA(0, 0, 0, isHighlighted ? 255 : 120);

            tessellator.addVertex(midX + x1m1, midY + y1m1, 0);
            tessellator.addVertex(midX + x2m1, midY + y2m1, 0);
            tessellator.addVertex(midX + x2m2, midY + y2m2, 0);
            tessellator.addVertex(midX + x1m2, midY + y1m2, 0);
        }



        // Draw block selecting buttons
        double btnWidth = 20;
        double padding = 2;
        double btnXOffset = (blockCount * btnWidth + (blockCount - 1) * padding) / 2;
        double btnYOffset = 80;

        for (int i = 0; i < blockCount; i++) {
            double x1 = midX + i * btnWidth + i * padding - btnXOffset;
            double x2 = x1 + btnWidth;
            double y1 = midY + btnYOffset;
            double y2 = y1 + btnWidth;

            boolean isHighlighted = x1 <= mouseX && x2 >= mouseX && y1 <= mouseY && y2 >= mouseY;

            if (isHighlighted) {
                switchToBlock = usableBlocks.get(i);
            }

            tessellator.setColorRGBA(0, 0, 0, isHighlighted ? 255 : 120);
            
            tessellator.addVertex(x1, y1, 0);
            tessellator.addVertex(x1, y2, 0);
            tessellator.addVertex(x2, y2, 0);
            tessellator.addVertex(x2, y1, 0);
        }

        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
    }

    @Override
    protected void mouseClicked(int x, int y, int key) {
        super.mouseClicked(x, y, key);

        if (switchToMode != null) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("mode", switchToMode.name());

            NetworkHandler.instance.sendToServer(new NBTControlPacket(data));

            playClick();
        }

        if (switchToBlock != null) {
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("block", Block.getIdFromBlock(switchToBlock.block));
            data.setByte("meta", (byte)switchToBlock.meta);

            NetworkHandler.instance.sendToServer(new NBTControlPacket(data));

            playClick();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static boolean inTriangle(final double x1, final double y1, final double x2, final double y2,
                final double x3, final double y3, final double x, final double y) {
        final double ab = (x1 - x) * (y2 - y) - (x2 - x) * (y1 - y);
        final double bc = (x2 - x) * (y3 - y) - (x3 - x) * (y2 - y);
        final double ca = (x3 - x) * (y1 - y) - (x1 - x) * (y3 - y);
        return sign(ab) == sign(bc) && sign(bc) == sign(ca);
    }

    private static int sign(final double n) {
        return n > 0 ? 1 : -1;
    }

    private static void playClick() {
        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
    }
    
}
