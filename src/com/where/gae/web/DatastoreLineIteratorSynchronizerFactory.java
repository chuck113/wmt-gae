package com.where.gae.web;

import com.where.web.LineIteratorSynchronizerFactory;
import com.where.web.LineIteratorSynchronizer;
import com.where.domain.alg.BranchIterator;
import com.where.domain.alg.LineIterator;

/**
 * @author Charles Kubicek
 */
public class DatastoreLineIteratorSynchronizerFactory implements LineIteratorSynchronizerFactory {
    public LineIteratorSynchronizer build(LineIterator lineIterator) {
        return new DatastoreLineIteratorSynchronizer(lineIterator);
    }
}
