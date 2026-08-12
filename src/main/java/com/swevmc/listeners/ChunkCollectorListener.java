package com.swevmc.listeners;

import com.swevmc.SwevsChunkCollector;
import com.swevmc.gui.CollectorGUI;
import com.swevmc.models.ChunkCollector;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class ChunkCollectorListener implements Listener {

    private final SwevsChunkCollector plugin;
    private final CollectorGUI gui;

    public ChunkCollectorListener(SwevsChunkCollector plugin) {
        this.plugin = plugin;
        this.gui = new CollectorGUI(plugin);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getCollectorItemFactory().isCollector(event.getItemInHand())) {

            if (!player.hasPermission("chunkcollector.use")) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("no-permission"));
                return;
            }

            if (plugin.getChunkCollectorManager().placeCollector(player, event.getBlock().getLocation())) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("collector-placed"));
            } else {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("collector-already-exists"));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (isCollectorBlock(event.getBlock())) {
            ChunkCollector collector = plugin.getChunkCollectorManager().getCollector(event.getBlock().getLocation());

            if (collector != null) {
                if (!collector.isOwner(player)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("collector-not-yours"));
                    return;
                }

                if (plugin.getChunkCollectorManager().removeCollector(player, event.getBlock().getLocation())) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("collector-removed"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Player player = event.getPlayer();

        if (isCollectorBlock(event.getClickedBlock())) {
            ChunkCollector collector = plugin.getChunkCollectorManager()
                    .getCollector(event.getClickedBlock().getLocation());

            if (collector != null) {
                if (!collector.isOwner(player)) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("collector-not-yours"));
                    return;
                }

                gui.openGUI(player, collector);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.getChunkCollectorManager().markPlayerDrop(event.getItemDrop());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.getChunkCollectorManager().getCollector(block.getLocation()) != null);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> plugin.getChunkCollectorManager().getCollector(block.getLocation()) != null);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        boolean movesCollector = event.getBlocks().stream()
                .anyMatch(block -> plugin.getChunkCollectorManager().getCollector(block.getLocation()) != null);
        if (movesCollector) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        boolean movesCollector = event.getBlocks().stream()
                .anyMatch(block -> plugin.getChunkCollectorManager().getCollector(block.getLocation()) != null);
        if (movesCollector) {
            event.setCancelled(true);
        }
    }

    private boolean isCollectorBlock(Material material) {
        String blockType = plugin.getConfigManager().getBlockType();

        switch (blockType) {
            case "BEACON":
            default:
                return material == Material.BEACON;
        }
    }

    private boolean isCollectorBlock(Block block) {
        return isCollectorBlock(block.getType());
    }

}
