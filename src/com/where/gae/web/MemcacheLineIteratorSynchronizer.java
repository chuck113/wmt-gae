package com.where.gae.web;

import com.where.domain.alg.LineIterator;
import com.where.gae.data.LineIterationResult;
import com.where.gae.data.LineIterationState;
import com.where.web.WmtProperties;
import com.where.web.JsonTransformer;
import com.google.appengine.api.memcache.stdimpl.GCacheFactory;

import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Date;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;

public class MemcacheLineIteratorSynchronizer extends DatastoreLineIteratorSynchronizer {

    final Logger LOG = Logger.getLogger(MemcacheLineIteratorSynchronizer.class);

    private Cache cache;
    private static final String DROP_CACHE = "dropCache";

    public MemcacheLineIteratorSynchronizer(LineIterator branchIterator) {
        super(branchIterator);
        try {
            CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
            Map cacheProps = Collections.singletonMap(GCacheFactory.EXPIRATION_DELTA_MILLIS, WmtProperties.DATA_VALIDITY_PERIOD_MS * 2);
            cache = cacheFactory.createCache(cacheProps);
        } catch (CacheException e) {
            LOG.warn("cache initialization exception "+e.getMessage(), e);
            cache = null;
        }
    }

        /**
     * If a parse is in progress but the valididty time was X minutes ago there is something wrong, i.e the request
     * that started the line parse failed in some way and the is
     */
    boolean isResultTimeExpiredForLongerThanShouldBe(LineIterationState state) {
        return state.isReItrationInProgress() &&
             (state.getResult().getValidityTime().getTime() + (VALIDITY_PERIOD * 3)) >= new Date().getTime();
    }

    /**
     * As memcahce can be emptied at anytime grab the last result from the datastore
     * if the value is null
     */
    private LineIterationState safeGet(String branch){
        LineIterationState state = (LineIterationState)cache.get(branch);
        if(state == null){
            LineIterationResult lastResult = super.getLastLineResultRegardlessOfTimeValidity(branch);
            state = new LineIterationState(lastResult);
            cache.put(branch, state);
        }
        return state;
    }

    private void dropFromCache(){
        LOG.warn("Emtpying cache");
        for(String line :WmtProperties.LINES_TO_ITERATE){
            cache.remove(line);
        }
    }


    @Override
    public String getLine(final String line) {
        LOG.warn("line is "+line);
        if(DROP_CACHE.equals(line)){
            dropFromCache();
            return "{  \"dropCache\" : \"OK\"}";
        } else if(DROP_ALL.equals(line)){
            super.dropAll();
            return "{  \"dropAll\" : \"OK\"}";
        }
        if(!validateLine(line)){
            return JsonTransformer.toJsonError("line "+line+" was invalid");
        }
        LineIterationState state = safeGet(line);
        LOG.warn("MemcacheLineIteratorSynchronizer.getLine.() isResultTimeValid(state.getResult()) is "+isResultTimeValid(state.getResult()));

        if(isResultTimeValid(state.getResult())){
            LOG.warn("MemcacheLineIteratorSynchronizer.getLine.() time is valid");
            return state.getResult().getResult();
        }else {
            LOG.warn("MemcacheLineIteratorSynchronizer.getLine.() isReItrationInProgress: "+state.isReItrationInProgress());
            if(state.isReItrationInProgress()){
                return state.getResult().getResult();
            } else{
                // doesn't matter is >1 threads do this concurrently because we rely on the
                // datastore cas update to synchronise the threads, and the memcache will only
                // be upated by the thread that successuflly completes the cas check
                LOG.warn("MemcacheLineIteratorSynchronizer.getLine.() setting in progress to true");

                state.setReItrationInProgress(true);
                cache.put(line, state);
                super.update(state.getResult(), new PostCommitWork(){
                    public void doWork(LineIterationResult newResult) {
                        LOG.warn("MemcacheLineIteratorSynchronizer.getLine.() GOT NEW RESULT: "+newResult);
                        cache.put(line, new LineIterationState(newResult));
                    }
                });
                LOG.warn("MemcacheLineIteratorSynchronizer.getLine.() new validity time is "+safeGet(line).getResult().getValidityTime());
                return safeGet(line).getResult().getResult();
            }
        }
    }
}