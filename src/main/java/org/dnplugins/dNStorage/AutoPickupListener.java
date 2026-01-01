package org.dnplugins.dNStorage;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener tự động nhặt vật phẩm rơi trên đất vào kho
 */
public class AutoPickupListener implements Listener {

    private final StorageManager storageManager;
    private final LanguageManager languageManager;
    private final Map<UUID, Boolean> autoPickupEnabled;

    public AutoPickupListener(JavaPlugin plugin, StorageManager storageManager, LanguageManager languageManager) {
        this.storageManager = storageManager;
        this.languageManager = languageManager;
        this.autoPickupEnabled = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Bật/tắt tự động nhặt cho người chơi
     */
    public void setAutoPickup(UUID playerId, boolean enabled) {
        autoPickupEnabled.put(playerId, enabled);
    }

    /**
     * Kiểm tra tự động nhặt có bật không
     */
    public boolean isAutoPickupEnabled(UUID playerId) {
        return autoPickupEnabled.getOrDefault(playerId, false);
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
