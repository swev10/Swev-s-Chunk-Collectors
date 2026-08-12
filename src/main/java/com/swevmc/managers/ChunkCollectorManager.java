package com.swevmc.managers;

import com.swevmc.SwevsChunkCollector;
import com.swevmc.models.ChunkCollector;
import com.swevmc.utils.DataManager;
import com.swevmc.items.CollectorItemFactory;
import com.swevmc.utils.BukkitTypes;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkCollectorManager {

    private final SwevsChunkCollector plugin;
    private final DataManager dataManager;
    private final CollectorItemFactory itemFactory;
    private final Map<UUID, ChunkCollector> collectors;
    private final Map<String, UUID> chunkCollectors;
    private final Map<String, UUID> locationCollectors;
    private final Map<UUID, Integer> playerCollectorCount;
    private final NamespacedKey playerDropKey;
    private BukkitTask collectionTask;
    private BukkitTask tickTask;
    private BukkitTask autosaveTask;
    private int particleTickCounter = 0;

    public ChunkCollectorManager(SwevsChunkCollector plugin, DataManager dataManager, CollectorItemFactory itemFactory) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.itemFactory = itemFactory;
        this.collectors = new ConcurrentHashMap<>();
        this.chunkCollectors = new ConcurrentHashMap<>();
        this.locationCollectors = new ConcurrentHashMap<>();
        this.playerCollectorCount = new ConcurrentHashMap<>();
        this.playerDropKey = new NamespacedKey(plugin, "player_dropped_item");
    }

    public void startTasks() {
        collectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                collectItems();
            }
        }.runTaskTimer(plugin, 0, plugin.getConfigManager().getCollectionSpeed());

        tickTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickCollectors();
            }
        }.runTaskTimer(plugin, 0, 20);

        autosaveTask = new BukkitRunnable() {
            @Override
            public void run() {
                saveCollectors();
            }
        }.runTaskTimer(plugin, 1200, 1200);
    }

    public void shutdown() {
        stopTasks();

        for (ChunkCollector collector : collectors.values()) {
            collector.removeHologram();
        }
    }

    public void restartTasks() {
        stopTasks();
        startTasks();
    }

    private void stopTasks() {
        if (collectionTask != null) {
            collectionTask.cancel();
            collectionTask = null;
        }
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    public boolean placeCollector(Player player, Location location) {
        if (!player.hasPermission("chunkcollector.use")) {
            return false;
        }

        if (!player.hasPermission("chunkcollector.bypass")) {
            int currentCount = playerCollectorCount.getOrDefault(player.getUniqueId(), 0);
            int maxCollectors = getMaxCollectorsForPlayer(player);
            if (currentCount >= maxCollectors) {
                return false;
            }
        }

        String chunkKey = getChunkKey(location);
        if (chunkCollectors.containsKey(chunkKey)) {
            return false;
        }

        ChunkCollector collector = new ChunkCollector(
                plugin,
                player.getUniqueId(),
                player.getName(),
                location);

        Material blockMaterial = getCollectorBlockMaterial();
        Material previousBlockType = location.getBlock().getType();

        plugin.getLogger().info("Placing collector at " + location.getWorld().getName() +
                " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() +
                " - Previous block: " + previousBlockType + ", Setting to: " + blockMaterial);

        location.getBlock().setType(blockMaterial);

        collectors.put(collector.getUuid(), collector);
        chunkCollectors.put(chunkKey, collector.getUuid());
        locationCollectors.put(getLocationKey(location), collector.getUuid());
        playerCollectorCount.merge(player.getUniqueId(), 1, Integer::sum);

        collector.createHologram();
        saveCollectors();

        location.getWorld().playSound(location,
                BukkitTypes.sound(plugin.getConfigManager().getSound("collector-place"),
                        org.bukkit.Sound.BLOCK_ANVIL_PLACE),
                1.0f, 1.0f);

        location.getWorld().spawnParticle(
                BukkitTypes.particle(plugin.getConfigManager().getEffect("collector-place-particle"),
                        org.bukkit.Particle.HAPPY_VILLAGER),
                location.clone().add(0.5, 1, 0.5), 20, 0.5, 0.5, 0.5, 0.1);

        return true;
    }

    public boolean removeCollector(Player player, Location location) {
        String locationKey = getLocationKey(location);
        UUID collectorUuid = locationCollectors.get(locationKey);

        if (collectorUuid == null) {
            return false;
        }

        ChunkCollector collector = collectors.get(collectorUuid);
        if (collector == null) {
            return false;
        }

        if (!collector.isOwner(player)) {
            return false;
        }

        collectors.remove(collectorUuid);
        chunkCollectors.remove(getChunkKey(collector.getLocation()));
        locationCollectors.remove(locationKey);
        playerCollectorCount.merge(player.getUniqueId(), -1, Integer::sum);

        collector.removeHologram();

        location.getBlock().setType(Material.AIR);

        ItemStack collectorItem = itemFactory.create(1);
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), collectorItem);
        } else {
            player.getInventory().addItem(collectorItem).values()
                    .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }

        location.getWorld().playSound(location,
                BukkitTypes.sound(plugin.getConfigManager().getSound("collector-remove"),
                        org.bukkit.Sound.BLOCK_ANVIL_BREAK),
                1.0f, 1.0f);

        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);

        location.getWorld().spawnParticle(
                BukkitTypes.particle(plugin.getConfigManager().getEffect("collector-break-particle"),
                        org.bukkit.Particle.EXPLOSION),
                location.clone().add(0.5, 0.5, 0.5), 50, 0.5, 0.5, 0.5, 0.1);

        saveCollectors();

        return true;
    }

    public ChunkCollector getCollector(Location location) {
        UUID collectorUuid = locationCollectors.get(getLocationKey(location));
        return collectorUuid != null ? collectors.get(collectorUuid) : null;
    }

    public ChunkCollector getCollector(UUID uuid) {
        return collectors.get(uuid);
    }

    public Collection<ChunkCollector> getCollectors() {
        return collectors.values();
    }

    public Collection<ChunkCollector> getPlayerCollectors(UUID playerUuid) {
        return collectors.values().stream()
                .filter(collector -> collector.getOwnerUuid().equals(playerUuid))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public int getPlayerCollectorCount(UUID playerUuid) {
        return playerCollectorCount.getOrDefault(playerUuid, 0);
    }

    private void collectItems() {
        for (ChunkCollector collector : collectors.values()) {
            if (!collector.isActive())
                continue;

            Location location = collector.getLocation();
            int chunkX = location.getBlockX() >> 4;
            int chunkZ = location.getBlockZ() >> 4;

            if (!location.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                continue;
            }

            Chunk chunk = location.getChunk();

            for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
                if (!(entity instanceof Item))
                    continue;

                Item item = (Item) entity;
                Location itemLocation = item.getLocation();

                if (item.getPersistentDataContainer().has(playerDropKey, PersistentDataType.BYTE))
                    continue;

                int y = itemLocation.getBlockY();
                if (y < plugin.getConfigManager().getMinCollectionHeight() ||
                        y > plugin.getConfigManager().getMaxCollectionHeight()) {
                    continue;
                }

                Material material = item.getItemStack().getType();
                if (!plugin.getConfigManager().getCollectibleItems().contains(material)) {
                    continue;
                }

                collector.collectItem(item.getItemStack(), itemLocation);
                item.remove();
            }
        }
    }

    private void tickCollectors() {
        particleTickCounter += 20;
        int frequency = Math.max(1, plugin.getConfigManager().getHappyVillagerParticleFrequency());
        boolean showParticles = particleTickCounter >= frequency;
        if (showParticles) {
            particleTickCounter = 0;
        }

        for (ChunkCollector collector : collectors.values()) {
            collector.tick();

            if (collector.isActive() && showParticles) {
                Location loc = collector.getLocation();
                if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                    continue;
                }

                loc.getWorld().spawnParticle(
                        org.bukkit.Particle.HAPPY_VILLAGER,
                        loc.clone().add(0.5, 1.5, 0.5),
                        3,
                        0.8, 0.5, 0.8,
                        0.0);
            }
        }
    }

    private String getChunkKey(Location location) {
        return location.getWorld().getName() + ":" + (location.getBlockX() >> 4) + ":" + (location.getBlockZ() >> 4);
    }

    private String getLocationKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":"
                + location.getBlockZ();
    }

    private Material getCollectorBlockMaterial() {
        String blockType = plugin.getConfigManager().getBlockType();

        switch (blockType) {
            case "BEACON":
            default:
                return Material.BEACON;
        }
    }

    public void loadCollectors() {
        List<ChunkCollector> loadedCollectors = dataManager.loadCollectors();

        plugin.getLogger().info("Loading " + loadedCollectors.size() + " collectors...");

        for (ChunkCollector collector : loadedCollectors) {
            collectors.put(collector.getUuid(), collector);
            chunkCollectors.put(getChunkKey(collector.getLocation()), collector.getUuid());
            locationCollectors.put(getLocationKey(collector.getLocation()), collector.getUuid());
            playerCollectorCount.merge(collector.getOwnerUuid(), 1, Integer::sum);

            Location location = collector.getLocation();
            Material blockMaterial = getCollectorBlockMaterial();
            Material currentBlockType = location.getBlock().getType();

            plugin.getLogger().info("Loading collector at " + location.getWorld().getName() +
                    " " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() +
                    " - Current block: " + currentBlockType + ", Setting to: " + blockMaterial);

            if (currentBlockType != blockMaterial) {
                location.getBlock().setType(blockMaterial);
                plugin.getLogger().info("Changed block from " + currentBlockType + " to " + blockMaterial);
            } else {
                plugin.getLogger().info("Block already correct type: " + blockMaterial);
            }

            collector.createHologram();
        }

        startTasks();
    }

    public void saveCollectors() {
        dataManager.saveCollectors(getCollectors());
    }

    public int getMaxCollectorsForPlayer(Player player) {
        if (player.hasPermission("chunkcollectors.max.unlimited")) {
            return Integer.MAX_VALUE;
        }

        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("chunkcollectors.max." + i)) {
                return i;
            }
        }

        return plugin.getConfigManager().getMaxCollectorsPerPlayer();
    }

    public void markPlayerDrop(Item item) {
        item.getPersistentDataContainer().set(playerDropKey, PersistentDataType.BYTE, (byte) 1);
    }
}
