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

public class MainMenuGUI {

    public static final String MENU_TAG = "§0§r§5";

    // Icon per shop ID (bisa dikustomisasi)
    private static final Map<String, Material> SHOP_ICONS = new HashMap<>();
    static {
        SHOP_ICONS.put("main",    Material.CHEST);
        SHOP_ICONS.put("ranks",   Material.NETHER_STAR);
        SHOP_ICONS.put("weapons", Material.DIAMOND_SWORD);
        SHOP_ICONS.put("armor",   Material.DIAMOND_CHESTPLATE);
        SHOP_ICONS.put("food",    Material.GOLDEN_APPLE);
        SHOP_ICONS.put("tools",   Material.DIAMOND_PICKAXE);
        SHOP_ICONS.put("misc",    Material.ENDER_CHEST);
        SHOP_ICONS.put("special", Material.BEACON);
    }

    private static final Material[] DEFAULT_ICONS = {
        Material.CHEST, Material.BARREL, Material.SHULKER_BOX,
        Material.ENDER_CHEST, Material.TRAPPED_CHEST, Material.BEACON,
        Material.NETHER_STAR, Material.BOOKSHELF
    };

    public static void open(Player player) {
        NextCredits plugin = NextCredits.getInstance();
        Map<String, Shop> shops = plugin.getShopManager().getAllShops();

        // Calculate inventory size based on shop count (min 27, max 54)
        int shopCount = shops.size();
        int size = shopCount <= 7 ? 27 : 54;

        Inventory inv = Bukkit.createInventory(null, size, MENU_TAG + "§6§l✦ Credit Shop ✦");

        // Fill with filler
        ItemStack filler = buildFiller();
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        // Calculate center slots based on shop count
        List<Integer> slots = getCenterSlots(size, shopCount);

        int idx = 0;
        for (Shop shop : shops.values()) {
            if (idx >= slots.size()) break;
            Material icon = SHOP_ICONS.getOrDefault(shop.getId(), DEFAULT_ICONS[idx % DEFAULT_ICONS.length]);
            inv.setItem(slots.get(idx), buildShopButton(shop, icon));
            idx++;
        }

        // Balance display (bottom right)
        long credits = plugin.getCreditsManager().getCredits(player);
        inv.setItem(size - 1, buildBalanceItem(credits));

        player.openInventory(inv);
    }

    private static List<Integer> getCenterSlots(int invSize, int count) {
        List<Integer> slots = new ArrayList<>();
        int rows = invSize / 9;

        if (count <= 1) {
            slots.add(13);
        } else if (count == 2) {
            slots.addAll(Arrays.asList(11, 15));
        } else if (count == 3) {
            slots.addAll(Arrays.asList(10, 13, 16));
        } else if (count == 4) {
            slots.addAll(Arrays.asList(10, 12, 14, 16));
        } else if (count <= 6) {
            slots.addAll(Arrays.asList(10, 11, 12, 13, 14, 15));
        } else if (count <= 7) {
            slots.addAll(Arrays.asList(10, 11, 12, 13, 14, 15, 16));
        } else {
            // For larger inventories, fill center rows
            for (int row = 1; row < rows - 1; row++) {
                for (int col = 1; col <= 7; col++) {
                    slots.add(row * 9 + col);
                    if (slots.size() >= count) return slots;
                }
            }
        }
        return slots;
    }

    private static ItemStack buildShopButton(Shop shop, Material icon) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(shop.getDisplayName());
            meta.setLore(Arrays.asList(
                "§7" + shop.getProducts().size() + " products available",
                "",
                "§aClick to open!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildBalanceItem(long credits) {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§lYour Balance");
            meta.setLore(Arrays.asList("§e" + credits + " credits"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    public static boolean isMainMenu(String title) { return title.startsWith(MENU_TAG); }
}
