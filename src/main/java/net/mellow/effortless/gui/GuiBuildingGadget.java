package net.mellow.effortless.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import net.mellow.effortless.Effortless;
import net.mellow.effortless.Keybinds;
import net.mellow.effortless.blocks.BlockMeta;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingMode;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.items.ItemBuildingGadget;
import net.mellow.effortless.network.NBTControlPacket;
import net.mellow.effortless.network.NetworkHandler;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

public class GuiBuildingGadget extends GuiScreen {

    private static ResourceLocation buildIcons = new ResourceLocation(Effortless.MODID, "textures/gui/icons.png");
    
    private static RenderItem renderItem = new RenderItem();

    private ItemStack gadget;
    private boolean itemless;

    private BuildingMode currentMode;
    private BlockMeta currentBlock;
    private Map<BuildingOption, BuildingAction> currentOptions;

    private List<BlockMeta> usableBlocks = new ArrayList<>();
    private List<ItemStack> usableBlockStacks = new ArrayList<>(); // tiny bit clunky but hey this is almost certainly getting refactored once I realise a lot of block data is stored in itemstack NBT for some mods like architecturecraft... wait shit

    // For mouse click interactions
    private BuildingMode switchToMode = null;
    private BlockMeta switchToBlock = null;
    private BuildingAction performAction = null;

    private BuildingOption switchToOptionName = null;
    private BuildingAction switchToOptionValue = null;

    private long blockNameTimerMs;
    private long lastMs;

    public GuiBuildingGadget(ItemStack stack) {
        this.gadget = stack;
    }

    public GuiBuildingGadget(ItemStack stack, boolean itemless) {
        this(stack);
        this.itemless = itemless;
    }

    @Override
    public void initGui() {
        currentMode = ItemBuildingGadget.getMode(gadget);
        currentBlock = ItemBuildingGadget.getSelected(gadget);
        currentOptions = ItemBuildingGadget.getOptions(gadget);

        EntityPlayer player = this.mc.thePlayer;
        for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);

            BlockMeta type = BlockMeta.fromStack(stack);
            if (type == null) continue;

            if (type.block.hasTileEntity(type.meta)) continue;
            if (usableBlocks.contains(type)) continue;

            usableBlocks.add(type);
            usableBlockStacks.add(stack);
        }
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
    public void handleMouseInput() {
        super.handleMouseInput();

        int scroll = Mouse.getEventDWheel();
        if (scroll != 0 && !usableBlocks.isEmpty()) {
            int index = usableBlocks.indexOf(currentBlock);

            if (scroll > 0) index--;
            if (scroll < 0) index++;

            if (index >= usableBlocks.size()) index = 0;
            if (index < 0) index = usableBlocks.size() - 1;

            switchToBlock = usableBlocks.get(index);
            blockNameTimerMs = 2_000;

            switchBlock();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);

        int numberKey = Character.getNumericValue(typedChar) - 1;
        if (numberKey >= 0 && numberKey < usableBlocks.size()) {
            switchToBlock = usableBlocks.get(numberKey);
            blockNameTimerMs = 2_000;

            switchBlock();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        
        super.drawScreen(mouseX, mouseY, partialTicks);

        Tessellator tessellator = Tessellator.instance;

        BuildingMode[] modes = itemless ? BuildingMode.getItemlessModes() : BuildingMode.getRegularModes();
        BuildingAction[] actions = BuildingAction.getGlobalActions();
        BuildingOption[] options = currentMode.options;

        double midX = width / 2;
        double midY = height / 2;
        double mx = mouseX - midX;
        double my = mouseY - midY;

        // reset mouseover selection
        switchToMode = null;
        switchToBlock = null;
        performAction = null;
        switchToOptionName = null;
        switchToOptionValue = null;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4d(1, 1, 1, 1);

        tessellator.startDrawingQuads();


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
        double radiansPer = 2 * Math.PI / modes.length;

        for (int i = 0; i < modes.length; i++) {
            BuildingMode mode = modes[i];

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
        double blockXOffset = -(usableBlocks.size() * btnWidth + (usableBlocks.size() - 1) * padding) / 2;
        double blockYOffset = 70;

        if (!itemless) {
            for (int i = 0; i < usableBlocks.size(); i++) {
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
        }



        // Draw action buttons
        double actionXOffset = -100 - (actions.length * btnWidth + (actions.length - 1) * padding); // right aligned
        double actionYOffset = -20;

        for (int i = 0; i < actions.length; i++) {
            BuildingAction action = actions[i];

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



        // Draw build mode options
        double optionXOffset = 100; // left aligned
        double optionYOffset = -20; 
        for (int i = 0; i < options.length; i++) {
            BuildingOption option = options[i];

            for (int o = 0; o < option.actions.length; o++) {
                BuildingAction action = option.actions[o];

                double x1 = midX + o * btnWidth + o * padding + optionXOffset;
                double x2 = x1 + btnWidth;
                double y1 = midY + i * btnWidth + i * 20 + optionYOffset;
                double y2 = y1 + btnWidth;

                boolean isSelected = action == currentOptions.get(option);
                boolean isHighlighted = x1 <= mouseX && x2 >= mouseX && y1 <= mouseY && y2 >= mouseY;

                if (isHighlighted) {
                    switchToOptionName = option;
                    switchToOptionValue = action;
                }

                tessellator.setColorRGBA(0, 0, 0, isHighlighted ? 255 : isSelected ? 170 : 80);
                
                tessellator.addVertex(x1, y1, 0);
                tessellator.addVertex(x1, y2, 0);
                tessellator.addVertex(x2, y2, 0);
                tessellator.addVertex(x2, y1, 0);
            }
        }



        tessellator.draw();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);



        // Draw radial menu icons + text
        double textDistance = 75;
        for (int i = 0; i < modes.length; i++) {
            BuildingMode mode = modes[i];

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
        if (!itemless) {
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.enableGUIStandardItemLighting();
    
            long thisMs = System.currentTimeMillis();
            blockNameTimerMs -= thisMs - lastMs;
            lastMs = thisMs;
    
            for (int i = 0; i < usableBlocks.size(); i++) {
                BlockMeta block = usableBlocks.get(i);
                double x = midX + i * btnWidth + i * padding + blockXOffset;
                double y = midY + blockYOffset;
                renderItem.renderItemIntoGUI(this.fontRendererObj, this.mc.getTextureManager(), new ItemStack(block.block, 1, block.meta), (int)x + 4, (int)y + 4);
    
                if (switchToBlock != null ? block.equals(switchToBlock) : blockNameTimerMs > 0 && block.equals(currentBlock)) {
                    ItemStack stack = usableBlockStacks.get(i);
                    String text = I18n.format(stack.getItem().getUnlocalizedName(stack) + ".name");
                    int tx = (int) midX - fontRendererObj.getStringWidth(text) / 2;
                    int ty = (int) (midY + blockYOffset + btnWidth + 8);
        
                    drawString(fontRendererObj, text, tx, ty, 0xFFFFFFFF);
                }
            }
    
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    
            if (usableBlocks.isEmpty()) {
                String text = I18n.format("buildingui.noblocks");
                int tx = (int) midX - fontRendererObj.getStringWidth(text) / 2;
                int ty = (int) (midY + blockYOffset + btnWidth + 8);
    
                drawString(fontRendererObj, text, tx, ty, 0xFFAA0000);
                GL11.glColor4d(1, 1, 1, 1);
            }
        }



        // Draw action button icons
        for (int i = 0; i < actions.length; i++) {
            BuildingAction action = actions[i];
            double x = midX + i * btnWidth + i * padding + actionXOffset;
            double y = midY + actionYOffset;

            mc.getTextureManager().bindTexture(buildIcons);
            drawTexturedModalRect((int)(x + 4), (int)(y + 4), action.iconX, action.iconY, 16, 16);

            if (action == performAction) {
                String text = I18n.format(action.getUnlocalizedName());
                int tx = (int) (midX - 100 - fontRendererObj.getStringWidth(text));
                int ty = (int) (midY + actionYOffset + btnWidth + padding + 6);
    
                drawString(fontRendererObj, text, tx, ty, 0xFFFFFFFF);
            }
        }



        // Draw option button icons
        for (int i = 0; i < options.length; i++) {
            BuildingOption option = options[i];

            for (int o = 0; o < option.actions.length; o++) {
                BuildingAction action = option.actions[o];

                double x = midX + o * btnWidth + o * padding + optionXOffset;
                double y = midY + i * btnWidth + i * 20 + optionYOffset;

                mc.getTextureManager().bindTexture(buildIcons);
                drawTexturedModalRect((int)(x + 4), (int)(y + 4), action.iconX, action.iconY, 16, 16);

                if (action == switchToOptionValue) {
                    String text = I18n.format(action.getUnlocalizedName());
                    int tx = (int) (midX + 100);
                    int ty = (int) (midY + optionYOffset + (options.length * btnWidth) + (options.length * padding) + 6);
        
                    drawString(fontRendererObj, text, tx, ty, 0xFFFFFFFF);
                }
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
        if (updateActions()) playClick();
    }

    private boolean updateActions() {
        boolean performedAction = switchBlock();

        if (switchToMode != null) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("mode", switchToMode.name());

            NetworkHandler.instance.sendToServer(new NBTControlPacket(data));
            currentMode = switchToMode;

            performedAction = true;
        }

        if (performAction != null) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("action", performAction.name());

            NetworkHandler.instance.sendToServer(new NBTControlPacket(data));

            performedAction = true;
        }

        if (switchToOptionName != null && switchToOptionValue != null) {
            NBTTagCompound data = new NBTTagCompound();
            data.setString("option", switchToOptionName.name());
            data.setString("value", switchToOptionValue.name());

            NetworkHandler.instance.sendToServer(new NBTControlPacket(data));
            currentOptions.put(switchToOptionName, switchToOptionValue);

            performedAction = true;
        }

        return performedAction;
    }

    private boolean switchBlock() {
        if (!itemless && switchToBlock != null) {
            NBTTagCompound data = new NBTTagCompound();
            data.setInteger("block", Block.getIdFromBlock(switchToBlock.block));
            data.setByte("meta", (byte)switchToBlock.meta);

            NetworkHandler.instance.sendToServer(new NBTControlPacket(data));
            currentBlock = switchToBlock;

            return true;
        }

        return false;
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
