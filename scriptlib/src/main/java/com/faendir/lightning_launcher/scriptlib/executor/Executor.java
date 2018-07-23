package com.faendir.lightning_launcher.scriptlib.executor;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import com.faendir.lightning_launcher.scriptlib.ExceptionHandler;
import com.faendir.lightning_launcher.scriptlib.Logger;
import com.faendir.lightning_launcher.scriptlib.ResultCallback;
import com.trianguloy.llscript.repository.aidl.ILightningService;

/**
 * Created on 25.07.2016.
 *
 * @author F43nd1r
 */

public interface Executor<T> {
    @WorkerThread
    void execute(@NonNull Context context, @NonNull ILightningService lightningService,
                 @NonNull ExceptionHandler exceptionHandler, @NonNull Logger logger, @NonNull ResultCallback<T> listener);
}
