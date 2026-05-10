package id.nextcredits.listeners;

import id.nextcredits.NextCredits;
import id.nextcredits.gui.ShopEditorGUI;
import id.nextcredits.gui.ShopGUI;
import id.nextcredits.models.Shop;
import id.nextcredits.models.ShopProduct;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ShopEditorListener implements Listener {

    private final NextCredits plugin;

    // Players waiting to type custom price: UUID -> [shopId, slotIndex, itemStack]
    private final Map<UUID, Object[]> awaitingPriceInput = new HashMap<>();

    // Track which shop each player is currently editing: UUID -> shopId
    private final Map<UUID, String> editingShop = new HashMap<>();

    // Track which slot is being price-edited: UUID -> slot
    private final Map<UUID, Integer> editingSlot = new HashMap<>();

    public ShopEditorListener(NextCredits plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // ── Shop Selector Editor ──
        if (ShopEditorGUI.isShopSelectorEditorInventory(title)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta()) return;
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            String name = meta.getDisplayName();

            // Create new shop button
            if (name.equals("§a§l+ Create New Shop")) {
                player.closeInventory();
                player.sendMessage(colorize("&6[Editor] &7Type the shop ID in chat (e.g. &emyshop&7):"));
                awaitingPriceInput.put(player.getUniqueId(), new Object[]{"CREATE_SHOP_ID"});
                return;
            }

            // Open existing shop editor
            List<String> lore = meta.getLore();
            if (lore == null) return;
            String shopId = null;
            for (String line : lore) {
                if (line.startsWith("§8ID: §7")) { shopId = line.replace("§8ID: §7", "").trim(); break; }
            }
            if (shopId == null) return;
            Shop shop = plugin.getShopManager().getShop(shopId);
            if (shop == null) return;
            editingShop.put(player.getUniqueId(), shopId);
            ShopEditorGUI.openEditor(player, shop);
        }

        // ── Main Shop Editor ──
        else if (ShopEditorGUI.isEditorInventory(title)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            int slot = event.getSlot();
            String shopId = editingShop.get(player.getUniqueId());
            if (shopId == null) return;
            Shop shop = plugin.getShopManager().getShop(shopId);
            if (shop == null) return;
            int size = shop.getSize();

            // Control buttons (bottom bar)
            if (slot == size - 1) { // Back
                ShopEditorGUI.openShopSelector(player);
                return;
            }
            if (slot == size - 9) { // Clear all
                plugin.getShopEditorManager().clearShop(shopId);
                player.sendMessage(colorize("&6[Editor] &cCleared all products from &e" + shop.getDisplayName()));
                Shop refreshed = plugin.getShopManager().getShop(shopId);
                if (refreshed != null) ShopEditorGUI.openEditor(player, refreshed);
                return;
            }

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            boolean isEmpty = clicked.getType().name().contains("GLASS_PANE");
            ShopProduct existingProduct = shop.getProductBySlot(slot);

            // LEFT CLICK on empty slot → add item from hand
            if (event.getClick() == ClickType.LEFT && isEmpty) {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == org.bukkit.Material.AIR) {
                    player.sendMessage(colorize("&cHold an item in your hand first!"));
                    return;
                }
                // Open price editor
                editingSlot.put(player.getUniqueId(), slot);
                ShopEditorGUI.openSlotEditor(player, shop, slot, hand.clone(), 100);
            }

            // LEFT CLICK on existing product → edit price
            else if (event.getClick() == ClickType.LEFT && existingProduct != null) {
                editingSlot.put(player.getUniqueId(), slot);
                ItemStack displayItem = new ItemStack(existingProduct.getMaterial(), existingProduct.getItemAmount());
                ShopEditorGUI.openSlotEditor(player, shop, slot, displayItem, existingProduct.getPrice());
            }

            // RIGHT CLICK on existing product → remove
            else if (event.getClick() == ClickType.RIGHT && existingProduct != null) {
                plugin.getShopEditorManager().removeProduct(shopId, slot);
                player.sendMessage(colorize("&6[Editor] &cRemoved product from slot &e" + slot));
                Shop refreshed = plugin.getShopManager().getShop(shopId);
                if (refreshed != null) ShopEditorGUI.openEditor(player, refreshed);
            }
        }

        // ── Slot Price Editor ──
        else if (ShopEditorGUI.isSlotEditorInventory(title)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null) return;
            if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

            int clickedSlot = event.getSlot();
            String shopId = editingShop.get(player.getUniqueId());
            Integer editSlot = editingSlot.get(player.getUniqueId());
            if (shopId == null || editSlot == null) return;

            Shop shop = plugin.getShopManager().getShop(shopId);
            if (shop == null) return;

            // Back button
            if (clickedSlot == 18) {
                ShopEditorGUI.openEditor(player, shop);
                return;
            }

            // Custom price button
            if (clickedSlot == 22) {
                player.closeInventory();
                player.sendMessage(colorize("&6[Editor] &7Type the price in chat (numbers only):"));
                ItemStack hand = player.getInventory().getItemInMainHand();
                ShopProduct existing = shop.getProductBySlot(editSlot);
                ItemStack itemToSave = existing != null
                    ? new ItemStack(existing.getMaterial(), existing.getItemAmount())
                    : (hand != null ? hand.clone() : new ItemStack(org.bukkit.Material.STONE));
                awaitingPriceInput.put(player.getUniqueId(), new Object[]{"CUSTOM_PRICE", shopId, editSlot, itemToSave});
                return;
            }

            // Preset price buttons (slots 0-8)
            if (clickedSlot >= 0 && clickedSlot <= 8) {
                ItemStack priceItem = event.getCurrentItem();
                if (priceItem == null || !priceItem.hasItemMeta()) return;
                String displayName = priceItem.getItemMeta().getDisplayName();
                // Parse price from name "§e§l100 Credits"
                try {
                    String priceStr = displayName.replaceAll("§.", "").replace(" Credits", "").trim();
                    long price = Long.parseLong(priceStr);

                    ShopProduct existing = shop.getProductBySlot(editSlot);
                    boolean success;
                    if (existing != null) {
                        success = plugin.getShopEditorManager().updatePrice(shopId, editSlot, price);
                    } else {
                        ItemStack hand = player.getInventory().getItemInMainHand();
                        if (hand == null || hand.getType() == org.bukkit.Material.AIR) {
                            player.sendMessage(colorize("&cHold the item in your hand!"));
                            return;
                        }
                        success = plugin.getShopEditorManager().addProduct(shopId, editSlot, hand.clone(), price);
                    }

                    if (success) {
                        player.sendMessage(colorize("&6[Editor] &aProduct saved! Slot &e" + editSlot + " &aprice set to &e" + price + " credits"));
                        Shop refreshed = plugin.getShopManager().getShop(shopId);
                        if (refreshed != null) ShopEditorGUI.openEditor(player, refreshed);
                    } else {
                        player.sendMessage(colorize("&cFailed to save product!"));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(colorize("&cError parsing price!"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!awaitingPriceInput.containsKey(uuid)) return;
        event.setCancelled(true);

        Object[] data = awaitingPriceInput.remove(uuid);
        String inputType = (String) data[0];
        String input = event.getMessage().trim();

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(colorize("&cCancelled."));
            return;
        }

        // CREATE_SHOP_ID
        if (inputType.equals("CREATE_SHOP_ID")) {
            String shopId = input.toLowerCase().replaceAll("[^a-z0-9_]", "");
            if (shopId.isEmpty()) { player.sendMessage(colorize("&cInvalid shop ID!")); return; }
            player.sendMessage(colorize("&6[Editor] &7Now type the display name for the shop:"));
            awaitingPriceInput.put(uuid, new Object[]{"CREATE_SHOP_NAME", shopId});
            return;
        }

        // CREATE_SHOP_NAME
        if (inputType.equals("CREATE_SHOP_NAME")) {
            String shopId = (String) data[1];
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                boolean success = plugin.getShopEditorManager().createShop(shopId, input, 4);
                if (success) {
                    player.sendMessage(colorize("&6[Editor] &aShop &e" + shopId + " &acreated!"));
                    ShopEditorGUI.openShopSelector(player);
                } else {
                    player.sendMessage(colorize("&cShop ID already exists!"));
                }
            });
            return;
        }

        // CUSTOM_PRICE
        if (inputType.equals("CUSTOM_PRICE")) {
            String shopId = (String) data[1];
            int editSlot = (int) data[2];
            ItemStack item = (ItemStack) data[3];

            try {
                long price = Long.parseLong(input);
                if (price <= 0) { player.sendMessage(colorize("&cPrice must be positive!")); return; }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Shop shop = plugin.getShopManager().getShop(shopId);
                    ShopProduct existing = shop != null ? shop.getProductBySlot(editSlot) : null;
                    boolean success;
                    if (existing != null) {
                        success = plugin.getShopEditorManager().updatePrice(shopId, editSlot, price);
                    } else {
                        success = plugin.getShopEditorManager().addProduct(shopId, editSlot, item, price);
                    }
                    if (success) {
                        player.sendMessage(colorize("&6[Editor] &aProduct saved with price &e" + price + " credits!"));
                        Shop refreshed = plugin.getShopManager().getShop(shopId);
                        if (refreshed != null) {
                            editingShop.put(uuid, shopId);
                            ShopEditorGUI.openEditor(player, refreshed);
                        }
                    } else {
                        player.sendMessage(colorize("&cFailed to save!"));
                    }
                });
            } catch (NumberFormatException e) {
                player.sendMessage(colorize("&cInvalid number! Type a valid price."));
            }
        }
    }

    private String colorize(String t) { return t.replace("&", "§"); }

    public Map<UUID, String> getEditingShop() { return editingShop; }
}
