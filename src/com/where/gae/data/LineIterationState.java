package com.where.gae.data;

import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.PersistenceCapable;
import java.io.Serializable;
import java.util.Date;

/**
 * The object used to store cached results in memache, it is keyed by the
 * line name
 *
 * @author Charles Kubicek
 */
public class LineIterationState implements Serializable {

    private final LineIterationResult result;
    private boolean reItrationInProgress = false;

    public LineIterationState(LineIterationResult result) {
        this.result = result;
    }

    public void setReItrationInProgress(boolean reItrationInProgress) {
        this.reItrationInProgress = reItrationInProgress;
    }

    public LineIterationResult getResult() {
        return result;
    }

    public boolean isReItrationInProgress() {
        return reItrationInProgress;
    }

    @Override
    public String toString() {
        return "LineIterationState{" +
                "result=" + result +
                ", reItrationInProgress=" + reItrationInProgress +
                '}';
    }
}
