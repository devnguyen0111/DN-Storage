package org.dnplugins.dNStorage;

import org.bukkit.plugin.java.JavaPlugin;

public final class DNStorage extends JavaPlugin {

    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private StorageManager storageManager;
    private StorageGUI storageGUI;
    private CommandHandler commandHandler;
    private AutoPickupListener autoPickupListener;

    @Override
    public void onEnable() {
        // Lưu config mặc định nếu chưa có
        saveDefaultConfig();

        // Lưu file ngôn ngữ mặc định
        saveResource("messages_vi.yml", false);
        saveResource("messages_en.yml", false);

        // Khởi tạo LanguageManager
        languageManager = new LanguageManager(this);

        // Khởi tạo DatabaseManager
        databaseManager = new DatabaseManager(this, languageManager);

        // Khởi tạo StorageManager
        storageManager = new StorageManager(this, databaseManager, languageManager);

        // Khởi tạo AutoPickupListener
        autoPickupListener = new AutoPickupListener(this, storageManager, languageManager);

        // Khởi tạo StorageGUI
        storageGUI = new StorageGUI(this, storageManager, autoPickupListener, languageManager);

        // Khởi tạo CommandHandler
        commandHandler = new CommandHandler(storageGUI, languageManager);

        // Đăng ký lệnh
        getCommand("kho").setExecutor(commandHandler);
        getCommand("kho").setTabCompleter(commandHandler);
        getCommand("storage").setExecutor(commandHandler);
        getCommand("storage").setTabCompleter(commandHandler);

        getLogger().info(languageManager.getMessage("plugin.enabled"));
        getLogger().info(languageManager.getMessage("message.storage.opened"));
    }

    @Override
    public void onDisable() {
        // Lưu dữ liệu khi plugin tắt
        if (storageManager != null) {
            storageManager.shutdown();
        }

        if (databaseManager != null) {
            databaseManager.closeConnection();
        }

        getLogger().info(languageManager.getMessage("plugin.disabled"));
    }
}
