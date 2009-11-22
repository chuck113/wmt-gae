package com.where.gae.test;

import com.google.apphosting.api.ApiProxy;
import com.google.appengine.tools.development.ApiProxyLocalImpl;

import junit.framework.TestCase;

import java.io.File;

public class LocalServiceHarness {

    public void setUp() throws Exception {
        ApiProxy.setEnvironmentForCurrentThread(new TestEnvironment());
        ApiProxy.setDelegate(new ApiProxyLocalImpl(new File(".")){});
    }

    public void tearDown() throws Exception {
        // not strictly necessary to null these out but there's no harm either
        ApiProxy.setDelegate(null);
        ApiProxy.setEnvironmentForCurrentThread(null);
    }
}
