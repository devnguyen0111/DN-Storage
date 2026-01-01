package org.dnplugins.dNStorage.core;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Quản lý sound effects cho plugin
 */
public class SoundManager {

    private final JavaPlugin plugin;
    @SuppressWarnings("unused")
    private final LanguageManager languageManager;
    private boolean soundsEnabled;
    private Sound guiOpenSound;
    private Sound guiCloseSound;
    private Sound itemAddSound;
    private Sound itemRemoveSound;
    private float volume;
    private float pitch;

    public SoundManager(JavaPlugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        loadConfig();
    }

    /**
     * Tải cấu hình sound từ config
     */
    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        soundsEnabled = config.getBoolean("sounds.enabled", true);

        // Load sound types
        String guiOpenSoundStr = config.getString("sounds.gui_open", "BLOCK_CHEST_OPEN");
        String guiCloseSoundStr = config.getString("sounds.gui_close", "BLOCK_CHEST_CLOSE");
        String itemAddSoundStr = config.getString("sounds.item_add", "ENTITY_ITEM_PICKUP");
        String itemRemoveSoundStr = config.getString("sounds.item_remove", "ENTITY_ITEM_PICKUP");

        try {
            @SuppressWarnings("deprecation")
            Sound sound = Sound.valueOf(guiOpenSoundStr);
            guiOpenSound = sound;
        } catch (IllegalArgumentException e) {
            guiOpenSound = Sound.BLOCK_CHEST_OPEN;
            plugin.getLogger().warning("Invalid sound for gui_open: " + guiOpenSoundStr + ". Using default.");
        }

        try {
            @SuppressWarnings("deprecation")
            Sound sound = Sound.valueOf(guiCloseSoundStr);
            guiCloseSound = sound;
        } catch (IllegalArgumentException e) {
            guiCloseSound = Sound.BLOCK_CHEST_CLOSE;
            plugin.getLogger().warning("Invalid sound for gui_close: " + guiCloseSoundStr + ". Using default.");
        }

        try {
            @SuppressWarnings("deprecation")
            Sound sound = Sound.valueOf(itemAddSoundStr);
            itemAddSound = sound;
        } catch (IllegalArgumentException e) {
            itemAddSound = Sound.ENTITY_ITEM_PICKUP;
            plugin.getLogger().warning("Invalid sound for item_add: " + itemAddSoundStr + ". Using default.");
        }

        try {
            @SuppressWarnings("deprecation")
            Sound sound = Sound.valueOf(itemRemoveSoundStr);
            itemRemoveSound = sound;
        } catch (IllegalArgumentException e) {
            itemRemoveSound = Sound.ENTITY_ITEM_PICKUP;
            plugin.getLogger().warning("Invalid sound for item_remove: " + itemRemoveSoundStr + ". Using default.");
        }

        volume = (float) config.getDouble("sounds.volume", 1.0);
        pitch = (float) config.getDouble("sounds.pitch", 1.0);
    }

    /**
     * Phát sound khi mở GUI
     */
    public void playGUIOpenSound(Player player) {
        if (soundsEnabled && guiOpenSound != null) {
            player.playSound(player.getLocation(), guiOpenSound, volume, pitch);
        }
    }

    /**
     * Phát sound khi đóng GUI
     */
    public void playGUICloseSound(Player player) {
        if (soundsEnabled && guiCloseSound != null) {
            player.playSound(player.getLocation(), guiCloseSound, volume, pitch);
        }
    }

    /**
     * Phát sound khi thêm vật phẩm
     */
    public void playItemAddSound(Player player) {
        if (soundsEnabled && itemAddSound != null) {
            player.playSound(player.getLocation(), itemAddSound, volume, pitch);
        }
    }

    /**
     * Phát sound khi lấy vật phẩm
     */
    public void playItemRemoveSound(Player player) {
        if (soundsEnabled && itemRemoveSound != null) {
            player.playSound(player.getLocation(), itemRemoveSound, volume, pitch);
        }
    }

    /**
     * Kiểm tra sound có được bật không
     */
    public boolean isSoundsEnabled() {
        return soundsEnabled;
    }
}
