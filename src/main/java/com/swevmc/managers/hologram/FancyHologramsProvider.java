package com.swevmc.managers.hologram;

import com.swevmc.SwevsChunkCollector;
import com.swevmc.models.ChunkCollector;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Location;

import java.util.List;

public class FancyHologramsProvider implements HologramProvider {

    private final SwevsChunkCollector plugin;

    public FancyHologramsProvider(SwevsChunkCollector plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createHologram(ChunkCollector collector) {
        String hologramName = "collector_" + collector.getUuid().toString();
        Location location = collector.getLocation().clone().add(0.5,
                plugin.getConfigManager().getHologramHeightOffset(), 0.5);

        TextHologramData data = new TextHologramData(hologramName, location);
        data.setText(getFormattedLines(collector));
        data.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);

        String backgroundStr = plugin.getConfigManager().getHologramBackground();
        if (backgroundStr != null && !backgroundStr.equalsIgnoreCase("TRANSPARENT")) {
            try {
                org.bukkit.Color color;
                if (backgroundStr.startsWith("#")) {

                    int r = Integer.valueOf(backgroundStr.substring(1, 3), 16);
                    int g = Integer.valueOf(backgroundStr.substring(3, 5), 16);
                    int b = Integer.valueOf(backgroundStr.substring(5, 7), 16);
                    color = org.bukkit.Color.fromRGB(r, g, b);
                } else {

                    java.awt.Color awtColor;
                    try {
                        java.lang.reflect.Field field = java.awt.Color.class.getField(backgroundStr.toLowerCase());
                        awtColor = (java.awt.Color) field.get(null);
                        color = org.bukkit.Color.fromRGB(awtColor.getRed(), awtColor.getGreen(), awtColor.getBlue());
                    } catch (Exception e) {

                        int r = Integer.valueOf(backgroundStr.substring(1, 3), 16);
                        int g = Integer.valueOf(backgroundStr.substring(3, 5), 16);
                        int b = Integer.valueOf(backgroundStr.substring(5, 7), 16);
                        color = org.bukkit.Color.fromRGB(r, g, b);
                    }
                }
                data.setBackground(color);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid hologram background color: " + backgroundStr);
            }
        }

        Hologram hologram = FancyHologramsPlugin.get().getHologramManager().create(data);
        FancyHologramsPlugin.get().getHologramManager().addHologram(hologram);
    }

    @Override
    public void updateHologram(ChunkCollector collector) {
        String hologramName = "collector_" + collector.getUuid().toString();
        Hologram hologram = FancyHologramsPlugin.get().getHologramManager().getHologram(hologramName).orElse(null);

        if (hologram != null && hologram.getData() instanceof TextHologramData textData) {
            textData.setText(getFormattedLines(collector));
            hologram.queueUpdate();
        } else {
            createHologram(collector);
        }
    }

    @Override
    public void removeHologram(ChunkCollector collector) {
        String hologramName = "collector_" + collector.getUuid().toString();
        Hologram hologram = FancyHologramsPlugin.get().getHologramManager().getHologram(hologramName).orElse(null);

        if (hologram != null) {
            FancyHologramsPlugin.get().getHologramManager().removeHologram(hologram);
        }
    }

    private List<String> getFormattedLines(ChunkCollector collector) {
        List<String> lines = plugin.getConfigManager().getHologramLines();
        return HologramFormatter.format(lines, collector);
    }
}
