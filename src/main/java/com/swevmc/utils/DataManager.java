package com.swevmc.utils;

import com.swevmc.SwevsChunkCollector;
import com.swevmc.models.ChunkCollector;
import com.swevmc.storage.StorageInterface;
import com.swevmc.storage.FileStorage;
import com.swevmc.storage.MySQLStorage;
import com.swevmc.storage.RedisStorage;
import com.swevmc.storage.CollectorSnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DataManager {
    
    private final SwevsChunkCollector plugin;
    private final ExecutorService saveExecutor;
    private StorageInterface storage;
    
    public DataManager(SwevsChunkCollector plugin) {
        this.plugin = plugin;
        this.saveExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "chunk-collector-storage");
            thread.setDaemon(true);
            return thread;
        });
        initializeStorage();
    }
    
    private void initializeStorage() {
        String storageType = plugin.getConfigManager().getStorageType();
        
        switch (storageType) {
            case "MYSQL":
                String mysqlHost = plugin.getConfigManager().getMySQLHost();
                int mysqlPort = plugin.getConfigManager().getMySQLPort();
                String mysqlDatabase = plugin.getConfigManager().getMySQLDatabase();
                String mysqlUsername = plugin.getConfigManager().getMySQLUsername();
                String mysqlPassword = plugin.getConfigManager().getMySQLPassword();
                boolean mysqlUseSSL = plugin.getConfigManager().getMySQLUseSSL();
                
                storage = new MySQLStorage(plugin, mysqlHost, mysqlPort, mysqlDatabase, mysqlUsername, mysqlPassword, mysqlUseSSL);
                plugin.getLogger().info("Initializing MySQL storage...");
                break;
                
            case "REDIS":
                String redisHost = plugin.getConfigManager().getRedisHost();
                int redisPort = plugin.getConfigManager().getRedisPort();
                String redisPassword = plugin.getConfigManager().getRedisPassword();
                int redisDatabase = plugin.getConfigManager().getRedisDatabase();
                boolean redisUseSSL = plugin.getConfigManager().getRedisUseSSL();
                
                storage = new RedisStorage(plugin, redisHost, redisPort, redisPassword, redisDatabase, redisUseSSL);
                plugin.getLogger().info("Initializing Redis storage...");
                break;
                
            case "FILE":
            default:
                storage = new FileStorage(plugin);
                plugin.getLogger().info("Initializing file storage...");
                break;
        }
        
        if (!storage.initialize()) {
            plugin.getLogger().severe("Failed to initialize " + storageType + " storage! Falling back to file storage.");
            storage.shutdown();
            storage = new FileStorage(plugin);
            if (!storage.initialize()) {
                plugin.getLogger().severe("Failed to initialize file storage as fallback!");
            }
        } else {
            plugin.getLogger().info("Successfully initialized " + storageType + " storage!");
        }
    }
    
    public void saveCollectors(Collection<ChunkCollector> collectors) {
        List<CollectorSnapshot> snapshots = createSnapshots(collectors);
        saveExecutor.execute(() -> storage.saveCollectors(snapshots));
    }

    public void saveCollectorsBlocking(Collection<ChunkCollector> collectors) {
        List<CollectorSnapshot> snapshots = createSnapshots(collectors);
        try {
            saveExecutor.submit(() -> storage.saveCollectors(snapshots)).get();
        } catch (Exception exception) {
            plugin.getLogger().severe("Failed to complete collector save: " + exception.getMessage());
        }
    }
    
    public List<ChunkCollector> loadCollectors() {
        if (storage != null) {
            return storage.loadCollectors();
        }
        return null;
    }
    
    public boolean isStorageConnected() {
        return storage != null && storage.isConnected();
    }
    
    public void shutdown() {
        saveExecutor.shutdown();
        try {
            if (!saveExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Timed out while waiting for collector saves to finish");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        if (storage != null) {
            storage.shutdown();
        }
    }

    private List<CollectorSnapshot> createSnapshots(Collection<ChunkCollector> collectors) {
        List<CollectorSnapshot> snapshots = new ArrayList<>(collectors.size());
        collectors.forEach(collector -> snapshots.add(CollectorSnapshot.from(collector)));
        return snapshots;
    }
}
