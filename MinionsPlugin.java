package com.rolleco.minions;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MinionsPlugin extends JavaPlugin {

    private MinionsConfig minionsConfig;
    private SkriptBridge skriptBridge;
    private MinionItemFactory itemFactory;
    private PlacedMinionStore placedMinionStore;
    private MinionManager minionManager;
    private MerchantStockService stockService;
    private MerchantGUI merchantGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        minionsConfig = new MinionsConfig(getConfig());

        skriptBridge = new SkriptBridge(this);
        itemFactory = new MinionItemFactory(this);
        placedMinionStore = new PlacedMinionStore(this);
        placedMinionStore.load();
        minionManager = new MinionManager(this, minionsConfig, itemFactory, skriptBridge, placedMinionStore);
        stockService = new MerchantStockService(this, minionsConfig);
        stockService.load();
        merchantGUI = new MerchantGUI(minionsConfig, itemFactory);

        minionManager.cleanupOrphanedEntities();
        minionManager.loadAllFromStore();
        minionManager.startIncomeTask();

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlacementListener(this), this);
        getServer().getPluginManager().registerEvents(new PickupListener(this), this);

        PluginCommand merchantCommand = getCommand("merchant");
        if (merchantCommand != null) merchantCommand.setExecutor(new MerchantCommand(this));

        PluginCommand reloadCommand = getCommand("minionsreload");
        if (reloadCommand != null) reloadCommand.setExecutor((sender, cmd, label, args) -> {
            reloadConfig();
            minionsConfig.reload(getConfig());
            sender.sendMessage(Util.color("&aMinions config reloaded."));
            return true;
        });

        if (!skriptBridge.isAvailable()) {
            getLogger().warning("[Minions] Skript was not detected at startup. Make sure Skript is installed and loads BEFORE "
                    + "this plugin (load order in plugin.yml/soft-depend), or Credits/Money will not sync correctly.");
        }

        getLogger().info("[Minions] Enabled with " + minionsConfig.minionTypes.size() + " minion types.");
    }

    @Override
    public void onDisable() {
        if (minionManager != null) minionManager.despawnAllAndSave();
        if (stockService != null) stockService.save();
    }

    public MinionsConfig getMinionsConfig() { return minionsConfig; }
    public SkriptBridge getSkriptBridge() { return skriptBridge; }
    public MinionItemFactory getItemFactory() { return itemFactory; }
    public MinionManager getMinionManager() { return minionManager; }
    public MerchantStockService getStockService() { return stockService; }
    public MerchantGUI getMerchantGUI() { return merchantGUI; }
}
