package id.nextcredits.gui;

import id.nextcredits.NextCredits;
import id.nextcredits.managers.RankManager;
import id.nextcredits.models.Shop;
import id.nextcredits.models.ShopProduct;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopGUI {

    public static final String GUI_TAG = "§0§r§0";

    public static void open(Player player, Shop shop) {
        NextCredits plugin = NextCredits.getInstance();
        String title = GUI_TAG + shop.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, 54, title);

        fillBorder(inv);

        RankManager rankManager = plugin.getRankManager();

        for (ShopProduct product : shop.getProducts()) {
            int slot = product.getSlot();
            if (slot < 0 || slot >= 54) continue;

            // Check purchase limit
            boolean locked = false;
            int count = 0;

            if (product.getRankTier() > 0) {
                // Rank product — check upgrade eligibility
                RankManager.RankPurchaseResult result = rankManager.canBuyRank(
                    player.getUniqueId(), shop, product);

                if (result == RankManager.RankPurchaseResult.ALREADY_OWNED) {
                    locked = true;
                    count = 1;
                }

                // Build rank item with upgrade price
                long upgradePrice = rankManager.calculateUpgradePrice(player.getUniqueId(), shop, product);
                inv.setItem(slot, buildRankItem(product, locked, upgradePrice,
                    rankManager.getPlayerRankTier(player.getUniqueId(), shop)));
            } else {
                // Normal/bundle product — check purchase limit
                if (product.getPurchaseLimit() > 0) {
                    count = plugin.getDatabaseManager().getPurchaseCount(
                        player.getUniqueId(), shop.getId(), product.getId());
                    locked = count >= product.getPurchaseLimit();
                }
                inv.setItem(slot, product.buildDisplayItem(count, locked));
            }
        }

        // Back button
        inv.setItem(45, buildBackButton());

        // Balance
        long credits = plugin.getCreditsManager().getCredits(player);
        inv.setItem(53, buildBalanceItem(credits));

        player.openInventory(inv);
    }

    private static ItemStack buildRankItem(ShopProduct product, boolean locked,
                                            long upgradePrice, int playerTier) {
        ItemStack item = new ItemStack(product.getMaterial(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(product.getDisplayName().replace("&", "§"));

        List<String> lore = new ArrayList<>();
        for (String l : product.getLore()) lore.add(l.replace("&", "§"));
        lore.add("");

        if (locked) {
            lore.add("§a§l✔ Already Owned");
            lore.add("§7You already have this rank or higher.");
        } else {
            lore.add("§eBase Price: §7" + product.getPrice() + " credits");
            if (playerTier > 0) {
                lore.add("§eUpgrade Price: §a" + upgradePrice + " credits");
                lore.add("§7(Discounted from your current rank)");
            } else {
                lore.add("§ePrice: §a" + upgradePrice + " credits");
            }
            lore.add("");
            lore.add("§aClick to purchase!");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static void fillBorder(Inventory inv) {
        ItemStack f = filler();
        for (int i = 0; i < 9; i++) inv.setItem(i, f);
        for (int i = 45; i < 54; i++) inv.setItem(i, f);
        for (int row = 1; row < 5; row++) {
            inv.setItem(row * 9, f);
            inv.setItem(row * 9 + 8, f);
        }
    }

    private static ItemStack filler() {
        ItemStack i = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = i.getItemMeta();
        if (m != null) { m.setDisplayName(" "); i.setItemMeta(m); }
        return i;
    }

    private static ItemStack buildBalanceItem(long credits) {
        ItemStack i = new ItemStack(Material.SUNFLOWER);
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName("§6Your Balance");
            m.setLore(Arrays.asList("§e" + credits + " credits"));
            i.setItemMeta(m);
        }
        return i;
    }

    private static ItemStack buildBackButton() {
        ItemStack i = new ItemStack(Material.ARROW);
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName("§7§l← Back");
            m.setLore(Arrays.asList("§7Return to main menu"));
            i.setItemMeta(m);
        }
        return i;
    }

    public static boolean isShopInventory(String title) { return title.startsWith(GUI_TAG); }
}
