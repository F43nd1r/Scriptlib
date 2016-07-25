package com.faendir.lightning_launcher.scriptlib;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.faendir.lightning_launcher.scriptlib.exception.PermissionNotGrantedException;
import com.faendir.lightning_launcher.scriptlib.exception.RepositoryImporterException;
import com.faendir.lightning_launcher.scriptlib.exception.RepositoryImporterMissingException;
import com.faendir.lightning_launcher.scriptlib.exception.RepositoryImporterOutdatedException;
import com.faendir.lightning_launcher.scriptlib.executor.ActionExecutor;
import com.faendir.lightning_launcher.scriptlib.executor.DirectScriptExecutor;
import com.faendir.lightning_launcher.scriptlib.executor.Executor;
import com.faendir.lightning_launcher.scriptlib.executor.ScriptExecutor;
import com.faendir.lightning_launcher.scriptlib.executor.ScriptLoader;
import com.trianguloy.llscript.repository.aidl.Script;

import java.util.Map;

/**
 * Created on 18.06.2016.
 *
 * @author F43nd1r
 */

public class ScriptManager {

    static Logger logger = new Logger();
    private ResponseManager responseManager;
    private final ServiceManager serviceManager;

    public ScriptManager(Context context) {
        responseManager = new ResponseManager(context);
        serviceManager = new ServiceManager(context, responseManager);
    }

    @WorkerThread
    public void bind() throws RepositoryImporterException {
        logger.log("bind");
        try {
            serviceManager.bind();
        } catch (RepositoryImporterOutdatedException e) {
            responseManager.outdatedImporter();
            throw e;
        } catch (RepositoryImporterMissingException e) {
            responseManager.noImporter();
            throw e;
        } catch (PermissionNotGrantedException e) {
            responseManager.permissionNotGranted();
            throw e;
        }
    }

    public void unbind() {
        logger.log("unbind");
        serviceManager.unbind();
    }

    /* Public API */

    /**
     * loads (or updates) a script in LL
     *
     * @param script the script
     * @return the id of the loaded script
     */
    @WorkerThread
    public int loadScript(@NonNull Script script) {
        return loadScript(script, true);
    }

    /**
     * loads (or updates) a script in LL
     *
     * @param script      the script
     * @param forceUpdate if they script should be updated even if there is already a script with the same name
     * @return the id of the loaded script
     */
    @WorkerThread
    public int loadScript(@NonNull Script script, boolean forceUpdate) {
        logger.log("LoadScript");
        return new ScriptLoader(script).setForceUpdate(forceUpdate).execute(serviceManager);
    }

    /**
     * Runs a script in LL (LL opens in the process)
     *
     * @param id   ID of the script
     * @param data Additional Data passed to the script (Returned by LL.getEvent().getData()). may be null or empty
     */
    @WorkerThread
    public void runScript(int id, @Nullable String data) {
        runScript(id, data, false);
    }

    /**
     * Runs a script in LL
     *
     * @param id         ID of the script
     * @param data       Additional Data passed to the script (Returned by LL.getEvent().getData()). may be null or empty
     * @param background if the script should be run in the background
     */
    @WorkerThread
    public void runScript(int id, @Nullable String data, boolean background) {
        logger.log("runScript");
        new ScriptExecutor(id).setData(data).setBackground(background).execute(serviceManager);
    }

    /**
     * run a script without importing it and receive a result string
     *
     * @param code script code. last line has to be "return [some_string]"
     * @return the result string
     */
    @WorkerThread
    public String runScriptForResult(@NonNull String code) {
        logger.log("runScriptForResult");
        return new DirectScriptExecutor(code).execute(serviceManager);
    }

    /**
     * run a script without importing it and receive a result string.
     * Note that this is only a shortcut method for {@link #runScriptForResult(int, Map)} with one "data" entry
     *
     * @param code script code resource id. last line has to be "return [some_string]"
     * @param data this will be available in the script as variable "data" (not LL.getEvent().getData()!)
     * @return the result string
     */
    public String runScriptForResult(@RawRes int code, @Nullable String data) {
        return new DirectScriptExecutor(code).putVariable("data", data).execute(serviceManager);
    }

    /**
     * run a script without importing it and receive a result string
     *
     * @param code      script code resource id. last line has to be "return [some_string]"
     * @param variables a map of variable names and their values, which will be available in the script
     * @return the result string
     */
    String runScriptForResult(@RawRes int code, @NonNull Map<String, String> variables) {
        return new DirectScriptExecutor(code).putVariables(variables).execute(serviceManager);
    }

    /**
     * run an action in LL
     *
     * @param actionId   ID of the action as defined by LL. {@link Action}
     * @param data       optional data
     * @param background if the action should be executed in background (not all actions make sense in the background)
     */
    @WorkerThread
    public void runAction(@Action int actionId, @Nullable String data, boolean background) {
        logger.log("runAction");
        new ActionExecutor(actionId).setData(data).setBackground(background).execute(serviceManager);
    }

    @WorkerThread
    public <T> T execute(Executor<T> executor) {
        return executor.execute(serviceManager);
    }

    public AsyncExecutorService getAsyncExecutorService() {
        return new AsyncExecutorService(serviceManager);
    }

    /**
     * enables extensive logging
     */
    public void enableDebug() {
        logger.setDebug(true);
    }

    public ResponseManager getResponseManager() {
        return responseManager;
    }

    public void setResponseManager(ResponseManager responseManager) {
        this.responseManager = responseManager;
        serviceManager.setResponseManager(responseManager);
    }

    /**
     * replace the built in Logger with a custom one
     *
     * @param logger the logger which should replace the current one
     */
    public void replaceLogger(@NonNull Logger logger) {
        ScriptManager.logger.log("Replacing Logger...");
        ScriptManager.logger = logger;
        ScriptManager.logger.log("Logger replaced");
    }

    /**
     * A logger which outputs to androids default console if in debug mode
     */
    public static class Logger {
        private boolean debug = false;
        private static final String TAG = "[SCRIPTLIB]";

        final void log(String msg) {
            if (debug) {
                Log.d(TAG, msg);
            }
        }

        final void warn(String msg) {
            Log.w(TAG, msg);
        }

        public final void setDebug(boolean debug) {
            this.debug = debug;
        }

        public final boolean getDebug() {
            return debug;
        }
    }
}
