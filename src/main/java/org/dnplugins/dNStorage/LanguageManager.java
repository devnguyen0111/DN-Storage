package org.dnplugins.dNStorage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Quản lý ngôn ngữ và thông điệp
 */
public class LanguageManager {

    private final JavaPlugin plugin;
    private FileConfiguration languageConfig;
    private String currentLanguage;

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    /**
     * Tải ngôn ngữ từ config
     */
    private void loadLanguage() {
        // Lấy ngôn ngữ từ config
        FileConfiguration config = plugin.getConfig();
        currentLanguage = config.getString("language", "vi").toLowerCase();

        // Tải file ngôn ngữ
        File languageFile = new File(plugin.getDataFolder(), "messages_" + currentLanguage + ".yml");

        // Nếu file không tồn tại, tạo từ resource
        if (!languageFile.exists()) {
            plugin.saveResource("messages_" + currentLanguage + ".yml", false);
        }

        // Tải file ngôn ngữ
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        // Tải default từ resource để có các key mặc định
        InputStream defaultStream = plugin.getResource("messages_" + currentLanguage + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            languageConfig.setDefaults(defaultConfig);
        }

        plugin.getLogger().info("Language loaded: " + currentLanguage.toUpperCase());
    }

    /**
     * Lấy thông điệp theo key
     */
    public String getMessage(String key) {
        String message = languageConfig.getString(key);
        if (message == null) {
            plugin.getLogger().warning("Language key not found: " + key);
            return "[" + key + "]";
        }
        return message.replace("&", "§");
    }

    /**
     * Lấy thông điệp với placeholder
     */
    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }

    /**
     * Reload ngôn ngữ
     */
    public void reload() {
        loadLanguage();
    }

    /**
     * Lấy ngôn ngữ hiện tại
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
