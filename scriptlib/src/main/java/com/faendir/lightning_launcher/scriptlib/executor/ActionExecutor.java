package com.faendir.lightning_launcher.scriptlib.executor;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.faendir.lightning_launcher.scriptlib.Action;
import com.faendir.lightning_launcher.scriptlib.ServiceManager;

/**
 * Created on 25.07.2016.
 *
 * @author F43nd1r
 */

public class ActionExecutor implements Executor<Void> {
    @Action
    private final int action;
    private String data;
    private boolean background;

    public ActionExecutor(@Action int action) {
        this.action = action;
        data = null;
        background = false;
    }

    public ActionExecutor setData(String data) {
        this.data = data;
        return this;
    }

    public ActionExecutor setBackground(boolean background) {
        this.background = background;
        return this;
    }

    @WorkerThread
    @Override
    public Void execute(@NonNull ServiceManager serviceManager) {
        serviceManager.runAction(action, data, background);
        return null;
    }
}
