package com.faendir.lightning_launcher.scriptlib.executor;

import android.content.Context;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import com.faendir.lightning_launcher.scriptlib.Action;
import com.faendir.lightning_launcher.scriptlib.ExceptionHandler;
import com.faendir.lightning_launcher.scriptlib.Logger;
import com.faendir.lightning_launcher.scriptlib.ResultCallback;
import com.trianguloy.llscript.repository.aidl.ILightningService;

/**
 * Executes an Action in LL
 *
 * @author F43nd1r
 * @since 25.07.2016
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

    @Override
    public void execute(@NonNull Context context, @NonNull ILightningService lightningService,
                        @NonNull ExceptionHandler exceptionHandler, @NonNull Logger logger, @NonNull ResultCallback<Void> listener) {
        try {
            lightningService.runAction(action, data, background);
            listener.onResult(null);
        } catch (RemoteException e) {
            exceptionHandler.onException(e);
        }
    }
}
