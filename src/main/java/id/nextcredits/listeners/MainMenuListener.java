package id.nextcredits.listeners;

import id.nextcredits.NextCredits;
import id.nextcredits.gui.MainMenuGUI;
import id.nextcredits.gui.ShopGUI;
import id.nextcredits.models.Shop;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MainMenuListener implements Listener {

    private final NextCredits plugin;

    public MainMenuListener(NextCredits plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        if (!MainMenuGUI.isMainMenu(title)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String displayName = meta.getDisplayName();

        // Find shop by display name
        for (Shop shop : plugin.getShopManager().getAllShops().values()) {
            if (shop.getDisplayName().equals(displayName)) {
                ShopGUI.open(player, shop);
                return;
            }
        }
    }
}
