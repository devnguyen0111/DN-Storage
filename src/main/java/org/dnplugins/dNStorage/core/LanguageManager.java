package org.dnplugins.dNStorage.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Quản lý ngôn ngữ và thông điệp
 */
public class LanguageManager {

    private final JavaPlugin plugin;
    private FileConfiguration languageConfig;
    private String currentLanguage;
    private List<String> availableLanguages;

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.availableLanguages = new ArrayList<>();
        loadAvailableLanguages();
        loadLanguage();
    }

    /**
     * Tải danh sách các ngôn ngữ có sẵn từ thư mục languages/
     */
    private void loadAvailableLanguages() {
        // Tạo thư mục languages nếu chưa có
        File languagesFolder = new File(plugin.getDataFolder(), "languages");
        boolean folderCreated = false;
        if (!languagesFolder.exists()) {
            languagesFolder.mkdirs();
            folderCreated = true;
        }

        // Copy các file ngôn ngữ từ resources/languages/ vào data folder nếu thư mục vừa được tạo
        // hoặc nếu thư mục trống
        File[] existingFiles = languagesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (folderCreated || (existingFiles != null && existingFiles.length == 0)) {
            // Copy từng file ngôn ngữ từ resources
            String[] languageFiles = {"vi.yml", "en.yml", "es.yml", "fr.yml", "de.yml", "zh.yml", "ja.yml", "ko.yml", "pt.yml", "ru.yml"};
            for (String langFile : languageFiles) {
                try {
                    plugin.saveResource("languages/" + langFile, false);
                } catch (Exception e) {
                    // File có thể không tồn tại trong resources, bỏ qua
                }
            }
        }

        // Quét các file .yml trong thư mục languages
        File[] files = languagesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String langCode = file.getName().replace(".yml", "");
                availableLanguages.add(langCode);
            }
        }

        // Nếu không có ngôn ngữ nào, thêm vi và en mặc định
        if (availableLanguages.isEmpty()) {
            availableLanguages.add("vi");
            availableLanguages.add("en");
        }

        plugin.getLogger().info("Available languages: " + String.join(", ", availableLanguages));
    }

    /**
     * Tải ngôn ngữ từ config
     */
    private void loadLanguage() {
        // Lấy ngôn ngữ từ config
        FileConfiguration config = plugin.getConfig();
        currentLanguage = config.getString("language", "vi").toLowerCase();

        // Kiểm tra xem ngôn ngữ có tồn tại không
        if (!availableLanguages.contains(currentLanguage)) {
            plugin.getLogger().warning("Language '" + currentLanguage + "' not found! Using 'vi' as fallback.");
            currentLanguage = "vi";
        }

        // Tải file ngôn ngữ từ thư mục languages/
        File languageFile = new File(plugin.getDataFolder(), "languages/" + currentLanguage + ".yml");

        // Nếu file không tồn tại trong data folder, copy từ resource
        if (!languageFile.exists()) {
            try {
                plugin.saveResource("languages/" + currentLanguage + ".yml", false);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load language file: " + currentLanguage + ".yml");
                // Fallback về vi nếu không tìm thấy
                if (!currentLanguage.equals("vi")) {
                    currentLanguage = "vi";
                    languageFile = new File(plugin.getDataFolder(), "languages/" + currentLanguage + ".yml");
                    try {
                        plugin.saveResource("languages/" + currentLanguage + ".yml", false);
                    } catch (Exception ex) {
                        plugin.getLogger().severe("Could not load default language file!");
                    }
                }
            }
        }

        // Tải file ngôn ngữ
        languageConfig = YamlConfiguration.loadConfiguration(languageFile);

        // Tải default từ resource để có các key mặc định
        InputStream defaultStream = plugin.getResource("languages/" + currentLanguage + ".yml");
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

    /**
     * Lấy danh sách các ngôn ngữ có sẵn
     */
    public List<String> getAvailableLanguages() {
        return new ArrayList<>(availableLanguages);
    }

    /**
     * Kiểm tra xem ngôn ngữ có tồn tại không
     */
    public boolean isLanguageAvailable(String languageCode) {
        return availableLanguages.contains(languageCode.toLowerCase());
    }
}

