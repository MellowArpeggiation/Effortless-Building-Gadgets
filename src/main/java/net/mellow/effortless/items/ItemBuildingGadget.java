package net.mellow.effortless.items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.AEColor;
import appeng.api.util.IConfigManager;
import com.gtnewhorizon.gtnhlib.item.ItemStackNBT;
import cpw.mods.fml.common.FMLLog;
import net.mellow.effortless.compat.CompatAE2;
import org.lwjgl.input.Keyboard;

import api.hbm.energymk2.IBatteryItem;
import baubles.api.BaubleType;
import baubles.api.expanded.BaubleExpandedSlots;
import baubles.api.expanded.IBaubleExpanded;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import cofh.api.energy.IEnergyContainerItem;
import net.mellow.effortless.Keybinds;
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

import static net.mellow.effortless.compat.CompatAE2.hasAE2;

@Optional.InterfaceList({
    @Optional.Interface(iface = "cofh.api.energy.IEnergyContainerItem", modid = Compat.MODID_COFH),
    @Optional.Interface(iface = "api.hbm.energymk2.IBatteryItem", modid = Compat.MODID_NTM),
    @Optional.Interface(iface = "baubles.api.expanded.IBaubleExpanded", modid = Compat.MODID_BAUBLES),
    @Optional.Interface(iface = "appeng.api.features.IWirelessTermHandler", modid = Compat.MODID_AE2)
})
public class ItemBuildingGadget extends ItemFlintAndSteel implements IItemRenderPreview, IItemGuiProvider, IItemControlReceiver, IEnergyContainerItem, IBatteryItem, IBaubleExpanded, IWirelessTermHandler {

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
        EnumChatFormatting chargeFormat = getEnergyStored(stack) >= capacity / 10 ? EnumChatFormatting.BLUE : EnumChatFormatting.RED;
        if (hasRF) list.add(chargeFormat + I18n.format("energy.stored.rf", MathUtil.getShortNumber(getEnergyStored(stack)), MathUtil.getShortNumber(getMaxEnergyStored(stack))));
        if (hasHE) list.add(chargeFormat + I18n.format("energy.stored.he", MathUtil.getShortNumber(getCharge(stack)), MathUtil.getShortNumber(getMaxCharge(stack))));
        list.add(EnumChatFormatting.YELLOW + I18n.format("hint.uikey.usage", Keyboard.getKeyName(Keybinds.uiKey.getKeyCode())));
        if (hasAE2) {
            final String encKey = ItemStackNBT.getString(stack, "encryptionKey");
            if(encKey == null || encKey.isEmpty()){
                list.add(EnumChatFormatting.RED + I18n.format("status.unlinked"));
            }
            else list.add(EnumChatFormatting.RED + I18n.format("status.linked"));
        }
    }

    public static boolean isRenderingOverlay = true;
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (isRenderingOverlay) {
            BuildingMode mode = getMode(stack);
            if (mode.handler != null) {
                String overlayOverride = mode.handler.getItemHighlight(stack);
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
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

        BuildingMode mode = getMode(stack);
        if (mode.handler == null) return stack;

        MovingObjectPosition mop = BuildModes.getMop(player, mode.handler.reach(stack));

        boolean requiresPower = !player.capabilities.isCreativeMode && (hasRF || hasHE);
        int energy = stack.stackTagCompound.getInteger("energy");

        if (requiresPower) {
            // require 10% charge to operate
            if (energy < capacity / 10) return stack;
        }

        int blocksPlaced = mode.handler.add(stack, selected, world, player, mop);

        if (requiresPower) {
            stack.stackTagCompound.setInteger("energy", Math.max(0, energy - blocksPlaced * consumption));
        }

        return stack;
    }

    @Override
    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        BuildingMode mode = getMode(stack);
        if (mode.handler == null) return false;

        mode.handler.clear(stack);
        return false;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float subX, float subY, float subZ) {
        return false;
    }


    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return (hasRF || hasHE) && getEnergyStored(stack) < getMaxEnergyStored(stack);
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


    @Override
    public void render(World world, EntityPlayer player, ItemStack stack, float partialTicks) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        BuildingMode mode = getMode(stack);
        if (mode.handler == null) return;
        mode.handler.render(stack, world, player, partialTicks);
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

    // POWERRRRRR
    private int capacity = 1_000_000; // 5 MHE
    private int consumption = 20; // 100 HE


    /// FE ///
    @Override
    public int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        int energy = stack.stackTagCompound.getInteger("energy");
        int energyReceived = Math.min(capacity - energy, maxReceive);

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
        return capacity;
    }
    /// /FE ///


    /// HE ///
    @Override
    public void chargeBattery(ItemStack stack, long power) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        int energy = stack.stackTagCompound.getInteger("energy");
        energy += Math.max(1, (int) (power / 5));
        stack.stackTagCompound.setInteger("energy", energy);
    }

    @Override
    public void setCharge(ItemStack stack, long power) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();
        stack.stackTagCompound.setInteger("energy", (int) (power / 5));
    }

    @Override
    public void dischargeBattery(ItemStack stack, long energy) {
    }

    @Override
    public long getCharge(ItemStack stack) {
        if (stack.stackTagCompound == null) return 0;
        return stack.stackTagCompound.getInteger("energy") * 5;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return capacity * 5;
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
    @Optional.Method(modid = Compat.MODID_AE2)
    @Override
    public boolean canHandle(ItemStack is) {
        return true;
    }
    @Optional.Method(modid = Compat.MODID_AE2)
    @Override
    public boolean usePower(EntityPlayer player, double amount, ItemStack is) {
        if (is.stackTagCompound == null) is.stackTagCompound = new NBTTagCompound();
        int currentEnergy = getEnergyStored(is);
        int toDeduct = (int) Math.ceil(amount * 2);
        if (currentEnergy >= toDeduct) {
            is.stackTagCompound.setInteger("energy", currentEnergy - toDeduct);
            return true;
        }
        return false;
    }
    @Optional.Method(modid = Compat.MODID_AE2)
    @Override
    public boolean hasPower(EntityPlayer player, double amount, ItemStack is) {
        int energy = is.stackTagCompound.getInteger("energy");
        return energy > capacity / 10;
    }
    @Optional.Method(modid = Compat.MODID_AE2)
    @Override
    public IConfigManager getConfigManager(ItemStack is) {
        return null;
    }
    @Optional.Method(modid = Compat.MODID_AE2)
    @Override
    public String getEncryptionKey(ItemStack item) {
        return ItemStackNBT.getString(item, "encryptionKey");
    }
    @Optional.Method(modid = Compat.MODID_AE2)
    @Override
    public void setEncryptionKey(ItemStack item, String encKey, String name) {
        final NBTTagCompound data = ItemStackNBT.get(item);
        final NBTTagCompound keys = data.getCompoundTag("encryptionKeys");

        if (!keys.hasKey(AEColor.values()[0].name()) && data.hasKey("encryptionKey")) {
            keys.setString(AEColor.values()[0].name(), data.getString("encryptionKey"));
        }

        String freeKey = "";
        for (int i = 0; i < 16; i++) {
            final String key = AEColor.values()[i].name();
            if (keys.hasKey(key)) {
                if (keys.getString(key).equals(encKey)) return;
            } else {
                freeKey = key;
                break;
            }
        }
        if (freeKey.isEmpty()) return;
        keys.setString(freeKey, encKey);
        data.setTag("encryptionKeys", keys);
        ItemStackNBT.of(item).setString("encryptionKey", encKey).setString("name", name);
    }
}
