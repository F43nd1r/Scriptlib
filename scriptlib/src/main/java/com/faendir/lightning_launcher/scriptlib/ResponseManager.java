package com.faendir.lightning_launcher.scriptlib;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.widget.Toast;

/**
 * Created by Lukas on 01.06.2016.
 */
@SuppressWarnings("WeakerAccess")
public class ResponseManager {

    private static final Uri URI = Uri.parse("market://details?id=com.trianguloy.llscript.repository");
    private static final Uri ALTERNATIVE_URI = Uri.parse("https://play.google.com/store/apps/details?id=com.trianguloy.llscript.repository");
    private final Context context;

    private boolean askForInstallation = true;
    private boolean toastIfPermissionNotGranted = true;
    private boolean useLightTheme = false;
    private int customTheme = 0;

    public ResponseManager(Context context) {
        this.context = context;
    }

    protected AlertDialog.Builder getThemedBuilder() {
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

    protected void permissionNotGranted() {
        if (toastIfPermissionNotGranted) {
            Toast.makeText(context, R.string.text_noPermission, Toast.LENGTH_LONG).show();
        }
        ScriptManager.logger.log("Permission denied");
    }

    protected void outdatedImporter() {
        askForInstallation("Repository Importer outdated",
                "This action requires a newer version of Repository Importer. Do you wish to update it?");

    }

    protected void noImporter() {
        askForInstallation("Repository Importer missing",
                "This action requires the Repository Importer to be installed. Do you wish to install it?");
    }

    protected void confirmUpdate(@NonNull DialogInterface.OnClickListener listener) {
        ScriptManager.logger.log("Asking user to confirm script update...");
        getThemedBuilder()
                .setTitle("Confirm update")
                .setMessage("A script with this name does already exist. Do you want to overwrite it?")
                .setNegativeButton("No", listener)
                .setPositiveButton("Yes", listener)
                .show();
    }

    protected void askForInstallation(@NonNull String title, @NonNull String message) {
        if (askForInstallation) {
            ScriptManager.logger.log("Asking user to install...");
            getThemedBuilder()
                    .setTitle(title)
                    .setMessage(message)
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ScriptManager.logger.log("User denied install");
                            dialogInterface.dismiss();
                        }
                    })
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
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
