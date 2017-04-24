package com.faendir.lightning_launcher.scriptlib.executor;

import android.content.Context;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.faendir.lightning_launcher.scriptlib.ExceptionHandler;
import com.faendir.lightning_launcher.scriptlib.Logger;
import com.faendir.lightning_launcher.scriptlib.ResultCallback;
import com.trianguloy.llscript.repository.aidl.ILightningService;

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

    public ScriptExecutor setBackground(boolean background) {
        this.background = background;
        return this;
    }

    public ScriptExecutor setData(String data) {
        this.data = data;
        return this;
    }

    @Override
    public void execute(@NonNull Context context, @NonNull ILightningService lightningService,
                        @NonNull ExceptionHandler exceptionHandler, @NonNull Logger logger, @NonNull ResultCallback<Void> listener) {
        if (scriptId < 0)
            logger.warn("Running script with negative id. Are you sure this is what you want to do?");
        try {
            logger.log("run script " + scriptId);
            lightningService.runScript(scriptId, data, background);
            listener.onResult(null);
        } catch (RemoteException e) {
            exceptionHandler.onException(e);
        }
    }
}
