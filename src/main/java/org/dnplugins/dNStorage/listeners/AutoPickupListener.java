package org.dnplugins.dNStorage.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.dnplugins.dNStorage.core.DatabaseManager;
import org.dnplugins.dNStorage.core.LanguageManager;
import org.dnplugins.dNStorage.core.StorageManager;
import org.dnplugins.dNStorage.enums.ItemCategory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener tự động nhặt vật phẩm rơi trên đất vào kho
 */
public class AutoPickupListener implements Listener {

    private final StorageManager storageManager;
    private final DatabaseManager databaseManager;
    private final LanguageManager languageManager;
    // Cache trong memory để tăng hiệu suất
    private final Map<UUID, Boolean> autoPickupCache;

    public AutoPickupListener(JavaPlugin plugin, StorageManager storageManager, DatabaseManager databaseManager,
            LanguageManager languageManager) {
        this.storageManager = storageManager;
        this.databaseManager = databaseManager;
        this.languageManager = languageManager;
        this.autoPickupCache = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Bật/tắt tự động nhặt cho người chơi
     */
    public void setAutoPickup(UUID playerId, boolean enabled) {
        // Lưu vào database
        databaseManager.setAutoPickup(playerId.toString(), enabled);
        // Cập nhật cache
        autoPickupCache.put(playerId, enabled);
    }

    /**
     * Kiểm tra tự động nhặt có bật không
     */
    public boolean isAutoPickupEnabled(UUID playerId) {
        // Kiểm tra cache trước
        if (autoPickupCache.containsKey(playerId)) {
            return autoPickupCache.get(playerId);
        }

        // Nếu không có trong cache, lấy từ database
        boolean enabled = databaseManager.getAutoPickup(playerId.toString());
        // Cập nhật cache
        autoPickupCache.put(playerId, enabled);
        return enabled;
    }

    /**
     * Load trạng thái auto-pickup từ database (gọi khi player join)
     */
    public void loadAutoPickup(UUID playerId) {
        boolean enabled = databaseManager.getAutoPickup(playerId.toString());
        autoPickupCache.put(playerId, enabled);
    }

    /**
     * Xóa cache của player (khi player logout)
     */
    public void clearCache(UUID playerId) {
        autoPickupCache.remove(playerId);
    }

    /**
     * Xử lý khi người chơi nhặt vật phẩm
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        UUID playerId = player.getUniqueId();

        // Kiểm tra tự động nhặt có bật không
        if (!isAutoPickupEnabled(playerId)) {
            return;
        }

        Item item = event.getItem();
        ItemStack itemStack = item.getItemStack();
        Material material = itemStack.getType();

        // Kiểm tra vật phẩm có thuộc danh mục nào không
        if (!ItemCategory.isCategorized(material)) {
            return;
        }

        int amount = itemStack.getAmount();

        // Thêm vào kho
        int added = storageManager.addItem(playerId, material, amount);

        if (added > 0) {
            // Hủy sự kiện nhặt vật phẩm (vật phẩm đã vào kho)
            event.setCancelled(true);
            item.remove();

            // Gửi thông báo
            player.sendMessage(languageManager.getMessage("autopickup.item_picked")
                    .replace("{amount}", formatNumber(added))
                    .replace("{item}", getMaterialDisplayName(material)));
        }
    }

    /**
     * Load trạng thái auto-pickup khi player join
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadAutoPickup(player.getUniqueId());
    }

    /**
     * Clear cache khi player quit
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        clearCache(player.getUniqueId());
    }

    /**
     * Lấy tên hiển thị của vật phẩm
     */
    private String getMaterialDisplayName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0)
                result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return result.toString();
    }

    /**
     * Định dạng số với dấu phẩy
     */
    private String formatNumber(int number) {
        return String.format("%,d", number);
    }
}
