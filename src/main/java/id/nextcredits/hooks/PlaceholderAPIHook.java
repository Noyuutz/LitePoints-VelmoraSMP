package id.nextcredits.hooks;

import id.nextcredits.NextCredits;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final NextCredits plugin;
    public PlaceholderAPIHook(NextCredits plugin) { this.plugin = plugin; }

    @Override public @NotNull String getIdentifier() { return "litepoints"; }
    @Override public @NotNull String getAuthor() { return "Noyuutz"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "0";
        long bal = plugin.getCreditsManager().getCredits(player.getUniqueId());
        return switch (params.toLowerCase()) {
            case "balance" -> String.valueOf(bal);
            case "balance_short" -> bal >= 1_000_000_000 ? String.format("%.1fB", bal/1_000_000_000.0)
                : bal >= 1_000_000 ? String.format("%.1fM", bal/1_000_000.0)
                : bal >= 1_000 ? String.format("%.1fK", bal/1_000.0)
                : String.valueOf(bal);
            default -> null;
        };
    }
}
