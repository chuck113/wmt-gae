package com.where.gae.data;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.google.common.collect.Iterables;

import javax.jdo.*;
import java.util.List;
import java.util.ConcurrentModificationException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * @author Charles Kubicek
 */
public class LineIterationResultDao {

    private final Logger LOG = Logger.getLogger(LineIterationResultDao.class.getName());

    /**
     * Removes all but the last x obtained results from the datastore
     * @param line
     */
    public void cleanUp(String line, int deleteRange) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try{
            Query query = pm.newQuery(LineIterationResult.class);
            query.setFilter("line == lineParam");
            query.setOrdering("validityTime desc");
            query.declareParameters("String lineParam");

            List<LineIterationResult> result = (List<LineIterationResult>) query.execute(line);

            if (result.size() > deleteRange) {
                pm.deletePersistentAll(Lists.newArrayList(result.listIterator(deleteRange)));
            }

        }finally{
            pm.close();
        }
    }

    public void dropAll() {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try{
            Query query = pm.newQuery(LineIterationResult.class);
            //pm.currentTransaction().begin();
            List<LineIterationResult> result = (List<LineIterationResult>) query.execute();
            pm.deletePersistentAll(Lists.newArrayList(result));
            //pm.currentTransaction().commit();
        }catch (Exception e){
            //pm.currentTransaction().rollback();               
            LOG.warn("while dropping all: "+e.getMessage(), e);
        }finally{
            pm.close();
        }
    }

    //TODO
    private class PersistenceFacade{
        private final PersistenceManager pm = PMF.get().getPersistenceManager();

        public PersistenceFacade(){
            pm.currentTransaction().begin();
        }

        public String save(LineIterationResult result){
            //Need to test this, unlikely
            pm.makePersistent(result);
            return result.getId();
        }
    }

    public String create(LineIterationResult result) {
        PersistenceManager pm = PMF.get().getPersistenceManager();

        try {
            pm.currentTransaction().begin();
            pm.makePersistent(result);
            pm.currentTransaction().commit();
            return result.getId();
        } finally {
            if (pm.currentTransaction().isActive()) {
                pm.currentTransaction().rollback();
            }
            pm.close();
        }
    }

    public void update(LineIterationResult newResult) {
        PersistenceManager pm = pm();
        try {

            pm.currentTransaction().begin();
            // We don't have a reference to the selected object.
            // So we have to look it up first,
            LineIterationResult existing = pm.getObjectById(LineIterationResult.class, newResult.getId());
            existing.copyDataInto(newResult);
            pm.currentTransaction().commit();
        } finally {
            if (pm.currentTransaction().isActive()) {
                pm.currentTransaction().rollback();
            }
            pm.close();
        }
    }

    private PersistenceManager pm() {
        return PMF.get().getPersistenceManager();
    }

    private List<LineIterationResult> getResultsCurrentValididtyFirst(PersistenceManager pm, String line) {
        Query query = pm.newQuery(LineIterationResult.class);
        query.setFilter("line == lineParam");
        query.setOrdering("validityTime desc");
        query.setRange(0, 5);
        query.declareParameters("String lineParam");

        return (List<LineIterationResult>) query.execute(line);
    }

    public LineIterationResult getLatestLineResult(String line) {
        PersistenceManager pm = pm();
        try {
            return getLatestLineResult(line, pm);
        } finally {
            pm.close();
        }
    }

    private LineIterationResult getLatestLineResult(String line, PersistenceManager pm) {
        List<LineIterationResult> lines = getResultsCurrentValididtyFirst(pm, line);
        if (lines.size() == 0) {
            LOG.warn("found no lines in datastore for line " + line + ", will return null");
            return null;
        }

        LineIterationResult res = pm.getObjectById(LineIterationResult.class, lines.get(0).getId());
        if (res.getResult() == null) {
            LOG.warn("result was null: " + res);
        }

        return pm.getObjectById(LineIterationResult.class, lines.get(0).getId());
    }

    private enum CasTestState {
        UNCHECKED, SUCCESS, FAILED
    }

    /**
     * 'Compare-and-set', will force an exception if the previousExpected value is different
     * to the value actually in the datastore
     *  
     * @param entryKey
     * @param previousExpected
     * @param newValue
     * @return
     */
    public boolean casUpdate(String entryKey, long previousExpected, long newValue) {
        LOG.warn("LineIterationResultDao.casUpdate key is "+entryKey);

        CasTestState casState = CasTestState.UNCHECKED;
        PersistenceManager pm = pm();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            LineIterationResult existing = pm.getObjectById(LineIterationResult.class, entryKey);
            if (existing.getCasValue() != previousExpected) {
                casState = CasTestState.FAILED;
            } else {
                existing.setCasValue(newValue);
                pm.makePersistent(existing);
                tx.commit();
                casState = CasTestState.SUCCESS;
            }
            // this has never been caught but we'd like it to be as this is the perfered method
            // method of isolation, must be a bug, or GAE throws concurrent modification exceptions
            // before it throws optimistic verification exceptions
        } catch (JDOOptimisticVerificationException e) {
            LOG.warn("LineIterationResultDao.update JDOOptimisticVerificationException " + e.getMessage());
            casState = CasTestState.FAILED;
        } catch (JDOException e) {
            LOG.warn("LineIterationResultDao.update exception: " + e.getClass().getName());
            if (isConcurrentModificationException(e)) {
                LOG.warn("LineIterationResultDao.update CONCURRENT MODIFICATION");
                casState = CasTestState.FAILED;
            } else {
                throw e;
            }
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
            pm.close();
        }
        return casState == CasTestState.SUCCESS;
    }

    private boolean isConcurrentModificationException(JDOException jdoException) {
        if (!(jdoException instanceof JDOCanRetryException)) {
            Throwable cause = jdoException;
            while (cause.getCause() != null) {
                LOG.warn("LineIterationResultDao.isConcurrentModificationException cause is " + cause.getCause().getClass().getName());
                if ((cause = cause.getCause()) instanceof ConcurrentModificationException) {
                    return true;
                }
            }
        }
        return false;
    }
}
