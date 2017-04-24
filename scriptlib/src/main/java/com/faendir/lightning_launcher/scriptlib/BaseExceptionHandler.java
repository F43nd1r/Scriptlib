package com.faendir.lightning_launcher.scriptlib;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.widget.Toast;

import com.faendir.lightning_launcher.scriptlib.exception.FailureException;
import com.faendir.lightning_launcher.scriptlib.exception.PermissionDeniedException;
import com.faendir.lightning_launcher.scriptlib.exception.RepositoryImporterMissingException;
import com.faendir.lightning_launcher.scriptlib.exception.RepositoryImporterOutdatedException;
import com.trianguloy.llscript.repository.aidl.Failure;

/**
 * Created on 01.06.2016.
 *
 * @author F43nd1r
 */
@SuppressWarnings("WeakerAccess")
public class BaseExceptionHandler implements ExceptionHandler {

    private static final Uri URI = Uri.parse("market://details?id=com.trianguloy.llscript.repository");
    private static final Uri ALTERNATIVE_URI = Uri.parse("https://play.google.com/store/apps/details?id=com.trianguloy.llscript.repository");

    private final Context context;

    private final boolean useLightTheme;
    private final int customTheme;

    public BaseExceptionHandler(Context context) {
        this(context, false, 0);
    }

    public BaseExceptionHandler(Context context, boolean useLightTheme) {
        this(context, useLightTheme, 0);
    }

    public BaseExceptionHandler(Context context, @StyleRes int customTheme) {
        this(context, false, customTheme);
    }

    protected BaseExceptionHandler(Context context, boolean useLightTheme, int customTheme) {
        this.context = context;
        this.useLightTheme = useLightTheme;
        this.customTheme = customTheme;
    }

    protected DialogActivity.Builder getThemedBuilder() {
        int theme;
        if (customTheme != 0) {
            theme = customTheme;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            theme = useLightTheme ? android.R.style.Theme_DeviceDefault_Light_Dialog_Alert : android.R.style.Theme_DeviceDefault_Dialog_Alert;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            //noinspection deprecation
            theme = useLightTheme ? AlertDialog.THEME_DEVICE_DEFAULT_LIGHT : AlertDialog.THEME_DEVICE_DEFAULT_DARK;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            //noinspection deprecation
            theme = useLightTheme ? AlertDialog.THEME_HOLO_LIGHT : AlertDialog.THEME_HOLO_DARK;
        } else {
            theme = android.R.style.Theme_Dialog;
        }
        return new DialogActivity.Builder(context, theme);
    }

    protected void permissionNotGranted() {
        new Handler(context.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, R.string.text_noPermission, Toast.LENGTH_LONG).show();
                ScriptManager.logger.log("Permission denied");
            }
        });
    }

    protected void outdatedImporter() {
        askForInstallation(R.string.title_outDated, R.string.message_outDated);

    }

    protected void noImporter() {
        askForInstallation(R.string.title_missing, R.string.message_missing);
    }

    protected void confirmUpdate(@NonNull ResultReceiver listener) {
        ScriptManager.logger.log("Asking user to confirm script update...");
        getThemedBuilder()
                .setTitle(R.string.title_confirmUpdate)
                .setMessage(R.string.message_confirmUpdate)
                .setButtons(android.R.string.yes, android.R.string.no, listener)
                .show();
    }

    protected void askForInstallation(@StringRes int title, @StringRes int message) {
        ScriptManager.logger.log("Asking user to install...");
        getThemedBuilder()
                .setTitle(title)
                .setMessage(message)
                .setButtons(android.R.string.yes, android.R.string.no, new ResultReceiver(new Handler(context.getMainLooper())) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        super.onReceiveResult(resultCode, resultData);
                        if (resultCode == AlertDialog.BUTTON_POSITIVE) {
                            ScriptManager.logger.log("User agreed to install");
                            Intent playStore = new Intent(Intent.ACTION_VIEW, URI);
                            ScriptManager.logger.log("Resolving Play Store...");
                            if (context.getPackageManager().resolveActivity(playStore, 0) != null) {
                                ScriptManager.logger.log("Forwarding to Play Store...");
                                if (!(context instanceof Activity)) {
                                    playStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                }
                                context.startActivity(playStore);
                            } else {
                                ScriptManager.logger.log("Play Store not resolved, forwarding to browser...");
                                Intent browser = new Intent(Intent.ACTION_VIEW, ALTERNATIVE_URI);
                                if (!(context instanceof Activity)) {
                                    browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                }
                                context.startActivity(browser);
                            }
                        } else {
                            ScriptManager.logger.log("User denied install");
                        }
                    }
                })
                .show();
    }

    @Override
    public void onException(Exception e) {
        if (e instanceof RepositoryImporterMissingException) {
            noImporter();
        } else if (e instanceof RepositoryImporterOutdatedException) {
            outdatedImporter();
        } else if (e instanceof PermissionDeniedException) {
            permissionNotGranted();
        } else if (e instanceof FailureException) {
            final FailureException failureException = (FailureException) e;
            if ((failureException.getFailure() == Failure.SCRIPT_ALREADY_EXISTS && failureException.getRetry() != null)) {
                confirmUpdate(new ResultReceiver(new Handler(Looper.getMainLooper())) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        if (resultCode == AlertDialog.BUTTON_POSITIVE) {
                            failureException.getRetry().retry();
                        }else {
                            onUnhandledException(failureException);
                        }
                    }
                });
            }
        } else {
            onUnhandledException(e);
        }
    }

    protected void onUnhandledException(Exception e){
        e.printStackTrace();
    }
}
