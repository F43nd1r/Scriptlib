package com.faendir.lightning_launcher.scriptlib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import com.faendir.lightning_launcher.scriptlib.exception.PermissionDeniedException;
import com.faendir.lightning_launcher.scriptlib.exception.RepositoryImporterMissingException;
import com.faendir.lightning_launcher.scriptlib.exception.RepositoryImporterOutdatedException;
import com.trianguloy.llscript.repository.aidl.ILightningService;

import static com.faendir.lightning_launcher.scriptlib.ScriptManager.logger;

/**
 * @author F43nd1r
 * @since 21.04.2017
 */

public class ServiceManager2 {
    private static final String INTENT = "net.pierrox.lightning_launcher.script.IMPORT";
    private static final int MIN_SERVICE_VERSION = 34;

    private final ServiceInfo serviceInfo;
    private final int version;
    private final Context context;
    private ServiceConnection serviceConnection;

    ServiceManager2(Context context) {
        this.context = context;
        logger.log("Resolving service...");
        Intent service = new Intent(INTENT);
        ResolveInfo info = context.getPackageManager().resolveService(service, 0);
        int version = -1;
        ServiceInfo serviceInfo = null;
        if (info != null) {
            logger.log("Service resolved");
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(info.serviceInfo.packageName, 0);
                //noinspection deprecation
                version = packageInfo.versionCode;
                logger.log("Service version: " + version);
                serviceInfo = info.serviceInfo;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        } else {
            logger.log("Service not resolved");
        }
        this.version = version;
        this.serviceInfo = serviceInfo;
    }

    void bind(final ExceptionHandler exceptionHandler, final ResultCallback<ILightningService> listener) {
        if (version >= MIN_SERVICE_VERSION) {
            PermissionActivity.checkForPermission(context, "net.pierrox.lightning_launcher.IMPORT_SCRIPTS", isGranted -> {
                if (isGranted) {
                    logger.log("Permission granted");
                    Intent intent = new Intent(INTENT);
                    intent.setClassName(serviceInfo.packageName, serviceInfo.name);
                    logger.log("Binding service...");
                    serviceConnection = new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName name, IBinder service) {
                            logger.log("Service bound");
                            listener.onResult(ILightningService.Stub.asInterface(service));
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName name) {
                        }
                    };
                    context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
                } else {
                    exceptionHandler.onException(new PermissionDeniedException());
                }
            });
        } else if (serviceInfo != null) {
            exceptionHandler.onException(new RepositoryImporterOutdatedException());
        }
        else {
            exceptionHandler.onException(new RepositoryImporterMissingException());
        }
    }

    void unbind() {
        try {
            context.unbindService(serviceConnection);
        } catch (IllegalArgumentException e) {
            logger.warn("Trying to unbind while not bound.");
        }
    }

}
