package id.nextcredits.commands;

import id.nextcredits.NextCredits;
import id.nextcredits.managers.CreditsManager;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ChaosKitsCommand implements CommandExecutor {

    private final NextCredits plugin;
    private static final long KIT_PRICE = 10500;
    private static final long REQUIRED_PLAYTIME_TICKS = 172800L * 20L;
    private static final String SHOP_ID = "chaoskits";
    private static final String PRODUCT_ID = "kings_kit";
    private static final int PURCHASE_LIMIT = 1;

    public ChaosKitsCommand(NextCredits plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /ckitsbuy <player>");
            return true;
        }

        Player player = Bukkit.getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        UUID uuid = player.getUniqueId();
        CreditsManager cm = plugin.getCreditsManager();

        // Check playtime
        long playtimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        if (playtimeTicks < REQUIRED_PLAYTIME_TICKS) {
            long remainingHours = (REQUIRED_PLAYTIME_TICKS - playtimeTicks) / 20 / 3600;
            player.sendMessage(colorize("&cYou need at least &e2 days &cof playtime!"));
            player.sendMessage(colorize("&cRemaining: &e" + remainingHours + " hours"));
            return true;
        }

        // Check purchase limit
        int count = plugin.getDatabaseManager().getPurchaseCount(uuid, SHOP_ID, PRODUCT_ID);
        if (count >= PURCHASE_LIMIT) {
            player.sendMessage(colorize("&cYou have already purchased the &c&lKing's Kit&c!"));
            return true;
        }

        // Check credits
        if (!cm.hasEnough(player, KIT_PRICE)) {
            player.sendMessage(colorize("&cNot enough credits! You need &e" + KIT_PRICE
                + " &ccredits. Your balance: &e" + cm.getCredits(player)));
            return true;
        }

        // Deduct credits
        if (!cm.removeCredits(player, KIT_PRICE, "Purchase: King's Kit")) {
            player.sendMessage(colorize("&cTransaction failed!"));
            return true;
        }

        // Mark as purchased
        plugin.getDatabaseManager().incrementPurchaseCount(uuid, SHOP_ID, PRODUCT_ID);
        plugin.getDatabaseManager().logTransaction(uuid, "PURCHASE", KIT_PRICE, "King's Kit");

        // Tell Skript to give the kit
        plugin.getServer().getScheduler().runTask(plugin, () ->
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ckitsgive " + player.getName())
        );

        return true;
    }

    private String colorize(String t) { return t.replace("&", "§"); }
}
