package org.dnplugins.dNStorage.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Quản lý kết nối và thao tác với database
 */
public class DatabaseManager {

    private final JavaPlugin plugin;
    private final LanguageManager languageManager;
    private Connection connection;
    private DatabaseType databaseType;

    public enum DatabaseType {
        SQLITE,
        MYSQL
    }

    public DatabaseManager(JavaPlugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        loadDatabaseConfig();
        initializeDatabase();
    }

    /**
     * Tải cấu hình database từ config.yml
     */
    private void loadDatabaseConfig() {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("database.type", "sqlite").toLowerCase();

        if (type.equals("mysql")) {
            databaseType = DatabaseType.MYSQL;
        } else {
            databaseType = DatabaseType.SQLITE;
        }
    }

    /**
     * Khởi tạo kết nối database
     */
    private void initializeDatabase() {
        try {
            if (databaseType == DatabaseType.MYSQL) {
                connectMySQL();
            } else {
                connectSQLite();
            }

            createTables();
            plugin.getLogger().info(languageManager.getMessage("database.connected"));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.connection_failed"), e);
        }
    }

    /**
     * Kết nối MySQL
     */
    private void connectMySQL() throws SQLException {
        try {
            // Load MySQL driver class
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE,
                    languageManager.getMessage("database.driver_not_found").replace("{driver}", "MySQL"), e);
            throw new SQLException("MySQL Driver not found", e);
        }

        FileConfiguration config = plugin.getConfig();

        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "dnstorage");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "");

        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true",
                host, port, database);

        connection = DriverManager.getConnection(url, username, password);
    }

    /**
     * Kết nối SQLite
     */
    private void connectSQLite() throws SQLException {
        try {
            // Load H2 driver class
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE,
                    languageManager.getMessage("database.driver_not_found").replace("{driver}", "H2"), e);
            throw new SQLException("H2 Driver not found", e);
        }

        String url = "jdbc:h2:file:" + plugin.getDataFolder().getAbsolutePath() +
                "/storage;MODE=MySQL;DATABASE_TO_LOWER=TRUE";
        connection = DriverManager.getConnection(url);
    }

    /**
     * Tạo bảng trong database
     */
    private void createTables() throws SQLException {
        String createTableSQL;

        if (databaseType == DatabaseType.MYSQL) {
            createTableSQL = "CREATE TABLE IF NOT EXISTS storage_items (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "category VARCHAR(20) NOT NULL, " +
                    "material VARCHAR(100) NOT NULL, " +
                    "amount INT NOT NULL DEFAULT 0, " +
                    "UNIQUE KEY unique_storage (player_uuid, category, material), " +
                    "INDEX idx_player (player_uuid), " +
                    "INDEX idx_category (category)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        } else {
            // SQLite/H2 syntax
            createTableSQL = "CREATE TABLE IF NOT EXISTS storage_items (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "category VARCHAR(20) NOT NULL, " +
                    "material VARCHAR(100) NOT NULL, " +
                    "amount INT NOT NULL DEFAULT 0, " +
                    "UNIQUE (player_uuid, category, material)" +
                    ")";
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);

            // Tạo index cho SQLite
            if (databaseType == DatabaseType.SQLITE) {
                try {
                    statement.execute("CREATE INDEX IF NOT EXISTS idx_player ON storage_items(player_uuid)");
                    statement.execute("CREATE INDEX IF NOT EXISTS idx_category ON storage_items(category)");
                } catch (SQLException e) {
                    // Index có thể đã tồn tại
                }
            }
        }

        // Tạo bảng player_settings để lưu trạng thái auto-pickup
        createPlayerSettingsTable();
    }

    /**
     * Lấy kết nối database
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                if (databaseType == DatabaseType.MYSQL) {
                    connectMySQL();
                } else {
                    connectSQLite();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.reconnect_failed"), e);
            return null;
        }

        if (connection == null) {
            plugin.getLogger().severe(languageManager.getMessage("database.connection_null"));
        }

        return connection;
    }

    /**
     * Thêm hoặc cập nhật vật phẩm trong database (Async)
     */
    public void upsertItem(String playerUuid, String category, String material, int amount) {
        // Chạy async để không block main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            upsertItemSync(playerUuid, category, material, amount);
        });
    }

    /**
     * Thêm hoặc cập nhật vật phẩm trong database (Synchronous - internal use)
     */
    private void upsertItemSync(String playerUuid, String category, String material, int amount) {
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().warning(languageManager.getMessage("database.cannot_add_item"));
            return;
        }

        if (databaseType == DatabaseType.MYSQL) {
            String sql = "INSERT INTO storage_items (player_uuid, category, material, amount) " +
                    "VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE amount = amount + ?";

            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, playerUuid);
                statement.setString(2, category);
                statement.setString(3, material);
                statement.setInt(4, amount);
                statement.setInt(5, amount);
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.add_item"), e);
            }
        } else {
            // SQLite/H2 - sử dụng UPDATE rồi INSERT
            try {
                // Thử cập nhật trước
                String updateSQL = "UPDATE storage_items SET amount = amount + ? " +
                        "WHERE player_uuid = ? AND category = ? AND material = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    updateStmt.setInt(1, amount);
                    updateStmt.setString(2, playerUuid);
                    updateStmt.setString(3, category);
                    updateStmt.setString(4, material);
                    int rows = updateStmt.executeUpdate();

                    // Nếu không có dòng nào được cập nhật, thêm mới
                    if (rows == 0) {
                        String insertSQL = "INSERT INTO storage_items (player_uuid, category, material, amount) " +
                                "VALUES (?, ?, ?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                            insertStmt.setString(1, playerUuid);
                            insertStmt.setString(2, category);
                            insertStmt.setString(3, material);
                            insertStmt.setInt(4, amount);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.add_item"), e);
            }
        }
    }

    /**
     * Lấy số lượng vật phẩm từ database (Synchronous - for backward compatibility)
     */
    public int getItemAmount(String playerUuid, String category, String material) {
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().warning(languageManager.getMessage("database.cannot_get_amount"));
            return 0;
        }

        String sql = "SELECT amount FROM storage_items " +
                "WHERE player_uuid = ? AND category = ? AND material = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            statement.setString(2, category);
            statement.setString(3, material);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("amount");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.get_amount"), e);
        }
        return 0;
    }

    /**
     * Lấy số lượng vật phẩm từ database (Async với callback)
     */
    public void getItemAmountAsync(String playerUuid, String category, String material, Consumer<Integer> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int amount = getItemAmount(playerUuid, category, material);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(amount));
        });
    }

    /**
     * Cập nhật số lượng vật phẩm (Async)
     */
    public void updateItemAmount(String playerUuid, String category, String material, int newAmount) {
        if (newAmount <= 0) {
            // Xóa vật phẩm nếu số lượng <= 0
            deleteItem(playerUuid, category, material);
            return;
        }

        // Chạy async để không block main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            updateItemAmountSync(playerUuid, category, material, newAmount);
        });
    }

    /**
     * Cập nhật số lượng vật phẩm (Synchronous - internal use)
     */
    private void updateItemAmountSync(String playerUuid, String category, String material, int newAmount) {
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().warning(languageManager.getMessage("database.cannot_update"));
            return;
        }

        String sql = "UPDATE storage_items SET amount = ? " +
                "WHERE player_uuid = ? AND category = ? AND material = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setInt(1, newAmount);
            statement.setString(2, playerUuid);
            statement.setString(3, category);
            statement.setString(4, material);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.update"), e);
        }
    }

    /**
     * Xóa vật phẩm khỏi database (Async)
     */
    public void deleteItem(String playerUuid, String category, String material) {
        // Chạy async để không block main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            deleteItemSync(playerUuid, category, material);
        });
    }

    /**
     * Xóa vật phẩm khỏi database (Synchronous - internal use)
     */
    private void deleteItemSync(String playerUuid, String category, String material) {
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().warning(languageManager.getMessage("database.cannot_delete"));
            return;
        }

        String sql = "DELETE FROM storage_items " +
                "WHERE player_uuid = ? AND category = ? AND material = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            statement.setString(2, category);
            statement.setString(3, material);
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.delete"), e);
        }
    }

    /**
     * Lấy tất cả vật phẩm trong một danh mục (Synchronous - for backward
     * compatibility)
     */
    public java.util.Map<String, Integer> getCategoryItems(String playerUuid, String category) {
        java.util.Map<String, Integer> items = new java.util.HashMap<>();

        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().warning(languageManager.getMessage("database.cannot_get_items"));
            return items;
        }

        String sql = "SELECT material, amount FROM storage_items " +
                "WHERE player_uuid = ? AND category = ?";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            statement.setString(2, category);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String material = resultSet.getString("material");
                int amount = resultSet.getInt("amount");
                items.put(material, amount);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.get_items"), e);
        }

        return items;
    }

    /**
     * Lấy tất cả vật phẩm trong một danh mục (Async với callback)
     */
    public void getCategoryItemsAsync(String playerUuid, String category, Consumer<Map<String, Integer>> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<String, Integer> items = getCategoryItems(playerUuid, category);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(items));
        });
    }

    /**
     * Batch upsert nhiều items cùng lúc (Async)
     * 
     * @param items Map<Material, Amount> để upsert
     */
    public void batchUpsertItems(String playerUuid, String category, Map<String, Integer> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            batchUpsertItemsSync(playerUuid, category, items);
        });
    }

    /**
     * Batch upsert nhiều items cùng lúc (Synchronous - internal use)
     */
    private void batchUpsertItemsSync(String playerUuid, String category, Map<String, Integer> items) {
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().warning(languageManager.getMessage("database.cannot_add_item"));
            return;
        }

        try {
            conn.setAutoCommit(false); // Bắt đầu transaction

            if (databaseType == DatabaseType.MYSQL) {
                String sql = "INSERT INTO storage_items (player_uuid, category, material, amount) " +
                        "VALUES (?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE amount = amount + ?";

                try (PreparedStatement statement = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, Integer> entry : items.entrySet()) {
                        statement.setString(1, playerUuid);
                        statement.setString(2, category);
                        statement.setString(3, entry.getKey());
                        statement.setInt(4, entry.getValue());
                        statement.setInt(5, entry.getValue());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            } else {
                // SQLite/H2 - sử dụng batch operations
                String updateSQL = "UPDATE storage_items SET amount = amount + ? " +
                        "WHERE player_uuid = ? AND category = ? AND material = ?";
                String insertSQL = "INSERT INTO storage_items (player_uuid, category, material, amount) " +
                        "VALUES (?, ?, ?, ?)";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
                        PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {

                    for (Map.Entry<String, Integer> entry : items.entrySet()) {
                        String material = entry.getKey();
                        int amount = entry.getValue();

                        // Thử update trước
                        updateStmt.setInt(1, amount);
                        updateStmt.setString(2, playerUuid);
                        updateStmt.setString(3, category);
                        updateStmt.setString(4, material);
                        updateStmt.addBatch();
                    }
                    updateStmt.executeBatch();

                    // Kiểm tra và insert những items chưa tồn tại
                    // (Cần query lại để biết items nào chưa được update)
                    for (Map.Entry<String, Integer> entry : items.entrySet()) {
                        String material = entry.getKey();
                        int amount = entry.getValue();

                        // Kiểm tra xem item đã tồn tại chưa
                        String checkSQL = "SELECT amount FROM storage_items " +
                                "WHERE player_uuid = ? AND category = ? AND material = ?";
                        try (PreparedStatement checkStmt = conn.prepareStatement(checkSQL)) {
                            checkStmt.setString(1, playerUuid);
                            checkStmt.setString(2, category);
                            checkStmt.setString(3, material);
                            ResultSet rs = checkStmt.executeQuery();
                            if (!rs.next()) {
                                // Chưa tồn tại, insert
                                insertStmt.setString(1, playerUuid);
                                insertStmt.setString(2, category);
                                insertStmt.setString(3, material);
                                insertStmt.setInt(4, amount);
                                insertStmt.addBatch();
                            }
                        }
                    }
                    insertStmt.executeBatch();
                }
            }

            conn.commit(); // Commit transaction
        } catch (SQLException e) {
            try {
                conn.rollback(); // Rollback nếu có lỗi
            } catch (SQLException rollbackEx) {
                plugin.getLogger().log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
            }
            plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.add_item"), e);
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to reset auto-commit", e);
            }
        }
    }

    /**
     * Tạo bảng player_settings để lưu các cài đặt của player
     */
    private void createPlayerSettingsTable() throws SQLException {
        String createTableSQL;

        if (databaseType == DatabaseType.MYSQL) {
            createTableSQL = "CREATE TABLE IF NOT EXISTS player_settings (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_uuid VARCHAR(36) NOT NULL UNIQUE, " +
                    "auto_pickup BOOLEAN NOT NULL DEFAULT FALSE, " +
                    "INDEX idx_player (player_uuid)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        } else {
            // SQLite/H2 syntax
            createTableSQL = "CREATE TABLE IF NOT EXISTS player_settings (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_uuid VARCHAR(36) NOT NULL UNIQUE, " +
                    "auto_pickup BOOLEAN NOT NULL DEFAULT FALSE" +
                    ")";
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);

            // Tạo index cho SQLite
            if (databaseType == DatabaseType.SQLITE) {
                try {
                    statement.execute("CREATE INDEX IF NOT EXISTS idx_player_settings ON player_settings(player_uuid)");
                } catch (SQLException e) {
                    // Index có thể đã tồn tại
                }
            }
        }
    }

    /**
     * Lấy trạng thái auto-pickup của player
     */
    public boolean getAutoPickup(String playerUuid) {
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().warning(languageManager.getMessage("database.cannot_get_amount"));
            return false;
        }

        String sql = "SELECT auto_pickup FROM player_settings WHERE player_uuid = ?";
        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            statement.setString(1, playerUuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean("auto_pickup");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.get_amount"), e);
        }
        return false; // Mặc định là false
    }

    /**
     * Lưu trạng thái auto-pickup của player
     */
    public void setAutoPickup(String playerUuid, boolean enabled) {
        Connection conn = getConnection();
        if (conn == null) {
            plugin.getLogger().warning(languageManager.getMessage("database.cannot_add_item"));
            return;
        }

        if (databaseType == DatabaseType.MYSQL) {
            // MySQL - sử dụng INSERT ... ON DUPLICATE KEY UPDATE
            String sql = "INSERT INTO player_settings (player_uuid, auto_pickup) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE auto_pickup = ?";
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setString(1, playerUuid);
                statement.setBoolean(2, enabled);
                statement.setBoolean(3, enabled);
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.add_item"), e);
            }
        } else {
            // SQLite/H2 - sử dụng UPDATE rồi INSERT
            try {
                // Thử cập nhật trước
                String updateSQL = "UPDATE player_settings SET auto_pickup = ? WHERE player_uuid = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSQL)) {
                    updateStmt.setBoolean(1, enabled);
                    updateStmt.setString(2, playerUuid);
                    int rows = updateStmt.executeUpdate();

                    // Nếu không có dòng nào được cập nhật, thêm mới
                    if (rows == 0) {
                        String insertSQL = "INSERT INTO player_settings (player_uuid, auto_pickup) VALUES (?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertSQL)) {
                            insertStmt.setString(1, playerUuid);
                            insertStmt.setBoolean(2, enabled);
                            insertStmt.executeUpdate();
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.add_item"), e);
            }
        }
    }

    /**
     * Đóng kết nối database
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info(languageManager.getMessage("database.connection_closed"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, languageManager.getMessage("database.error.close"), e);
        }
    }
}
