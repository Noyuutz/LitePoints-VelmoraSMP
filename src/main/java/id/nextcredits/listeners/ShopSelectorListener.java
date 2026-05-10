package id.nextcredits.listeners;

import id.nextcredits.NextCredits;
import id.nextcredits.gui.ShopGUI;
import id.nextcredits.gui.ShopSelectorGUI;
import id.nextcredits.models.Shop;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ShopSelectorListener implements Listener {

    private final NextCredits plugin;

    public ShopSelectorListener(NextCredits plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!ShopSelectorGUI.isSelectorInventory(title)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasLore()) return;

        // Extract shop ID from lore line "§8ID: §7<id>"
        List<String> lore = meta.getLore();
        if (lore == null) return;

        String shopId = null;
        for (String line : lore) {
            if (line.startsWith("§8ID: §7")) {
                shopId = line.replace("§8ID: §7", "").trim();
                break;
            }
        }

        if (shopId == null) return;

        Shop shop = plugin.getShopManager().getShop(shopId);
        if (shop == null) {
            player.sendMessage("§cShop not found!");
            return;
        }

        ShopGUI.open(player, shop);
    }
}
