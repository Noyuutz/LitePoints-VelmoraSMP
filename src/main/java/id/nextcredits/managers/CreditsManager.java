package id.nextcredits.managers;

import id.nextcredits.NextCredits;
import org.bukkit.entity.Player;

import java.util.UUID;

public class CreditsManager {

    private final NextCredits plugin;

    public CreditsManager(NextCredits plugin) {
        this.plugin = plugin;
    }

    public long getCredits(UUID uuid) {
        return plugin.getDatabaseManager().getCredits(uuid);
    }

    public long getCredits(Player player) {
        return getCredits(player.getUniqueId());
    }

    public void addCredits(Player player, long amount, String reason) {
        plugin.getDatabaseManager().addCredits(player.getUniqueId(), player.getName(), amount);
        plugin.getDatabaseManager().logTransaction(player.getUniqueId(), "ADD", amount, reason);
    }

    public void addCredits(UUID uuid, String playerName, long amount, String reason) {
        plugin.getDatabaseManager().addCredits(uuid, playerName, amount);
        plugin.getDatabaseManager().logTransaction(uuid, "ADD", amount, reason);
    }

    public boolean removeCredits(Player player, long amount, String reason) {
        boolean success = plugin.getDatabaseManager().removeCredits(player.getUniqueId(), amount);
        if (success) {
            plugin.getDatabaseManager().logTransaction(player.getUniqueId(), "REMOVE", amount, reason);
        }
        return success;
    }

    public void setCredits(Player player, long amount) {
        plugin.getDatabaseManager().setCredits(player.getUniqueId(), player.getName(), amount);
        plugin.getDatabaseManager().logTransaction(player.getUniqueId(), "SET", amount, "Admin set");
    }

    public void setCredits(UUID uuid, String playerName, long amount) {
        plugin.getDatabaseManager().setCredits(uuid, playerName, amount);
        plugin.getDatabaseManager().logTransaction(uuid, "SET", amount, "Admin set");
    }

    public boolean hasEnough(Player player, long amount) {
        return getCredits(player) >= amount;
    }
}
