package org.dnplugins.dNStorage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
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
     * Thêm hoặc cập nhật vật phẩm trong database
     */
    public void upsertItem(String playerUuid, String category, String material, int amount) {
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
     * Lấy số lượng vật phẩm từ database
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
     * Cập nhật số lượng vật phẩm
     */
    public void updateItemAmount(String playerUuid, String category, String material, int newAmount) {
        if (newAmount <= 0) {
            // Xóa vật phẩm nếu số lượng <= 0
            deleteItem(playerUuid, category, material);
            return;
        }

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
     * Xóa vật phẩm khỏi database
     */
    public void deleteItem(String playerUuid, String category, String material) {
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
     * Lấy tất cả vật phẩm trong một danh mục
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
