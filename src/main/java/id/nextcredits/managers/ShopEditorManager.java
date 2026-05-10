package id.nextcredits.managers;

import id.nextcredits.NextCredits;
import id.nextcredits.models.Shop;
import id.nextcredits.models.ShopProduct;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ShopEditorManager {

    private final NextCredits plugin;

    public ShopEditorManager(NextCredits plugin) {
        this.plugin = plugin;
    }

    // Add or update a product in a shop
    public boolean addProduct(String shopId, int slot, ItemStack item, long price) {
        Shop shop = plugin.getShopManager().getShop(shopId);
        if (shop == null) return false;

        File shopFile = new File(plugin.getDataFolder(), "shops/" + shopId + ".yml");
        if (!shopFile.exists()) return false;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);

        String itemName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
            ? item.getItemMeta().getDisplayName()
            : "&f" + formatMaterialName(item.getType().name());

        String productId = "item_slot_" + slot;

        cfg.set("items." + productId + ".material", item.getType().name());
        cfg.set("items." + productId + ".name", itemName);
        cfg.set("items." + productId + ".amount", item.getAmount());
        cfg.set("items." + productId + ".price", price);
        cfg.set("items." + productId + ".slot", slot);

        List<String> lore = new ArrayList<>();
        if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
            lore = item.getItemMeta().getLore();
        }
        cfg.set("items." + productId + ".lore", lore);

        try {
            cfg.save(shopFile);
            plugin.getShopManager().loadShops(); // Reload shops
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving shop: " + e.getMessage());
            return false;
        }
    }

    // Remove a product from a shop by slot
    public boolean removeProduct(String shopId, int slot) {
        File shopFile = new File(plugin.getDataFolder(), "shops/" + shopId + ".yml");
        if (!shopFile.exists()) return false;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);

        String targetKey = null;
        if (cfg.contains("items")) {
            for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
                if (cfg.getInt("items." + key + ".slot") == slot) {
                    targetKey = key;
                    break;
                }
            }
        }

        if (targetKey == null) return false;

        cfg.set("items." + targetKey, null);

        try {
            cfg.save(shopFile);
            plugin.getShopManager().loadShops();
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving shop: " + e.getMessage());
            return false;
        }
    }

    // Update price of existing product
    public boolean updatePrice(String shopId, int slot, long newPrice) {
        File shopFile = new File(plugin.getDataFolder(), "shops/" + shopId + ".yml");
        if (!shopFile.exists()) return false;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);

        if (cfg.contains("items")) {
            for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
                if (cfg.getInt("items." + key + ".slot") == slot) {
                    cfg.set("items." + key + ".price", newPrice);
                    try {
                        cfg.save(shopFile);
                        plugin.getShopManager().loadShops();
                        return true;
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.WARNING, "Error saving shop: " + e.getMessage());
                        return false;
                    }
                }
            }
        }
        return false;
    }

    // Clear all products from a shop
    public boolean clearShop(String shopId) {
        File shopFile = new File(plugin.getDataFolder(), "shops/" + shopId + ".yml");
        if (!shopFile.exists()) return false;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(shopFile);
        cfg.set("items", null);

        try {
            cfg.save(shopFile);
            plugin.getShopManager().loadShops();
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error clearing shop: " + e.getMessage());
            return false;
        }
    }

    // Create a new shop file
    public boolean createShop(String shopId, String displayName, int rows) {
        File shopFile = new File(plugin.getDataFolder(), "shops/" + shopId + ".yml");
        if (shopFile.exists()) return false;

        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("display-name", displayName);
        cfg.set("rows", rows);

        try {
            cfg.save(shopFile);
            plugin.getShopManager().loadShops();
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error creating shop: " + e.getMessage());
            return false;
        }
    }

    // Get shop ID from display name
    public String getShopIdByDisplayName(String displayName) {
        for (Shop shop : plugin.getShopManager().getAllShops().values()) {
            if (shop.getDisplayName().equals(displayName)) return shop.getId();
        }
        return null;
    }

    private String formatMaterialName(String material) {
        String[] words = material.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }
}
