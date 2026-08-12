package com.swevmc.items;

import com.swevmc.SwevsChunkCollector;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class CollectorItemFactory {

    private final SwevsChunkCollector plugin;
    private final NamespacedKey itemKey;

    public CollectorItemFactory(SwevsChunkCollector plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "chunk_collector");
    }

    public ItemStack create(int amount) {
        ItemStack item = new ItemStack(getMaterial(), amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§lChunk Collector");
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);

        List<String> lore = new ArrayList<>();
        lore.add("§7Right-click to place this collector");
        lore.add("§7Collects items automatically in the chunk");
        lore.add("§7Default charge time: §e" + plugin.getConfigManager().getDefaultChargeMinutes() + " minutes");
        double hourlyCost = plugin.getConfigManager().getRechargeCostPerMinute() * 60;
        lore.add("§7Recharge cost: §a$" + String.format("%.2f", hourlyCost) + " per hour");
        lore.add("");
        lore.add("§8§l[CLICK TO PLACE]");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isCollector(ItemStack item) {
        if (item == null || item.getType() != getMaterial() || !item.hasItemMeta()) {
            return false;
        }
        Byte value = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    public Material getMaterial() {
        return switch (plugin.getConfigManager().getBlockType()) {
            case "BEACON" -> Material.BEACON;
            default -> Material.BEACON;
        };
    }
}
