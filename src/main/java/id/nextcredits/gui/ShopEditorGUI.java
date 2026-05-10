package id.nextcredits.gui;

import id.nextcredits.NextCredits;
import id.nextcredits.models.Shop;
import id.nextcredits.models.ShopProduct;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class ShopEditorGUI {

    public static final String EDITOR_TAG = "§0§r§2";
    public static final String SLOT_EDITOR_TAG = "§0§r§3";
    public static final String SHOP_SELECTOR_TAG = "§0§r§4";

    // ── Shop Selector (pilih shop mana yang mau diedit) ──
    public static void openShopSelector(Player player) {
        NextCredits plugin = NextCredits.getInstance();
        int size = 27;
        Inventory inv = Bukkit.createInventory(null, size, SHOP_SELECTOR_TAG + "§6§lAdmin — Select Shop");

        int slot = 10;
        for (Shop shop : plugin.getShopManager().getAllShops().values()) {
            if (slot >= 17) break;
            ItemStack item = new ItemStack(Material.CHEST);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(shop.getDisplayName());
                meta.setLore(Arrays.asList(
                    "§7Products: §e" + shop.getProducts().size(),
                    "§8ID: §7" + shop.getId(),
                    "",
                    "§aClick to edit this shop"
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        // Add "New Shop" button
        ItemStack newShop = new ItemStack(Material.LIME_DYE);
        ItemMeta newMeta = newShop.getItemMeta();
        if (newMeta != null) {
            newMeta.setDisplayName("§a§l+ Create New Shop");
            newMeta.setLore(Arrays.asList("§7Click to create a new shop"));
            newShop.setItemMeta(newMeta);
        }
        inv.setItem(22, newShop);

        fillEmpty(inv, size);
        player.openInventory(inv);
    }

    // ── Main Editor (edit slot-slot di shop) ──
    public static void openEditor(Player player, Shop shop) {
        int size = shop.getSize();
        String title = EDITOR_TAG + "§c§lEditing: §r" + shop.getDisplayName();
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill all slots with "empty slot" indicator
        for (int i = 0; i < size; i++) {
            inv.setItem(i, buildEmptySlot(i));
        }

        // Place existing products
        for (ShopProduct product : shop.getProducts()) {
            int slot = product.getSlot();
            if (slot >= 0 && slot < size) {
                inv.setItem(slot, buildEditableItem(product));
            }
        }

        // Bottom bar controls
        inv.setItem(size - 9, buildControlItem(Material.RED_DYE, "§c§lClear All Products", "§7Removes all products from this shop"));
        inv.setItem(size - 5, buildControlItem(Material.PAPER, "§e§lShop Info", "§8ID: §7" + shop.getId(), "§8Rows: §7" + shop.getRows(), "§8Products: §e" + shop.getProducts().size()));
        inv.setItem(size - 1, buildControlItem(Material.ARROW, "§7§lBack", "§7Return to shop selector"));

        player.openInventory(inv);
        player.sendMessage(colorize("&8[&6Editor&8] &7Hold an item and &aLeft Click &7an empty slot to add it. &cRight Click &7an existing item to remove it."));
    }

    // ── Slot Editor (set harga setelah item ditambah) ──
    public static void openSlotEditor(Player player, Shop shop, int slot, ItemStack item, long currentPrice) {
        Inventory inv = Bukkit.createInventory(null, 27, SLOT_EDITOR_TAG + "§6§lSet Price — Slot " + slot);

        // Show the item being edited
        ItemStack display = item.clone();
        inv.setItem(13, display);

        // Price options
        long[] prices = {10, 25, 50, 100, 250, 500, 1000, 2500, 5000};
        Material[] colors = {
            Material.WHITE_DYE, Material.ORANGE_DYE, Material.YELLOW_DYE,
            Material.LIME_DYE, Material.CYAN_DYE, Material.BLUE_DYE,
            Material.PURPLE_DYE, Material.MAGENTA_DYE, Material.RED_DYE
        };

        int[] priceSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        for (int i = 0; i < prices.length; i++) {
            ItemStack priceItem = new ItemStack(colors[i]);
            ItemMeta meta = priceItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e§l" + prices[i] + " Credits");
                meta.setLore(Arrays.asList(
                    "§7Click to set price to §e" + prices[i] + " credits",
                    currentPrice == prices[i] ? "§a✔ Currently selected" : ""
                ));
                priceItem.setItemMeta(meta);
            }
            inv.setItem(priceSlots[i], priceItem);
        }

        // Custom price button
        ItemStack custom = new ItemStack(Material.NAME_TAG);
        ItemMeta customMeta = custom.getItemMeta();
        if (customMeta != null) {
            customMeta.setDisplayName("§b§lCustom Price");
            customMeta.setLore(Arrays.asList(
                "§7Click and type in chat",
                "§7to set a custom price"
            ));
            custom.setItemMeta(customMeta);
        }
        inv.setItem(22, custom);

        // Back button
        inv.setItem(18, buildControlItem(Material.ARROW, "§7§lBack", "§7Return to shop editor"));

        fillEmpty(inv, 27);
        player.openInventory(inv);
    }

    // ── Helpers ──

    private static ItemStack buildEmptySlot(int slot) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§7Empty Slot #" + slot);
            meta.setLore(Arrays.asList(
                "§8Hold an item and",
                "§aLeft Click §8to add it here"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildEditableItem(ShopProduct product) {
        ItemStack item = new ItemStack(product.getMaterial(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(product.getDisplayName()));
            meta.setLore(Arrays.asList(
                "§ePrice: §a" + product.getPrice() + " credits",
                "§eAmount: §f" + product.getItemAmount() + "x",
                "",
                "§aLeft Click §7to edit price",
                "§cRight Click §7to remove"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildControlItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void fillEmpty(Inventory inv, int size) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); filler.setItemMeta(meta); }
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, filler);
        }
    }

    public static boolean isEditorInventory(String title) { return title.startsWith(EDITOR_TAG); }
    public static boolean isSlotEditorInventory(String title) { return title.startsWith(SLOT_EDITOR_TAG); }
    public static boolean isShopSelectorEditorInventory(String title) { return title.startsWith(SHOP_SELECTOR_TAG); }

    private static String colorize(String t) { return t.replace("&", "§"); }
}
