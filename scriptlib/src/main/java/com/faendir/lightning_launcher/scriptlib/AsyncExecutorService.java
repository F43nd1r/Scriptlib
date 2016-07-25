package com.faendir.lightning_launcher.scriptlib;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.faendir.lightning_launcher.scriptlib.exception.RepositoryImporterException;
import com.faendir.lightning_launcher.scriptlib.executor.Executor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created on 25.07.2016.
 *
 * @author F43nd1r
 */

public class AsyncExecutorService {
    private final ServiceManager serviceManager;
    private final LinkedHashMap<Executor, ResultCallback> map;
    private ExceptionHandler exceptionHandler;

    public AsyncExecutorService(@NonNull ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        map = new LinkedHashMap<>();
    }

    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serviceManager.bind();
                    for (Iterator<Map.Entry<Executor, ResultCallback>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry<Executor, ResultCallback> entry = iterator.next();
                        Object result = entry.getKey().execute(serviceManager);
                        if (entry.getValue() != null) {
                            //noinspection unchecked
                            entry.getValue().onResult(result);
                        }
                        iterator.remove();
                    }
                    serviceManager.unbind();
                } catch (RepositoryImporterException e) {
                    if (exceptionHandler != null) {
                        exceptionHandler.onException(e);
                    }
                }

            }
        }).start();
    }

    public AsyncExecutorService add(@NonNull Executor executor) {
        return add(executor, null);
    }

    public <T> AsyncExecutorService add(@NonNull Executor<T> executor, @Nullable ResultCallback<T> callback) {
        map.put(executor, callback);
        return this;
    }

    public AsyncExecutorService setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        return this;
    }

    public interface ExceptionHandler {
        void onException(RepositoryImporterException e);
    }

    /**
     * Created on 25.07.2016.
     *
     * @author F43nd1r
     */
    public interface ResultCallback<T> {
        void onResult(T result);
    }
}
