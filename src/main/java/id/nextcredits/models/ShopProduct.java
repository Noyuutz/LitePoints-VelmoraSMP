package id.nextcredits.models;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopProduct {
    private final String id, displayName;
    private final Material material;
    private final int itemAmount, slot;
    private final long price;
    private final List<String> lore;
    private final String type; // "item", "command", "bundle"
    private final List<String> commands;
    private final int purchaseLimit; // -1 = unlimited
    private final List<BundleItem> bundleItems;
    private final int rankTier; // 0 = not a rank, 1-6 = rank tier

    public ShopProduct(String id, String displayName, Material material, int itemAmount,
                       long price, List<String> lore, int slot, String type,
                       List<String> commands, int purchaseLimit,
                       List<BundleItem> bundleItems, int rankTier) {
        this.id = id; this.displayName = displayName; this.material = material;
        this.itemAmount = itemAmount; this.price = price; this.lore = lore;
        this.slot = slot; this.type = type; this.commands = commands;
        this.purchaseLimit = purchaseLimit; this.bundleItems = bundleItems;
        this.rankTier = rankTier;
    }

    public ItemStack buildDisplayItem(int currentCount, boolean locked) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(colorize(displayName));
        List<String> full = new ArrayList<>();
        for (String l : lore) full.add(colorize(l));
        full.add("");
        full.add("§ePrice: §a" + price + " credits");

        if (isBundle()) {
            full.add("§eType: §fStarter Kit");
        } else if (isCommand()) {
            full.add("§eType: §fRank");
        } else {
            full.add("§eAmount: §f" + itemAmount + "x");
        }

        if (purchaseLimit > 0) {
            full.add("");
            if (locked) {
                full.add("§c§lSOLD OUT §7(" + currentCount + "/" + purchaseLimit + ")");
                full.add("§cYou've reached the purchase limit!");
            } else {
                full.add("§7Purchased: §e" + currentCount + "§7/§e" + purchaseLimit);
            }
        }

        if (!locked) full.add(""); full.add(locked ? "§c✘ Cannot purchase" : "§aClick to purchase!");

        meta.setLore(full);
        item.setItemMeta(meta);
        return item;
    }

    // Backwards compat
    public ItemStack buildDisplayItem() {
        return buildDisplayItem(0, false);
    }

    public List<ItemStack> buildBundleItems() {
        List<ItemStack> items = new ArrayList<>();
        if (bundleItems != null) {
            for (BundleItem bi : bundleItems) {
                items.add(bi.build());
            }
        }
        return items;
    }

    public ItemStack buildRewardItem() { return new ItemStack(material, itemAmount); }
    public boolean isCommand() { return "command".equalsIgnoreCase(type); }
    public boolean isBundle() { return "bundle".equalsIgnoreCase(type); }
    private String colorize(String t) { return t.replace("&", "§"); }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public long getPrice() { return price; }
    public int getItemAmount() { return itemAmount; }
    public int getSlot() { return slot; }
    public String getType() { return type; }
    public List<String> getCommands() { return commands; }
    public int getPurchaseLimit() { return purchaseLimit; }
    public List<BundleItem> getBundleItems() { return bundleItems; }
    public int getRankTier() { return rankTier; }

    // ── Inner class for bundle items ──
    public static class BundleItem {
        private final Material material;
        private final Map<Enchantment, Integer> enchants;

        public BundleItem(Material material, Map<Enchantment, Integer> enchants) {
            this.material = material;
            this.enchants = enchants;
        }

        public ItemStack build() {
            ItemStack item = new ItemStack(material, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
            }
            enchants.forEach((ench, lvl) -> item.addUnsafeEnchantment(ench, lvl));
            return item;
        }
    }
}
