package org.dnplugins.dNStorage;

import org.bukkit.plugin.java.JavaPlugin;
import org.dnplugins.dNStorage.commands.CommandHandler;
import org.dnplugins.dNStorage.core.DatabaseManager;
import org.dnplugins.dNStorage.core.LanguageManager;
import org.dnplugins.dNStorage.core.SoundManager;
import org.dnplugins.dNStorage.core.StorageManager;
import org.dnplugins.dNStorage.gui.StorageGUI;
import org.dnplugins.dNStorage.listeners.AutoPickupListener;

public final class DNStorage extends JavaPlugin {

    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private StorageManager storageManager;
    private StorageGUI storageGUI;
    private CommandHandler commandHandler;
    private AutoPickupListener autoPickupListener;
    private SoundManager soundManager;

    @Override
    public void onEnable() {
        // Lưu config mặc định nếu chưa có
        saveDefaultConfig();

        // Khởi tạo LanguageManager
        languageManager = new LanguageManager(this);

        // Khởi tạo SoundManager
        soundManager = new SoundManager(this, languageManager);

        // Khởi tạo DatabaseManager
        databaseManager = new DatabaseManager(this, languageManager);

        // Khởi tạo StorageManager
        storageManager = new StorageManager(this, databaseManager, languageManager);

        // Khởi tạo AutoPickupListener
        autoPickupListener = new AutoPickupListener(this, storageManager, databaseManager, languageManager);

        // Khởi tạo StorageGUI
        storageGUI = new StorageGUI(this, storageManager, autoPickupListener, languageManager, soundManager);

        // Khởi tạo CommandHandler
        commandHandler = new CommandHandler(storageGUI, languageManager, this);

        // Đăng ký lệnh
        getCommand("kho").setExecutor(commandHandler);
        getCommand("kho").setTabCompleter(commandHandler);
        getCommand("storage").setExecutor(commandHandler);
        getCommand("storage").setTabCompleter(commandHandler);

        getLogger().info(languageManager.getMessage("plugin.enabled"));
        getLogger().info(languageManager.getMessage("message.storage.opened"));
    }

    /**
     * Reload config và messages
     */
    public void reloadPlugin() {
        // Reload config
        reloadConfig();

        // Reload language manager
        languageManager.reload();

        // Reload sound manager
        soundManager.loadConfig();

        getLogger().info(languageManager.getMessage("plugin.reloaded"));
    }

    /**
     * Lấy LanguageManager
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
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
