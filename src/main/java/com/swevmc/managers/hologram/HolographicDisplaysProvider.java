package com.swevmc.managers.hologram;

import com.swevmc.SwevsChunkCollector;
import com.swevmc.models.ChunkCollector;
import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HolographicDisplaysProvider implements HologramProvider {

    private final SwevsChunkCollector plugin;
    private final HolographicDisplaysAPI api;
    private final Map<UUID, Hologram> holograms = new HashMap<>();

    public HolographicDisplaysProvider(SwevsChunkCollector plugin) {
        this.plugin = plugin;
        this.api = HolographicDisplaysAPI.get(plugin);
    }

    @Override
    public void createHologram(ChunkCollector collector) {
        removeHologram(collector);

        Location location = collector.getLocation().clone().add(0.5,
                plugin.getConfigManager().getHologramHeightOffset() + 2, 0.5);
        Hologram hologram = api.createHologram(location);

        updateHologramLines(hologram, collector);

        holograms.put(collector.getUuid(), hologram);
    }

    @Override
    public void updateHologram(ChunkCollector collector) {
        Hologram hologram = holograms.get(collector.getUuid());
        if (hologram == null || hologram.isDeleted()) {
            createHologram(collector);
            return;
        }

        updateHologramLines(hologram, collector);
    }

    @Override
    public void removeHologram(ChunkCollector collector) {
        Hologram hologram = holograms.remove(collector.getUuid());
        if (hologram != null) {
            hologram.delete();
        }
    }

    private void updateHologramLines(Hologram hologram, ChunkCollector collector) {
        hologram.getLines().clear();
        List<String> lines = HologramFormatter.format(plugin.getConfigManager().getHologramLines(), collector);
        for (String line : lines) {
            hologram.getLines().appendText(line);
        }
    }
}
