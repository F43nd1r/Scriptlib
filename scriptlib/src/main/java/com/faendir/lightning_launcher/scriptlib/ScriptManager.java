package com.faendir.lightning_launcher.scriptlib;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages scripts in LL
 *
 * @author Lukas Morawietz
 */
final public class ScriptManager {
    private ScriptManager() {
    }

    static Logger LOGGER = new Logger();

    static final String CODE = "code";
    static final String NAME = "name";
    static final String FLAGS = "flags";
    static final String LOADED_SCRIPT_ID = "loadedScriptId";
    static final String LISTENER_ID = "listenerId";
    static final String FORCE_UPDATE = "forceUpdate";
    private static final String FORWARD = "forward";
    private static final String BACKGROUND = "background";

    static final String STATUS = "status";
    static final int STATUS_LAUNCHER_PROBLEM = 3;
    static final int STATUS_UPDATE_CONFIRMATION_REQUIRED = 2;
    static final int STATUS_OK = 1;

    private static final String RECEIVER = "receiver";
    private static final String INTENT = "net.pierrox.lightning_launcher.script.IMPORT";
    private static final Uri URI = Uri.parse("market://details?id=com.trianguloy.llscript.repository");
    private static final ComponentName COMPONENTNAME = ComponentName.unflattenFromString("net.pierrox.lightning_launcher_extreme/net.pierrox.lightning_launcher.activities.Dashboard");

    private static final Map<Integer, Listener> listeners = new HashMap<>();
    private static int nextListenerIndex = 0;

    private static boolean debug = false;
    private static boolean askForInstallation = true;

    /**
     * loads (or updates) a script into LL
     *
     * @param context  Context of calling class
     * @param code     Code of the script
     * @param name     Name of the script
     * @param flags    Flags of the script, has to be a sum of: 0=None, 1=Disabled, 2=AppMenu, 4=ItemMenu, 8=CustomMenu
     * @param listener Gets called when the load process is completed
     */
    public static void loadScript(@NonNull Context context, @NonNull String code, @NonNull String name, int flags, @NonNull final Listener listener) {
        loadScript(context, code, name, flags, true, listener);
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
     */
    public static void loadScript(@NonNull Context context, @NonNull String code, @NonNull String name, int flags, boolean forceUpdate, @NonNull final Listener listener) {
        LOGGER.log("LoadScript call");
        listeners.put(nextListenerIndex, listener);
        Intent intent = new Intent(context, ScriptActivity.class);
        intent.putExtra(CODE, code);
        intent.putExtra(NAME, name);
        intent.putExtra(FLAGS, flags);
        intent.putExtra(LISTENER_ID, nextListenerIndex);
        intent.putExtra(FORCE_UPDATE, forceUpdate);
        nextListenerIndex++;
        LOGGER.log("Starting Activity for communication...");
        context.startActivity(intent);
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
     */
    public static void loadScript(@NonNull Context context, @RawRes int codeResourceId, @NonNull String name, int flags, @NonNull final Listener listener) throws IOException {
        loadScript(context, codeResourceId, name, flags, true, listener);
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
     */
    public static void loadScript(@NonNull Context context, @RawRes int codeResourceId, @NonNull String name, int flags, boolean forceUpdate, @NonNull final Listener listener) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(codeResourceId);
        byte[] bytes = new byte[inputStream.available()];
        inputStream.read(bytes);
        inputStream.close();
        String code = new String(bytes);
        loadScript(context, code, name, flags, forceUpdate, listener);
    }

    static void respondTo(int listenerId, int scriptId) {
        LOGGER.log("Received positive response");
        LOGGER.log("Forwarding response to caller...");
        listeners.get(listenerId).OnLoadFinished(scriptId);
    }

    static void notifyError(int listenerId) {
        LOGGER.log("Received error");
        LOGGER.log("Notifying caller of Error...");
        listeners.get(listenerId).OnError();
    }

    static void updateConfirmation(final int listenerId, final Intent intent) {
        LOGGER.log("Received request to confirm Update");
        LOGGER.log("Trying to ask Caller whether to update or not...");
        listeners.get(listenerId).confirmUpdate(new UpdateCallback() {
            @Override
            public void callback(Context context, boolean update) {
                LOGGER.log("caller responded - force Update: " + update);
                if (update) {
                    LOGGER.log("Performing Update by loading again...");
                    loadScript(context, intent.getStringExtra(CODE), intent.getStringExtra(NAME), (int) intent.getDoubleExtra(FLAGS, 0), true, listeners.get(listenerId));
                }
            }
        });
    }


    static void loadScriptInternal(final Activity context, int listenerId, String code, String name, int flags, boolean forceUpdate) {
        LOGGER.log("Resolving service...");
        Intent intent = new Intent(INTENT);
        ResolveInfo info = context.getPackageManager().resolveService(intent, 0);
        if (info == null) {
            LOGGER.log("Service not resolved: Repository Importer seems to be missing");
            LOGGER.log("Notifying caller of Error");
            listeners.get(listenerId).OnError();
            if (askForInstallation) {
                LOGGER.log("Asking user to install...");
                new AlertDialog.Builder(context)
                        .setTitle("Repository Importer missing")
                        .setMessage("This action requires the Repository Importer to be installed. Do you wish to install it?")
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                LOGGER.log("User denied install");
                                dialogInterface.dismiss();
                            }
                        })
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                LOGGER.log("User agreed to install");
                                dialogInterface.dismiss();
                                LOGGER.log("Forwarding to Play Store...");
                                context.startActivity(new Intent(Intent.ACTION_VIEW, URI));
                            }
                        })
                        .show();
            }
        } else {
            LOGGER.log("Service resolved");
            intent.setClassName(info.serviceInfo.packageName, info.serviceInfo.name);
            intent.putExtra(CODE, code);
            intent.putExtra(NAME, name);
            intent.putExtra(FLAGS, flags);
            intent.putExtra(RECEIVER, context.getComponentName().flattenToString());
            intent.putExtra(FORCE_UPDATE, forceUpdate);
            LOGGER.log("Calling Repository Importer...");
            context.startService(intent);
        }
    }

    /**
     * Runs a script in LL (LL opens in the process)
     *
     * @param context    Context of calling class
     * @param id         ID of the script, usually as returned by a {@link com.faendir.lightning_launcher.scriptlib.ScriptManager.Listener Listener}
     * @param data       Additional Data passed to the script (Returned by LL.getEvent().getData()). may be null or empty
     * @param background Whether or not the script should be run in the background
     */
    public static void runScript(@NonNull Context context, int id, @Nullable String data, boolean background) {
        LOGGER.log("runScript call");
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(COMPONENTNAME);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("a", 35);
        i.putExtra("d", id + "/" + (data != null ? data : ""));
        i.putExtra(BACKGROUND, background);
        LOGGER.log("Running script...");
        sendIntentToLauncher(context, i);
    }

    public static void runScript(@NonNull Context context, int id, @Nullable String data) {
        runScript(context, id, data, false);
    }

    /**
     * Sends any intent to LL
     * by default to the Dashboard activity.
     *
     * @param context Context of calling class
     * @param intent  The intent to send
     */
    public static void sendIntentToLauncher(@NonNull Context context, @NonNull Intent intent) {
        if (intent.getPackage() == null) {
            intent.setPackage(COMPONENTNAME.getPackageName());
        }
        if (intent.getComponent() == null || intent.getComponent().getClassName() == null) {
            ComponentName componentName = new ComponentName(intent.getPackage(), COMPONENTNAME.getClassName());
            intent.setComponent(componentName);
        }
        Intent service = new Intent(INTENT);
        ResolveInfo info = context.getPackageManager().resolveService(service, 0);
        if (info != null) {
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(info.serviceInfo.packageName, 0);
                if (packageInfo.versionCode >= 22) {
                    service.setClassName(info.serviceInfo.packageName, info.serviceInfo.name);
                    service.putExtra(FORWARD, intent);
                    context.startService(service);
                    return;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        //legacy and fallback
        context.startActivity(intent);
    }

    /**
     * enables extensive logging
     */
    public static void enableDebug() {
        debug = true;
    }

    /**
     * set if the library should ask for the repository importer if it is missing (default is true)
     *
     * @param value the value
     */
    public static void askForRepositoryImporterInstallationIfMissing(boolean value) {
        askForInstallation = value;
    }

    /**
     * replace the built in Logger with a custom one (also enables debugging)
     *
     * @param logger the logger which should replace the current one
     */
    public static void replaceLogger(Logger logger) {
        LOGGER = logger;
        enableDebug();
    }

    /**
     * A Listener which gets called when the load process is completed. Used to retrieve the Id of the loaded Script
     */
    public static abstract class Listener {
        public abstract void OnLoadFinished(int id);

        public void OnError() {
            LOGGER.log("Caller did not implement onError. Error was ignored");
        }

        public void confirmUpdate(UpdateCallback callback) {
            throw new UnsupportedOperationException("Implement confirmUpdate in your listener or set the forceUpdate flag to true");
        }
    }

    /**
     * a Callback that is run if a script does already exists in the launcher and forceUpdate is false.
     */
    public static abstract class UpdateCallback {
        public abstract void callback(Context context, boolean update);
    }

    /**
     * A logger which outputs to androids default console
     */
    public static class Logger {

        void log(String msg) {
            if (debug)
                Log.d("[SCRIPTLIB]", msg);
        }
    }
}
