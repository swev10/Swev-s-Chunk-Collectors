package com.swevmc.managers.hologram;

import com.swevmc.SwevsChunkCollector;
import com.swevmc.models.ChunkCollector;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.util.List;

public class DecentHologramsProvider implements HologramProvider {

    private final SwevsChunkCollector plugin;

    public DecentHologramsProvider(SwevsChunkCollector plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createHologram(ChunkCollector collector) {
        String hologramName = "collector_" + collector.getUuid().toString();
        Location location = collector.getLocation().clone().add(0.5,
                plugin.getConfigManager().getHologramHeightOffset(), 0.5);

        if (DHAPI.getHologram(hologramName) != null) {
            DHAPI.removeHologram(hologramName);
        }

        DHAPI.createHologram(hologramName, location, getFormattedLines(collector));
    }

    @Override
    public void updateHologram(ChunkCollector collector) {
        String hologramName = "collector_" + collector.getUuid().toString();
        Hologram hologram = DHAPI.getHologram(hologramName);

        if (hologram != null) {
            DHAPI.setHologramLines(hologram, getFormattedLines(collector));
        } else {
            createHologram(collector);
        }
    }

    @Override
    public void removeHologram(ChunkCollector collector) {
        String hologramName = "collector_" + collector.getUuid().toString();
        if (DHAPI.getHologram(hologramName) != null) {
            DHAPI.removeHologram(hologramName);
        }
    }

    private List<String> getFormattedLines(ChunkCollector collector) {
        List<String> lines = plugin.getConfigManager().getHologramLines();
        return HologramFormatter.format(lines, collector);
    }
}
