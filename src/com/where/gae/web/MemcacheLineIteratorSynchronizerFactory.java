package com.where.gae.web;

import com.where.web.LineIteratorSynchronizerFactory;
import com.where.web.LineIteratorSynchronizer;
import com.where.domain.alg.LineIterator;


public class MemcacheLineIteratorSynchronizerFactory implements LineIteratorSynchronizerFactory {
    public LineIteratorSynchronizer build(LineIterator branchIterator) {
        return new MemcacheLineIteratorSynchronizer(branchIterator);
    }
}