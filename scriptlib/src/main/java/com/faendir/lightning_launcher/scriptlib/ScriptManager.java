package com.faendir.lightning_launcher.scriptlib;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.trianguloy.llscript.repository.aidl.Script;

/**
 * Created by Lukas on 18.06.2016.
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
        try {
            serviceManager.bind();
        } catch (RepositoryImporterOutdatedException e) {
            responseManager.outdatedImporter();
            throw e;
        } catch (RepositoryImporterMissingException e) {
            responseManager.noImporter();
            throw e;
        }catch (PermissionNotGrantedException e){
            responseManager.permissionNotGranted();
            throw e;
        }
    }

    public void unbind(){
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
     * @param script the script
     * @return the id of the loaded script
     */
    @WorkerThread
    public int loadScript(@NonNull Script script, boolean forceUpdate) {
        logger.log("LoadScript");
        return serviceManager.loadScript(script, forceUpdate);
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
        serviceManager.runScript(id, data, background);
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
        return serviceManager.runScriptForResult(code);
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
        serviceManager.runAction(actionId, data, background);
    }

    /**
     * enables extensive logging
     */
    public void enableDebug() {
        logger.setDebug(true);
    }

    public ResponseManager getResponseManager(){
        return responseManager;
    }

    public void setResponseManager(ResponseManager responseManager) {
        this.responseManager = responseManager;
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
