package id.nextcredits.listeners;

import id.nextcredits.NextCredits;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final NextCredits plugin;

    public PlayerJoinListener(NextCredits plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Run async so DB doesn't block main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            long current = plugin.getCreditsManager().getCredits(player.getUniqueId());

            // First time join — give starter credits
            if (current == 0 && !plugin.getDatabaseManager().playerExists(player.getUniqueId())) {
                long starter = plugin.getConfig().getLong("starter-credits", 0);
                if (starter > 0) {
                    plugin.getCreditsManager().addCredits(
                        player.getUniqueId(),
                        player.getName(),
                        starter,
                        "Starter credits"
                    );
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                        player.sendMessage(colorize(plugin.getConfig()
                            .getString("messages.starter-credits",
                                "&aWelcome! You received &e{amount} &astarter credits!")
                            .replace("{amount}", String.valueOf(starter))))
                    );
                }
            } else {
                // Update player name in case it changed
                plugin.getDatabaseManager().updatePlayerName(player.getUniqueId(), player.getName());
            }
        });
    }

    private String colorize(String text) { return text.replace("&", "§"); }
}
