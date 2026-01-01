package org.dnplugins.dNStorage;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Quản lý lưu trữ vật phẩm theo danh mục cho từng người chơi
 * Sử dụng database để lưu trữ dữ liệu
 */
public class StorageManager {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final LanguageManager languageManager;
    // Cache trong memory để tăng hiệu suất
    private final Map<UUID, Map<ItemCategory.Category, Map<Material, Integer>>> cache;

    public StorageManager(JavaPlugin plugin, DatabaseManager databaseManager, LanguageManager languageManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.languageManager = languageManager;
        this.cache = new HashMap<>();
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

        // Cập nhật database
        databaseManager.upsertItem(playerUuid, categoryName, materialName, amount);

        // Cập nhật cache
        cache.computeIfAbsent(playerId, k -> new HashMap<>());
        cache.get(playerId).computeIfAbsent(category, k -> new HashMap<>());
        Map<Material, Integer> categoryStorage = cache.get(playerId).get(category);
        int currentAmount = categoryStorage.getOrDefault(material, 0);
        categoryStorage.put(material, currentAmount + amount);

        return amount;
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

        // Lấy số lượng hiện tại từ database
        int currentAmount = databaseManager.getItemAmount(playerUuid, categoryName, materialName);
        if (currentAmount <= 0) {
            return 0;
        }

        int removed = Math.min(amount, currentAmount);
        int newAmount = currentAmount - removed;

        // Cập nhật database
        databaseManager.updateItemAmount(playerUuid, categoryName, materialName, newAmount);

        // Cập nhật cache
        Map<ItemCategory.Category, Map<Material, Integer>> playerStorage = cache.get(playerId);
        if (playerStorage != null) {
            Map<Material, Integer> categoryStorage = playerStorage.get(category);
            if (categoryStorage != null) {
                if (newAmount > 0) {
                    categoryStorage.put(material, newAmount);
                } else {
                    categoryStorage.remove(material);
                    if (categoryStorage.isEmpty()) {
                        playerStorage.remove(category);
                    }
                }
            }
        }

        return removed;
    }

    /**
     * Lấy số lượng vật phẩm trong kho
     */
    public int getItemAmount(UUID playerId, Material material) {
        ItemCategory.Category category = ItemCategory.getCategory(material);
        if (category == null) {
            return 0;
        }

        // Kiểm tra cache trước
        Map<ItemCategory.Category, Map<Material, Integer>> playerStorage = cache.get(playerId);
        if (playerStorage != null) {
            Map<Material, Integer> categoryStorage = playerStorage.get(category);
            if (categoryStorage != null && categoryStorage.containsKey(material)) {
                return categoryStorage.get(material);
            }
        }

        // Nếu không có trong cache, lấy từ database
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
     * Lấy tất cả vật phẩm trong một danh mục
     */
    public Map<Material, Integer> getCategoryItems(UUID playerId, ItemCategory.Category category) {
        // Kiểm tra cache trước
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
        }

        return result;
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
