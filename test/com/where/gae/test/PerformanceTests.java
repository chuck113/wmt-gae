package com.where.gae.test;

import junit.framework.TestCase;

import java.net.URL;
import java.net.URLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.CyclicBarrier;
import java.util.List;

import org.apache.commons.io.IOUtils;

/**
 * Created by IntelliJ IDEA.
 * User: ck
 * Date: 17-Nov-2009
 * Time: 22:09:44
 * To change this template use File | Settings | File Templates.
 */
public class PerformanceTests extends TestCase {

    private String shorten(String st, int size){
        if(st == null)return "";
        else if(st.length() < size)return st ;
        else return st.substring(st.length()-size, st.length());
    }

    private String shortenAndRemoveEndOfLine(String st, int size){
        String shortened = shorten(st, size);
        return shortened.replace('\n', ' ' );
    }

    public void testKillIt()throws Exception{
        final String appHost = "ckconcurrent.appspot.com";
        final String localHost = "localhost:8080";
        final URL url = new URL("http://"+appHost+"/rest/branches/victoria");

        final int threads = 25;
        final int loops = 20;
        final long maxMillisBetweenRequests = 300;
        final long exactMillisBetweenRequests = 200;
        final int[] doneCount = new int[]{0};

        Runnable r = new Runnable(){
            public void run() {
                for(int i=0; i<loops; i++){
                    try {
                        URLConnection urlConnection = url.openConnection();
                        urlConnection.setReadTimeout(120 * 1000);
                        InputStream in = urlConnection.getInputStream();
                        String res = IOUtils.toString(in);
                        in.close();
                        System.out.println("PerformanceTests.run "+Thread.currentThread().getName()+" completed iteration "+i+": "+shortenAndRemoveEndOfLine(res, 60));
                        Thread.sleep(new Double(new Long(maxMillisBetweenRequests).doubleValue() * Math.random()).longValue());
                        //Thread.sleep(exactMillisBetweenRequests);
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }
                doneCount[0]++;
                System.out.println("PerformanceTests.run "+Thread.currentThread().getName()+" DONE");
            }
        };

        Executor executor = Executors.newFixedThreadPool(threads);
        for(int i=0; i<threads; i++){
            Thread.sleep(300);
            executor.execute(r);
        }
        while(doneCount[0] < threads){
            Thread.sleep(100);
        }
    }
}
