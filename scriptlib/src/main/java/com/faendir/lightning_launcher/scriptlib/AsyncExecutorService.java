package com.faendir.lightning_launcher.scriptlib;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
    private ResultCallback<BindResult> bindResultHandler;
    private boolean keepAlive;

    public AsyncExecutorService(@NonNull ServiceManager serviceManager, ResponseManager responseManager) {
        this.serviceManager = serviceManager;
        map = new LinkedHashMap<>();
        bindResultHandler = new DefaultBindResultHandler(responseManager);
        keepAlive = false;
    }

    public void start() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BindResult r = serviceManager.bind();
                if(bindResultHandler != null) bindResultHandler.onResult(r);
                if(r == BindResult.OK) {
                    for (Iterator<Map.Entry<Executor, ResultCallback>> iterator = map.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry<Executor, ResultCallback> entry = iterator.next();
                        Object result = entry.getKey().execute(serviceManager);
                        if (entry.getValue() != null) {
                            //noinspection unchecked
                            entry.getValue().onResult(result);
                        }
                        iterator.remove();
                    }
                    if(!keepAlive) {
                        serviceManager.unbind();
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

    public AsyncExecutorService setKeepAliveAfterwards(boolean keepAlive){
        this.keepAlive = keepAlive;
        return this;
    }

    public AsyncExecutorService setBindResultHandler(ResultCallback<BindResult> bindResultHandler) {
        this.bindResultHandler = bindResultHandler;
        return this;
    }

}
