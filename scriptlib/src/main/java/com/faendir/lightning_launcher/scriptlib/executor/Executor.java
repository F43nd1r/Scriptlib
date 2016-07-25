package com.faendir.lightning_launcher.scriptlib.executor;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.faendir.lightning_launcher.scriptlib.ServiceManager;

/**
 * Created on 25.07.2016.
 *
 * @author F43nd1r
 */

public interface Executor<T> {
    @WorkerThread
    T execute(@NonNull ServiceManager serviceManager);
}
