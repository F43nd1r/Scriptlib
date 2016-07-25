package com.faendir.lightning_launcher.scriptlib.executor;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.faendir.lightning_launcher.scriptlib.ServiceManager;
import com.trianguloy.llscript.repository.aidl.Script;

/**
 * Created on 25.07.2016.
 *
 * @author F43nd1r
 */

public class ScriptLoader implements Executor<Integer> {
    private final Script script;
    private boolean forceUpdate;

    public ScriptLoader(@NonNull Script script) {
        this.script = script;
        forceUpdate = true;
    }

    public ScriptLoader setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
        return this;
    }

    @WorkerThread
    @Override
    public Integer execute(@NonNull ServiceManager serviceManager) {
        return serviceManager.loadScript(script, forceUpdate);
    }
}
