package net.mellow.effortless;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.effortless.items.ModItems;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

public class Crafting {
    
    public static void register() {
        // Automatically detect relevant mods and use ingredients from them for crafting

        // Surrounding metal should use processed steel if available
        Object metal = "ingotIron";
        if (OreDictionary.doesOreNameExist("plateSteel")) {
            metal = "plateSteel";
        } else if (OreDictionary.doesOreNameExist("ingotSteel")) {
            metal = "ingotSteel";
        }

        // Transmitter OR display component
        Object transmitter = Items.ender_eye;
        if (OreDictionary.doesOreNameExist("stickLongSteelMagnetic")) {
            transmitter = "stickLongSteelMagnetic";
        } else {
            transmitter = tryGetItem("hbm:item.crt_display", transmitter);
        }

        // Circuit
        Object circuit = Blocks.redstone_lamp;
        if (OreDictionary.doesOreNameExist("circuitBasic")) {
            circuit = "circuitBasic";
        } else {
            ModContainer ntm = Loader.instance().getIndexedModList().get("hbm");
            if (ntm != null) {
                Item item = (Item) Item.itemRegistry.getObject("hbm:item.circuit");
                if (item != null) {
                    // james why did you add space circuits in between existing circuits
                    circuit = new ItemStack(item, 1, isSpace(ntm) ? 8 : 7);
                }
            }
        }


        GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(ModItems.building_gadget), new Object[] { "IEI", " G ", "IRI", 'I', metal, 'E', transmitter, 'G', "blockGlass", 'R', circuit }));
    }

    private static Object tryGetItem(String name, Object fallback) {
        Object item = Item.itemRegistry.getObject(name);
        return item != null ? item : fallback;
    }

    private static boolean isSpace(ModContainer container) {
        return container.getName().contains("Space");
    }

}
