package com.where.gae.web;

import com.where.web.LineIteratorSynchronizer;
import com.where.web.WmtProperties;
import com.where.web.JsonTransformer;
import com.where.gae.data.LineIterationResultDao;
import com.where.gae.data.LineIterationResult;
import com.where.domain.alg.LineIterator;

import java.util.Date;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;

/**
 * @author Charles Kubicek
 */
public class DatastoreLineIteratorSynchronizer implements LineIteratorSynchronizer {

    private final Logger LOG = Logger.getLogger(DatastoreLineIteratorSynchronizer.class);
    final LineIterationResultDao lineDao = new LineIterationResultDao();
    final LineIterator lineIterator;

    public static final long VALIDITY_PERIOD = WmtProperties.DATA_VALIDITY_PERIOD_MS;
    public static final String INITIAL_ENTRY = WmtProperties.INITIAL_POINTS_ENTRY;
    private final Set<String> validLines = WmtProperties.LINES_TO_ITERATE;
    static final String DROP_ALL = "dropAll";

    public DatastoreLineIteratorSynchronizer(LineIterator lineIterator) {
        this.lineIterator = lineIterator;
    }

    /**
     * Really need some other process to do this before everything starts because the
     * datastore may endup with muliple bootstrap entries
     *
     * @param line
     * @return
     */
    LineIterationResult makeBootstrapLineEntry(String line) {
        LineIterationResult result = new LineIterationResult(line, INITIAL_ENTRY, new Date(0));
        lineDao.create(result);
        LOG.debug("DatastoreLineIteratorSynchronizer.getLine created initial value for  " + line);
        return result;
    }

    boolean isResultTimeValid(LineIterationResult result) {
        return (result.getValidityTime().getTime() + VALIDITY_PERIOD) >= new Date().getTime();
    }

    long randomLong() {
        return new Double(Math.random() * (10000000.0d)).longValue();
    }

    boolean validateLine(String line) {
        return !StringUtils.isEmpty(line) && validLines.contains(line);
    }

    void dropAll() {
        lineDao.dropAll();
    }

    /**
     * If another process adds a new entry inbetween geting the latest line result and the cas check
     * this process will do the cas check on an older entry which will fail the cas check and return
     * the previous value which is fine.
     *
     * @param line
     * @return
     */
    public String getLine(String line) {
        if (DROP_ALL.equals(line)) {
            dropAll();
            return "{  \"dropAll\" : \"OK\"}";
        }
        if (!validateLine(line)) {
            return JsonTransformer.toJsonError("line '" + line + "' was invalid");
        }

        LineIterationResult result = lineDao.getLatestLineResult(line);

        if (result == null) {
            result = makeBootstrapLineEntry(line);
        }

        if (isResultTimeValid(result)) {
            return result.getResult();
        } else {
            return update(result, PostCommitWork.NULL_POST_COMMIT_WORK).getResult();
        }
    }

    LineIterationResult getLastLineResultRegardlessOfTimeValidity(String line) {
        LineIterationResult result = lineDao.getLatestLineResult(line);

        if (result == null) {
            result = makeBootstrapLineEntry(line);
        }

        return result;
    }

    public interface PostCommitWork {
        void doWork(LineIterationResult newResult);

        final PostCommitWork NULL_POST_COMMIT_WORK = new PostCommitWork() {
            public void doWork(LineIterationResult newResult) {
            }
        };
    }

    /**
     * @param result
     * @param postCommitWork may never be used, only invoked if the cas test has passed
     * @return
     * @throws Exception
     */
    LineIterationResult update(LineIterationResult result, PostCommitWork postCommitWork) {
        String line = result.getLine();
        //LOG.debug("DatastoreLineIteratorSynchronizer.getLine starting cas on resuld id: " + result);
        if (lineDao.casUpdate(result.getId(), 0, randomLong())) {
            LOG.warn("DatastoreLineIteratorSynchronizer.getLine STARTING WORK FOR " + line);
            String resultJson;
            try {
               resultJson = JsonTransformer.toJson(lineIterator.run(line));
            } catch (Exception e) {
                LOG.error("failed to parse: " + e.getMessage(), e);
                //resultJson = "{\"error\" : \"  " + e.getMessage() + "  \" }";
                resultJson = JsonTransformer.toJsonError(e.getMessage());
            }
            //gaeSleep(2000);
            //String resultJson = "result recorded at "+new Date();
            LineIterationResult newResult = new LineIterationResult(line, resultJson, new Date());
            lineDao.create(newResult);
            postCommitWork.doWork(newResult);
            lineDao.cleanUp(line, 5);
            //LOG.warn("DatastoreLineIteratorSynchronizer.getLine updated new result with id "+newId);

            return newResult;
        } else {
            //LOG.debug("DatastoreLineIteratorSynchronizer.getLine returing result from cache after failed result was being produced");
            return result;
        }
    }

    void gaeSleep(long time) {
        try {
            synchronized (this) {
                this.wait(time);
            }
        } catch (Exception e) {
            System.out.println("DatastoreLineIteratorSynchronizer.gaeSleep got " + e.getMessage());
        }
    }

}
