package net.mellow.effortless.compat;

import net.mellow.effortless.api.BlockRegistry;

public class Compat {

    public static final String MODID_NTM = "hbm";
    public static final String MODID_COFH = "CoFHAPI";
    public static final String MODID_BAUBLES = "Baubles|Expanded";
    public static final String MODID_AE2 = "appliedenergistics2";

    public static void register() {

        // ArchitectureCraft
        BlockRegistry.addToWhitelist("ArchitectureCraft", "shape");
        BlockRegistry.addToWhitelist("ArchitectureCraft", "shapeSE");

        // Carpenter's Blocks
        BlockRegistry.addToWhitelist("CarpentersBlocks", "blockCarpentersBlock");
        BlockRegistry.addToWhitelist("CarpentersBlocks", "blockCarpentersBarrier");
        BlockRegistry.addToWhitelist("CarpentersBlocks", "blockCarpentersButton");
        BlockRegistry.addToWhitelist("CarpentersBlocks", "blockCarpentersGate");
        BlockRegistry.addToWhitelist("CarpentersBlocks", "blockCarpentersLadder");
        BlockRegistry.addToWhitelist("CarpentersBlocks", "blockCarpentersPressurePlate");
        BlockRegistry.addToWhitelist("CarpentersBlocks", "blockCarpentersSlope");
        BlockRegistry.addToWhitelist("CarpentersBlocks", "blockCarpentersStairs");
        BlockRegistry.addToWhitelist("CarpentersBlocks", "blockCarpentersTorch");

        // Storage Drawers
        registerStorageDrawers("StorageDrawers");
        registerStorageDrawers("StorageDrawersBop");
        registerStorageDrawers("StorageDrawersForestry");
        registerStorageDrawers("StorageDrawersNatura");
        registerStorageDrawers("StorageDrawersErebus");
        registerStorageDrawers("StorageDrawersMisc");

        // JABBA
        BlockRegistry.addToLeftClickBlacklist("JABBA", "barrel");

    }

    // Storage Drawers has compat for a loooot of mods, we try to support them all here
    private static void registerStorageDrawers(String modid) {
        BlockRegistry.addToLeftClickBlacklist(modid, "fullDrawers1");
        BlockRegistry.addToLeftClickBlacklist(modid, "fullDrawers2");
        BlockRegistry.addToLeftClickBlacklist(modid, "fullDrawers4");
        BlockRegistry.addToLeftClickBlacklist(modid, "halfDrawers2");
        BlockRegistry.addToLeftClickBlacklist(modid, "halfDrawers4");
        BlockRegistry.addToLeftClickBlacklist(modid, "compDrawers");
        BlockRegistry.addToLeftClickBlacklist(modid, "fullCustom1");
        BlockRegistry.addToLeftClickBlacklist(modid, "fullCustom2");
        BlockRegistry.addToLeftClickBlacklist(modid, "fullCustom4");
        BlockRegistry.addToLeftClickBlacklist(modid, "halfCustom2");
        BlockRegistry.addToLeftClickBlacklist(modid, "halfCustom4");
    }

}
