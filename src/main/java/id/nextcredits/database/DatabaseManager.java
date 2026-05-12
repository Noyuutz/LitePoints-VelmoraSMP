package id.nextcredits.database;

import id.nextcredits.NextCredits;
import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    private final NextCredits plugin;
    private Connection connection;

    public DatabaseManager(NextCredits plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) dataFolder.mkdirs();
            File dbFile = new File(dataFolder, "litepoints.db");
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
            }
            plugin.getLogger().info("Connected to SQLite database!");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to SQLite: " + e.getMessage());
            return false;
        }
    }

    public void disconnect() {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error closing DB: " + e.getMessage()); }
    }

    public void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS nc_credits (" +
                "uuid TEXT NOT NULL PRIMARY KEY, " +
                "player_name TEXT NOT NULL, " +
                "credits INTEGER NOT NULL DEFAULT 0, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP);");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS nc_transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uuid TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "amount INTEGER NOT NULL, " +
                "reason TEXT, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP);");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS nc_purchase_limits (" +
                "uuid TEXT NOT NULL, " +
                "shop_id TEXT NOT NULL, " +
                "product_id TEXT NOT NULL, " +
                "purchase_count INTEGER NOT NULL DEFAULT 0, " +
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
                "ON CONFLICT(uuid) DO UPDATE SET player_name = ?, credits = ?")) {
            ps.setString(1, uuid.toString()); ps.setString(2, playerName); ps.setLong(3, amount);
            ps.setString(4, playerName); ps.setLong(5, amount); ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error: " + e.getMessage()); }
    }

    public void addCredits(UUID uuid, String playerName, long amount) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO nc_credits (uuid, player_name, credits) VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET player_name = ?, credits = credits + ?")) {
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
                "ON CONFLICT(uuid, shop_id, product_id) DO UPDATE SET purchase_count = purchase_count + 1")) {
            ps.setString(1, uuid.toString()); ps.setString(2, shopId); ps.setString(3, productId);
            ps.executeUpdate();
        } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error: " + e.getMessage()); }
    }

    public boolean hasReachedLimit(UUID uuid, String shopId, String productId, int maxLimit) {
        return getPurchaseCount(uuid, shopId, productId) >= maxLimit;
    }

    public void lockLowerRanks(UUID uuid, String shopId, List<String> lowerRankIds) {
        for (String rankId : lowerRankIds) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO nc_purchase_limits (uuid, shop_id, product_id, purchase_count) VALUES (?, ?, ?, 999) " +
                    "ON CONFLICT(uuid, shop_id, product_id) DO UPDATE SET purchase_count = 999")) {
                ps.setString(1, uuid.toString()); ps.setString(2, shopId); ps.setString(3, rankId);
                ps.executeUpdate();
            } catch (SQLException e) { plugin.getLogger().log(Level.WARNING, "Error locking rank: " + e.getMessage()); }
        }
    }

    public Connection getConnection() { return connection; }
}
