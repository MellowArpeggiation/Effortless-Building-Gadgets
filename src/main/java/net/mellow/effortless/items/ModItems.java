package net.mellow.effortless.items;

import cpw.mods.fml.common.registry.GameRegistry;
import net.mellow.effortless.Effortless;
import net.minecraft.item.Item;

public class ModItems {
    
    public static void register() {
        initItems();
        registerItems();
    }

    public static Item building_gadget;

    private static void initItems() {

        building_gadget = new ItemBuildingGadget()
            .setUnlocalizedName("building_gadget")
            .setTextureName(Effortless.MODID + ":gadget");

    }

    private static void registerItems() {

        register(building_gadget);

    }

    private static void register(Item item) {
        GameRegistry.registerItem(item, item.getUnlocalizedName());
    }

}
