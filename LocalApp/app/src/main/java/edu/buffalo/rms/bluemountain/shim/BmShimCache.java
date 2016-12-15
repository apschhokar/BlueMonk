package edu.buffalo.rms.bluemountain.shim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.buffalo.rms.bluemountain.framework.BmFileSystemFramework;
import edu.buffalo.rms.bluemountain.shim.BmChunk;

/**
 * Created by sharath on 9/18/16.
 * Cache maintains the chunks
 */
public class BmShimCache {
    static Map<String, BmChunk> chunkMap = new HashMap<>();
    int CHUNK_MAP_SZ = 4;
    BmFileSystemFramework bmFramework;

    public BmShimCache() {
        bmFramework = BmFileSystemFramework.getInstance();
    }

    public void addChunkToCache(String id, BmChunk chunk) {
        if (chunkMap.size() == CHUNK_MAP_SZ) {
            //one chunk has to be flushed
            evictChunk();
        }
        chunkMap.put(id, chunk);
    }

    private BmChunk evictChunk() {
        //TODO: Implement LRU
        List<String> keys = new ArrayList(chunkMap.keySet());
        int randomIndex = new Random().nextInt(keys.size());
        String randomChunk = keys.get(randomIndex);
        //bmFramework.flushChunk(randomChunk);
        return chunkMap.remove(randomChunk);
    }

    public void delChunkInCache(String id) {
        chunkMap.remove(id);
    }

    public BmChunk getChunk(String chunkId) {
        return chunkMap.get(chunkId);
    }
}
