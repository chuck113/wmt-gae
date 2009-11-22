package com.where.gae.web;

import com.where.web.BranchIteratorSynchronizerFactory;
import com.where.web.BranchIteratorSynchronizer;
import com.where.domain.alg.BranchIterator;

/**
 * @author Charles Kubicek
 */
public class DatastoreBranchIteratorSynchronizerFactory implements BranchIteratorSynchronizerFactory {
    public BranchIteratorSynchronizer build(BranchIterator branchIterator) {
        return new DatastoreBranchIteratorSynchronizer(branchIterator);
    }
}
