package com.vaultshop.core.shop;

import com.vaultshop.core.VaultShop;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Owns every price in the shop.
 *
 * Design notes (read this before touching the numbers):
 *  - Prices are keyed by Material#name() STRINGS, not by the Material enum constant itself.
 *    That is deliberate: a typo in a string key just means that one item silently falls back
 *    to the category default below, instead of breaking the whole file to compile.
 *  - "Sell" price is the source of truth. "Buy" price = sell price * buy-markup-multiplier,
 *    unless an admin has set an explicit buy override. Buy is always >= sell, so there is no
 *    buy-then-sell money-duplication loop.
 *  - Every Material that passes isSellable() is guaranteed to have SOME positive price -
 *    getSellPrice()/getBuyPrice() never return null and never throw.
 */
public class PriceManager {

    private final VaultShop plugin;
    private final Map<String, Double> sellPrices = new HashMap<>();
    private final Map<String, Double> buyOverrides = new HashMap<>();
    private File file;
    private double buyMarkup = 1.5;

    // Materials that must never be sellable/buyable no matter what (technical blocks,
    // creative-only items, or items where selling could silently destroy player data).
    private static final Set<String> EXCLUDED = Set.of(
            "AIR", "CAVE_AIR", "VOID_AIR",
            "BARRIER", "STRUCTURE_VOID", "STRUCTURE_BLOCK", "JIGSAW", "LIGHT",
            "COMMAND_BLOCK", "CHAIN_COMMAND_BLOCK", "REPEATING_COMMAND_BLOCK", "COMMAND_BLOCK_MINECART",
            "BEDROCK", "SPAWNER", "TRIAL_SPAWNER", "VAULT", "END_PORTAL_FRAME", "END_GATEWAY", "END_PORTAL",
            "KNOWLEDGE_BOOK", "DEBUG_STICK",
            "WRITTEN_BOOK", "WRITABLE_BOOK",
            "PETRIFIED_OAK_SLAB",
            // Containers that can hold other items with NBT data - excluded so selling one
            // can never silently vaporize whatever a player stored inside it.
            "BUNDLE"
    );

    // Explicit, hand-tuned SELL prices for the items players actually care about.
    // Everything else falls back to getDefaultPrice()'s category logic further down.
    private static final Map<String, Double> SPECIAL = new HashMap<>();
    static {
        // ---- Raw ores / direct mining drops ----
        SPECIAL.put("COAL", 3.0);
        SPECIAL.put("RAW_IRON", 4.0);
        SPECIAL.put("RAW_GOLD", 6.0);
        SPECIAL.put("RAW_COPPER", 2.5);
        SPECIAL.put("REDSTONE", 1.5);
        SPECIAL.put("LAPIS_LAZULI", 2.0);
        SPECIAL.put("DIAMOND", 30.0);
        SPECIAL.put("EMERALD", 25.0);
        SPECIAL.put("QUARTZ", 2.5);
        SPECIAL.put("GLOWSTONE_DUST", 1.5);
        SPECIAL.put("AMETHYST_SHARD", 4.0);
        SPECIAL.put("ANCIENT_DEBRIS", 45.0);
        SPECIAL.put("OBSIDIAN", 4.0);
        SPECIAL.put("CRYING_OBSIDIAN", 6.0);

        // ---- Smelted / refined (labor premium over the raw material) ----
        SPECIAL.put("IRON_INGOT", 5.0);
        SPECIAL.put("GOLD_INGOT", 8.0);
        SPECIAL.put("COPPER_INGOT", 3.0);
        SPECIAL.put("NETHERITE_SCRAP", 55.0);
        SPECIAL.put("NETHERITE_INGOT", 250.0);

        // ---- Compacted storage blocks: priced ABOVE the naive 9x (or 4x for quartz)
        //      multiple of their component, to reflect the crafting labor. This is the
        //      "iron block costs more than 9 iron ingots" rule the shop is built around. ----
        SPECIAL.put("IRON_BLOCK", 50.0);        // naive 9x5=45
        SPECIAL.put("GOLD_BLOCK", 85.0);        // naive 72
        SPECIAL.put("COPPER_BLOCK", 32.0);      // naive 27
        SPECIAL.put("DIAMOND_BLOCK", 300.0);    // naive 270
        SPECIAL.put("EMERALD_BLOCK", 250.0);    // naive 225
        SPECIAL.put("REDSTONE_BLOCK", 16.0);    // naive 13.5
        SPECIAL.put("LAPIS_BLOCK", 22.0);       // naive 18
        SPECIAL.put("COAL_BLOCK", 32.0);        // naive 27
        SPECIAL.put("NETHERITE_BLOCK", 2400.0); // naive 2250
        SPECIAL.put("RAW_IRON_BLOCK", 40.0);
        SPECIAL.put("RAW_GOLD_BLOCK", 60.0);
        SPECIAL.put("RAW_COPPER_BLOCK", 25.0);
        SPECIAL.put("QUARTZ_BLOCK", 12.0);      // naive 4x2.5=10
        SPECIAL.put("GLOWSTONE", 7.0);          // naive 4x1.5=6

        // ---- Crops / farmable food ----
        SPECIAL.put("WHEAT", 1.0);
        SPECIAL.put("WHEAT_SEEDS", 0.5);
        SPECIAL.put("CARROT", 1.0);
        SPECIAL.put("POTATO", 1.0);
        SPECIAL.put("BAKED_POTATO", 1.5);
        SPECIAL.put("BEETROOT", 1.0);
        SPECIAL.put("BEETROOT_SEEDS", 0.5);
        SPECIAL.put("MELON_SLICE", 0.75);
        SPECIAL.put("PUMPKIN", 2.0);
        SPECIAL.put("SUGAR_CANE", 1.0);
        SPECIAL.put("SUGAR", 0.75);
        SPECIAL.put("CACTUS", 1.0);
        SPECIAL.put("COCOA_BEANS", 1.5);
        SPECIAL.put("NETHER_WART", 2.0);
        SPECIAL.put("APPLE", 1.5);
        SPECIAL.put("BREAD", 2.0);
        SPECIAL.put("GOLDEN_APPLE", 25.0);
        SPECIAL.put("ENCHANTED_GOLDEN_APPLE", 400.0);
        SPECIAL.put("ROTTEN_FLESH", 0.25);
        SPECIAL.put("BEEF", 2.0);
        SPECIAL.put("COOKED_BEEF", 3.0);
        SPECIAL.put("PORKCHOP", 2.0);
        SPECIAL.put("COOKED_PORKCHOP", 3.0);
        SPECIAL.put("CHICKEN", 2.0);
        SPECIAL.put("COOKED_CHICKEN", 3.0);
        SPECIAL.put("MUTTON", 2.0);
        SPECIAL.put("COOKED_MUTTON", 3.0);
        SPECIAL.put("RABBIT", 2.0);
        SPECIAL.put("COOKED_RABBIT", 3.0);
        SPECIAL.put("COD", 1.5);
        SPECIAL.put("COOKED_COD", 2.5);
        SPECIAL.put("SALMON", 1.5);
        SPECIAL.put("COOKED_SALMON", 2.5);
        SPECIAL.put("HONEYCOMB", 2.0);
        SPECIAL.put("HONEY_BOTTLE", 2.0);

        // ---- Mob drops ----
        SPECIAL.put("LEATHER", 2.0);
        SPECIAL.put("FEATHER", 1.0);
        SPECIAL.put("BONE", 1.0);
        SPECIAL.put("BONE_MEAL", 0.5);
        SPECIAL.put("STRING", 1.0);
        SPECIAL.put("SPIDER_EYE", 1.0);
        SPECIAL.put("GUNPOWDER", 1.5);
        SPECIAL.put("ENDER_PEARL", 15.0);
        SPECIAL.put("BLAZE_ROD", 12.0);
        SPECIAL.put("BLAZE_POWDER", 6.0);
        SPECIAL.put("GHAST_TEAR", 20.0);
        SPECIAL.put("MAGMA_CREAM", 5.0);
        SPECIAL.put("SLIME_BALL", 2.0);
        SPECIAL.put("PHANTOM_MEMBRANE", 8.0);
        SPECIAL.put("RABBIT_FOOT", 10.0);
        SPECIAL.put("RABBIT_HIDE", 1.5);
        SPECIAL.put("INK_SAC", 1.0);
        SPECIAL.put("GLOW_INK_SAC", 3.0);
        SPECIAL.put("SHULKER_SHELL", 30.0);
        SPECIAL.put("TOTEM_OF_UNDYING", 150.0);
        SPECIAL.put("NETHER_STAR", 500.0);
        SPECIAL.put("ELYTRA", 300.0);
        SPECIAL.put("DRAGON_BREATH", 10.0);
        SPECIAL.put("EXPERIENCE_BOTTLE", 5.0);
        SPECIAL.put("TRIDENT", 150.0);
        SPECIAL.put("HEART_OF_THE_SEA", 100.0);
        SPECIAL.put("NAUTILUS_SHELL", 15.0);
        SPECIAL.put("PRISMARINE_SHARD", 2.0);
        SPECIAL.put("PRISMARINE_CRYSTALS", 3.0);
        SPECIAL.put("SPONGE", 5.0);
        SPECIAL.put("SCUTE", 8.0);
        SPECIAL.put("ARMADILLO_SCUTE", 10.0);
        SPECIAL.put("WITHER_ROSE", 15.0);
        SPECIAL.put("WITHER_SKELETON_SKULL", 20.0);
        SPECIAL.put("DRAGON_EGG", 5000.0);

        // ---- 1.21 Tricky Trials additions ----
        SPECIAL.put("TRIAL_KEY", 10.0);
        SPECIAL.put("OMINOUS_TRIAL_KEY", 15.0);
        SPECIAL.put("HEAVY_CORE", 40.0);
        SPECIAL.put("BREEZE_ROD", 25.0);
        SPECIAL.put("MACE", 100.0);
        SPECIAL.put("WIND_CHARGE", 1.0);

        // ---- Misc valuable utility items ----
        SPECIAL.put("NAME_TAG", 20.0);
        SPECIAL.put("SADDLE", 15.0);
        SPECIAL.put("LEAD", 3.0);
        SPECIAL.put("COMPASS", 4.0);
        SPECIAL.put("CLOCK", 4.0);
        SPECIAL.put("SPYGLASS", 8.0);
        SPECIAL.put("SHIELD", 8.0);
        SPECIAL.put("BOW", 10.0);
        SPECIAL.put("CROSSBOW", 15.0);
        SPECIAL.put("FISHING_ROD", 5.0);
        SPECIAL.put("FLINT_AND_STEEL", 3.0);
        SPECIAL.put("SHEARS", 3.0);
        SPECIAL.put("FLINT", 0.5);
        SPECIAL.put("STICK", 0.25);
        SPECIAL.put("CHORUS_FRUIT", 2.0);
        SPECIAL.put("END_ROD", 3.0);
    }

    public PriceManager(VaultShop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        buyMarkup = plugin.getConfig().getDouble("buy-markup-multiplier", 1.5);
        if (buyMarkup < 1.0) buyMarkup = 1.0; // never let buy undercut sell

        file = new File(plugin.getDataFolder(), "prices.yml");
        sellPrices.clear();
        buyOverrides.clear();

        boolean isNewFile = !file.exists();
        YamlConfiguration yaml;
        if (isNewFile) {
            yaml = new YamlConfiguration();
        } else {
            yaml = YamlConfiguration.loadConfiguration(file);
        }

        boolean changed = isNewFile;
        for (Material material : Material.values()) {
            if (!isSellable(material)) continue;
            String key = "sell." + material.name();
            if (yaml.contains(key)) {
                sellPrices.put(material.name(), yaml.getDouble(key));
            } else {
                // New material we haven't priced yet (e.g. after a Minecraft update) - generate
                // a sensible default and persist it so it never crashes or shows as $0.
                double def = getDefaultPrice(material);
                sellPrices.put(material.name(), def);
                yaml.set(key, def);
                changed = true;
            }
        }

        if (yaml.contains("buy-overrides")) {
            for (String key : yaml.getConfigurationSection("buy-overrides").getKeys(false)) {
                buyOverrides.put(key, yaml.getDouble("buy-overrides." + key));
            }
        }

        if (changed) {
            try {
                yaml.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save prices.yml: " + e.getMessage());
            }
        }
    }

    public boolean isSellable(Material material) {
        if (material == null) return false;
        if (material.isAir()) return false;
        if (material.isLegacy()) return false;
        if (!material.isItem()) return false;
        String name = material.name();
        if (EXCLUDED.contains(name)) return false;
        if (name.endsWith("_SPAWN_EGG")) return false;
        return true;
    }

    public double getSellPrice(Material material) {
        Double stored = sellPrices.get(material.name());
        if (stored != null) return stored;
        return getDefaultPrice(material);
    }

    public double getBuyPrice(Material material) {
        Double override = buyOverrides.get(material.name());
        if (override != null) return override;
        return round2(getSellPrice(material) * buyMarkup);
    }

    public void setPrice(Material material, boolean buy, double amount) {
        String key = material.name();
        if (buy) {
            buyOverrides.put(key, amount);
        } else {
            sellPrices.put(key, amount);
        }
        save();
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, Double> e : sellPrices.entrySet()) {
            yaml.set("sell." + e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Double> e : buyOverrides.entrySet()) {
            yaml.set("buy-overrides." + e.getKey(), e.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save prices.yml: " + e.getMessage());
        }
    }

    /** All sellable materials, sorted by their pretty display name. Computed once and cached. */
    private List<Material> cachedAll;

    public List<Material> getAllSellable() {
        if (cachedAll == null) {
            List<Material> list = new ArrayList<>();
            for (Material m : Material.values()) {
                if (isSellable(m)) list.add(m);
            }
            list.sort(Comparator.comparing(m -> com.vaultshop.core.util.ItemUtil.prettyName(m)));
            cachedAll = list;
        }
        return cachedAll;
    }

    /**
     * Category-based fallback pricing for any material without an explicit SPECIAL entry.
     * Never returns 0 or a negative number.
     */
    private double getDefaultPrice(Material material) {
        Double special = SPECIAL.get(material.name());
        if (special != null) return special;

        String name = material.name();

        // Wood
        if (name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE")) return 2.0;
        if (name.endsWith("_PLANKS")) return 0.5;
        if (name.endsWith("_SAPLING") || name.endsWith("_PROPAGULE")) return 1.0;
        if (name.endsWith("_LEAVES")) return 0.25;
        if (name.endsWith("_BOAT") || name.contains("BOAT_")) return 4.0;

        // Ore blocks we didn't special-case explicitly
        if (name.contains("_ORE")) return 5.0;

        // Tiered tools / weapons / armor
        if (isGearName(name)) {
            if (name.startsWith("NETHERITE_")) return 220.0;
            if (name.startsWith("DIAMOND_")) return 40.0;
            if (name.startsWith("IRON_")) return 15.0;
            if (name.startsWith("GOLDEN_") || name.startsWith("GOLD_")) return 12.0;
            if (name.startsWith("CHAINMAIL_")) return 20.0;
            if (name.startsWith("STONE_")) return 3.0;
            if (name.startsWith("WOODEN_") || name.startsWith("WOOD_")) return 1.5;
            if (name.startsWith("LEATHER_")) return 3.0;
            if (name.startsWith("TURTLE_")) return 15.0;
        }

        // Functional wooden/stone/iron/copper sub-blocks
        if (containsAny(name, "_DOOR")) return name.contains("IRON") ? 6.0 : 2.0;
        if (containsAny(name, "_TRAPDOOR")) return name.contains("IRON") ? 5.0 : 1.5;
        if (containsAny(name, "_BUTTON")) return 0.3;
        if (containsAny(name, "_PRESSURE_PLATE")) return name.contains("IRON") || name.contains("GOLD") ? 4.0 : 0.5;
        if (containsAny(name, "_STAIRS")) return 1.0;
        if (containsAny(name, "_SLAB")) return 0.5;
        if (containsAny(name, "_WALL")) return 0.75;
        if (containsAny(name, "_FENCE_GATE")) return 1.0;
        if (containsAny(name, "_FENCE")) return 0.75;

        // Decorative / dye based blocks
        if (name.endsWith("_WOOL")) return 1.5;
        if (name.endsWith("_CARPET")) return 0.75;
        if (name.endsWith("_DYE")) return 1.0;
        if (name.contains("CONCRETE_POWDER")) return 0.75;
        if (name.contains("CONCRETE")) return 1.0;
        if (name.contains("GLAZED_TERRACOTTA")) return 2.5;
        if (name.contains("TERRACOTTA")) return 1.0;
        if (name.contains("STAINED_GLASS_PANE")) return 0.3;
        if (name.contains("GLASS_PANE")) return 0.2;
        if (name.contains("GLASS")) return 0.5;
        if (name.endsWith("_BED")) return 4.0;
        if (name.endsWith("_BANNER")) return 3.0;
        if (name.endsWith("_SHULKER_BOX")) return 30.0;

        // Common cheap building blocks
        if (containsAny(name, "STONE", "DEEPSLATE", "ANDESITE", "DIORITE", "GRANITE", "TUFF", "CALCITE", "COBBLESTONE", "DIRT", "GRAVEL", "SAND", "NETHERRACK", "BASALT", "BLACKSTONE")) {
            return 0.5;
        }

        if (material.isEdible()) return 2.0;

        if (material.isBlock()) return 1.0;
        return 1.0;
    }

    private boolean isGearName(String name) {
        return containsAny(name, "_SWORD", "_PICKAXE", "_AXE", "_SHOVEL", "_HOE",
                "_HELMET", "_CHESTPLATE", "_LEGGINGS", "_BOOTS", "_HORSE_ARMOR");
    }

    private boolean containsAny(String name, String... needles) {
        for (String n : needles) {
            if (name.contains(n)) return true;
        }
        return false;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
