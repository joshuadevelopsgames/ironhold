package kingdom.smp.rtf.rivermap;

import java.util.concurrent.TimeUnit;

import kingdom.smp.rtf.concurrent.cache.Cache;
import kingdom.smp.rtf.concurrent.cache.CacheManager;
import kingdom.smp.rtf.concurrent.cache.map.StampedLongMap;
import kingdom.smp.rtf.util.PosUtil;

public class RiverCache {
    protected RiverGenerator generator;
    protected Cache<Rivermap> cache;
    
    public RiverCache(RiverGenerator generator) {
        this.cache = CacheManager.createCache(32, 5L, 1L, TimeUnit.MINUTES, StampedLongMap::new);
        this.generator = generator;
    }
    
    public Rivermap getRivers(int x, int z) {
        return this.cache.computeIfAbsent(PosUtil.pack(x, z), id -> {
        	return this.generator.generateRivers(PosUtil.unpackLeft(id), PosUtil.unpackRight(id), id);
        });
    }
}
