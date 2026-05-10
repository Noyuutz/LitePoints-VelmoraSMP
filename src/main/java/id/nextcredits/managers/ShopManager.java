package id.nextcredits.managers;

import id.nextcredits.NextCredits;
import id.nextcredits.models.Shop;
import id.nextcredits.models.ShopProduct;
import id.nextcredits.models.ShopProduct.BundleItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class ShopManager {
    private final NextCredits plugin;
    private final Map<String, Shop> shops = new LinkedHashMap<>();

    public ShopManager(NextCredits plugin) { this.plugin = plugin; loadShops(); }

    public void loadShops() {
        shops.clear();
        File shopsFolder = new File(plugin.getDataFolder(), "shops");
        if (!shopsFolder.exists()) {
            shopsFolder.mkdirs();
            plugin.saveResource("shops/main.yml", false);
            plugin.saveResource("shops/ranks.yml", false);
            plugin.saveResource("shops/kits.yml", false);
        }
        File[] files = shopsFolder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
                String id = file.getName().replace(".yml", "");
                List<ShopProduct> products = new ArrayList<>();
                ConfigurationSection itemsSection = cfg.getConfigurationSection("items");

                if (itemsSection != null) {
                    for (String key : itemsSection.getKeys(false)) {
                        ConfigurationSection item = itemsSection.getConfigurationSection(key);
                        if (item == null) continue;

                        Material mat = Material.getMaterial(item.getString("material", "STONE").toUpperCase());
                        if (mat == null) mat = Material.STONE;

                        String type = item.getString("type", "item");
                        List<String> commands = item.getStringList("commands");
                        int purchaseLimit = item.getInt("purchase-limit", -1);
                        int rankTier = item.getInt("rank-tier", 0);

                        // Parse bundle items
                        List<BundleItem> bundleItems = new ArrayList<>();
                        ConfigurationSection bundleSection = item.getConfigurationSection("bundle-items");
                        if (bundleSection != null) {
                            for (String bKey : bundleSection.getKeys(false)) {
                                ConfigurationSection bItem = bundleSection.getConfigurationSection(bKey);
                                if (bItem == null) continue;
                                Material bMat = Material.getMaterial(bItem.getString("material", "STONE").toUpperCase());
                                if (bMat == null) continue;

                                Map<Enchantment, Integer> enchants = new HashMap<>();
                                ConfigurationSection enchSection = bItem.getConfigurationSection("enchants");
                                if (enchSection != null) {
                                    for (String enchKey : enchSection.getKeys(false)) {
                                        Enchantment ench = Enchantment.getByName(enchKey.toUpperCase());
                                        if (ench != null) enchants.put(ench, enchSection.getInt(enchKey));
                                    }
                                }
                                bundleItems.add(new BundleItem(bMat, enchants));
                            }
                        }

                        products.add(new ShopProduct(
                            key, item.getString("name", key), mat,
                            item.getInt("amount", 1), item.getLong("price", 100),
                            item.getStringList("lore"), item.getInt("slot", 0),
                            type, commands, purchaseLimit, bundleItems, rankTier
                        ));
                    }
                }
                shops.put(id, new Shop(id, cfg.getString("display-name", id), cfg.getInt("rows", 6), products));
                plugin.getLogger().info("Loaded shop: " + id + " (" + products.size() + " products)");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error loading shop: " + file.getName(), e);
            }
        }
        plugin.getLogger().info("Loaded " + shops.size() + " shop(s).");
    }

    public Shop getShop(String id) { return shops.get(id.toLowerCase()); }
    public Map<String, Shop> getAllShops() { return Collections.unmodifiableMap(shops); }
    public boolean shopExists(String id) { return shops.containsKey(id.toLowerCase()); }

    // Get all rank product IDs with tier lower than given tier
    public List<String> getLowerRankIds(Shop shop, int tier) {
        List<String> lower = new ArrayList<>();
        for (ShopProduct p : shop.getProducts()) {
            if (p.getRankTier() > 0 && p.getRankTier() < tier) {
                lower.add(p.getId());
            }
        }
        return lower;
    }
}
