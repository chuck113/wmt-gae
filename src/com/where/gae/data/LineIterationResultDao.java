package com.where.gae.data;

import com.google.appengine.api.datastore.DatastoreServiceFactory;

import javax.jdo.*;
import java.util.List;
import java.util.ConcurrentModificationException;

import org.apache.log4j.Logger;

/**
 * @author Charles Kubicek
 */
public class LineIterationResultDao {

    private final Logger LOG = Logger.getLogger(LineIterationResultDao.class.getName());

    public String create(LineIterationResult result) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        
        try {
            pm.currentTransaction().begin();
            pm.makePersistent(result);
            pm.currentTransaction().commit();
            //LOG.warn("LineIterationResultDao.create committed "+result);

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

    private List<LineIterationResult> getAllResultsCurrentValididtyFirst(PersistenceManager pm, String line) {
            Query query = pm.newQuery(LineIterationResult.class);
            query.setFilter("line == lineParam");
            query.setOrdering("validityTime desc");
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
            List<LineIterationResult> lines = getAllResultsCurrentValididtyFirst(pm,line);
            if(lines.size() == 0){
                LOG.warn("found no lines in datastore for line "+line+", will return null");
                return null;
            }

            LineIterationResult res = pm.getObjectById(LineIterationResult.class, lines.get(0).getId());
            if(res.getResult() == null){
                LOG.warn("result was null: "+res);
            }

            return pm.getObjectById(LineIterationResult.class, lines.get(0).getId());
    }

    private enum CasTestState{
        UNCHECKED,SUCCESS,FAILED
    }

    public boolean casUpdate(String entryKey, long previousExpected, long newValue) {
        CasTestState casState = CasTestState.UNCHECKED;
        PersistenceManager pm = pm();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            LineIterationResult existing = pm.getObjectById(LineIterationResult.class, entryKey);//getLatestLineResult(line, pm);
            //LOG.warn( "LineIterationResultDao.casUpdate "+System.currentTimeMillis()+" for id "+existing.getId()+" cas is "+existing.getCasValue()+", version is "+existing.getVersionField());
            if (existing.getCasValue() != previousExpected) {
                //LOG.warn( "LineIterationResultDao.casUpdate "+System.currentTimeMillis()+" for id "+existing.getId()+" current result is not as expected, NOT committing, cas is "+existing.getCasValue());
                casState = CasTestState.FAILED;
            } else {
//                long versionField = existing.getVersionField();
//                LOG.warn( "LineIterationResultDao.casUpdate "+System.currentTimeMillis()+" for id "+existing.getId()+" current result is "+previousExpected+" as expected, committing");
//                LOG.warn( "LineIterationResultDao.casUpdate begining sleep at "+System.currentTimeMillis());
////                synchronized (this){
////                    try{this.wait(5000);}catch(Exception e){}
////                }
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                }
//                LOG.warn( "LineIterationResultDao.casUpdate ended sleep at "+System.currentTimeMillis());

                existing.setCasValue(newValue);
                pm.makePersistent(existing);
                tx.commit();
                //LOG.warn("LineIterationResultDao.casUpdate version before was "+versionField+", version now is "+existing.getVersionField());
                //if(existing.getVersionField() != versionField){
                //    LOG.warn( "LineIterationResultDao.casUpdate "+System.currentTimeMillis()+" for id "+existing.getId()+" comitted cas but got incorrect version field, returning as failed");
                //    casState = CasTestState.FAILED;
                //} else {
                    //LOG.warn( "LineIterationResultDao.casUpdate "+System.currentTimeMillis()+" for id "+existing.getId()+" committed result "+newValue);
                    casState = CasTestState.SUCCESS;
                //}
            }
        // this has never been caught but we'd like it to be as this is the perfered method
        // method of isolation, must be a bug, or GAE throws concurrent modification exceptions
        // before it throws optimistic verification exceptions
        }catch (JDOOptimisticVerificationException e){
            LOG.warn( "LineIterationResultDao.update JDOOptimisticVerificationException "+e.getMessage());
            casState = CasTestState.FAILED;    
        } catch (JDOException e) {
            LOG.warn( "LineIterationResultDao.update exception: "+e.getClass().getName());                            
            if (isConcurrentModificationException(e)) {
                LOG.warn( "LineIterationResultDao.update CONCURRENT MODIFICATION");
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
                LOG.warn("LineIterationResultDao.isConcurrentModificationException cause is "+cause.getCause().getClass().getName());
                if ((cause = cause.getCause()) instanceof ConcurrentModificationException) {
                    return true;
                }
            }
        }
        return false;
    }
}
