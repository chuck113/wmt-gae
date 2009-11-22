package com.where.gae.data;

import javax.jdo.PersistenceManagerFactory;
import javax.jdo.JDOHelper;

/**
 * @author Charles Kubicek
 */
public class PMF {

    private static final PersistenceManagerFactory pmfInstance = JDOHelper.getPersistenceManagerFactory("transactions-optional");

    private PMF() {}

    public static PersistenceManagerFactory get() {
        return pmfInstance;
    }
}