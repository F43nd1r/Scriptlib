package com.faendir.lightning_launcher.scriptlib;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;


public final class ScriptActivity extends Activity {
    private static final String PERMISSION = "net.pierrox.lightning_launcher.IMPORT_SCRIPTS";
    private static final int ID_IMPORT = 1;
    private static final int ID_RUN = 2;


    private int respondTo;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        boolean allowFinish = true;
        this.intent = intent;
        ScriptManager.logger.log("Resolving intent...");
        if (intent.hasExtra(ScriptManager.STATUS)) {
            ScriptManager.logger.log("Intent seems to be valid");
            Object status = intent.getExtras().get(ScriptManager.STATUS);
            int code = status instanceof Number ? ((Number) status).intValue() : -1;
            switch (code) {
                case ScriptManager.STATUS_OK:
                    ScriptManager.logger.log("Repository Importer answered with Status OK");
                    ScriptManager.respondTo(respondTo, (int) intent.getDoubleExtra(ScriptManager.LOADED_SCRIPT_ID, -1));
                    break;
                case ScriptManager.STATUS_LAUNCHER_PROBLEM:
                    ScriptManager.logger.log("Repository Importer answered with Status LAUNCHER_PROBLEM");
                    ScriptManager.notifyError(respondTo);
                    break;
                case ScriptManager.STATUS_UPDATE_CONFIRMATION_REQUIRED:
                    ScriptManager.logger.log("Repository Importer answered with Status UPDATE_CONFIRMATION_REQUIRED");
                    ScriptManager.updateConfirmation(respondTo, intent);
                    break;
                default:
                    invalidCall();
            }
        } else if (intent.hasExtra(ScriptManager.CODE) && intent.hasExtra(ScriptManager.NAME)) {
            if (requestPermission(ID_IMPORT)) {
                loadImport();
            } else {
                allowFinish = false;
            }
        } else if (intent.hasExtra((ScriptManager.SERVICE_INTENT))) {
            if (requestPermission(ID_RUN)) {
                loadRun();
            } else {
                allowFinish = false;
            }
        } else {
            invalidCall();
        }
        if (allowFinish) {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ScriptManager.logger.log("Permission granted");
            switch (requestCode) {
                case ID_IMPORT:
                    loadImport();
                    break;
                case ID_RUN:
                    loadRun();
                    break;
            }
        } else {
            Toast.makeText(this, R.string.text_noPermission, Toast.LENGTH_LONG).show();
            ScriptManager.logger.log("Permission denied");
        }
        finish();
    }

    private void loadImport() {
        ScriptManager.logger.log("Activity started for communication");
        respondTo = intent.getIntExtra(ScriptManager.LISTENER_ID, -1);
        if (respondTo != -1) {
            ScriptManager.logger.log("Initializing communication request");
            ScriptManager.loadScriptInternal(this, respondTo, intent.getStringExtra(ScriptManager.CODE),
                    intent.getStringExtra(ScriptManager.NAME),
                    intent.getIntExtra(ScriptManager.FLAGS, 0),
                    intent.getBooleanExtra(ScriptManager.FORCE_UPDATE, false));
        }
    }

    private void loadRun() {
        ScriptManager.logger.log("Activity started for communication");
        ScriptManager.logger.log("Running script...");
        startService((Intent) intent.getParcelableExtra(ScriptManager.SERVICE_INTENT));
    }

    private void invalidCall() {
        ScriptManager.logger.log("Intent was invalid.");
        throw new UnsupportedOperationException("You should not call this Activity at all.");
    }

    private boolean requestPermission(int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            ScriptManager.logger.log("Requesting permission...");
            requestPermissions(new String[]{PERMISSION}, id);
            return false;
        }
        return true;
    }
}
