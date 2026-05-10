package id.nextcredits.commands;

import id.nextcredits.NextCredits;
import id.nextcredits.gui.MainMenuGUI;
import id.nextcredits.gui.ShopGUI;
import id.nextcredits.gui.ShopSelectorGUI;
import id.nextcredits.models.Shop;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class ShopCommand implements CommandExecutor, TabCompleter {
    private final NextCredits plugin;
    public ShopCommand(NextCredits plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cOnly players!"); return true; }
        if (!player.hasPermission("litepoints.shop")) { player.sendMessage(colorize("&cNo permission!")); return true; }

        // /creditshop or /shops → open main menu
        if (cmd.getName().equalsIgnoreCase("creditshop") || cmd.getName().equalsIgnoreCase("shops")) {
            MainMenuGUI.open(player);
            return true;
        }

        // /shop [id] → direct open specific shop
        String shopId = args.length > 0 ? args[0].toLowerCase() : "main";
        if (!plugin.getShopManager().shopExists(shopId)) {
            if (plugin.getShopManager().getAllShops().isEmpty()) { player.sendMessage(colorize("&cNo shops available!")); return true; }
            if (args.length > 0) { player.sendMessage(colorize("&cShop '&e" + shopId + "&c' not found!")); return true; }
            shopId = plugin.getShopManager().getAllShops().keySet().iterator().next();
        }
        ShopGUI.open(player, plugin.getShopManager().getShop(shopId));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1) return new ArrayList<>(plugin.getShopManager().getAllShops().keySet());
        return null;
    }

    private String colorize(String t) { return t.replace("&", "§"); }
}
