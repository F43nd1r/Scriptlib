package com.faendir.lightning_launcher.scriptlib.exception;

import com.trianguloy.llscript.repository.aidl.Failure;

/**
 * @author F43nd1r
 * @since 21.04.2017
 */
public class FailureException extends Exception {
    private final Failure failure;
    private final Retry retry;

    public FailureException(Failure failure) {
        this(failure,null);
    }

    public FailureException(Failure failure, Retry retry) {
        super("Action failed with reason " + failure.name());
        this.failure = failure;
        this.retry = retry;
    }

    public Failure getFailure() {
        return failure;
    }

    public Retry getRetry() {
        return retry;
    }

    public interface Retry{
        void retry();
    }
}
