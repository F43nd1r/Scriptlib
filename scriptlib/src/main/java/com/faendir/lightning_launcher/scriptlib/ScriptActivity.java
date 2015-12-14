package com.faendir.lightning_launcher.scriptlib;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;


public class ScriptActivity extends Activity {
    private static final String PERMISSION = "net.pierrox.lightning_launcher.IMPORT_SCRIPTS";


    private int respondTo;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        boolean allowFinish = true;
        this.intent = intent;
        ScriptManager.LOGGER.log("Resolving intent...");
        if (intent.hasExtra(ScriptManager.STATUS)) {
            ScriptManager.LOGGER.log("Intent seems to be valid");
            Object status = intent.getExtras().get(ScriptManager.STATUS);
            int code = (status != null && status instanceof Number) ? ((Number) status).intValue() : -1;
            switch (code) {
                case ScriptManager.STATUS_OK:
                    ScriptManager.LOGGER.log("Repository Importer answered with Status OK");
                    ScriptManager.respondTo(respondTo, (int) intent.getDoubleExtra(ScriptManager.LOADED_SCRIPT_ID, -1));
                    break;
                case ScriptManager.STATUS_LAUNCHER_PROBLEM:
                    ScriptManager.LOGGER.log("Repository Importer answered with Status LAUNCHER_PROBLEM");
                    ScriptManager.notifyError(respondTo);
                    break;
                case ScriptManager.STATUS_UPDATE_CONFIRMATION_REQUIRED:
                    ScriptManager.LOGGER.log("Repository Importer answered with Status UPDATE_CONFIRMATION_REQUIRED");
                    ScriptManager.updateConfirmation(respondTo, intent);
                    break;
                default:
                    invalidCall();
            }
        } else if (intent.hasExtra(ScriptManager.CODE) && intent.hasExtra(ScriptManager.NAME)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                ScriptManager.LOGGER.log("Requesting permission...");
                requestPermissions(new String[]{PERMISSION}, 0);
                allowFinish = false;
            } else load();
        } else invalidCall();
        if (allowFinish) finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ScriptManager.LOGGER.log("Permission granted");
            load();
        } else {
            Toast.makeText(this, R.string.text_noPermission, Toast.LENGTH_LONG).show();
            ScriptManager.LOGGER.log("Permission denied");
        }
        finish();
    }

    private void load() {
        ScriptManager.LOGGER.log("Activity started for communication");
        respondTo = intent.getIntExtra(ScriptManager.LISTENER_ID, -1);
        if (respondTo != -1) {
            ScriptManager.LOGGER.log("Initializing communication request");
            ScriptManager.loadScriptInternal(this, respondTo, intent.getStringExtra(ScriptManager.CODE),
                    intent.getStringExtra(ScriptManager.NAME),
                    intent.getIntExtra(ScriptManager.FLAGS, 0),
                    intent.getBooleanExtra(ScriptManager.FORCE_UPDATE, false));
        }
    }

    private void invalidCall() {
        ScriptManager.LOGGER.log("Intent was invalid.");
        throw new UnsupportedOperationException("You should not call this Activity at all.");
    }
}
