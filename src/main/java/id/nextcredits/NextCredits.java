package id.nextcredits;

import id.nextcredits.commands.CreditsCommand;
import id.nextcredits.commands.ShopCommand;
import id.nextcredits.database.DatabaseManager;
import id.nextcredits.hooks.PlaceholderAPIHook;
import id.nextcredits.listeners.*;
import id.nextcredits.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class NextCredits extends JavaPlugin {
    private static NextCredits instance;
    private DatabaseManager databaseManager;
    private CreditsManager creditsManager;
    private ShopManager shopManager;
    private RankManager rankManager;
    private ShopEditorManager shopEditorManager;
    private ShopEditorListener shopEditorListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Failed to connect to MySQL! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        databaseManager.createTables();

        creditsManager = new CreditsManager(this);
        shopManager = new ShopManager(this);
        rankManager = new RankManager(this);
        shopEditorManager = new ShopEditorManager(this);
        shopEditorListener = new ShopEditorListener(this);

        CreditsCommand creditsCmd = new CreditsCommand(this);
        ShopCommand shopCmd = new ShopCommand(this);
        getCommand("credits").setExecutor(creditsCmd);
        getCommand("credits").setTabCompleter(creditsCmd);
        getCommand("creditshop").setExecutor(shopCmd);

        getServer().getPluginManager().registerEvents(new MainMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopSelectorListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(shopEditorListener, this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIHook(this).register();
            getLogger().info("Hooked into PlaceholderAPI!");
        }

        getLogger().info("LitePoints v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("LitePoints disabled!");
    }

    public static NextCredits getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public CreditsManager getCreditsManager() { return creditsManager; }
    public ShopManager getShopManager() { return shopManager; }
    public RankManager getRankManager() { return rankManager; }
    public ShopEditorManager getShopEditorManager() { return shopEditorManager; }
    public ShopEditorListener getShopEditorListener() { return shopEditorListener; }
}
