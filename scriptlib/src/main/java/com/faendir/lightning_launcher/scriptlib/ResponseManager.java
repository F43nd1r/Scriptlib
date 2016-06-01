package com.faendir.lightning_launcher.scriptlib;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * Created by Lukas on 01.06.2016.
 */
public class ResponseManager {

    private static final Uri URI = Uri.parse("market://details?id=com.trianguloy.llscript.repository");
    private static final Uri ALTERNATIVE_URI = Uri.parse("https://play.google.com/store/apps/details?id=com.trianguloy.llscript.repository");

    private boolean askForInstallation = true;
    private boolean toastIfPermissionNotGranted = true;
    private boolean useLightTheme = false;
    private int customTheme = 0;

    ResponseManager() {
    }

    void notifyError(Context context, final ScriptManager.Listener listener, final ErrorCode errorCode) {
        if (listener != null) {
            ScriptManager.logger.log("Notifying caller of Error...");
            new Handler(context.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listener.onError(errorCode);
                }
            });
        } else {
            ScriptManager.logger.warn("No listener, Error " + errorCode.name() + " will be ignored");
        }

    }

    AlertDialog.Builder getThemedBuilder(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return new AlertDialog.Builder(context);
        }
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
        return new AlertDialog.Builder(context, theme);
    }

    void permissionNotGranted(@NonNull Context context, @NonNull ScriptManager.Listener listener) {
        if (toastIfPermissionNotGranted) {
            Toast.makeText(context, R.string.text_noPermission, Toast.LENGTH_LONG).show();
        }
        notifyError(context, listener, ErrorCode.PERMISSION_DENIED);
        ScriptManager.logger.log("Permission denied");
    }

    void outdatedImporter(@NonNull final Context context, @Nullable ScriptManager.Listener listener, @Nullable final Runnable onButtonClick) {
        askForInstallation(context, "Repository Importer outdated",
                "This action requires a newer version of Repository Importer. Do you wish to update it?", listener, onButtonClick);

    }

    void noImporter(@NonNull final Context context, @Nullable ScriptManager.Listener listener, @Nullable final Runnable onButtonClick) {
        askForInstallation(context, "Repository Importer missing",
                "This action requires the Repository Importer to be installed. Do you wish to install it?", listener, onButtonClick);
    }

    private void askForInstallation(@NonNull final Context context, @NonNull String title, @NonNull String message, @Nullable ScriptManager.Listener listener, @Nullable final Runnable onButtonClick) {
        notifyError(context, listener, ErrorCode.NO_IMPORTER);
        if (askForInstallation) {
            ScriptManager.logger.log("Asking user to install...");
            getThemedBuilder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialogInterface, int i) {
                            ScriptManager.logger.log("User denied install");
                            dialogInterface.dismiss();
                            if (onButtonClick != null) onButtonClick.run();
                        }
                    })
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(@NonNull DialogInterface dialogInterface, int i) {
                            ScriptManager.logger.log("User agreed to install");
                            dialogInterface.dismiss();
                            Intent playStore = new Intent(Intent.ACTION_VIEW, URI);
                            ScriptManager.logger.log("Resolving Play Store...");
                            if (context.getPackageManager().resolveActivity(playStore, 0) != null) {
                                ScriptManager.logger.log("Forwarding to Play Store...");
                                context.startActivity(playStore);
                            } else {
                                ScriptManager.logger.log("Play Store not resolved, forwarding to browser...");
                                context.startActivity(new Intent(Intent.ACTION_VIEW, ALTERNATIVE_URI));
                            }
                            if (onButtonClick != null) onButtonClick.run();
                        }
                    })
                    .show();
        }
    }

    public void setAskForInstallation(boolean askForInstallation) {
        this.askForInstallation = askForInstallation;
    }

    public void setToastIfPermissionNotGranted(boolean toastIfPermissionNotGranted) {
        this.toastIfPermissionNotGranted = toastIfPermissionNotGranted;
    }

    public void setUseLightTheme(boolean useLightTheme) {
        this.useLightTheme = useLightTheme;
    }

    public void setCustomTheme(int customTheme) {
        this.customTheme = customTheme;
    }
}
