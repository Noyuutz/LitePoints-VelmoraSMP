package id.nextcredits.models;

import java.util.List;

public class Shop {

    private final String id;
    private final String displayName;
    private final int rows;
    private final List<ShopProduct> products;

    public Shop(String id, String displayName, int rows, List<ShopProduct> products) {
        this.id = id;
        this.displayName = displayName;
        this.rows = rows;
        this.products = products;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName.replace("&", "§"); }
    public int getRows() { return Math.max(1, Math.min(rows, 6)); }
    public int getSize() { return getRows() * 9; }
    public List<ShopProduct> getProducts() { return products; }

    public ShopProduct getProductBySlot(int slot) {
        return products.stream().filter(p -> p.getSlot() == slot).findFirst().orElse(null);
    }
}
