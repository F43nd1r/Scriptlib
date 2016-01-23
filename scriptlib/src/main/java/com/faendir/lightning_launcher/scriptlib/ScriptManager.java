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
@SuppressWarnings("unused")
public final class ScriptManager {
    private ScriptManager() {
    }

    static Logger logger = new Logger();

    static final String CODE = "code";
    static final String NAME = "name";
    static final String FLAGS = "flags";
    static final String LOADED_SCRIPT_ID = "loadedScriptId";
    static final String LISTENER_ID = "listenerId";
    static final String FORCE_UPDATE = "forceUpdate";
    static final String SERVICE_INTENT = "intent";
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

    private static final Map<Integer, Listener> LISTENERS = new HashMap<>();
    private static int nextListenerIndex = 0;

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
        logger.log("LoadScript call");
        LISTENERS.put(nextListenerIndex, listener);
        Intent intent = new Intent(context, ScriptActivity.class);
        intent.putExtra(CODE, code);
        intent.putExtra(NAME, name);
        intent.putExtra(FLAGS, flags);
        intent.putExtra(LISTENER_ID, nextListenerIndex);
        intent.putExtra(FORCE_UPDATE, forceUpdate);
        nextListenerIndex++;
        logger.log("Starting Activity for communication...");
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
        logger.log("Loading script from raw resource");
        InputStream inputStream = context.getResources().openRawResource(codeResourceId);
        byte[] bytes = new byte[1024];
        StringBuilder builder = new StringBuilder(inputStream.available());
        int count;
        while ((count = inputStream.read(bytes)) > 0){
            builder.append(new String(bytes,0,count));
        }
        inputStream.close();
        String code = builder.toString();
        loadScript(context, code, name, flags, forceUpdate, listener);
    }

    static void respondTo(int listenerId, int scriptId) {
        logger.log("Received positive response (ID = " + scriptId + ")");
        logger.log("Forwarding response to caller...");
        Listener listener = LISTENERS.get(listenerId);
        if (listener != null) listener.onLoadFinished(scriptId);
    }

    static void notifyError(int listenerId) {
        logger.log("Received error");
        logger.log("Notifying caller of Error...");
        Listener listener = LISTENERS.get(listenerId);
        if (listener != null) listener.onError();
    }

    static void updateConfirmation(final int listenerId, @NonNull final Intent intent) {
        logger.log("Received request to confirm Update");
        logger.log("Trying to ask Caller whether to update or not...");
        Listener listener = LISTENERS.get(listenerId);
        if (listener != null) listener.confirmUpdate(new UpdateCallback() {
            @Override
            public void callback(@NonNull Context context, boolean update) {
                logger.log("caller responded - force Update: " + update);
                if (update) {
                    logger.log("Performing Update by loading again...");
                    loadScript(context, intent.getStringExtra(CODE), intent.getStringExtra(NAME), (int) intent.getDoubleExtra(FLAGS, 0), true, LISTENERS.get(listenerId));
                }
            }
        });
    }


    static void loadScriptInternal(@NonNull final Activity context, int listenerId, String code, String name, int flags, boolean forceUpdate) {
        logger.log("Resolving service...");
        Intent intent = new Intent(INTENT);
        ResolveInfo info = context.getPackageManager().resolveService(intent, 0);
        if (info == null) {
            logger.log("Service not resolved: Repository Importer seems to be missing");
            logger.log("Notifying caller of Error");
            Listener listener = LISTENERS.get(listenerId);
            if (listener != null) listener.onError();
            if (askForInstallation) {
                logger.log("Asking user to install...");
                new AlertDialog.Builder(context)
                        .setTitle("Repository Importer missing")
                        .setMessage("This action requires the Repository Importer to be installed. Do you wish to install it?")
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(@NonNull DialogInterface dialogInterface, int i) {
                                logger.log("User denied install");
                                dialogInterface.dismiss();
                            }
                        })
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(@NonNull DialogInterface dialogInterface, int i) {
                                logger.log("User agreed to install");
                                dialogInterface.dismiss();
                                logger.log("Forwarding to Play Store...");
                                context.startActivity(new Intent(Intent.ACTION_VIEW, URI));
                            }
                        })
                        .show();
            }
        } else {
            logger.log("Service resolved");
            intent.setClassName(info.serviceInfo.packageName, info.serviceInfo.name);
            intent.putExtra(CODE, code);
            intent.putExtra(NAME, name);
            intent.putExtra(FLAGS, flags);
            intent.putExtra(RECEIVER, context.getComponentName().flattenToString());
            intent.putExtra(FORCE_UPDATE, forceUpdate);
            logger.log("Calling Repository Importer...");
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
        logger.log("runScript call");
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(COMPONENTNAME);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("a", 35);
        i.putExtra("d", id + "/" + (data != null ? data : ""));
        i.putExtra(BACKGROUND, background);
        sendIntentToLauncher(context, i);
    }

    /**
     * Runs a script in LL (LL opens in the process)
     *
     * @param context Context of calling class
     * @param id      ID of the script, usually as returned by a {@link com.faendir.lightning_launcher.scriptlib.ScriptManager.Listener Listener}
     * @param data    Additional Data passed to the script (Returned by LL.getEvent().getData()). may be null or empty
     */
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
        logger.log("Sending intent to Launcher...");
        if (intent.getPackage() == null) {
            intent.setPackage(COMPONENTNAME.getPackageName());
            logger.log("Added missing package name");
        }
        if (intent.getComponent() == null || intent.getComponent().getClassName() == null) {
            ComponentName componentName = new ComponentName(intent.getPackage(), COMPONENTNAME.getClassName());
            intent.setComponent(componentName);
            logger.log("Added missing class");
        }
        logger.log("Resolving service...");
        Intent service = new Intent(INTENT);
        ResolveInfo info = context.getPackageManager().resolveService(service, 0);
        if (info != null) {
            logger.log("Service resolved");
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(info.serviceInfo.packageName, 0);
                if (packageInfo.versionCode >= 22) {
                    logger.log("Service version high enough: Script can be run in background");
                    service.setClassName(info.serviceInfo.packageName, info.serviceInfo.name);
                    service.putExtra(FORWARD, intent);
                    logger.log("Forwarding...");
                    Intent activity = new Intent(context, ScriptActivity.class);
                    activity.putExtra(SERVICE_INTENT, service);
                    context.startActivity(activity);
                    return;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        logger.log("Service not resolved or too low version: Script can NOT be run in background");
        //legacy and fallback
        logger.log("Running script...");
        context.startActivity(intent);
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
        askForInstallation = value;
        logger.log("ask for installation mode set to: " + value);
    }

    /**
     * replace the built in Logger with a custom one
     *
     * @param logger the logger which should replace the current one
     */
    public static void replaceLogger(Logger logger) {
        ScriptManager.logger = logger;
        ScriptManager.logger.log("Logger replaced");
    }

    /**
     * A Listener which gets called when the load process is completed. Used to retrieve the Id of the loaded Script
     */
    public abstract static class Listener {
        public abstract void onLoadFinished(int id);

        public void onError() {
            logger.log("Caller did not implement onError. Error was ignored");
        }

        public void confirmUpdate(UpdateCallback callback) {
            throw new UnsupportedOperationException("Implement confirmUpdate in your listener or set the forceUpdate flag to true");
        }
    }

    /**
     * a Callback that is run if a script does already exists in the launcher and forceUpdate is false.
     */
    public abstract static class UpdateCallback {
        public abstract void callback(Context context, boolean update);
    }

    /**
     * A logger which outputs to androids default console if in debug mode
     */
    public static class Logger {
        private boolean debug = false;

        final void log(String msg) {
            if (debug) {
                Log.d("[SCRIPTLIB]", msg);
            }
        }

        void setDebug(boolean debug) {
            this.debug = debug;
        }

        boolean getDebug() {
            return debug;
        }
    }
}
