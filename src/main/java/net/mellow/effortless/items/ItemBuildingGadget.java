package net.mellow.effortless.items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.input.Keyboard;

import api.hbm.energymk2.IBatteryItem;
import baubles.api.BaubleType;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import cofh.api.energy.IEnergyContainerItem;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.mellow.effortless.Config;
import net.mellow.effortless.Keybinds;
import net.mellow.effortless.blocks.ConstructionSet;
import net.mellow.effortless.blocks.PlaceableStack;
import net.mellow.effortless.buildmode.BaseBuildMode.Operation;
import net.mellow.effortless.buildmode.BuildModes;
import net.mellow.effortless.buildmode.History;
import net.mellow.effortless.buildmode.ModeOptions.BuildingAction;
import net.mellow.effortless.buildmode.ModeOptions.BuildingMode;
import net.mellow.effortless.buildmode.ModeOptions.BuildingOption;
import net.mellow.effortless.compat.Compat;
import net.mellow.effortless.compat.CompatBaublesExpanded;
import net.mellow.effortless.gui.GuiBuildingGadget;
import net.mellow.effortless.network.IItemControlReceiver;
import net.mellow.effortless.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemFlintAndSteel;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

@Optional.InterfaceList({
    @Optional.Interface(iface = "cofh.api.energy.IEnergyContainerItem", modid = Compat.MODID_COFH),
    @Optional.Interface(iface = "api.hbm.energymk2.IBatteryItem", modid = Compat.MODID_NTM),
    @Optional.Interface(iface = "baubles.api.expanded.IBaubleExpanded", modid = Compat.MODID_BAUBLES),
})
public class ItemBuildingGadget extends ItemFlintAndSteel implements IItemRenderPreview, IItemGuiProvider, IItemControlReceiver, IEnergyContainerItem, IBatteryItem, IBaubleExpanded {

    // why ItemFlintAndSteel?
    // A bunch of mods like Adventure Backpacks use these classes to determine if something is a "tool",
    // and this Item subclass is by far the easiest one to inherit without breaking anything

    private static boolean hasRF;
    private static boolean hasHE;

    static {
        try {
            Class.forName("cofh.api.energy.IEnergyContainerItem");
            hasRF = true;
        } catch (Exception ex) {
            hasRF = false;
        }
        hasHE = Loader.isModLoaded("hbm");
    }

    public ItemBuildingGadget() {
        this.setCreativeTab(CreativeTabs.tabTools);
        this.setMaxStackSize(1);
        this.setFull3D();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean bool) {
        if (Config.consumesEnergy) {
            EnumChatFormatting chargeFormat = getEnergyStored(stack) >= Config.capacityRF / 10 ? EnumChatFormatting.BLUE : EnumChatFormatting.RED;
            if (hasRF) list.add(chargeFormat + I18n.format("energy.stored.rf", MathUtil.getShortNumber(getEnergyStored(stack)), MathUtil.getShortNumber(getMaxEnergyStored(stack))));
            if (hasHE) list.add(chargeFormat + I18n.format("energy.stored.he", MathUtil.getShortNumber(getCharge(stack)), MathUtil.getShortNumber(getMaxCharge(stack))));
        }

        if (CompatBaublesExpanded.loaded) {
            list.add(EnumChatFormatting.GRAY + I18n.format("item.building_gadget.bauble"));
        }

        list.add(EnumChatFormatting.YELLOW + I18n.format("hint.uikey.usage", Keyboard.getKeyName(Keybinds.uiKey.getKeyCode())));
    }

    public static boolean isRenderingOverlay = true;

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (isRenderingOverlay) {
            BuildingMode mode = getMode(stack);
            if (mode.handler != null) {
                String overlayOverride = ConstructionSet.getItemHighlight(stack);
                if (overlayOverride != null) return overlayOverride;
            }
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        return onItemRightClick(stack, world, player, getSelected(stack));
    }

    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player, ItemStack selected) {
        onItemClick(stack, world, player, selected, Operation.PLACE);

        return stack;
    }

    public boolean onItemLeftClick(EntityPlayer player, ItemStack stack) {
        return onItemClick(stack, player.worldObj, player, stack, Operation.BREAK);
    }

    // Return true if a left click interaction should be cancelled
    public boolean onItemClick(ItemStack stack, World world, EntityPlayer player, ItemStack selected, Operation operation) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

        BuildingMode mode = getMode(stack);
        if (mode.handler == null) return false;

        // Clicking the other mouse button cancels whatever operation we're doing
        if (mode.handler.isPlacing(stack) && getOperation(stack) != operation) {
            return mode.handler.clear(stack);
        }

        stack.stackTagCompound.setString("operation", operation.toString());

        MovingObjectPosition mop = BuildModes.getMop(player, mode.handler.reach(stack));
        if (mop == null) return false; // only occurs on NaN

        boolean requiresPower = Config.consumesEnergy && !player.capabilities.isCreativeMode && (hasRF || hasHE);
        int energy = stack.stackTagCompound.getInteger("energy");

        if (requiresPower) {
            // require 10% charge to operate
            if (energy < Config.capacityRF / 10) return false;
        }

        if (operation == Operation.PLACE) {
            mode.handler.savePlaceable(stack, selected, world, player, mop);
        }
        
        // attempt to commit blocks to world if true, can still be cancelled if the set doesn't resolve
        if (mode.handler.click(stack, world, player, mop, operation)) {
            ConstructionSet set = mode.handler.getBlocks(stack, world, player, mop, operation);
            if (set == null) return false;

            mode.handler.clear(stack);
            int blocksModified = 0;

            if (operation == Operation.PLACE) {
                PlaceableStack placed = mode.handler.getPlaceable(stack);
                if (placed == null) return false;
    
                blocksModified = set.build(world, player, placed, false);
            } else {
                blocksModified = set.destroy(world, player);
            }

            if (requiresPower) {
                stack.stackTagCompound.setInteger("energy", Math.max(0, energy - blocksModified * Config.consumptionRF));
            }
        }

        return false;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float subX, float subY, float subZ) {
        return false;
    }


    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return Config.consumesEnergy && (hasRF || hasHE) && getEnergyStored(stack) < getMaxEnergyStored(stack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1 - (double) getEnergyStored(stack) / (double) getMaxEnergyStored(stack);
    }


    public static ItemStack getSelected(ItemStack stack) {
        if (stack.stackTagCompound == null) return new ItemStack(Blocks.stone);
        ItemStack selected = ItemStack.loadItemStackFromNBT(stack.stackTagCompound.getCompoundTag("selected"));
        if (selected == null) selected = new ItemStack(Blocks.stone);
        return selected;
    }

    // mode is stored as a string so inserting new modes won't fuck up existing tools
    public static BuildingMode getMode(ItemStack stack) {
        if (stack.stackTagCompound == null || !stack.stackTagCompound.hasKey("mode")) return BuildingMode.values()[1];
        try {
            return BuildingMode.valueOf(stack.stackTagCompound.getString("mode"));
        } catch (IllegalArgumentException ex) {
            return BuildingMode.values()[1];
        }
    }

    public static Map<BuildingOption, BuildingAction> getOptions(ItemStack stack) {
        Map<BuildingOption, BuildingAction> map = new HashMap<>();
        if (stack.stackTagCompound == null) return map;

        for (BuildingOption option : BuildingOption.values()) {
            try {
                map.put(option, BuildingAction.valueOf(stack.stackTagCompound.getString(option.name())));
            } catch (IllegalArgumentException ex) {
                map.put(option, option.actions[0]);
            }
        }

        return map;
    }

    public static BuildingAction getAction(ItemStack stack, BuildingOption option) {
        if (stack.stackTagCompound == null) return option.actions[0];
        try {
            return BuildingAction.valueOf(stack.stackTagCompound.getString(option.name()));
        } catch (IllegalArgumentException ex) {
            return option.actions[0];
        }
    }

    public static Operation getOperation(ItemStack stack) {
        if (stack.stackTagCompound == null) return Operation.PLACE;
        try {
            return Operation.valueOf(stack.stackTagCompound.getString("operation"));
        } catch (IllegalArgumentException ex) {
            return Operation.PLACE;
        }
    }

    private ConstructionSet lastRendered;
    private long lastTick;

    private ConstructionSet getCachedSet(World world, EntityPlayer player, ItemStack stack, MovingObjectPosition mop, BuildingMode mode, Operation operation) {
        if (!mode.handler.shouldRender(stack)) return null;

        if (world.getTotalWorldTime() == lastTick) {
            return lastRendered;
        }

        lastRendered = mode.handler.getBlocks(stack, world, player, mop, operation);
        lastTick = world.getTotalWorldTime();

        return lastRendered;
    }

    @Override
    public void render(World world, EntityPlayer player, ItemStack stack, float partialTicks) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

        BuildingMode mode = getMode(stack);
        if (mode.handler == null) return;

        MovingObjectPosition mop = BuildModes.getMop(player, mode.handler.reach(stack));
        if (mop == null) return; // only occurs for NaN

        Operation operation = getOperation(stack);

        ConstructionSet set = getCachedSet(world, player, stack, mop, mode, operation);
        if (set == null) {
            Minecraft.getMinecraft().renderGlobal.drawSelectionBox(player, mop, 0, partialTicks);
        } else {
            set.render(player, partialTicks, operation, mode.handler.showHighlight(stack));
        }
    }

    @Override
    public void provideGui(ItemStack stack, EntityPlayer player, ItemStack held) {
        FMLCommonHandler.instance().showGuiScreen(new GuiBuildingGadget(stack, stack != held));
    }

    @Override
    public void receiveControl(EntityPlayer player, ItemStack stack, NBTTagCompound nbt) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

        BuildingMode mode = getMode(stack);
        if (mode.handler != null) {
            getMode(stack).handler.clear(stack);
        }

        if (nbt.hasKey("mode")) stack.stackTagCompound.setString("mode", nbt.getString("mode"));
        if (nbt.hasKey("selected")) stack.stackTagCompound.setTag("selected", nbt.getTag("selected"));
        if (nbt.hasKey("meta")) stack.stackTagCompound.setByte("meta", nbt.getByte("meta"));

        if (nbt.hasKey("option")) {
            stack.stackTagCompound.setString(nbt.getString("option"), nbt.getString("value"));
        }

        if (nbt.hasKey("action")) {
            String action = nbt.getString("action");

            switch (action) {
                case "UNDO": History.undo(player.worldObj, player); break;
                case "REDO": History.redo(player.worldObj, player); break;
            }
        }

        CompatBaublesExpanded.syncBaubles(player);
    }


    /// FE ///
    @Override
    public int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        int energy = stack.stackTagCompound.getInteger("energy");
        int energyReceived = Math.min(Config.capacityRF - energy, maxReceive);

        if (!simulate) {
            energy += energyReceived;
            stack.stackTagCompound.setInteger("energy", energy);
        }

        return energyReceived;
    }

    @Override
    public int extractEnergy(ItemStack stack, int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored(ItemStack stack) {
        if (stack.stackTagCompound == null) return 0;
        return stack.stackTagCompound.getInteger("energy");
    }

    @Override
    public int getMaxEnergyStored(ItemStack stack) {
        return Config.capacityRF;
    }
    /// /FE ///


    /// HE ///
    @Override
    public void chargeBattery(ItemStack stack, long power) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        int energy = stack.stackTagCompound.getInteger("energy");
        energy += Math.max(1, Math.round(power / Config.conversionHEtoRF));
        stack.stackTagCompound.setInteger("energy", energy);
    }

    @Override
    public void setCharge(ItemStack stack, long power) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setInteger("energy", Math.round(power / Config.conversionHEtoRF));
    }

    @Override
    public void dischargeBattery(ItemStack stack, long energy) {}

    @Override
    public long getCharge(ItemStack stack) {
        if (stack.stackTagCompound == null) return 0;
        return Math.round(stack.stackTagCompound.getInteger("energy") * Config.conversionHEtoRF);
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return Math.round(Config.capacityRF * Config.conversionHEtoRF);
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return 10_000;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return 0;
    }
    /// /HE ///

    @Override
    public boolean canEquip(ItemStack stack, EntityLivingBase entity) {
        return true;
    }

    @Override
    public boolean canUnequip(ItemStack stack, EntityLivingBase entity) {
        return true;
    }

    @Override
    public BaubleType getBaubleType(ItemStack stack) {
        return null;
    }

    @Override
    public void onEquipped(ItemStack stack, EntityLivingBase entity) {}

    @Override
    public void onUnequipped(ItemStack stack, EntityLivingBase entity) {}

    @Override
    public void onWornTick(ItemStack stack, EntityLivingBase entity) {}

    @Override
    public String[] getBaubleTypes(ItemStack stack) {
        return new String[] { BaubleExpandedSlots.charmType };
    }

}
