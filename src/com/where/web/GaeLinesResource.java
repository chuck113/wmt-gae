package com.where.web;

import com.where.domain.DaoFactory;
import com.where.domain.alg.LineIteratorImpl;
import com.where.tfl.grabber.ArrivalBoardScraper;
import com.where.gae.web.MemcacheLineIteratorSynchronizer;
import com.google.appengine.api.memcache.stdimpl.GCacheFactory;

import javax.cache.Cache;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;
import javax.cache.CacheException;
import java.util.Map;
import java.util.Collections;

import org.apache.log4j.Logger;

public class GaeLinesResource extends AbstractLinesResource {

    final Logger LOG = Logger.getLogger(GaeLinesResource.class);
    private static final String MEMCACHED_DAO_FACTORY_KEY = "MEMCACHED_DAO_FACTORY_KEY";

    private Cache cache;
    private DaoFactory cachedDaoFactory;

    public GaeLinesResource() {
        try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            Map cacheProps = Collections.singletonMap(GCacheFactory.EXPIRATION_DELTA_MILLIS, 120 * 1000);
            cache = cacheFactory.createCache(cacheProps);
        } catch (CacheException e) {
            LOG.warn("cache initialization exception " + e.getMessage(), e);
            cache = null;
        }
    }

    LineIteratorSynchronizer getLineIteratorSynchronizer(LineIteratorImpl lineIterator) {
        return new MemcacheLineIteratorSynchronizer(lineIterator);
    }

    DaoFactory getDaoFactory() {
        if (cachedDaoFactory == null) {
            cachedDaoFactory = (DaoFactory) cache.get(MEMCACHED_DAO_FACTORY_KEY);
            if (cachedDaoFactory == null) {
                LOG.warn("No cached Dao factory found in memcache, creating");
                System.out.println("GaeLinesResource.getDaoFactory No cached Dao factory found in memcache, creating");
                cachedDaoFactory = new ClasspathFileDataSource().getDaoFactory();
                cache.put(MEMCACHED_DAO_FACTORY_KEY, cachedDaoFactory);
            }
        }
        return cachedDaoFactory;
    }
}
