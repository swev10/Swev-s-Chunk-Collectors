package com.swevmc.storage;

import com.swevmc.models.ChunkCollector;
import java.util.List;

public interface StorageInterface {
    
    boolean initialize();
    
    void shutdown();
    
    boolean saveCollectors(List<CollectorSnapshot> collectors);
    
    List<ChunkCollector> loadCollectors();
    
    boolean isConnected();
}
