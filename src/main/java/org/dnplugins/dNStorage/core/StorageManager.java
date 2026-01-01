package org.dnplugins.dNStorage.core;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.dnplugins.dNStorage.enums.ItemCategory;

import java.util.*;

/**
 * Quản lý lưu trữ vật phẩm theo danh mục cho từng người chơi
 * Sử dụng database để lưu trữ dữ liệu
 */
public class StorageManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final LanguageManager languageManager;
    // Cache trong memory để tăng hiệu suất (Lazy loading)
    private final Map<UUID, Map<ItemCategory.Category, Map<Material, Integer>>> cache;
    // Track các category đã được load để tránh load lại không cần thiết
    private final Map<UUID, Set<ItemCategory.Category>> loadedCategories;

    public StorageManager(JavaPlugin plugin, DatabaseManager databaseManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.languageManager = languageManager;
        this.cache = new HashMap<>();
        this.loadedCategories = new HashMap<>();
    }

    /**
     * Thêm vật phẩm vào kho
     */
    public int addItem(UUID playerId, Material material, int amount) {
        ItemCategory.Category category = ItemCategory.getCategory(material);
        if (category == null) {
            return 0; // Vật phẩm không thuộc danh mục nào
        }

        String playerUuid = playerId.toString();
        String categoryName = category.name();
        String materialName = material.name();

        // Cập nhật database (async)
        databaseManager.upsertItem(playerUuid, categoryName, materialName, amount);

        // Cập nhật cache (optimistic update)
        cache.computeIfAbsent(playerId, k -> new HashMap<>());
        cache.get(playerId).computeIfAbsent(category, k -> new HashMap<>());
        Map<Material, Integer> categoryStorage = cache.get(playerId).get(category);
        int currentAmount = categoryStorage.getOrDefault(material, 0);
        categoryStorage.put(material, currentAmount + amount);

        return amount;
    }

    /**
     * Batch add nhiều items cùng lúc (sử dụng batch operations)
     */
    public void batchAddItems(UUID playerId, Map<Material, Integer> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // Nhóm items theo category
        Map<ItemCategory.Category, Map<String, Integer>> itemsByCategory = new HashMap<>();

        for (Map.Entry<Material, Integer> entry : items.entrySet()) {
            Material material = entry.getKey();
            ItemCategory.Category category = ItemCategory.getCategory(material);
            if (category == null) {
                continue;
            }

            itemsByCategory.computeIfAbsent(category, k -> new HashMap<>())
                    .put(material.name(), entry.getValue());
        }

        // Batch upsert từng category
        String playerUuid = playerId.toString();
        for (Map.Entry<ItemCategory.Category, Map<String, Integer>> entry : itemsByCategory.entrySet()) {
            ItemCategory.Category category = entry.getKey();
            Map<String, Integer> categoryItems = entry.getValue();

            // Batch upsert vào database
            databaseManager.batchUpsertItems(playerUuid, category.name(), categoryItems);

            // Cập nhật cache
            cache.computeIfAbsent(playerId, k -> new HashMap<>());
            cache.get(playerId).computeIfAbsent(category, k -> new HashMap<>());
            Map<Material, Integer> categoryStorage = cache.get(playerId).get(category);

            for (Map.Entry<String, Integer> itemEntry : categoryItems.entrySet()) {
                try {
                    Material material = Material.valueOf(itemEntry.getKey());
                    int amount = itemEntry.getValue();
                    int currentAmount = categoryStorage.getOrDefault(material, 0);
                    categoryStorage.put(material, currentAmount + amount);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(languageManager.getMessage("storage.invalid_material")
                            .replace("{material}", itemEntry.getKey()));
                }
            }
        }
    }

    /**
     * Lấy vật phẩm từ kho
     */
    public int removeItem(UUID playerId, Material material, int amount) {
        ItemCategory.Category category = ItemCategory.getCategory(material);
        if (category == null) {
            return 0;
        }

        String playerUuid = playerId.toString();
        String categoryName = category.name();
        String materialName = material.name();

        // Kiểm tra cache trước (Lazy loading)
        int currentAmount = 0;
        Map<ItemCategory.Category, Map<Material, Integer>> playerStorage = cache.get(playerId);
        if (playerStorage != null) {
            Map<Material, Integer> categoryStorage = playerStorage.get(category);
            if (categoryStorage != null && categoryStorage.containsKey(material)) {
                currentAmount = categoryStorage.get(material);
            }
        }

        // Nếu không có trong cache, lấy từ database (synchronous fallback)
        if (currentAmount == 0) {
            currentAmount = databaseManager.getItemAmount(playerUuid, categoryName, materialName);
            // Cập nhật cache
            if (currentAmount > 0) {
                cache.computeIfAbsent(playerId, k -> new HashMap<>());
                cache.get(playerId).computeIfAbsent(category, k -> new HashMap<>());
                cache.get(playerId).get(category).put(material, currentAmount);
            }
        }

        if (currentAmount <= 0) {
            return 0;
        }

        int removed = Math.min(amount, currentAmount);
        int newAmount = currentAmount - removed;

        // Cập nhật database (async)
        databaseManager.updateItemAmount(playerUuid, categoryName, materialName, newAmount);

        // Cập nhật cache (optimistic update)
        if (playerStorage != null) {
            Map<Material, Integer> categoryStorage = playerStorage.get(category);
            if (categoryStorage != null) {
                if (newAmount > 0) {
                    categoryStorage.put(material, newAmount);
                } else {
                    categoryStorage.remove(material);
                    if (categoryStorage.isEmpty()) {
                        playerStorage.remove(category);
                        loadedCategories.getOrDefault(playerId, new HashSet<>()).remove(category);
                    }
                }
            }
        }

        return removed;
    }

    /**
     * Lấy số lượng vật phẩm trong kho (Synchronous - for backward compatibility)
     */
    public int getItemAmount(UUID playerId, Material material) {
        ItemCategory.Category category = ItemCategory.getCategory(material);
        if (category == null) {
            return 0;
        }

        // Kiểm tra cache trước (Lazy loading)
        Map<ItemCategory.Category, Map<Material, Integer>> playerStorage = cache.get(playerId);
        if (playerStorage != null) {
            Map<Material, Integer> categoryStorage = playerStorage.get(category);
            if (categoryStorage != null && categoryStorage.containsKey(material)) {
                return categoryStorage.get(material);
            }
        }

        // Nếu không có trong cache, lấy từ database (synchronous fallback)
        String playerUuid = playerId.toString();
        String categoryName = category.name();
        String materialName = material.name();
        int amount = databaseManager.getItemAmount(playerUuid, categoryName, materialName);

        // Cập nhật cache
        if (amount > 0) {
            cache.computeIfAbsent(playerId, k -> new HashMap<>());
            cache.get(playerId).computeIfAbsent(category, k -> new HashMap<>());
            cache.get(playerId).get(category).put(material, amount);
        }

        return amount;
    }

    /**
     * Lấy số lượng vật phẩm trong kho (Async với callback)
     */
    public void getItemAmountAsync(UUID playerId, Material material, java.util.function.Consumer<Integer> callback) {
        ItemCategory.Category category = ItemCategory.getCategory(material);
        if (category == null) {
            callback.accept(0);
            return;
        }

        // Kiểm tra cache trước (Lazy loading)
        Map<ItemCategory.Category, Map<Material, Integer>> playerStorage = cache.get(playerId);
        if (playerStorage != null) {
            Map<Material, Integer> categoryStorage = playerStorage.get(category);
            if (categoryStorage != null && categoryStorage.containsKey(material)) {
                callback.accept(categoryStorage.get(material));
                return;
            }
        }

        // Nếu không có trong cache, lấy từ database async
        String playerUuid = playerId.toString();
        String categoryName = category.name();
        String materialName = material.name();

        databaseManager.getItemAmountAsync(playerUuid, categoryName, materialName, amount -> {
            // Cập nhật cache
            if (amount > 0) {
                cache.computeIfAbsent(playerId, k -> new HashMap<>());
                cache.get(playerId).computeIfAbsent(category, k -> new HashMap<>());
                cache.get(playerId).get(category).put(material, amount);
            }
            callback.accept(amount);
        });
    }

    /**
     * Lấy tất cả vật phẩm trong một danh mục (Synchronous - for backward
     * compatibility)
     */
    public Map<Material, Integer> getCategoryItems(UUID playerId, ItemCategory.Category category) {
        // Kiểm tra cache trước (Lazy loading)
        Map<ItemCategory.Category, Map<Material, Integer>> playerStorage = cache.get(playerId);
        if (playerStorage != null && playerStorage.containsKey(category)) {
            return new HashMap<>(playerStorage.get(category));
        }

        // Nếu không có trong cache, lấy từ database
        String playerUuid = playerId.toString();
        String categoryName = category.name();
        Map<String, Integer> items = databaseManager.getCategoryItems(playerUuid, categoryName);

        // Chuyển đổi sang Map<Material, Integer>
        Map<Material, Integer> result = new HashMap<>();
        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            try {
                Material material = Material.valueOf(entry.getKey());
                result.put(material, entry.getValue());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning(languageManager.getMessage("storage.invalid_material")
                        .replace("{material}", entry.getKey()));
            }
        }

        // Cập nhật cache
        if (!result.isEmpty()) {
            cache.computeIfAbsent(playerId, k -> new HashMap<>());
            cache.get(playerId).put(category, new HashMap<>(result));
            loadedCategories.computeIfAbsent(playerId, k -> new HashSet<>()).add(category);
        }

        return result;
    }

    /**
     * Lấy tất cả vật phẩm trong một danh mục (Async với callback)
     */
    public void getCategoryItemsAsync(UUID playerId, ItemCategory.Category category,
            java.util.function.Consumer<Map<Material, Integer>> callback) {
        // Kiểm tra cache trước (Lazy loading)
        Map<ItemCategory.Category, Map<Material, Integer>> playerStorage = cache.get(playerId);
        if (playerStorage != null && playerStorage.containsKey(category)) {
            callback.accept(new HashMap<>(playerStorage.get(category)));
            return;
        }

        // Nếu không có trong cache, lấy từ database async
        String playerUuid = playerId.toString();
        String categoryName = category.name();

        databaseManager.getCategoryItemsAsync(playerUuid, categoryName, items -> {
            // Chuyển đổi sang Map<Material, Integer>
            Map<Material, Integer> result = new HashMap<>();
            for (Map.Entry<String, Integer> entry : items.entrySet()) {
                try {
                    Material material = Material.valueOf(entry.getKey());
                    result.put(material, entry.getValue());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning(languageManager.getMessage("storage.invalid_material")
                            .replace("{material}", entry.getKey()));
                }
            }

            // Cập nhật cache
            if (!result.isEmpty()) {
                cache.computeIfAbsent(playerId, k -> new HashMap<>());
                cache.get(playerId).put(category, new HashMap<>(result));
                loadedCategories.computeIfAbsent(playerId, k -> new HashSet<>()).add(category);
            }

            callback.accept(result);
        });
    }

    /**
     * Lấy tổng số vật phẩm trong kho của người chơi
     */
    public int getTotalItems(UUID playerId) {
        int total = 0;
        Map<ItemCategory.Category, Map<Material, Integer>> playerStorage = cache.get(playerId);

        if (playerStorage != null) {
            for (Map<Material, Integer> categoryItems : playerStorage.values()) {
                for (int amount : categoryItems.values()) {
                    total += amount;
                }
            }
        } else {
            // Nếu không có trong cache, tính từ database
            for (ItemCategory.Category category : ItemCategory.Category.values()) {
                Map<Material, Integer> items = getCategoryItems(playerId, category);
                for (int amount : items.values()) {
                    total += amount;
                }
            }
        }

        return total;
    }

    /**
     * Xóa cache của một người chơi (để reload từ database)
     */
    public void clearCache(UUID playerId) {
        cache.remove(playerId);
        loadedCategories.remove(playerId);
    }

    /**
     * Xóa cache của một category cụ thể (Lazy loading - chỉ reload category đó khi
     * cần)
     */
    public void clearCategoryCache(UUID playerId, ItemCategory.Category category) {
        Map<ItemCategory.Category, Map<Material, Integer>> playerStorage = cache.get(playerId);
        if (playerStorage != null) {
            playerStorage.remove(category);
            loadedCategories.getOrDefault(playerId, new HashSet<>()).remove(category);
        }
    }

    /**
     * Xóa toàn bộ cache
     */
    public void clearAllCache() {
        cache.clear();
    }

    /**
     * Lưu dữ liệu khi plugin tắt
     */
    public void shutdown() {
        // Database tự động lưu, chỉ cần đóng kết nối
        databaseManager.closeConnection();
    }
}
