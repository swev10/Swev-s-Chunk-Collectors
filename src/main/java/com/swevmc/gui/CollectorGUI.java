package com.swevmc.gui;

import com.swevmc.models.ChunkCollector;
import com.swevmc.SwevsChunkCollector;
import com.swevmc.utils.BukkitTypes;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CollectorGUI implements Listener {

    private final SwevsChunkCollector plugin;

    public CollectorGUI(SwevsChunkCollector plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void openGUI(Player player, ChunkCollector collector) {
        CollectorHolder holder = new CollectorHolder(collector.getUuid(), plugin.getConfigManager().getGuiTitle());
        Inventory inventory = holder.getInventory();

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(" ");
        glass.setItemMeta(glassMeta);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, glass);
        }

        inventory.setItem(12, createChargeButton(collector, player));
        inventory.setItem(13, collector.getGuiItem());

        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        closeMeta.setDisplayName("§cClose");
        closeButton.setItemMeta(closeMeta);
        inventory.setItem(15, closeButton);

        player.openInventory(inventory);
        playOpenSound(player);
    }

    private ItemStack createChargeButton(ChunkCollector collector, Player player) {
        ItemStack button = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = button.getItemMeta();
        List<String> lore = new ArrayList<>();

        if (collector.getTimeRemaining() < collector.getMaxDuration(player)) {
            meta.setDisplayName(plugin.getConfigManager().getGuiRechargeButton());
            lore.add("§7Click to add charge");
            long chargeMinutes = plugin.getConfigManager().getDefaultChargeMinutes();
            double cost = chargeMinutes * plugin.getConfigManager().getRechargeCostPerMinute();
            lore.add("§7Add: §e" + chargeMinutes + " minutes");
            lore.add("§7Cost: §a$" + String.format("%.2f", cost));

            long availableSeconds = collector.getMaxDuration(player) - collector.getTimeRemaining();
            if (availableSeconds < chargeMinutes * 60) {
                lore.add("§e(Will fill to max)");
            }
        } else {
            meta.setDisplayName("§cCollector Full");
            lore.add("§7This collector is fully charged.");
        }

        meta.setLore(lore);
        button.setItemMeta(meta);
        return button;
    }

    private void playOpenSound(Player player) {
        player.playSound(player.getLocation(),
                BukkitTypes.sound(plugin.getConfigManager().getSound("gui-open"), org.bukkit.Sound.UI_BUTTON_CLICK),
                0.5f, 1.0f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CollectorHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        if (clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (clickedItem.getType() != Material.GOLD_INGOT) {
            return;
        }

        ChunkCollector collector = plugin.getChunkCollectorManager().getCollector(holder.getCollectorUuid());
        if (collector == null || !collector.isOwner(player)) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix()
                    + plugin.getConfigManager().getMessage("collector-not-found"));
            return;
        }

        if (collector.addCharge(player)) {
            plugin.getChunkCollectorManager().saveCollectors();
            openGUI(player, collector);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof CollectorHolder) {
            event.setCancelled(true);
        }
    }

    private static final class CollectorHolder implements InventoryHolder {

        private final UUID collectorUuid;
        private final Inventory inventory;

        private CollectorHolder(UUID collectorUuid, String title) {
            this.collectorUuid = collectorUuid;
            this.inventory = Bukkit.createInventory(this, 27, title);
        }

        private UUID getCollectorUuid() {
            return collectorUuid;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
