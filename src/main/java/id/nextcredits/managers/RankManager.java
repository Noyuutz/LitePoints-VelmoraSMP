package id.nextcredits.managers;

import id.nextcredits.NextCredits;
import id.nextcredits.models.ShopProduct;
import id.nextcredits.models.Shop;

import java.util.UUID;

public class RankManager {

    private final NextCredits plugin;

    // Rank tiers in order
    private static final int MAX_TIER = 6;

    public RankManager(NextCredits plugin) {
        this.plugin = plugin;
    }

    // Get the highest rank tier the player currently owns
    public int getPlayerRankTier(UUID uuid, Shop rankShop) {
        int highestTier = 0;
        for (ShopProduct product : rankShop.getProducts()) {
            if (product.getRankTier() > 0) {
                int count = plugin.getDatabaseManager().getPurchaseCount(uuid, rankShop.getId(), product.getId());
                if (count > 0 && product.getRankTier() > highestTier) {
                    highestTier = product.getRankTier();
                }
            }
        }
        return highestTier;
    }

    // Get the product of the player's current rank
    public ShopProduct getPlayerCurrentRank(UUID uuid, Shop rankShop) {
        int highestTier = 0;
        ShopProduct currentRank = null;
        for (ShopProduct product : rankShop.getProducts()) {
            if (product.getRankTier() > 0) {
                int count = plugin.getDatabaseManager().getPurchaseCount(uuid, rankShop.getId(), product.getId());
                if (count > 0 && product.getRankTier() > highestTier) {
                    highestTier = product.getRankTier();
                    currentRank = product;
                }
            }
        }
        return currentRank;
    }

    // Calculate actual price to pay (with upgrade discount)
    public long calculateUpgradePrice(UUID uuid, Shop rankShop, ShopProduct targetRank) {
        ShopProduct currentRank = getPlayerCurrentRank(uuid, rankShop);
        if (currentRank == null) return targetRank.getPrice(); // No rank yet, full price
        return Math.max(0, targetRank.getPrice() - currentRank.getPrice()); // Pay the difference
    }

    // Check if player can buy this rank
    public RankPurchaseResult canBuyRank(UUID uuid, Shop rankShop, ShopProduct targetRank) {
        int playerTier = getPlayerRankTier(uuid, rankShop);
        int targetTier = targetRank.getRankTier();

        if (playerTier >= targetTier) {
            return RankPurchaseResult.ALREADY_OWNED;
        }
        return RankPurchaseResult.CAN_BUY;
    }

    public enum RankPurchaseResult {
        CAN_BUY,
        ALREADY_OWNED
    }
}
