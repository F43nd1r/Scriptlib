package com.faendir.lightning_launcher.scriptlib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.StyleRes;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.trianguloy.llscript.repository.aidl.Failure;
import com.trianguloy.llscript.repository.aidl.IImportCallback;
import com.trianguloy.llscript.repository.aidl.ILightningService;
import com.trianguloy.llscript.repository.aidl.IResultCallback;
import com.trianguloy.llscript.repository.aidl.Script;

import java.io.IOException;
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

    private static final String INTENT = "net.pierrox.lightning_launcher.script.IMPORT";
    private static final Uri URI = Uri.parse("market://details?id=com.trianguloy.llscript.repository");
    private static final Uri ALTERNATIVE_URI = Uri.parse("https://play.google.com/store/apps/details?id=com.trianguloy.llscript.repository");
    private static final ComponentName COMPONENTNAME = ComponentName.unflattenFromString("net.pierrox.lightning_launcher_extreme/net.pierrox.lightning_launcher.activities.Dashboard");
    private static final int MINIMUM_BIND_VERSION = 30;

    private static boolean askForInstallation = true;
    private static boolean toastIfPermissionNotGranted = true;
    private static boolean useLightTheme = false;
    private static int customTheme = 0;

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
    public static void loadScript(@NonNull final Context context, @NonNull final Script script, @NonNull final Listener listener, final boolean forceUpdate) {
        logger.log("LoadScript call");
        Pair<ServiceInfo, Boolean> pair = getServiceWithMinimumVersion(context, MINIMUM_BIND_VERSION);
        if (pair.second) {
            bindServiceAndCall(context, pair.first, listener, new ServiceFunction() {
                @Override
                public void run(ILightningService service) {
                    try {
                        logger.log("importing into LL...");
                        service.importScript(script, forceUpdate, new IImportCallback.Stub() {
                            @Override
                            public void onFinish(final int scriptId) throws RemoteException {
                                new Handler(context.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.onLoadFinished(scriptId);
                                    }
                                });
                                finish();
                            }

                            @Override
                            public void onFailure(Failure failure) throws RemoteException {
                                switch (failure) {
                                    case SCRIPT_ALREADY_EXISTS:
                                        new Handler(context.getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                listener.confirmUpdate(new UpdateCallback(script, listener));
                                            }
                                        });
                                        break;
                                    case LAUNCHER_INVALID:
                                        notifyError(context, listener, ErrorCode.LAUNCHER_PROBLEM);
                                        break;
                                    case INVALID_INPUT:
                                        notifyError(context, listener, ErrorCode.INVALID_INPUT);
                                }
                                finish();
                            }
                        });
                    } catch (SecurityException e) {
                        logger.log("SecurityException when calling service.");
                        notifyError(context, listener, ErrorCode.SECURITY_EXCEPTION);
                        finish();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        finish();
                    }
                }
            });
        } else if (pair.first != null) {
            loadScriptLegacy(context, script, forceUpdate, listener);
        } else {
            noImporter(context, listener, null);
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
    public static void runScript(@NonNull Context context, final int id, @Nullable final String data, final boolean background) {
        logger.log("runScript call");
        Pair<ServiceInfo, Boolean> pair = getServiceWithMinimumVersion(context, MINIMUM_BIND_VERSION);
        if (pair.second) {
            bindServiceAndCall(context, pair.first, null, new ServiceFunction() {
                @Override
                public void run(ILightningService service) {
                    try {
                        logger.log("running script...");
                        service.runScript(id, data, background);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    finish();
                }
            });
        } else {
            runScriptLegacy(context, id, data, background);
        }

    }

    /**
     * run a script without importing it and receive a result string
     * @param context a context
     * @param code script code. last line has to be "return <some_string>"
     * @param listener gets called when the script was executed. Has to implement {@link Listener#onResult(String)}
     */
    public static void runScriptForResult(@NonNull Context context, @NonNull final String code, final Listener listener) {
        Pair<ServiceInfo, Boolean> pair = getServiceWithMinimumVersion(context, MINIMUM_BIND_VERSION);
        if (pair.second) {
            bindServiceAndCall(context, pair.first, listener, new ServiceFunction() {
                @Override
                void run(ILightningService service) {
                    try {
                        service.runScriptForResult(code, new IResultCallback.Stub() {
                            @Override
                            public void onResult(String result) throws RemoteException {
                                listener.onResult(result);
                            }

                            @Override
                            public void onFailure(Failure failure) throws RemoteException {
                                if (failure == Failure.EVAL_FAILED) {
                                    listener.onError(ErrorCode.EVAL_FAILED);
                                }
                            }
                        });
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * run an action in LL
     *
     * @param context    a context
     * @param actionId   ID of the action as defined by LL. See http://www.lightninglauncher.com/scripting/reference/api/reference/net/pierrox/lightning_launcher/script/api/EventHandler.html
     * @param data       optional data
     * @param background if the action should be executed in background (not all actions make sense in the background)
     */
    public static void runAction(@NonNull Context context, final int actionId, @Nullable final String data, final boolean background) {
        logger.log("runAction call");
        Pair<ServiceInfo, Boolean> pair = getServiceWithMinimumVersion(context, MINIMUM_BIND_VERSION);
        if (pair.second) {
            bindServiceAndCall(context, pair.first, null, new ServiceFunction() {
                @Override
                public void run(ILightningService service) {
                    try {
                        logger.log("running action in LL...");
                        service.runAction(actionId, data, background);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    finish();
                }
            });
        } else {
            logger.log("Legacy running action");
            Intent intent = new Intent();
            intent.setComponent(COMPONENTNAME);
            intent.putExtra("a", actionId);
            intent.putExtra("d", data);
            sendIntentToLauncher(context, intent);
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
        askForInstallation = value;
        logger.log("ask for installation mode set to: " + value);
    }

    /**
     * set if the library should toast if the permission was not granted (default is true)
     *
     * @param value the value
     */
    public static void toastIfPermissionNotGranted(boolean value) {
        toastIfPermissionNotGranted = value;
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
        useLightTheme = true;
    }

    /**
     * Use a custom theme if a dialog is shown
     *
     * @param theme the custom theme
     */
    public static void useCustomDialogTheme(@StyleRes int theme) {
        customTheme = theme;
    }

    /* Internal API */

    @SuppressLint("InlinedApi")
    private static AlertDialog.Builder getThemedBuilder(@NonNull Context context) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int theme;
            if (customTheme != 0) {
                theme = customTheme;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                theme = useLightTheme ? android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                //noinspection deprecation
                theme = useLightTheme ? AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : AlertDialog.THEME_DEVICE_DEFAULT_DARK;
            } else {
                //noinspection deprecation
                theme = useLightTheme ? AlertDialog.THEME_HOLO_LIGHT : AlertDialog.THEME_HOLO_DARK;
            }
            builder = new AlertDialog.Builder(context, theme);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        return builder;
    }

    static void permissionNotGranted(@NonNull Context context, @NonNull Listener listener) {
        if (toastIfPermissionNotGranted) {
            Toast.makeText(context, R.string.text_noPermission, Toast.LENGTH_LONG).show();
        }
        notifyError(context, listener, ErrorCode.PERMISSION_DENIED);
        logger.log("Permission denied");
    }

    @NonNull
    private static Pair<ServiceInfo, Boolean> getServiceWithMinimumVersion(@NonNull Context context, int version) {
        logger.log("Resolving service...");
        Intent service = new Intent(INTENT);
        ResolveInfo info = context.getPackageManager().resolveService(service, 0);
        if (info != null) {
            logger.log("Service resolved");
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(info.serviceInfo.packageName, 0);
                if (packageInfo.versionCode >= version) {
                    return new Pair<>(info.serviceInfo, true);
                } else {
                    logger.log("Service resolved, but lower version than needed.");
                    return new Pair<>(info.serviceInfo, false);
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        } else {
            logger.log("Service not resolved");
        }
        return new Pair<>(null, false);
    }

    private static void bindServiceAndCall(@NonNull final Context context, @NonNull final ServiceInfo info, final Listener listener, @NonNull final ServiceFunction call) {
        PermissionActivity.checkForPermission(context, "net.pierrox.lightning_launcher.IMPORT_SCRIPTS", new PermissionActivity.PermissionCallback() {
            @Override
            public void handlePermissionResult(boolean isGranted) {
                if (isGranted) {
                    logger.log("Permission granted");
                    Intent intent = new Intent(INTENT);
                    intent.setClassName(info.packageName, info.name);
                    logger.log("Binding service...");
                    context.bindService(intent, new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            logger.log("Service bound");
                            call.setFinishInfo(context, this);
                            call.run(ILightningService.Stub.asInterface(service));
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            logger.log("Service disconnected");
                        }
                    }, Context.BIND_AUTO_CREATE);
                } else {
                    permissionNotGranted(context, listener);
                }
            }
        });
    }

    private static void noImporter(@NonNull final Context context, @Nullable Listener listener, @Nullable final Runnable onButtonClick) {
        notifyError(context, listener, ErrorCode.NO_IMPORTER);
        if (askForInstallation) {
            logger.log("Asking user to install...");
            getThemedBuilder(context)
                    .setTitle("Repository Importer missing")
                    .setMessage("This action requires the Repository Importer to be installed. Do you wish to install it?")
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialogInterface, int i) {
                            logger.log("User denied install");
                            dialogInterface.dismiss();
                            if (onButtonClick != null) onButtonClick.run();
                        }
                    })
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialogInterface, int i) {
                            logger.log("User agreed to install");
                            dialogInterface.dismiss();
                            Intent playStore = new Intent(Intent.ACTION_VIEW, URI);
                            logger.log("Resolving Play Store...");
                            if (context.getPackageManager().resolveActivity(playStore, 0) != null) {
                                logger.log("Forwarding to Play Store...");
                                context.startActivity(playStore);
                            } else {
                                logger.log("Play Store not resolved, forwarding to browser...");
                                context.startActivity(new Intent(Intent.ACTION_VIEW, ALTERNATIVE_URI));
                            }
                            if (onButtonClick != null) onButtonClick.run();
                        }
                    })
                    .show();
        }
    }

    static void notifyError(Context context, final Listener listener, final ErrorCode errorCode) {
        if (listener != null) {
            logger.log("Notifying caller of Error...");
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listener.onError(errorCode);
                }
            });
        }

    }

    /* Public classes (For extension) */

    /**
     * A Listener which gets called when the load process is completed. Used to retrieve the Id of the loaded Script
     */
    public abstract static class Listener {
        public void onLoadFinished(int id) {
        }

        public void onResult(String result) {

        }

        public void onError(ErrorCode errorCode) {
            logger.log("Caller did not implement onError. Error was ignored");
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

        final void log(String msg) {
            if (debug) {
                Log.d("[SCRIPTLIB]", msg);
            }
        }

        public final void setDebug(boolean debug) {
            this.debug = debug;
        }

        public final boolean getDebug() {
            return debug;
        }
    }

    /* Private classes */

    /**
     * a Callback that is run if a script does already exists in the launcher and forceUpdate is false.
     */
    private static class UpdateCallback {
        private final Script script;
        private final Listener listener;

        public UpdateCallback(Script script, Listener listener) {
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

    private abstract static class ServiceFunction {
        private ServiceConnection connection;
        private Context context;

        abstract void run(ILightningService service);

        void setFinishInfo(Context context, ServiceConnection connection) {
            this.connection = connection;
        }

        void finish() {
            if (context != null && connection != null) {
                context.unbindService(connection);
            }
        }
    }

    /* Deprecated */

    @Deprecated
    static final String CODE = "code";
    @Deprecated
    static final String NAME = "name";
    @Deprecated
    static final String FLAGS = "flags";
    @Deprecated
    static final String LOADED_SCRIPT_ID = "loadedScriptId";
    @Deprecated
    static final String LISTENER_ID = "listenerId";
    @Deprecated
    static final String FORCE_UPDATE = "forceUpdate";
    @Deprecated
    static final String SERVICE_INTENT = "intent";
    @Deprecated
    private static final String FORWARD = "forward";
    @Deprecated
    private static final String BACKGROUND = "background";
    @Deprecated
    private static final String RECEIVER = "receiver";

    @Deprecated
    static final String STATUS = "status";
    @Deprecated
    static final int STATUS_LAUNCHER_PROBLEM = 3;
    @Deprecated
    static final int STATUS_UPDATE_CONFIRMATION_REQUIRED = 2;
    @Deprecated
    static final int STATUS_OK = 1;

    @Deprecated
    private static final Map<Integer, Listener> LISTENERS = new HashMap<>();
    @Deprecated
    private static int nextListenerIndex = 0;

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
        Pair<ServiceInfo, Boolean> pair = getServiceWithMinimumVersion(context, 22);
        if (pair.second) {
            Intent service = new Intent(INTENT);
            logger.log("Service version high enough: Script can be run in background");
            service.setClassName(pair.first.packageName, pair.first.name);
            service.putExtra(FORWARD, intent);
            logger.log("Forwarding...");
            Intent activity = new Intent(context, ScriptActivity.class);
            activity.putExtra(SERVICE_INTENT, service);
            context.startActivity(activity);
            return;
        }
        logger.log("Service not resolved or too low version: Script can NOT be run in background");
        //legacy and fallback
        logger.log("Running script...");
        context.startActivity(intent);
    }

    @Deprecated
    private static void loadScriptLegacy(@NonNull Context context, @NonNull Script script, boolean forceUpdate, @NonNull final Listener listener) {
        logger.log("LoadScriptLegacy");
        LISTENERS.put(nextListenerIndex, listener);
        Intent intent = new Intent(context, ScriptActivity.class);
        intent.putExtra(CODE, script.getCode());
        intent.putExtra(NAME, script.getName());
        intent.putExtra(FLAGS, script.getFlags());
        intent.putExtra(LISTENER_ID, nextListenerIndex);
        intent.putExtra(FORCE_UPDATE, forceUpdate);
        nextListenerIndex++;
        logger.log("Starting Activity for communication...");
        context.startActivity(intent);
    }

    @Deprecated
    private static void runScriptLegacy(@NonNull Context context, int id, @Nullable String data, boolean background) {
        logger.log("runScriptLegacy");
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(COMPONENTNAME);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("a", 35);
        i.putExtra("d", id + "/" + (data != null ? data : ""));
        i.putExtra(BACKGROUND, background);
        sendIntentToLauncher(context, i);
    }

    @Deprecated
    static boolean loadScriptInternal(@NonNull final Activity context, int listenerId, String code, String name, int flags, boolean forceUpdate) {
        logger.log("Resolving service...");
        final Intent intent = new Intent(INTENT);
        ResolveInfo info = context.getPackageManager().resolveService(intent, 0);
        if (info == null) {
            logger.log("Service not resolved: Repository Importer seems to be missing");
            noImporter(context, LISTENERS.get(listenerId), new Runnable() {
                @Override
                public void run() {
                    context.finish();
                }
            });
            return false;
        } else {
            logger.log("Service resolved");
            intent.setClassName(info.serviceInfo.packageName, info.serviceInfo.name);
            intent.putExtra(CODE, code);
            intent.putExtra(NAME, name);
            intent.putExtra(FLAGS, flags);
            intent.putExtra(RECEIVER, context.getComponentName().flattenToString());
            intent.putExtra(FORCE_UPDATE, forceUpdate);
            logger.log("Calling Repository Importer...");
            try {
                context.startService(intent);
            } catch (SecurityException e) {
                logger.log("SecurityException when calling service.");
                notifyError(context, listenerId, ErrorCode.SECURITY_EXCEPTION);
            }
        }
        return true;
    }

    @Deprecated
    static void respondTo(int listenerId, int scriptId) {
        logger.log("Received positive response (ID = " + scriptId + ")");
        logger.log("Forwarding response to caller...");
        Listener listener = LISTENERS.get(listenerId);
        if (listener != null) listener.onLoadFinished(scriptId);
    }

    @Deprecated
    static void notifyError(Context context, int listenerId, ErrorCode errorCode) {
        Listener listener = LISTENERS.get(listenerId);
        notifyError(context, listener, errorCode);
    }

    @Deprecated
    static void updateConfirmation(final int listenerId, @NonNull final Intent intent) {
        logger.log("Received request to confirm Update");
        logger.log("Trying to ask Caller whether to update or not...");
        Listener listener = LISTENERS.get(listenerId);
        if (listener != null)
            //noinspection WrongConstant
            listener.confirmUpdate(new UpdateCallback(new Script(intent.getStringExtra(CODE), intent.getStringExtra(NAME), (int) intent.getDoubleExtra(FLAGS, 0)), listener));
    }

    @Deprecated
    static void permissionNotGranted(Context context, int listenerId) {
        permissionNotGranted(context, LISTENERS.get(listenerId));
    }
}
