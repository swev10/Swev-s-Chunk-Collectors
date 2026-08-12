package com.swevmc.managers;

import com.swevmc.SwevsChunkCollector;
import com.swevmc.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.stream.Collectors;

public class ConfigManager {

    private final SwevsChunkCollector plugin;
    private FileConfiguration config;
    private Set<Material> collectibleItems = Collections.emptySet();

    public ConfigManager(SwevsChunkCollector plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadCollectibleItems();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadCollectibleItems();
    }

    public String getMessage(String key) {
        return ColorUtils.translateColors(config.getString("messages." + key, "&cMessage not found: " + key));
    }

    public String getMessage(String key, String... placeholders) {
        String message = getMessage(key);
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }

    public String getPrefix() {
        return getMessage("prefix");
    }

    public int getCollectionSpeed() {
        return Math.max(1, config.getInt("settings.collection-speed", 20));
    }

    public int getCollectionEffectCooldown() {
        return config.getInt("settings.collection-effect-cooldown", 100);
    }

    public int getHappyVillagerParticleFrequency() {
        return config.getInt("settings.happy-villager-particle-frequency", 60);
    }

    public int getMaxCollectionHeight() {
        return config.getInt("settings.max-collection-height", 319);
    }

    public int getMinCollectionHeight() {
        return config.getInt("settings.min-collection-height", -64);
    }

    public int getMaxCollectorsPerPlayer() {
        return config.getInt("settings.max-collectors-per-player", 10);
    }

    public int getDefaultChargeMinutes() {
        return config.getInt("settings.default-charge-minutes", 60);
    }

    public double getRechargeCostPerMinute() {
        return config.getDouble("settings.recharge-cost-per-minute", 100.0);
    }

    public int getAutosellInterval() {
        return config.getInt("settings.autosell-interval", 60);
    }

    public Set<Material> getCollectibleItems() {
        return collectibleItems;
    }

    private void loadCollectibleItems() {
        List<String> itemNames = config.getStringList("collectible-items");
        collectibleItems = Collections.unmodifiableSet(itemNames.stream()
                .map(String::toUpperCase)
                .map(name -> {
                    try {
                        return Material.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material in config: " + name);
                        return null;
                    }
                })
                .filter(material -> material != null)
                .collect(Collectors.toSet()));
    }

    public List<String> getHologramLines() {
        return config.getStringList("hologram.lines");
    }

    public double getHologramHeightOffset() {
        return config.getDouble("hologram.height-offset", 2.0);
    }

    public int getHologramUpdateInterval() {
        return config.getInt("hologram.update-interval", 20);
    }

    public String getSound(String key) {
        return config.getString("sounds." + key, "BLOCK_ANVIL_PLACE");
    }

    public String getEffect(String key) {
        return config.getString("effects." + key, "VILLAGER_HAPPY");
    }

    public String getGuiTitle() {
        return ColorUtils.translateColors(config.getString("messages.gui-title", "&6Chunk Collector"));
    }

    public String getGuiRechargeCost() {
        return ColorUtils
                .translateColors(config.getString("messages.gui-recharge-cost", "&7Recharge Cost: &a$${cost}"));
    }

    public String getGuiTimeRemaining() {
        return ColorUtils
                .translateColors(config.getString("messages.gui-time-remaining", "&7Time Remaining: &e${time}"));
    }

    public String getGuiItemsCollected() {
        return ColorUtils
                .translateColors(config.getString("messages.gui-items-collected", "&7Items Collected: &a${amount}"));
    }

    public String getGuiMoneyEarned() {
        return ColorUtils
                .translateColors(config.getString("messages.gui-money-earned", "&7Money Earned: &a$${amount}"));
    }

    public String getGuiRechargeButton() {
        return ColorUtils.translateColors(config.getString("messages.gui-recharge-button", "&aRecharge Collector"));
    }

    public String getGuiCollectorInfo() {
        return ColorUtils.translateColors(config.getString("messages.gui-collector-info", "&6Chunk Collector Info"));
    }

    public String getStorageType() {
        return config.getString("storage.type", "FILE").toUpperCase();
    }

    public String getMySQLHost() {
        return config.getString("storage.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("storage.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("storage.mysql.database", "chunkcollectors");
    }

    public String getMySQLUsername() {
        return config.getString("storage.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return config.getString("storage.mysql.password", "");
    }

    public boolean getMySQLUseSSL() {
        return config.getBoolean("storage.mysql.use-ssl", false);
    }

    public String getRedisHost() {
        return config.getString("storage.redis.host", "localhost");
    }

    public int getRedisPort() {
        return config.getInt("storage.redis.port", 6379);
    }

    public String getRedisPassword() {
        return config.getString("storage.redis.password", "");
    }

    public int getRedisDatabase() {
        return config.getInt("storage.redis.database", 0);
    }

    public boolean getRedisUseSSL() {
        return config.getBoolean("storage.redis.use-ssl", false);
    }

    public String getBlockType() {
        return config.getString("block.type", "BEACON").toUpperCase();
    }

    public String getEconomyPlugin() {
        return config.getString("economy.plugin", "SHOPGUIPLUS");
    }

    public double getFallbackPrice() {
        return config.getDouble("economy.fallback-price", 1.0);
    }

    public double getPriceMultiplier() {
        return config.getDouble("economy.price-multiplier", 1.0);
    }

    public double getCustomPrice(Material material) {
        return config.getDouble("economy.prices." + material.name(), getFallbackPrice());
    }

    public String getHologramProvider() {
        return config.getString("settings.hologram-provider", "AUTO");
    }

    public String getHologramBackground() {
        return config.getString("hologram.background", "TRANSPARENT");
    }
}
