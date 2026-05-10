package id.nextcredits.commands;

import id.nextcredits.NextCredits;
import id.nextcredits.managers.CreditsManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CreditsCommand implements CommandExecutor, TabCompleter {

    private final NextCredits plugin;
    private final CreditsManager cm;

    public CreditsCommand(NextCredits plugin) {
        this.plugin = plugin;
        this.cm = plugin.getCreditsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // /credits — show own balance
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cConsole must specify a player.");
                return true;
            }
            long bal = cm.getCredits(player);
            player.sendMessage(colorize(plugin.getConfig().getString("messages.balance",
                    "&aYour balance: &e{balance} credits").replace("{balance}", String.valueOf(bal))));
            return true;
        }

        switch (args[0].toLowerCase()) {

            // /credits balance [player]
            case "balance", "bal" -> {
                if (args.length < 2) {
                    if (!(sender instanceof Player player)) { sender.sendMessage("§cSpecify player."); return true; }
                    sender.sendMessage(colorize("&aYour balance: &e" + cm.getCredits((Player) sender) + " credits"));
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                long bal = cm.getCredits(target.getUniqueId());
                sender.sendMessage(colorize("&a" + target.getName() + "'s balance: &e" + bal + " credits"));
            }

            // /credits give <player> <amount>
            case "give", "add" -> {
                if (!sender.hasPermission("litepoints.admin")) {
                    sender.sendMessage(colorize("&cNo permission!"));
                    return true;
                }
                if (args.length < 3) { sender.sendMessage("§cUsage: /credits give <player> <amount>"); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                try {
                    long amount = Long.parseLong(args[2]);
                    if (amount <= 0) { sender.sendMessage("§cAmount must be positive!"); return true; }
                    cm.addCredits(target.getUniqueId(), target.getName(), amount, "Admin give by " + sender.getName());
                    sender.sendMessage(colorize("&aGave &e" + amount + " credits &ato &e" + target.getName()));
                    Player online = target.getPlayer();
                    if (online != null) online.sendMessage(colorize("&aYou received &e" + amount + " credits!"));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid amount!");
                }
            }

            // /credits take <player> <amount>
            case "take", "remove" -> {
                if (!sender.hasPermission("litepoints.admin")) {
                    sender.sendMessage(colorize("&cNo permission!")); return true;
                }
                if (args.length < 3) { sender.sendMessage("§cUsage: /credits take <player> <amount>"); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                try {
                    long amount = Long.parseLong(args[2]);
                    if (amount <= 0) { sender.sendMessage("§cAmount must be positive!"); return true; }
                    boolean ok = plugin.getDatabaseManager().removeCredits(target.getUniqueId(), amount);
                    if (ok) {
                        plugin.getDatabaseManager().logTransaction(target.getUniqueId(), "REMOVE", amount, "Admin take by " + sender.getName());
                        sender.sendMessage(colorize("&aTook &e" + amount + " credits &afrom &e" + target.getName()));
                    } else {
                        sender.sendMessage(colorize("&c" + target.getName() + " doesn't have enough credits!"));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid amount!");
                }
            }

            // /credits set <player> <amount>
            case "set" -> {
                if (!sender.hasPermission("litepoints.admin")) {
                    sender.sendMessage(colorize("&cNo permission!")); return true;
                }
                if (args.length < 3) { sender.sendMessage("§cUsage: /credits set <player> <amount>"); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                try {
                    long amount = Long.parseLong(args[2]);
                    if (amount < 0) { sender.sendMessage("§cAmount cannot be negative!"); return true; }
                    cm.setCredits(target.getUniqueId(), target.getName(), amount);
                    sender.sendMessage(colorize("&aSet &e" + target.getName() + "'s &acredits to &e" + amount));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid amount!");
                }
            }

            // /credits reload
            case "reload" -> {
                if (!sender.hasPermission("litepoints.admin")) {
                    sender.sendMessage(colorize("&cNo permission!")); return true;
                }
                plugin.reloadConfig();
                plugin.getShopManager().loadShops();
                sender.sendMessage(colorize("&aNextCredits reloaded!"));
            }

            case "panel", "editor", "edit" -> {
                if (!sender.hasPermission("litepoints.admin")) { sender.sendMessage(colorize("&cNo permission!")); return true; }
                if (!(sender instanceof Player player)) { sender.sendMessage("00a7cOnly players!"); return true; }
                id.nextcredits.gui.ShopEditorGUI.openShopSelector(player);
            }
            default -> sender.sendMessage(colorize("&cUsage: /credits [balance|give|take|set|reload|panel]"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("balance", "give", "take", "set", "reload");
        return null;
    }

    private String colorize(String text) { return text.replace("&", "§"); }
}
