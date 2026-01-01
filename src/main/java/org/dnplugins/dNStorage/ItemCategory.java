package org.dnplugins.dNStorage;

import org.bukkit.Material;

import java.util.*;

/**
 * Phân loại vật phẩm thành các danh mục: Quặng, Block xây dựng, Block gỗ
 */
public class ItemCategory {

    public enum Category {
        ORE("Quặng", Material.IRON_INGOT),
        BUILDING("Block Xây Dựng", Material.STONE_BRICKS),
        WOOD("Block Gỗ", Material.OAK_PLANKS);

        private final String displayName;
        private final Material icon;

        Category(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }
    }

    private static final Map<Material, Category> CATEGORY_MAP = new HashMap<>();

    static {
        // Quặng
        registerOres();
        // Block xây dựng
        registerBuildingBlocks();
        // Block gỗ
        registerWoodBlocks();
    }

    private static void registerOres() {
        Material[] ores = {
                Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
                Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
                Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
                Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
                Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
                Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
                Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
                Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE,
                Material.ANCIENT_DEBRIS,
                // Ingots và gems
                Material.COAL, Material.IRON_INGOT, Material.COPPER_INGOT,
                Material.GOLD_INGOT, Material.EMERALD, Material.DIAMOND,
                Material.LAPIS_LAZULI, Material.QUARTZ, Material.NETHERITE_INGOT,
                Material.REDSTONE
        };

        for (Material material : ores) {
            CATEGORY_MAP.put(material, Category.ORE);
        }
    }

    private static void registerBuildingBlocks() {
        Material[] buildingBlocks = {
                // Stone variants
                Material.STONE, Material.COBBLESTONE, Material.STONE_BRICKS,
                Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS,
                Material.CHISELED_STONE_BRICKS, Material.SMOOTH_STONE,
                Material.GRANITE, Material.POLISHED_GRANITE,
                Material.DIORITE, Material.POLISHED_DIORITE,
                Material.ANDESITE, Material.POLISHED_ANDESITE,
                Material.DEEPSLATE, Material.COBBLED_DEEPSLATE,
                Material.DEEPSLATE_BRICKS, Material.DEEPSLATE_TILES,
                Material.POLISHED_DEEPSLATE,
                // Bricks
                Material.BRICKS, Material.BRICK_SLAB, Material.BRICK_STAIRS,
                Material.BRICK_WALL,
                // Concrete
                Material.WHITE_CONCRETE, Material.ORANGE_CONCRETE, Material.MAGENTA_CONCRETE,
                Material.LIGHT_BLUE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
                Material.PINK_CONCRETE, Material.GRAY_CONCRETE, Material.LIGHT_GRAY_CONCRETE,
                Material.CYAN_CONCRETE, Material.PURPLE_CONCRETE, Material.BLUE_CONCRETE,
                Material.BROWN_CONCRETE, Material.GREEN_CONCRETE, Material.RED_CONCRETE,
                Material.BLACK_CONCRETE,
                // Terracotta
                Material.TERRACOTTA, Material.WHITE_TERRACOTTA, Material.ORANGE_TERRACOTTA,
                Material.MAGENTA_TERRACOTTA, Material.LIGHT_BLUE_TERRACOTTA,
                Material.YELLOW_TERRACOTTA, Material.LIME_TERRACOTTA,
                Material.PINK_TERRACOTTA, Material.GRAY_TERRACOTTA,
                Material.LIGHT_GRAY_TERRACOTTA, Material.CYAN_TERRACOTTA,
                Material.PURPLE_TERRACOTTA, Material.BLUE_TERRACOTTA,
                Material.BROWN_TERRACOTTA, Material.GREEN_TERRACOTTA,
                Material.RED_TERRACOTTA, Material.BLACK_TERRACOTTA,
                // Glass
                Material.GLASS, Material.WHITE_STAINED_GLASS, Material.ORANGE_STAINED_GLASS,
                Material.MAGENTA_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS,
                Material.YELLOW_STAINED_GLASS, Material.LIME_STAINED_GLASS,
                Material.PINK_STAINED_GLASS, Material.GRAY_STAINED_GLASS,
                Material.LIGHT_GRAY_STAINED_GLASS, Material.CYAN_STAINED_GLASS,
                Material.PURPLE_STAINED_GLASS, Material.BLUE_STAINED_GLASS,
                Material.BROWN_STAINED_GLASS, Material.GREEN_STAINED_GLASS,
                Material.RED_STAINED_GLASS, Material.BLACK_STAINED_GLASS,
                // Sand and Sandstone
                Material.SAND, Material.SANDSTONE, Material.CUT_SANDSTONE,
                Material.CHISELED_SANDSTONE, Material.SMOOTH_SANDSTONE,
                Material.RED_SAND, Material.RED_SANDSTONE, Material.CUT_RED_SANDSTONE,
                Material.CHISELED_RED_SANDSTONE, Material.SMOOTH_RED_SANDSTONE,
                // Nether blocks
                Material.NETHERRACK, Material.NETHER_BRICKS, Material.RED_NETHER_BRICKS,
                Material.CRIMSON_NYLIUM, Material.WARPED_NYLIUM,
                // End blocks
                Material.END_STONE, Material.END_STONE_BRICKS,
                // Other
                Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.BLACKSTONE,
                Material.POLISHED_BLACKSTONE, Material.POLISHED_BLACKSTONE_BRICKS
        };

        for (Material material : buildingBlocks) {
            CATEGORY_MAP.put(material, Category.BUILDING);
        }
    }

    private static void registerWoodBlocks() {
        Material[] woodBlocks = {
                // Oak
                Material.OAK_LOG, Material.OAK_WOOD, Material.OAK_PLANKS,
                Material.OAK_STAIRS, Material.OAK_SLAB, Material.OAK_FENCE,
                Material.OAK_FENCE_GATE, Material.OAK_DOOR, Material.OAK_TRAPDOOR,
                Material.OAK_BUTTON, Material.OAK_PRESSURE_PLATE,
                // Spruce
                Material.SPRUCE_LOG, Material.SPRUCE_WOOD, Material.SPRUCE_PLANKS,
                Material.SPRUCE_STAIRS, Material.SPRUCE_SLAB, Material.SPRUCE_FENCE,
                Material.SPRUCE_FENCE_GATE, Material.SPRUCE_DOOR, Material.SPRUCE_TRAPDOOR,
                Material.SPRUCE_BUTTON, Material.SPRUCE_PRESSURE_PLATE,
                // Birch
                Material.BIRCH_LOG, Material.BIRCH_WOOD, Material.BIRCH_PLANKS,
                Material.BIRCH_STAIRS, Material.BIRCH_SLAB, Material.BIRCH_FENCE,
                Material.BIRCH_FENCE_GATE, Material.BIRCH_DOOR, Material.BIRCH_TRAPDOOR,
                Material.BIRCH_BUTTON, Material.BIRCH_PRESSURE_PLATE,
                // Jungle
                Material.JUNGLE_LOG, Material.JUNGLE_WOOD, Material.JUNGLE_PLANKS,
                Material.JUNGLE_STAIRS, Material.JUNGLE_SLAB, Material.JUNGLE_FENCE,
                Material.JUNGLE_FENCE_GATE, Material.JUNGLE_DOOR, Material.JUNGLE_TRAPDOOR,
                Material.JUNGLE_BUTTON, Material.JUNGLE_PRESSURE_PLATE,
                // Acacia
                Material.ACACIA_LOG, Material.ACACIA_WOOD, Material.ACACIA_PLANKS,
                Material.ACACIA_STAIRS, Material.ACACIA_SLAB, Material.ACACIA_FENCE,
                Material.ACACIA_FENCE_GATE, Material.ACACIA_DOOR, Material.ACACIA_TRAPDOOR,
                Material.ACACIA_BUTTON, Material.ACACIA_PRESSURE_PLATE,
                // Dark Oak
                Material.DARK_OAK_LOG, Material.DARK_OAK_WOOD, Material.DARK_OAK_PLANKS,
                Material.DARK_OAK_STAIRS, Material.DARK_OAK_SLAB, Material.DARK_OAK_FENCE,
                Material.DARK_OAK_FENCE_GATE, Material.DARK_OAK_DOOR, Material.DARK_OAK_TRAPDOOR,
                Material.DARK_OAK_BUTTON, Material.DARK_OAK_PRESSURE_PLATE,
                // Crimson
                Material.CRIMSON_STEM, Material.CRIMSON_HYPHAE, Material.CRIMSON_PLANKS,
                Material.CRIMSON_STAIRS, Material.CRIMSON_SLAB, Material.CRIMSON_FENCE,
                Material.CRIMSON_FENCE_GATE, Material.CRIMSON_DOOR, Material.CRIMSON_TRAPDOOR,
                Material.CRIMSON_BUTTON, Material.CRIMSON_PRESSURE_PLATE,
                // Warped
                Material.WARPED_STEM, Material.WARPED_HYPHAE, Material.WARPED_PLANKS,
                Material.WARPED_STAIRS, Material.WARPED_SLAB, Material.WARPED_FENCE,
                Material.WARPED_FENCE_GATE, Material.WARPED_DOOR, Material.WARPED_TRAPDOOR,
                Material.WARPED_BUTTON, Material.WARPED_PRESSURE_PLATE,
                // Cherry
                Material.CHERRY_LOG, Material.CHERRY_WOOD, Material.CHERRY_PLANKS,
                Material.CHERRY_STAIRS, Material.CHERRY_SLAB, Material.CHERRY_FENCE,
                Material.CHERRY_FENCE_GATE, Material.CHERRY_DOOR, Material.CHERRY_TRAPDOOR,
                Material.CHERRY_BUTTON, Material.CHERRY_PRESSURE_PLATE,
                // Mangrove
                Material.MANGROVE_LOG, Material.MANGROVE_WOOD, Material.MANGROVE_PLANKS,
                Material.MANGROVE_STAIRS, Material.MANGROVE_SLAB, Material.MANGROVE_FENCE,
                Material.MANGROVE_FENCE_GATE, Material.MANGROVE_DOOR, Material.MANGROVE_TRAPDOOR,
                Material.MANGROVE_BUTTON, Material.MANGROVE_PRESSURE_PLATE,
                // Bamboo
                Material.BAMBOO_BLOCK, Material.BAMBOO_PLANKS, Material.BAMBOO_STAIRS,
                Material.BAMBOO_SLAB, Material.BAMBOO_FENCE, Material.BAMBOO_FENCE_GATE,
                Material.BAMBOO_DOOR, Material.BAMBOO_TRAPDOOR, Material.BAMBOO_BUTTON,
                Material.BAMBOO_PRESSURE_PLATE,
                // Stripped variants
                Material.STRIPPED_OAK_LOG, Material.STRIPPED_OAK_WOOD,
                Material.STRIPPED_SPRUCE_LOG, Material.STRIPPED_SPRUCE_WOOD,
                Material.STRIPPED_BIRCH_LOG, Material.STRIPPED_BIRCH_WOOD,
                Material.STRIPPED_JUNGLE_LOG, Material.STRIPPED_JUNGLE_WOOD,
                Material.STRIPPED_ACACIA_LOG, Material.STRIPPED_ACACIA_WOOD,
                Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_WOOD,
                Material.STRIPPED_CRIMSON_STEM, Material.STRIPPED_CRIMSON_HYPHAE,
                Material.STRIPPED_WARPED_STEM, Material.STRIPPED_WARPED_HYPHAE,
                Material.STRIPPED_CHERRY_LOG, Material.STRIPPED_CHERRY_WOOD,
                Material.STRIPPED_MANGROVE_LOG, Material.STRIPPED_MANGROVE_WOOD,
                Material.STRIPPED_BAMBOO_BLOCK
        };

        for (Material material : woodBlocks) {
            CATEGORY_MAP.put(material, Category.WOOD);
        }
    }

    /**
     * Lấy danh mục của một vật phẩm
     */
    public static Category getCategory(Material material) {
        return CATEGORY_MAP.getOrDefault(material, null);
    }

    /**
     * Kiểm tra vật phẩm có thuộc danh mục nào không
     */
    public static boolean isCategorized(Material material) {
        return CATEGORY_MAP.containsKey(material);
    }

    /**
     * Lấy tất cả vật phẩm trong một danh mục
     */
    public static List<Material> getMaterialsInCategory(Category category) {
        List<Material> materials = new ArrayList<>();
        for (Map.Entry<Material, Category> entry : CATEGORY_MAP.entrySet()) {
            if (entry.getValue() == category) {
                materials.add(entry.getKey());
            }
        }
        return materials;
    }
}
