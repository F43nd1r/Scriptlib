package com.faendir.lightning_launcher.scriptlib.executor;

import android.content.Context;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import com.faendir.lightning_launcher.scriptlib.ExceptionHandler;
import com.faendir.lightning_launcher.scriptlib.Logger;
import com.faendir.lightning_launcher.scriptlib.ResultCallback;
import com.faendir.lightning_launcher.scriptlib.exception.FailureException;
import com.trianguloy.llscript.repository.aidl.Failure;
import com.trianguloy.llscript.repository.aidl.IImportCallback;
import com.trianguloy.llscript.repository.aidl.ILightningService;
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

    @Override
    public void execute(@NonNull final Context context, @NonNull final ILightningService lightningService,
                        @NonNull final ExceptionHandler exceptionHandler, @NonNull final Logger logger, @NonNull final ResultCallback<Integer> listener) {
        try {
            logger.log("Importing into LL...");
            lightningService.importScript(script, forceUpdate, new IImportCallback.Stub() {

                @Override
                public void onFinish(int scriptId) throws RemoteException {
                    listener.onResult(scriptId);
                    logger.log("Import finished");
                    if(runScript) {
                        new ScriptExecutor(scriptId)
                                .setBackground(background)
                                .setData(data)
                                .execute(context, lightningService, exceptionHandler, logger, new ResultCallback<Void>() {
                                    @Override
                                    public void onResult(Void result) {
                                    }
                                });
                    }
                }

                @Override
                public void onFailure(Failure failure) throws RemoteException {
                    exceptionHandler.onException(new FailureException(failure, new FailureException.Retry() {
                        @Override
                        public void retry() {
                            setForceUpdate(true);
                            execute(context, lightningService, exceptionHandler, logger, listener);
                        }
                    }));
                }
            });
        } catch (SecurityException e) {
            exceptionHandler.onException(e);
            logger.log("SecurityException when calling service.");
        } catch (RemoteException e) {
            exceptionHandler.onException(e);
        }
    }
}
