package com.swevmc.storage;

import com.swevmc.SwevsChunkCollector;
import com.swevmc.models.ChunkCollector;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileStorage implements StorageInterface {

    private final SwevsChunkCollector plugin;
    private final File dataFile;
    private YamlConfiguration config;

    public FileStorage(SwevsChunkCollector plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "collectors.yml");
    }

    @Override
    public boolean initialize() {
        try {
            if (!dataFile.exists()) {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            }
            config = YamlConfiguration.loadConfiguration(dataFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to initialize file storage: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void shutdown() {
    }

    @Override
    public synchronized boolean saveCollectors(List<CollectorSnapshot> collectors) {
        try {
            config.set("collectors", null);

            for (int i = 0; i < collectors.size(); i++) {
                CollectorSnapshot collector = collectors.get(i);
                String path = "collectors." + i;

                config.set(path + ".uuid", collector.uuid().toString());
                config.set(path + ".ownerUuid", collector.ownerUuid().toString());
                config.set(path + ".ownerName", collector.ownerName());
                config.set(path + ".world", collector.world());
                config.set(path + ".x", collector.x());
                config.set(path + ".y", collector.y());
                config.set(path + ".z", collector.z());
                config.set(path + ".yaw", collector.yaw());
                config.set(path + ".pitch", collector.pitch());
                config.set(path + ".createdAt", collector.createdAt());
                config.set(path + ".timeRemaining", collector.timeRemaining());
                config.set(path + ".itemsCollected", collector.itemsCollected());
                config.set(path + ".active", collector.active());
                config.set(path + ".maxChargeTime", collector.maxChargeTime());
                config.set(path + ".totalMoneyEarned", collector.totalMoneyEarned());
                config.set(path + ".lastAutosellTime", collector.lastAutosellTime());
                config.set(path + ".virtualItems", collector.virtualItems());
            }

            config.save(dataFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save collectors to file: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<ChunkCollector> loadCollectors() {
        try {
            List<ChunkCollector> collectors = new ArrayList<>();

            if (config.contains("collectors")) {
                int index = 0;
                while (config.contains("collectors." + index + ".uuid")) {
                    String path = "collectors." + index;

                    try {
                        UUID uuid = UUID.fromString(config.getString(path + ".uuid"));
                        UUID ownerUuid = UUID.fromString(config.getString(path + ".ownerUuid"));
                        String ownerName = config.getString(path + ".ownerName");

                        String worldName = config.getString(path + ".world");
                        double x = config.getDouble(path + ".x");
                        double y = config.getDouble(path + ".y");
                        double z = config.getDouble(path + ".z");
                        float yaw = (float) config.getDouble(path + ".yaw");
                        float pitch = (float) config.getDouble(path + ".pitch");

                        org.bukkit.World world = plugin.getServer().getWorld(worldName);
                        if (world == null) {
                            plugin.getLogger().warning("World not found: " + worldName + " for collector " + uuid);
                            index++;
                            continue;
                        }

                        org.bukkit.Location location = new org.bukkit.Location(world, x, y, z, yaw, pitch);
                        long createdAt = config.getLong(path + ".createdAt");
                        long timeRemaining = config.getLong(path + ".timeRemaining");
                        int itemsCollected = config.getInt(path + ".itemsCollected");
                        boolean active = config.getBoolean(path + ".active");
                        long maxChargeTime = config.getLong(path + ".maxChargeTime", 0);
                        double totalMoneyEarned = config.getDouble(path + ".totalMoneyEarned", 0.0);
                        long lastAutosellTime = config.getLong(path + ".lastAutosellTime", 0);

                        ChunkCollector collector = new ChunkCollector(plugin, uuid, ownerUuid, ownerName, location,
                                createdAt, timeRemaining, itemsCollected, active);
                        collector.setMaxChargeTime(maxChargeTime);
                        collector.setTotalMoneyEarned(totalMoneyEarned);
                        collector.setLastAutosellTime(lastAutosellTime);
                        java.util.Map<org.bukkit.Material, Integer> virtualItems = new java.util.HashMap<>();
                        org.bukkit.configuration.ConfigurationSection itemSection = config
                                .getConfigurationSection(path + ".virtualItems");
                        if (itemSection != null) {
                            for (String materialName : itemSection.getKeys(false)) {
                                org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialName);
                                int amount = itemSection.getInt(materialName);
                                if (material != null && amount > 0) {
                                    virtualItems.put(material, amount);
                                }
                            }
                        }
                        collector.setVirtualItems(virtualItems);

                        collectors.add(collector);
                    } catch (Exception e) {
                        plugin.getLogger()
                                .warning("Failed to load collector at index " + index + ": " + e.getMessage());
                    }

                    index++;
                }
            }

            return collectors;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load collectors from file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isConnected() {
        return dataFile.exists() && config != null;
    }
}
