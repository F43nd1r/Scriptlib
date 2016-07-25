package com.faendir.lightning_launcher.scriptlib.executor;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.faendir.lightning_launcher.scriptlib.ServiceManager;

/**
 * Created on 25.07.2016.
 *
 * @author F43nd1r
 */

public class ScriptExecutor implements Executor<Void> {
    private final int scriptId;
    private String data;
    private boolean background;

    public ScriptExecutor(int scriptId) {
        this.scriptId = scriptId;
        data = null;
        background = false;
    }

    @WorkerThread
    @Override
    public Void execute(@NonNull ServiceManager serviceManager) {
        serviceManager.runScript(scriptId, data, background);
        return null;
    }

    public ScriptExecutor setBackground(boolean background) {
        this.background = background;
        return this;
    }

    public ScriptExecutor setData(String data) {
        this.data = data;
        return this;
    }
}
