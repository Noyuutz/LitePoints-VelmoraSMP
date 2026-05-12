package id.nextcredits.listeners;

import id.nextcredits.NextCredits;
import id.nextcredits.database.DatabaseManager;
import id.nextcredits.gui.MainMenuGUI;
import id.nextcredits.gui.ShopGUI;
import id.nextcredits.managers.CreditsManager;
import id.nextcredits.managers.RankManager;
import id.nextcredits.models.Shop;
import id.nextcredits.models.ShopProduct;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ShopGUIListener implements Listener {
    private final NextCredits plugin;
    public ShopGUIListener(NextCredits plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!ShopGUI.isShopInventory(title)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        int slot = event.getSlot();
        if (slot == 45) { MainMenuGUI.open(player); return; }

        String shopName = title.replace(ShopGUI.GUI_TAG, "");
        Shop targetShop = null;
        for (Shop shop : plugin.getShopManager().getAllShops().values()) {
            if (shop.getDisplayName().equals(shopName)) { targetShop = shop; break; }
        }
        if (targetShop == null) return;

        ShopProduct product = targetShop.getProductBySlot(slot);
        if (product == null) return;

        handlePurchase(player, product, targetShop);
    }

    private void handlePurchase(Player player, ShopProduct product, Shop shop) {
        CreditsManager cm = plugin.getCreditsManager();
        DatabaseManager db = plugin.getDatabaseManager();
        RankManager rm = plugin.getRankManager();

        // ── Rank Product ──
        if (product.isCommand() && product.getRankTier() > 0) {
            // Check if already owned or higher rank
            RankManager.RankPurchaseResult result = rm.canBuyRank(player.getUniqueId(), shop, product);
            if (result == RankManager.RankPurchaseResult.ALREADY_OWNED) {
                player.sendMessage(colorize("&cYou already own this rank or a higher one!"));
                return;
            }

            // Calculate upgrade price
            long upgradePrice = rm.calculateUpgradePrice(player.getUniqueId(), shop, product);

            // Check credits
            if (!cm.hasEnough(player, upgradePrice)) {
                player.sendMessage(colorize("&cNot enough credits! You need &e" + upgradePrice + " &ccredits."));
                return;
            }

            // Deduct credits
            if (!cm.removeCredits(player, upgradePrice, "Rank upgrade: " + product.getId())) {
                player.sendMessage(colorize("&cTransaction failed!")); return;
            }

            // Execute commands
            for (String cmd : product.getCommands()) {
                String finalCmd = cmd.replace("{player}", player.getName());
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd));
            }

            // Mark as purchased & lock lower ranks
            db.incrementPurchaseCount(player.getUniqueId(), shop.getId(), product.getId());
            List<String> lowerRanks = plugin.getShopManager().getLowerRankIds(shop, product.getRankTier());
            db.lockLowerRanks(player.getUniqueId(), shop.getId(), lowerRanks);

            // Message
            if (upgradePrice < product.getPrice()) {
                player.sendMessage(colorize("&aPurchased rank &e" + product.getDisplayName()
                    + " &afor &e" + upgradePrice + " credits &7(upgrade discount)&a! Remaining: &e"
                    + cm.getCredits(player)));
            } else {
                player.sendMessage(colorize("&aPurchased rank &e" + product.getDisplayName()
                    + " &afor &e" + upgradePrice + " credits&a! Remaining: &e" + cm.getCredits(player)));
            }

            ShopGUI.open(player, shop);
            return;
        }

        // ── Bundle (Starter Kit) ──
        if (product.isBundle()) {
            if (product.getPurchaseLimit() > 0) {
                int count = db.getPurchaseCount(player.getUniqueId(), shop.getId(), product.getId());
                if (count >= product.getPurchaseLimit()) {
                    player.sendMessage(colorize("&cYou've reached the purchase limit for &e" + product.getDisplayName() + "&c!"));
                    return;
                }
            }
            List<ItemStack> items = product.buildBundleItems();
            int emptySlots = 0;
            for (ItemStack i : player.getInventory().getContents()) { if (i == null) emptySlots++; }
            if (emptySlots < items.size()) {
                player.sendMessage(colorize("&cNot enough inventory space! Need &e" + items.size() + " &cfree slots."));
                return;
            }
            if (!cm.hasEnough(player, product.getPrice())) {
                player.sendMessage(colorize("&cNot enough credits! You need &e" + product.getPrice() + " &ccredits."));
                return;
            }
            if (!cm.removeCredits(player, product.getPrice(), "Purchase kit: " + product.getId())) {
                player.sendMessage(colorize("&cTransaction failed!")); return;
            }
            items.forEach(item -> player.getInventory().addItem(item));
            db.incrementPurchaseCount(player.getUniqueId(), shop.getId(), product.getId());
            int newCount = db.getPurchaseCount(player.getUniqueId(), shop.getId(), product.getId());
            player.sendMessage(colorize("&aPurchased &e" + product.getDisplayName()
                + " &afor &e" + product.getPrice() + " credits&a! (&e" + newCount + "&a/&e" + product.getPurchaseLimit() + "&a) Remaining: &e" + cm.getCredits(player)));
            ShopGUI.open(player, shop);
            return;
        }

        // ── Normal Item ──
        if (product.getPurchaseLimit() > 0) {
            int count = db.getPurchaseCount(player.getUniqueId(), shop.getId(), product.getId());
            if (count >= product.getPurchaseLimit()) {
                player.sendMessage(colorize("&cYou've reached the purchase limit!"));
                return;
            }
        }
        if (!cm.hasEnough(player, product.getPrice())) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.not-enough-credits",
                "&cNot enough credits! You need &e{price} &ccredits.").replace("{price}", String.valueOf(product.getPrice()))));
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(colorize(plugin.getConfig().getString("messages.inventory-full", "&cInventory full!")));
            return;
        }
        if (!cm.removeCredits(player, product.getPrice(), "Purchase: " + product.getId())) {
            player.sendMessage(colorize("&cTransaction failed!")); return;
        }
        player.getInventory().addItem(product.buildRewardItem());
        if (product.getPurchaseLimit() > 0) db.incrementPurchaseCount(player.getUniqueId(), shop.getId(), product.getId());
        player.sendMessage(colorize(plugin.getConfig().getString("messages.purchase-success",
            "&aPurchased &e{amount}x {item} &afor &e{price} credits&a! Remaining: &e{balance}")
            .replace("{amount}", String.valueOf(product.getItemAmount()))
            .replace("{item}", product.getDisplayName())
            .replace("{price}", String.valueOf(product.getPrice()))
            .replace("{balance}", String.valueOf(cm.getCredits(player)))));
        ShopGUI.open(player, shop);
    }

    private String colorize(String t) { return t.replace("&", "§"); }
}
