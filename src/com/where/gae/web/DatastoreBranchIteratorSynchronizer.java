package com.where.gae.web;

import com.where.web.BranchIteratorSynchronizer;
import com.where.web.ResultTransformer;
import com.where.web.WmtProperties;
import com.where.gae.data.LineIterationResultDao;
import com.where.gae.data.LineIterationResult;
import com.where.domain.alg.BranchIterator;

import java.util.Date;
import java.util.Set;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;

/**
 * @author Charles Kubicek
 */
public class DatastoreBranchIteratorSynchronizer implements BranchIteratorSynchronizer {

    private final Logger LOG = Logger.getLogger(DatastoreBranchIteratorSynchronizer.class);
    private final LineIterationResultDao lineDao = new LineIterationResultDao();
    private final BranchIterator branchIterator;

    private static long VALIDITY_PERIOD = WmtProperties.DATA_VALIDITY_PERIOD_MS;

    public DatastoreBranchIteratorSynchronizer(BranchIterator branchIterator) {
        this.branchIterator = branchIterator;
    }

    /**
     * Really need some other process to do this before everything starts because the
     * datastore may endup with muliple bootstrap entries
     *
     * @param branch
     * @return
     */
    private LineIterationResult makeBootstrapBranchEntry(String branch) {
        LineIterationResult result = new LineIterationResult(branch, "{initialValue}", new Date(0));
        lineDao.create(result);
        LOG.debug("DatastoreBranchIteratorSynchronizer.getBranch created initial value for  " + branch);
        return result;
    }

    private boolean isResultTimeValid(LineIterationResult result) {
        return (result.getValidityTime().getTime() + VALIDITY_PERIOD) >= new Date().getTime();
    }

    private long randomLong() {
        return new Double(Math.random() * (10000000.0d)).longValue();
    }

    /**
     * If another process adds a new entry inbetween geting the latest line result and the cas check
     * this process will do the cas check on an older entry which will fail the cas check and return
     * the previous value which is fine.
     *
     * @param branch
     * @return
     */
    public String getBranch(String branch) {
        LineIterationResult result = lineDao.getLatestLineResult(branch);

        if (result == null) {
            result = makeBootstrapBranchEntry(branch);
        }

        if (isResultTimeValid(result)) {
            //LOG.debug("DatastoreBranchIteratorSynchronizer.getBranch returing result within validity period: "+result);
            return result.getResult();
        } else {
            //LOG.debug("DatastoreBranchIteratorSynchronizer.getBranch starting cas on resuld id: " + result);
            if (lineDao.casUpdate(result.getId(), 0, randomLong())) {
                LOG.warn("DatastoreBranchIteratorSynchronizer.getBranch STARTING WORK FOR " + branch);
                String resultJson = ResultTransformer.toJson(branchIterator.run(branch));
                //String resultJson = "result recorded at "+new Date().getTime();
                LineIterationResult newResult = new LineIterationResult(branch, resultJson, new Date());
                String newId = lineDao.create(newResult);
                //LOG.warn("DatastoreBranchIteratorSynchronizer.getBranch updated new result with id "+newId);

                return resultJson;
            } else {
                //LOG.debug("DatastoreBranchIteratorSynchronizer.getBranch returing result from cache after failed result was being produced");
                return result.getResult();
            }
        }
    }

    private void sleep(long time) {
        try {
            synchronized (this) {
                this.wait(time);
            }
        } catch (Exception e) {
            System.out.println("DatastoreBranchIteratorSynchronizer.sleep got " + e.getMessage());
        }
    }
}
