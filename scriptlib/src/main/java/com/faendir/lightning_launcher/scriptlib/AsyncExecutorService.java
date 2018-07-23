package com.faendir.lightning_launcher.scriptlib;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.faendir.lightning_launcher.scriptlib.executor.Executor;
import com.trianguloy.llscript.repository.aidl.ILightningService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 25.07.2016.
 *
 * @author F43nd1r
 */

public class AsyncExecutorService {
    @NonNull
    private final Context context;
    private final ServiceManager2 serviceManager;
    private final ExceptionHandler exceptionHandler;
    private final Logger logger;
    private final List<ExecutorWithCallback<?>> list;
    private boolean keepAlive;
    private ILightningService lightningService;

    public AsyncExecutorService(@NonNull Context context, @NonNull ServiceManager2 serviceManager, ExceptionHandler exceptionHandler, Logger logger) {
        this.context = context;
        this.serviceManager = serviceManager;
        this.exceptionHandler = exceptionHandler;
        this.logger = logger;
        list = new ArrayList<>();
        keepAlive = false;
    }

    public void start() {
        if(lightningService != null && lightningService.asBinder().isBinderAlive()){
            next();
        }else {
            serviceManager.bind(exceptionHandler, result -> {
                lightningService = result;
                next();
            });
        }
    }

    public AsyncExecutorService add(@NonNull Executor<?> executor) {
        return add(executor, null);
    }

    public <T> AsyncExecutorService add(@NonNull Executor<T> executor, @Nullable ResultCallback<T> callback) {
        list.add(new ExecutorWithCallback<>(executor, callback));
        return this;
    }

    public AsyncExecutorService setKeepAliveAfterwards(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public void unbind() {
        logger.log("unbind");
        serviceManager.unbind();
    }

    private void next() {
        if (!list.isEmpty()) {
            execute(list.get(0));
        } else if (!keepAlive) {
            serviceManager.unbind();
        }
    }

    private <T> void execute(final ExecutorWithCallback<T> entry) {
        entry.getExecutor().execute(context, lightningService, exceptionHandler, logger, result -> {
            entry.getResultCallback().onResult(result);
            list.remove(entry);
            next();
        });
    }

    private static class ExecutorWithCallback<T> {
        private final Executor<T> executor;
        private final ResultCallback<T> resultCallback;

        private ExecutorWithCallback(Executor<T> executor, ResultCallback<T> resultCallback) {
            this.executor = executor;
            this.resultCallback = resultCallback;
        }

        Executor<T> getExecutor() {
            return executor;
        }

        ResultCallback<T> getResultCallback() {
            return resultCallback;
        }
    }
}
