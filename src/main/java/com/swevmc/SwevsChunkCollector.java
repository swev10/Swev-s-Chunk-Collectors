package com.swevmc;

import com.swevmc.commands.ChunkCollectorCommand;
import com.swevmc.listeners.ChunkCollectorListener;
import com.swevmc.items.CollectorItemFactory;
import com.swevmc.managers.ChunkCollectorManager;
import com.swevmc.managers.ConfigManager;
import com.swevmc.managers.EconomyManager;
import com.swevmc.managers.EconomyPriceManager;
import com.swevmc.managers.HologramManager;
import com.swevmc.utils.DataManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class SwevsChunkCollector extends JavaPlugin {

    private ConfigManager configManager;
    private ChunkCollectorManager collectorManager;
    private EconomyManager economyManager;
    private EconomyPriceManager economyPriceManager;
    private HologramManager hologramManager;
    private DataManager dataManager;
    private CollectorItemFactory collectorItemFactory;
    private Economy economy;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        if (!checkDependencies()) {
            getLogger().severe("Required dependencies not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!initializeManagers()) {
            return;
        }
        registerCommands();
        registerListeners();
        collectorManager.loadCollectors();

        getLogger().info("SwevsChunkCollector has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (collectorManager != null) {
            collectorManager.shutdown();
        }

        if (dataManager != null) {
            if (collectorManager != null) {
                dataManager.saveCollectorsBlocking(collectorManager.getCollectors());
            }
            dataManager.shutdown();
        }

        getLogger().info("SwevsChunkCollector has been disabled!");
    }

    private boolean checkDependencies() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault plugin not found!");
            return false;
        }

        String economyPlugin = getConfigManager().getEconomyPlugin();
        if ("SHOPGUIPLUS".equals(economyPlugin.toUpperCase())) {
            if (getServer().getPluginManager().getPlugin("ShopGUIPlus") == null) {
                getLogger().warning("ShopGUIPlus not found! Using fallback pricing.");
            } else {
                getLogger().info("ShopGUIPlus found and will be used for item pricing.");
            }
        }

        return true;
    }

    private boolean initializeManagers() {
        if (!setupEconomy()) {
            getLogger().severe("Failed to setup economy! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        economyManager = new EconomyManager(economy);
        economyPriceManager = new EconomyPriceManager(this);
        hologramManager = new HologramManager(this);
        if (!hologramManager.init()) {
            getLogger().severe("No supported hologram plugin found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }
        dataManager = new DataManager(this);
        collectorItemFactory = new CollectorItemFactory(this);
        collectorManager = new ChunkCollectorManager(this, dataManager, collectorItemFactory);
        return true;
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void registerCommands() {
        try {
            ChunkCollectorCommand chunkCollectorCommand = new ChunkCollectorCommand(this);
            getCommand("chunkcollector").setExecutor(chunkCollectorCommand);
            getCommand("chunkcollector").setTabCompleter(chunkCollectorCommand);
        } catch (Exception e) {
            getLogger().severe("Failed to register commands: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerListeners() {
        try {
            getServer().getPluginManager().registerEvents(new ChunkCollectorListener(this), this);
        } catch (Exception e) {
            getLogger().severe("Failed to register listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ChunkCollectorManager getChunkCollectorManager() {
        return collectorManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public EconomyPriceManager getEconomyPriceManager() {
        return economyPriceManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public CollectorItemFactory getCollectorItemFactory() {
        return collectorItemFactory;
    }
}
