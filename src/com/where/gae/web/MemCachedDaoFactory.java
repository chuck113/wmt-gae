package com.where.gae.web;

import com.where.domain.*;

/**
 */
public class MemCachedDaoFactory implements DaoFactory {

    private final DaoFactory delegate;

    public static MemCachedDaoFactory buildFrom(DaoFactory delegate){
        return new MemCachedDaoFactory(delegate);
    }

    private MemCachedDaoFactory(DaoFactory delegate){
        this.delegate = delegate;
    }

    public BranchDao getBranchDao() {
        return delegate.getBranchDao();
    }

    public BranchStopDao getBranchStopDao() {
        return delegate.getBranchStopDao();
    }

    public LineDao getLineDao() {
        return delegate.getLineDao();
    }


}
