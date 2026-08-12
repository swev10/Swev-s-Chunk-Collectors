package com.swevmc.storage;

import com.swevmc.models.ChunkCollector;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record CollectorSnapshot(
        UUID uuid,
        UUID ownerUuid,
        String ownerName,
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        long createdAt,
        long timeRemaining,
        int itemsCollected,
        boolean active,
        long maxChargeTime,
        double totalMoneyEarned,
        long lastAutosellTime,
        Map<String, Integer> virtualItems) {

    public CollectorSnapshot {
        virtualItems = Map.copyOf(virtualItems);
    }

    public static CollectorSnapshot from(ChunkCollector collector) {
        Location location = collector.getLocation();
        Map<String, Integer> items = new LinkedHashMap<>();
        collector.getVirtualItems().forEach((material, amount) -> items.put(material.name(), amount));
        return new CollectorSnapshot(
                collector.getUuid(),
                collector.getOwnerUuid(),
                collector.getOwnerName(),
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                collector.getCreatedAt(),
                collector.getTimeRemaining(),
                collector.getItemsCollected(),
                collector.isActive(),
                collector.getMaxChargeTime(),
                collector.getTotalMoneyEarned(),
                collector.getLastAutosellTime(),
                items);
    }

    public Map<Material, Integer> materialItems() {
        Map<Material, Integer> items = new LinkedHashMap<>();
        virtualItems.forEach((name, amount) -> {
            Material material = Material.matchMaterial(name);
            if (material != null && amount > 0) {
                items.put(material, amount);
            }
        });
        return items;
    }
}
