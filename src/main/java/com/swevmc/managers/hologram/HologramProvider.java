package com.swevmc.managers.hologram;

import com.swevmc.models.ChunkCollector;

public interface HologramProvider {
    void createHologram(ChunkCollector collector);

    void updateHologram(ChunkCollector collector);

    void removeHologram(ChunkCollector collector);

}
