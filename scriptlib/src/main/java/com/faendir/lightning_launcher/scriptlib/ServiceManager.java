package com.faendir.lightning_launcher.scriptlib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.trianguloy.llscript.repository.aidl.Failure;
import com.trianguloy.llscript.repository.aidl.IImportCallback;
import com.trianguloy.llscript.repository.aidl.ILightningService;
import com.trianguloy.llscript.repository.aidl.IResultCallback;
import com.trianguloy.llscript.repository.aidl.Script;

/**
 * Created by Lukas on 01.06.2016.
 */
class ServiceManager {

    private static final String INTENT = "net.pierrox.lightning_launcher.script.IMPORT";

    enum Feature {
        BIND(30),
        FORWARD_INTENT(22);
        private final int version;

        Feature(int version) {
            this.version = version;
        }

        int getVersion() {
            return version;
        }
    }

    private static ServiceManager instance;

    static ServiceManager getInstance(Context context, ResponseManager responseManager) {
        if (instance == null) {
            instance = new ServiceManager(context, responseManager);
        }
        return instance;
    }

    private final ServiceInfo serviceInfo;
    private final int version;
    private final ResponseManager responseManager;

    private ServiceManager(Context context, ResponseManager responseManager) {
        this.responseManager = responseManager;
        ScriptManager.logger.log("Resolving service...");
        Intent service = new Intent(INTENT);
        ResolveInfo info = context.getPackageManager().resolveService(service, 0);
        int version = -1;
        ServiceInfo serviceInfo = null;
        if (info != null) {
            ScriptManager.logger.log("Service resolved");
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(info.serviceInfo.packageName, 0);
                version = packageInfo.versionCode;
                ScriptManager.logger.log("Service version: " + version);
                serviceInfo = info.serviceInfo;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        } else {
            ScriptManager.logger.log("Service not resolved");
        }
        this.version = version;
        this.serviceInfo = serviceInfo;
    }

    boolean supports(Feature feature) {
        return version >= feature.getVersion();
    }

    boolean serviceExists() {
        return serviceInfo != null;
    }

    void loadScript(@NonNull final Context context, @NonNull final Script script, @NonNull final ScriptManager.Listener listener, final boolean forceUpdate) {
        bindServiceAndCall(context, listener, new ServiceFunction() {
            @Override
            public void run(ILightningService service) {
                try {
                    ScriptManager.logger.log("importing into LL...");
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
                                            listener.confirmUpdate(new ScriptManager.UpdateCallback(script, listener));
                                        }
                                    });
                                    break;
                                case LAUNCHER_INVALID:
                                    responseManager.notifyError(context, listener, ErrorCode.LAUNCHER_PROBLEM);
                                    break;
                                case INVALID_INPUT:
                                    responseManager.notifyError(context, listener, ErrorCode.INVALID_INPUT);
                                    break;
                            }
                            finish();
                        }
                    });
                } catch (SecurityException e) {
                    ScriptManager.logger.log("SecurityException when calling service.");
                    responseManager.notifyError(context, listener, ErrorCode.SECURITY_EXCEPTION);
                    finish();
                } catch (RemoteException e) {
                    e.printStackTrace();
                    finish();
                }
            }
        });
    }

    void runScript(@NonNull Context context, final int id, @Nullable final String data, final boolean background) {
        bindServiceAndCall(context, null, new ServiceFunction() {
            @Override
            public void run(ILightningService service) {
                try {
                    ScriptManager.logger.log("running script...");
                    service.runScript(id, data, background);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                finish();
            }
        });
    }

    void runScriptForResult(@NonNull Context context, @NonNull final String code, final ScriptManager.Listener listener) {
        bindServiceAndCall(context, listener, new ServiceFunction() {
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

    void runAction(@NonNull Context context, @Action final int actionId, @Nullable final String data, final boolean background) {
        bindServiceAndCall(context, null, new ServiceFunction() {
            @Override
            public void run(ILightningService service) {
                try {
                    ScriptManager.logger.log("running action in LL...");
                    service.runAction(actionId, data, background);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                finish();
            }
        });
    }

    ServiceInfo getService(){
        return serviceInfo;
    }

    private void bindServiceAndCall(final Context context, final ScriptManager.Listener listener, @NonNull final ServiceFunction call) {
        PermissionActivity.checkForPermission(context, "net.pierrox.lightning_launcher.IMPORT_SCRIPTS", new PermissionActivity.PermissionCallback() {
            @Override
            public void handlePermissionResult(boolean isGranted) {
                if (isGranted) {
                    ScriptManager.logger.log("Permission granted");
                    Intent intent = new Intent(INTENT);
                    intent.setClassName(serviceInfo.packageName, serviceInfo.name);
                    ScriptManager.logger.log("Binding service...");
                    context.bindService(intent, new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            ScriptManager.logger.log("Service bound");
                            call.setFinishInfo(context, this);
                            call.run(ILightningService.Stub.asInterface(service));
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                            ScriptManager.logger.log("Service disconnected");
                        }
                    }, Context.BIND_AUTO_CREATE);
                } else {
                    responseManager.permissionNotGranted(context, listener);
                }
            }
        });
    }

    private abstract static class ServiceFunction {
        private ServiceConnection connection;
        private Context context;

        abstract void run(ILightningService service);

        void setFinishInfo(Context context, ServiceConnection connection) {
            this.context = context;
            this.connection = connection;
        }

        void finish() {
            if (context != null && connection != null) {
                context.unbindService(connection);
            }
        }
    }
}
