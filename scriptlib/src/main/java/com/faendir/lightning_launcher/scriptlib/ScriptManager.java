package com.faendir.lightning_launcher.scriptlib;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.StyleRes;
import android.util.Log;

import com.trianguloy.llscript.repository.aidl.Script;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Manages scripts in LL
 *
 * @author Lukas Morawietz
 */
@SuppressWarnings("unused")
public final class ScriptManager {

    private ScriptManager() {
    }

    static Logger logger = new Logger();

    private static final ResponseManager RESPONSE_MANAGER = new ResponseManager();
    private static final LegacyManager LEGACY_MANAGER = new LegacyManager(RESPONSE_MANAGER);

    /* Public API */

    /**
     * loads (or updates) a script into LL
     *
     * @param context  Context of calling class
     * @param script   the script
     * @param listener Gets called when the load process is completed. Has to implement {@link Listener#onLoadFinished(int)}
     */
    public static void loadScript(@NonNull Context context, @NonNull Script script, @NonNull Listener listener) {
        loadScript(context, script, listener, true);
    }

    /**
     * loads (or updates) a script into LL
     *
     * @param context     Context of calling class
     * @param script      the script
     * @param listener    Gets called when the load process is completed. Has to implement {@link Listener#onLoadFinished(int)}
     * @param forceUpdate if false Listener has to implement {@link Listener#confirmUpdate(UpdateCallback)} to resolve conflicts!
     */
    public static void loadScript(@NonNull Context context, @NonNull Script script, @NonNull Listener listener, boolean forceUpdate) {
        logger.log("LoadScript call");
        ServiceManager serviceManager = ServiceManager.getInstance(context, RESPONSE_MANAGER);
        if (serviceManager.supports(ServiceManager.Feature.BIND)) {
            serviceManager.loadScript(context, script, listener, forceUpdate);
        } else if (serviceManager.serviceExists()) {
            LEGACY_MANAGER.loadScriptLegacy(context, script, forceUpdate, listener);
        } else {
            RESPONSE_MANAGER.noImporter(context, listener, null);
        }

    }

    /**
     * Runs a script in LL (LL opens in the process)
     *
     * @param context Context of calling class
     * @param id      ID of the script, usually as returned by a {@link Listener Listener}
     * @param data    Additional Data passed to the script (Returned by LL.getEvent().getData()). may be null or empty
     */
    public static void runScript(@NonNull Context context, int id, @Nullable String data) {
        runScript(context, id, data, false);
    }

    /**
     * Runs a script in LL
     *
     * @param context    Context of calling class
     * @param id         ID of the script, usually as returned by a {@link Listener Listener}
     * @param data       Additional Data passed to the script (Returned by LL.getEvent().getData()). may be null or empty
     * @param background if the script should be run in the background
     */
    public static void runScript(@NonNull Context context, int id, @Nullable String data, boolean background) {
        logger.log("runScript call");
        ServiceManager serviceManager = ServiceManager.getInstance(context, RESPONSE_MANAGER);
        if (serviceManager.supports(ServiceManager.Feature.BIND)) {
            serviceManager.runScript(context, id, data, background);
        } else {
            LEGACY_MANAGER.runScriptLegacy(context, id, data, background);
        }

    }

    /**
     * run a script without importing it and receive a result string
     *
     * @param context  a context
     * @param code     script code. last line has to be "return [some_string]"
     * @param listener gets called when the script was executed. Has to implement {@link Listener#onResult(String)}
     */
    public static void runScriptForResult(@NonNull Context context, @NonNull String code, Listener listener) {
        ServiceManager serviceManager = ServiceManager.getInstance(context, RESPONSE_MANAGER);
        if (serviceManager.supports(ServiceManager.Feature.BIND)) {
            serviceManager.runScriptForResult(context, code, listener);
        } else {
            logger.warn("The installed Repository Importer instance doesn't support #runScriptForResult");
            RESPONSE_MANAGER.outdatedImporter(context, listener, null);
        }
    }

    /**
     * run an action in LL
     *
     * @param context    a context
     * @param actionId   ID of the action as defined by LL. {@link Action}
     * @param data       optional data
     * @param background if the action should be executed in background (not all actions make sense in the background)
     */
    public static void runAction(@NonNull Context context, @Action int actionId, @Nullable String data, boolean background) {
        logger.log("runAction call");
        ServiceManager serviceManager = ServiceManager.getInstance(context, RESPONSE_MANAGER);
        if (serviceManager.supports(ServiceManager.Feature.BIND)) {
            serviceManager.runAction(context, actionId, data, background);
        } else {
            LEGACY_MANAGER.runAction(context, actionId, data, background);
        }
    }

    /**
     * enables extensive logging
     */
    public static void enableDebug() {
        logger.setDebug(true);
    }

    /**
     * set if the library should ask for the repository importer if it is missing (default is true)
     *
     * @param value the value
     */
    public static void askForRepositoryImporterInstallationIfMissing(boolean value) {
        RESPONSE_MANAGER.setAskForInstallation(value);
        logger.log("ask for installation mode set to: " + value);
    }

    /**
     * set if the library should toast if the permission was not granted (default is true)
     *
     * @param value the value
     */
    public static void toastIfPermissionNotGranted(boolean value) {
        RESPONSE_MANAGER.setToastIfPermissionNotGranted(value);
        logger.log("toast if permission not granted mode set to: " + value);
    }

    /**
     * replace the built in Logger with a custom one
     *
     * @param logger the logger which should replace the current one
     */
    public static void replaceLogger(@NonNull Logger logger) {
        ScriptManager.logger.log("Replacing Logger...");
        ScriptManager.logger = logger;
        ScriptManager.logger.log("Logger replaced");
    }

    /**
     * If a Dialog is shown, the default is to use a Dark theme
     *
     * @param use if a light theme should be used
     */
    public static void useLightDialogTheme(boolean use) {
        RESPONSE_MANAGER.setUseLightTheme(use);
    }

    /**
     * Use a custom theme if a dialog is shown
     *
     * @param theme the custom theme
     */
    public static void useCustomDialogTheme(@StyleRes int theme) {
        RESPONSE_MANAGER.setCustomTheme(theme);
    }

    /* Public classes */

    /**
     * A Listener which gets called when the load process is completed. Used to retrieve the Id of the loaded Script
     */
    public abstract static class Listener {
        public void onLoadFinished(int id) {
        }

        public void onResult(String result) {
        }

        public void onError(ErrorCode errorCode) {
            logger.warn("Caller did not implement onError. Error was ignored");
        }

        public void confirmUpdate(UpdateCallback callback) {
            throw new UnsupportedOperationException("Implement confirmUpdate in your listener or set the forceUpdate flag to true");
        }
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

    /**
     * a Callback that is run if a script does already exists in the launcher and forceUpdate is false.
     */
    public static class UpdateCallback {
        private final Script script;
        private final Listener listener;

        UpdateCallback(Script script, Listener listener) {
            this.script = script;
            this.listener = listener;
        }

        public void callback(Context context, boolean update) {
            logger.log("caller responded - force Update: " + update);
            if (update) {
                logger.log("Performing Update by loading again...");
                loadScript(context, script, listener, true);
            }
        }
    }


    /* Deprecated */

    /**
     * loads (or updates) a script into LL
     *
     * @param context  Context of calling class
     * @param code     Code of the script
     * @param name     Name of the script
     * @param flags    Flags of the script, has to be a sum of: 0=None, 1=Disabled, 2=AppMenu, 4=ItemMenu, 8=CustomMenu
     * @param listener Gets called when the load process is completed
     * @deprecated use {@link #loadScript(Context, Script, Listener)} instead
     */
    @Deprecated
    public static void loadScript(@NonNull Context context, @NonNull String code, @NonNull String name, int flags, @NonNull final Listener listener) {
        loadScript(context, new Script(code, name, flags), listener);
    }

    /**
     * loads (or updates) a script into LL
     *
     * @param context     Context of calling class
     * @param code        Code of the script
     * @param name        Name of the script
     * @param flags       Flags of the script, has to be a sum of: 0=None, 1=Disabled, 2=AppMenu, 4=ItemMenu, 8=CustomMenu
     * @param forceUpdate if false confirmUpdate gets called and asks for confirmation
     * @param listener    Gets called when the load process is completed
     * @deprecated use {@link #loadScript(Context, Script, Listener, boolean)} instead
     */
    @Deprecated
    public static void loadScript(@NonNull Context context, @NonNull final String code, @NonNull final String name, final int flags, final boolean forceUpdate, @NonNull final Listener listener) {
        loadScript(context, new Script(code, name, flags), listener, forceUpdate);
    }

    /**
     * loads (or updates) a script into LL
     *
     * @param context        Context of calling class
     * @param codeResourceId Resource ID for a raw file containing the script code
     * @param name           Name of the script
     * @param flags          Flags of the script, has to be a sum of: 0=None, 1=Disabled, 2=AppMenu, 4=ItemMenu, 8=CustomMenu
     * @param listener       Gets called when the load process is completed
     * @throws IOException if the resource could not be loaded
     * @deprecated use {@link #loadScript(Context, Script, Listener)} instead
     */
    @Deprecated
    public static void loadScript(@NonNull Context context, @RawRes int codeResourceId, @NonNull String name, int flags, @NonNull final Listener listener) throws IOException {
        loadScript(context, new Script(context, codeResourceId, name, flags), listener, true);
    }

    /**
     * loads (or updates) a script into LL
     *
     * @param context        Context of calling class
     * @param codeResourceId Resource ID for a raw file containing the script code
     * @param name           Name of the script
     * @param flags          Flags of the script, has to be a sum of: 0=None, 1=Disabled, 2=AppMenu, 4=ItemMenu, 8=CustomMenu
     * @param forceUpdate    if false confirmUpdate gets called and asks for confirmation
     * @param listener       Gets called when the load process is completed
     * @throws IOException if the resource could not be loaded
     * @deprecated use {@link #loadScript(Context, Script, Listener, boolean)} instead
     */
    @Deprecated
    public static void loadScript(@NonNull Context context, @RawRes int codeResourceId, @NonNull String name, int flags, boolean forceUpdate, @NonNull final Listener listener) throws IOException {
        loadScript(context, new Script(context, codeResourceId, name, flags), listener, forceUpdate);
    }

    /**
     * Sends any intent to LL
     * by default to the Dashboard activity.
     *
     * @param context Context of calling class
     * @param intent  The intent to send
     * @deprecated use {@link #runAction(Context, int, String, boolean)} instead
     */
    @Deprecated
    public static void sendIntentToLauncher(@NonNull Context context, @NonNull Intent intent) {
        LEGACY_MANAGER.sendIntentToLauncher(context, intent);
    }

}
