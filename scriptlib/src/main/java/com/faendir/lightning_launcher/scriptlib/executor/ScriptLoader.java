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
    private boolean runScript;
    private boolean background;
    private String data;

    public ScriptLoader(@NonNull Script script) {
        this.script = script;
        forceUpdate = true;
        runScript = false;
        background = false;
        data = null;
    }

    public ScriptLoader setForceUpdate(boolean forceUpdate) {
        this.forceUpdate = forceUpdate;
        return this;
    }

    public ScriptLoader setRunScript(boolean runScript) {
        this.runScript = runScript;
        return this;
    }

    public ScriptLoader setRunInBackground(boolean background) {
        this.background = background;
        return this;
    }

    public ScriptLoader setRunData(String data) {
        this.data = data;
        return this;
    }

    @WorkerThread
    @Override
    public Integer execute(@NonNull ServiceManager serviceManager) {
        int result = serviceManager.loadScript(script, forceUpdate);
        if (runScript) {
            serviceManager.runScript(result, data, background);
        }
        return result;
    }
}
