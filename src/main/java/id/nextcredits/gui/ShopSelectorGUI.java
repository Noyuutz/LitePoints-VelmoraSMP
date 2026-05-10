package id.nextcredits.gui;

import id.nextcredits.NextCredits;
import id.nextcredits.models.Shop;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopSelectorGUI {

    public static final String SELECTOR_TAG = "§0§r§1";
    private static final Material[] SHOP_ICONS = {
        Material.CHEST, Material.BARREL, Material.SHULKER_BOX,
        Material.ENDER_CHEST, Material.TRAPPED_CHEST
    };

    public static void open(Player player) {
        NextCredits plugin = NextCredits.getInstance();
        Map<String, Shop> shops = plugin.getShopManager().getAllShops();

        int size = Math.min(54, (int) Math.ceil(shops.size() / 9.0) * 9);
        if (size == 0) size = 9;

        String title = SELECTOR_TAG + "§6§lSelect a Shop";
        Inventory inv = Bukkit.createInventory(null, size, title);

        int slot = 0;
        int iconIndex = 0;
        for (Shop shop : shops.values()) {
            if (slot >= size) break;
            Material icon = SHOP_ICONS[iconIndex % SHOP_ICONS.length];
            inv.setItem(slot, buildShopIcon(shop, icon));
            slot++;
            iconIndex++;
        }

        // Fill empty slots
        ItemStack filler = buildFiller();
        for (int i = slot; i < size; i++) inv.setItem(i, filler);

        player.openInventory(inv);
    }

    private static ItemStack buildShopIcon(Shop shop, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(shop.getDisplayName());
        meta.setLore(Arrays.asList(
            "§7Click to open this shop",
            "§8ID: §7" + shop.getId(),
            "§8Products: §e" + shop.getProducts().size()
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    public static boolean isSelectorInventory(String title) {
        return title.startsWith(SELECTOR_TAG);
    }
}
