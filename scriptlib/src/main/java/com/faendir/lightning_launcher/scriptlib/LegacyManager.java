package com.faendir.lightning_launcher.scriptlib;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.trianguloy.llscript.repository.aidl.Script;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Lukas on 01.06.2016.
 */
class LegacyManager {

    private static final String FORWARD = "forward";
    static final String CODE = "code";
    static final String NAME = "name";
    static final String FLAGS = "flags";
    static final String LOADED_SCRIPT_ID = "loadedScriptId";
    static final String LISTENER_ID = "listenerId";
    static final String FORCE_UPDATE = "forceUpdate";
    static final String SERVICE_INTENT = "intent";
    static final String STATUS = "status";
    static final int STATUS_LAUNCHER_PROBLEM = 3;
    static final int STATUS_UPDATE_CONFIRMATION_REQUIRED = 2;
    static final int STATUS_OK = 1;
    private static final String BACKGROUND = "background";
    private static final String RECEIVER = "receiver";
    static final ComponentName COMPONENTNAME = ComponentName.unflattenFromString("net.pierrox.lightning_launcher_extreme/net.pierrox.lightning_launcher.activities.Dashboard");


    final Map<Integer, ScriptManager.Listener> LISTENERS = new HashMap<>();
    private int nextListenerIndex = 0;
    private final ResponseManager responseManager;

    LegacyManager(ResponseManager responseManager) {
        this.responseManager = responseManager;
    }

    void loadScriptLegacy(@NonNull Context context, @NonNull Script script, boolean forceUpdate, @NonNull final ScriptManager.Listener listener) {
        ScriptManager.logger.log("LoadScriptLegacy");
        LISTENERS.put(nextListenerIndex, listener);
        Intent intent = new Intent(context, ScriptActivity.class);
        intent.putExtra(CODE, script.getCode());
        intent.putExtra(NAME, script.getName());
        intent.putExtra(FLAGS, script.getFlags());
        intent.putExtra(LISTENER_ID, nextListenerIndex);
        intent.putExtra(FORCE_UPDATE, forceUpdate);
        nextListenerIndex++;
        ScriptManager.logger.log("Starting Activity for communication...");
        context.startActivity(intent);
    }

    void runScriptLegacy(@NonNull Context context, int id, @Nullable String data, boolean background) {
        ScriptManager.logger.log("runScriptLegacy");
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(COMPONENTNAME);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra("a", 35);
        i.putExtra("d", id + "/" + (data != null ? data : ""));
        i.putExtra(BACKGROUND, background);
        ScriptManager.sendIntentToLauncher(context, i);
    }

    boolean loadScriptInternal(@NonNull final Activity context, int listenerId, String code, String name, int flags, boolean forceUpdate) {
        ScriptManager.logger.log("Resolving service...");
        ServiceInfo info = ServiceManager.getInstance(context, responseManager).getService();
        if (info == null) {
            ScriptManager.logger.log("Service not resolved: Repository Importer seems to be missing");
            responseManager.noImporter(context, LISTENERS.get(listenerId), new Runnable() {
                @Override
                public void run() {
                    context.finish();
                }
            });
            return false;
        } else {
            final Intent intent = new Intent();
            ScriptManager.logger.log("Service resolved");
            intent.setClassName(info.packageName, info.name);
            intent.putExtra(CODE, code);
            intent.putExtra(NAME, name);
            intent.putExtra(FLAGS, flags);
            intent.putExtra(RECEIVER, context.getComponentName().flattenToString());
            intent.putExtra(FORCE_UPDATE, forceUpdate);
            ScriptManager.logger.log("Calling Repository Importer...");
            try {
                context.startService(intent);
            } catch (SecurityException e) {
                ScriptManager.logger.warn("SecurityException when calling service.");
                notifyError(context, listenerId, ErrorCode.SECURITY_EXCEPTION);
            }
        }
        return true;
    }

    void respondTo(int listenerId, int scriptId) {
        ScriptManager.logger.log("Received positive response (ID = " + scriptId + ")");
        ScriptManager.logger.log("Forwarding response to caller...");
        ScriptManager.Listener listener = LISTENERS.get(listenerId);
        if (listener != null) listener.onLoadFinished(scriptId);
    }

    void notifyError(Context context, int listenerId, ErrorCode errorCode) {
        ScriptManager.Listener listener = LISTENERS.get(listenerId);
        responseManager.notifyError(context, listener, errorCode);
    }

    void updateConfirmation(final int listenerId, @NonNull final Intent intent) {
        ScriptManager.logger.log("Received request to confirm Update");
        ScriptManager.logger.log("Trying to ask Caller whether to update or not...");
        ScriptManager.Listener listener = LISTENERS.get(listenerId);
        if (listener != null)
            //noinspection WrongConstant
            listener.confirmUpdate(new ScriptManager.UpdateCallback(new Script(intent.getStringExtra(CODE), intent.getStringExtra(NAME), (int) intent.getDoubleExtra(FLAGS, 0)), listener));
    }

    void permissionNotGranted(Context context, int listenerId) {
        responseManager.permissionNotGranted(context, LISTENERS.get(listenerId));
    }
    
    void runAction(@NonNull Context context, @Action final int actionId, @Nullable final String data, boolean background){
        ScriptManager.logger.log("Legacy running action");
        Intent intent = new Intent();
        intent.setComponent(COMPONENTNAME);
        intent.putExtra("a", actionId);
        intent.putExtra("d", data);
        intent.putExtra(BACKGROUND, background);
        sendIntentToLauncher(context, intent);
    }
    
    void sendIntentToLauncher(Context context, Intent intent){
        ScriptManager.logger.log("Sending intent to Launcher...");
        if (intent.getPackage() == null) {
            intent.setPackage(COMPONENTNAME.getPackageName());
            ScriptManager.logger.log("Added missing package name");
        }
        if (intent.getComponent() == null || intent.getComponent().getClassName() == null) {
            ComponentName componentName = new ComponentName(intent.getPackage(), COMPONENTNAME.getClassName());
            intent.setComponent(componentName);
            ScriptManager.logger.log("Added missing class");
        }
        ServiceManager serviceManager = ServiceManager.getInstance(context, responseManager);
        if (serviceManager.supports(ServiceManager.Feature.FORWARD_INTENT)) {
            ScriptManager.logger.log("Service version high enough: Script can be run in background");
            ServiceInfo serviceInfo = serviceManager.getService();
            Intent service = new Intent();
            service.setClassName(serviceInfo.packageName, serviceInfo.name);
            service.putExtra(FORWARD, intent);
            ScriptManager.logger.log("Forwarding...");
            Intent activity = new Intent(context, ScriptActivity.class);
            activity.putExtra(SERVICE_INTENT, service);
            context.startActivity(activity);
            return;
        }
        if(intent.getBooleanExtra(BACKGROUND,false)) {
            ScriptManager.logger.log("Service not resolved or too low version: Script will not be run in background ");
        }
        //legacy and fallback
        ScriptManager.logger.log("Running script...");
        context.startActivity(intent);
    }
}
