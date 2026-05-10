package id.nextcredits.database;

import id.nextcredits.NextCredits;
import org.bukkit.configuration.file.FileConfiguration;
import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    private final NextCredits plugin;
    private Connection connection;
    private String host, database, username, password;
    private int port;

    public DatabaseManager(NextCredits plugin) {
        this.plugin = plugin;
        FileConfiguration cfg = plugin.getConfig();
        host = cfg.getString("mysql.host", "localhost");
        port = cfg.getInt("mysql.port", 3306);
        database = cfg.getString("mysql.database", "nextcredits");
        username = cfg.getString("mysql.username", "root");
        password = cfg.getString("mysql.password", "password");
    }

    public boolean connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
            connection = DriverManager.getConnection(url, username, password);
            plugin.getLogger().info("Connected to MySQL!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to MySQL: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error closing DB: " + e.getMessage()); }
    }

    public void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Credits table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS nc_credits (" +
                "uuid VARCHAR(36) NOT NULL PRIMARY KEY, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "credits BIGINT NOT NULL DEFAULT 0, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);");

            // Transactions table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS nc_transactions (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "uuid VARCHAR(36) NOT NULL, " +
                "type ENUM('ADD','REMOVE','SET','PURCHASE') NOT NULL, " +
                "amount BIGINT NOT NULL, " +
                "reason VARCHAR(255), " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP);");

            // Purchase limits table
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS nc_purchase_limits (" +
                "uuid VARCHAR(36) NOT NULL, " +
                "shop_id VARCHAR(64) NOT NULL, " +
                "product_id VARCHAR(64) NOT NULL, " +
                "purchase_count INT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (uuid, shop_id, product_id));");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating tables: " + e.getMessage());
        }
    }

    // ── Credits ──

    public long getCredits(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT credits FROM nc_credits WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("credits");
        } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error: " + e.getMessage()); }
        return 0;
    }

    public void setCredits(UUID uuid, String playerName, long amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO nc_credits (uuid, player_name, credits) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = ?, credits = ?")) {
            ps.setString(1, uuid.toString()); ps.setString(2, playerName); ps.setLong(3, amount);
            ps.setString(4, playerName); ps.setLong(5, amount); ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error: " + e.getMessage()); }
    }

    public void addCredits(UUID uuid, String playerName, long amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO nc_credits (uuid, player_name, credits) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE player_name = ?, credits = credits + ?")) {
            ps.setString(1, uuid.toString()); ps.setString(2, playerName); ps.setLong(3, amount);
            ps.setString(4, playerName); ps.setLong(5, amount); ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error: " + e.getMessage()); }
    }

    public boolean removeCredits(UUID uuid, long amount) {
        if (getCredits(uuid) < amount) return false;
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE nc_credits SET credits = credits - ? WHERE uuid = ?")) {
            ps.setLong(1, amount); ps.setString(2, uuid.toString()); ps.executeUpdate(); return true;
        } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error: " + e.getMessage()); return false; }
    }

    public void logTransaction(UUID uuid, String type, long amount, String reason) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO nc_transactions (uuid, type, amount, reason) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString()); ps.setString(2, type);
            ps.setLong(3, amount); ps.setString(4, reason); ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error: " + e.getMessage()); }
    }

    public boolean playerExists(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT 1 FROM nc_credits WHERE uuid = ?")) {
            ps.setString(1, uuid.toString()); return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public void updatePlayerName(UUID uuid, String newName) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE nc_credits SET player_name = ? WHERE uuid = ?")) {
            ps.setString(1, newName); ps.setString(2, uuid.toString()); ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error: " + e.getMessage()); }
    }

    // ── Purchase Limits ──

    public int getPurchaseCount(UUID uuid, String shopId, String productId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT purchase_count FROM nc_purchase_limits WHERE uuid = ? AND shop_id = ? AND product_id = ?")) {
            ps.setString(1, uuid.toString()); ps.setString(2, shopId); ps.setString(3, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("purchase_count");
        } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error: " + e.getMessage()); }
        return 0;
    }

    public void incrementPurchaseCount(UUID uuid, String shopId, String productId) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO nc_purchase_limits (uuid, shop_id, product_id, purchase_count) VALUES (?, ?, ?, 1) " +
                "ON DUPLICATE KEY UPDATE purchase_count = purchase_count + 1")) {
            ps.setString(1, uuid.toString()); ps.setString(2, shopId); ps.setString(3, productId);
            ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error: " + e.getMessage()); }
    }

    public boolean hasReachedLimit(UUID uuid, String shopId, String productId, int maxLimit) {
        return getPurchaseCount(uuid, shopId, productId) >= maxLimit;
    }

    // Lock all lower ranks when higher rank is purchased
    public void lockLowerRanks(UUID uuid, String shopId, java.util.List<String> lowerRankIds) {
        for (String rankId : lowerRankIds) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO nc_purchase_limits (uuid, shop_id, product_id, purchase_count) VALUES (?, ?, ?, 999) " +
                    "ON DUPLICATE KEY UPDATE purchase_count = 999")) {
                ps.setString(1, uuid.toString()); ps.setString(2, shopId); ps.setString(3, rankId);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error locking rank: " + e.getMessage()); }
        }
    }

    public Connection getConnection() { return connection; }
}
