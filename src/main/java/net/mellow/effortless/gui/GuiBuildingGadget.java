package net.mellow.effortless.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import net.mellow.effortless.Effortless;
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
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GuiBuildingGadget extends GuiScreen {

    private static ResourceLocation buildIcons = new ResourceLocation(Effortless.MODID, "textures/gui/icons.png");
    
    private static RenderItem renderItem = new RenderItem();

    private ItemStack gadget;

    private BuildingMode currentMode;
    private BlockMeta currentBlock;

    private List<BlockMeta> usableBlocks = new ArrayList<>();
    private List<ItemStack> usableBlockStacks = new ArrayList<>(); // tiny bit clunky but hey this is almost certainly getting refactored once I realise a lot of block data is stored in itemstack NBT for some mods like architecturecraft... wait shit

    // For mouse click interactions
    private BuildingMode switchToMode = null;
    private BlockMeta switchToBlock = null;
    private BuildingAction performAction = null;

    public static enum BuildingAction {
        UNDO(16, 0),
        REDO(32, 0);

        public final int iconX;
        public final int iconY;

        private BuildingAction(int iconX, int iconY) {
            this.iconX = iconX;
            this.iconY = iconY;
        }

        public String getAction() {
            return name().toLowerCase(Locale.ROOT);
        }
        
        public String getUnlocalizedName() {
            return "buildingaction." + getAction() + ".name";
        }

        public String getUnlocalizedDesc() {
            return "buildingaction." + getAction() + ".desc";
        }
    }

    public GuiBuildingGadget(ItemStack stack) {
        this.gadget = stack;
    }

    @Override
    public void initGui() {
        currentMode = ItemBuildingGadget.getMode(gadget);
        currentBlock = ItemBuildingGadget.getSelected(gadget);

        EntityPlayer player = this.mc.thePlayer;
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);

            if (stack == null) continue;
            if (!(stack.getItem() instanceof ItemBlock)) continue;

            BlockMeta type = new BlockMeta(((ItemBlock) stack.getItem()).field_150939_a, stack.getItem().getMetadata(stack.getItemDamage()));

            if (type.block.hasTileEntity(type.meta)) continue;
            if (usableBlocks.contains(type)) continue;

            usableBlocks.add(type);
            usableBlockStacks.add(stack);
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
        int actionCount = BuildingAction.values().length;

        double midX = width / 2;
        double midY = height / 2;
        double mx = mouseX - midX;
        double my = mouseY - midY;

        // reset mouseover selection
        switchToMode = null;
        switchToBlock = null;
        performAction = null;

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
            BuildingMode mode = BuildingMode.values()[i];

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

            boolean isSelected = mode == currentMode;
            boolean isMouseInQuad = inTriangle(x1m1, y1m1, x2m2, y2m2, x2m1, y2m1, mx, my)
                || inTriangle(x1m1, y1m1, x1m2, y1m2, x2m2, y2m2, mx, my);
            boolean isHighlighted = begin <= mr && mr <= end && isMouseInQuad;

            if (isHighlighted) {
                switchToMode = mode;
            }

            tessellator.setColorRGBA(0, 0, 0, isHighlighted ? 255 : isSelected ? 170 : 80);

            tessellator.addVertex(midX + x1m1, midY + y1m1, 0);
            tessellator.addVertex(midX + x2m1, midY + y2m1, 0);
            tessellator.addVertex(midX + x2m2, midY + y2m2, 0);
            tessellator.addVertex(midX + x1m2, midY + y1m2, 0);
        }



        // Draw block selecting buttons
        double btnWidth = 24;
        double padding = 2;
        double blockXOffset = -(blockCount * btnWidth + (blockCount - 1) * padding) / 2;
        double blockYOffset = 70;

        for (int i = 0; i < blockCount; i++) {
            BlockMeta block = usableBlocks.get(i);

            double x1 = midX + i * btnWidth + i * padding + blockXOffset;
            double x2 = x1 + btnWidth;
            double y1 = midY + blockYOffset;
            double y2 = y1 + btnWidth;

            boolean isSelected = block.equals(currentBlock);
            boolean isHighlighted = x1 <= mouseX && x2 >= mouseX && y1 <= mouseY && y2 >= mouseY;

            if (isHighlighted) {
                switchToBlock = block;
            }

            tessellator.setColorRGBA(0, 0, 0, isHighlighted ? 255 : isSelected ? 170 : 80);
            
            tessellator.addVertex(x1, y1, 0);
            tessellator.addVertex(x1, y2, 0);
            tessellator.addVertex(x2, y2, 0);
            tessellator.addVertex(x2, y1, 0);
        }



        // Draw action buttons
        double actionXOffset = -(actionCount * btnWidth + (actionCount - 1) * padding) / 2 - 120;
        double actionYOffset = 0;

        for (int i = 0; i < actionCount; i++) {
            BuildingAction action = BuildingAction.values()[i];

            double x1 = midX + i * btnWidth + i * padding + actionXOffset;
            double x2 = x1 + btnWidth;
            double y1 = midY + actionYOffset;
            double y2 = y1 + btnWidth;

            boolean isHighlighted = x1 <= mouseX && x2 >= mouseX && y1 <= mouseY && y2 >= mouseY;

            if (isHighlighted) {
                performAction = action;
            }

            tessellator.setColorRGBA(0, 0, 0, isHighlighted ? 255 : 80);
            
            tessellator.addVertex(x1, y1, 0);
            tessellator.addVertex(x1, y2, 0);
            tessellator.addVertex(x2, y2, 0);
            tessellator.addVertex(x2, y1, 0);
        }

        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);



        // Draw radial menu icons + text
        double textDistance = 75;
        for (int i = 0; i < modeCount; i++) {
            BuildingMode mode = BuildingMode.values()[i];

            double begin = i * radiansPer - qtrCircle;
            double end = (i + 1) * radiansPer - qtrCircle;

            double x1 = Math.cos(begin);
            double x2 = Math.cos(end);
            double y1 = Math.sin(begin);
            double y2 = Math.sin(end);

            double x = (x1 + x2) * 0.5;
            double y = (y1 + y2) * 0.5;
            
            // icon
            double iconX = x * (ringOuter * 0.55 + 0.45 * ringInner);
            double iconY = y * (ringOuter * 0.55 + 0.45 * ringInner);

            mc.getTextureManager().bindTexture(buildIcons);
            drawTexturedModalRect((int)(midX + iconX - 8), (int)(midY + iconY - 8), mode.iconX, mode.iconY, 16, 16);

            // text
            if (mode == switchToMode) {
                int fx = (int) (x * textDistance);
                int fy = (int) (y * textDistance) - fontRendererObj.FONT_HEIGHT / 2;
                String text = I18n.format(mode.getUnlocalizedName());
    
                if (x <= -0.2) {
                    fx -= fontRendererObj.getStringWidth(text);
                } else if (-0.2 <= x && x <= 0.2) {
                    fx -= fontRendererObj.getStringWidth(text) / 2;
                }
    
                drawString(fontRendererObj, text, (int) midX + fx, (int) midY + fy, 0xFFFFFFFF);
            }
        }



        // Draw block selecting icons
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();

        for (int i = 0; i < blockCount; i++) {
            BlockMeta block = usableBlocks.get(i);
            double x = midX + i * btnWidth + i * padding + blockXOffset;
            double y = midY + blockYOffset;
            renderItem.renderItemIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), new ItemStack(block.block, 1, block.meta), (int)x + 4, (int)y + 4);

            if (block.equals(switchToBlock)) {
                ItemStack stack = usableBlockStacks.get(i);
                String text = I18n.format(stack.getItem().getUnlocalizedName(stack) + ".name");
                int tx = (int) midX - fontRendererObj.getStringWidth(text) / 2;
                int ty = (int) (midY + blockYOffset + btnWidth + 8);
    
                drawString(fontRendererObj, text, tx, ty, 0xFFFFFFFF);
            }
        }

        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);



        // Draw action button icons
        for (int i = 0; i < actionCount; i++) {
            BuildingAction action = BuildingAction.values()[i];
            double x = midX + i * btnWidth + i * padding + actionXOffset;
            double y = midY + actionYOffset;

            mc.getTextureManager().bindTexture(buildIcons);
            drawTexturedModalRect((int)(x + 4), (int)(y + 4), action.iconX, action.iconY, 16, 16);

            if (action == performAction) {
                String text = I18n.format(action.getUnlocalizedName());
                int tx = (int) (midX - 120 - fontRendererObj.getStringWidth(text) / 2);
                int ty = (int) (midY + actionYOffset + btnWidth + 8);
    
                drawString(fontRendererObj, text, tx, ty, 0xFFFFFFFF);
            }
        }



        // Draw helpful tooltip description, if available
        if (switchToMode != null) {
            String unloc = switchToMode.getUnlocalizedDesc();
            String text = I18n.format(unloc);

            if (!text.equals(unloc)) {
                int tx = (int) midX - fontRendererObj.getStringWidth(text) / 2;
                int ty = (int) midY - 100;

                drawString(fontRendererObj, text, tx, ty, 0xDD888888);
            }
        }
    }

    @Override
    protected void mouseClicked(int x, int y, int key) {
        super.mouseClicked(x, y, key);

        if (switchToMode != null) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("mode", switchToMode.name());

            NetworkHandler.instance.sendToServer(new NBTControlPacket(data));
            currentMode = switchToMode;

            playClick();
        }

        if (switchToBlock != null) {
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("block", Block.getIdFromBlock(switchToBlock.block));
            data.setByte("meta", (byte)switchToBlock.meta);

            NetworkHandler.instance.sendToServer(new NBTControlPacket(data));
            currentBlock = switchToBlock;

            playClick();
        }

        if (performAction != null) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("action", performAction.getAction());

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
