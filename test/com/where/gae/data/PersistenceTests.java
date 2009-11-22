package com.where.gae.data;

import junit.framework.TestCase;
import com.where.gae.test.LocalDatastoreTestHarness;
import com.where.gae.test.LocalServiceHarness;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Query;
import com.google.common.collect.Lists;

import java.util.Date;
import java.util.List;
import java.util.Collections;

/**
 * @author Charles Kubicek
 */
public class PersistenceTests extends TestCase {

    private LocalDatastoreTestHarness gaeHarness;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        (gaeHarness = new LocalDatastoreTestHarness()).setUp();
    }

    @Override
    public void tearDown() throws Exception {
        gaeHarness.setUp();
    }

    public void testSave() throws Exception{
        LineIterationResultDao dao = new LineIterationResultDao();
        LineIterationResult info1 = new LineIterationResult("victoria", "{}", new Date());
        LineIterationResult info2 = new LineIterationResult("victoria", "{}", new Date());
        dao.create(info1);
        dao.create(info2);

        Query query = new Query(LineIterationResult.class.getSimpleName());
        assertEquals(2, DatastoreServiceFactory.getDatastoreService().prepare(query).countEntities());
    }

    public void testUpdate() throws Exception{
        LineIterationResultDao dao = new LineIterationResultDao();
        LineIterationResult info1 = new LineIterationResult("victoria", "{}", new Date());
        LineIterationResult info2 = new LineIterationResult("victoria", "{}", new Date());
        dao.create(info1);
        dao.create(info2);

        info1.setResult("{res1}");
        info2.setResult("{res2}");

        Query query = new Query(LineIterationResult.class.getSimpleName());
        assertEquals(2, DatastoreServiceFactory.getDatastoreService().prepare(query).countEntities());
        //assertEquals("{res1}", dao.get("0").getResult());
        //assertEquals("{res2}", dao.get("1").getResult());
    }

    private LineIterationResult newData(int i){
        return new LineIterationResult("victoria", "{}", new Date());
    }

    public void testSaveMultiThreaded() throws Exception{
        final LineIterationResultDao dao = new LineIterationResultDao();
        int datas = 1;
        int threads = 2;
        final List<LineIterationResult> entries = Lists.newArrayList();

        for(int i=0; i<datas; i++){
            LineIterationResult iterationResult = newData(i);
            entries.add(iterationResult);
            dao.create(iterationResult);
        }

        Query query = new Query(LineIterationResult.class.getSimpleName());
        assertEquals(datas, DatastoreServiceFactory.getDatastoreService().prepare(query).countEntities());

        class Runner implements Runnable{

            final String newValue;
            final LineIterationResult existingData;

            Runner(String newValue, LineIterationResult existingId) {
                this.newValue = newValue;
                this.existingData = existingId;
                try {
                    new LocalServiceHarness().setUp();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            public void run() {
                existingData.setResult(newValue);
                dao.update(existingData);
            }
        }

        List<Thread> threadList = Lists.newArrayList();

        for(int i=0; i<threads; i++){
            Thread t = new Thread(new Runner("{res"+i+"}", entries.get(0)));
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    System.out.println("exception is: "+e.getMessage());
                    e.printStackTrace();
                }
            });
            threadList.add(t);
            t.start();
        }

        Thread.sleep(2000);
    }
//
//    public void testDelete() {
//        ContactInfoDAO dao = new ContactInfoDAO();
//        ContactInfo info1 = new ContactInfo("John", "Doe", "650-555-5555");
//        dao.addContact(info1);
//
//        dao.deleteContact(info1);
//        Query query = new Query(ContactInfo.class.getSimpleName());
//        assertEquals(0, DatastoreServiceFactory.getDatastoreService().prepare(query).countEntities());
//    }
}
