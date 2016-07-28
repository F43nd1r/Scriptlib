package com.faendir.lightning_launcher.scriptlib;

import android.util.Log;

/**
 * A logger which outputs to androids default console if in debug mode
 */
public class DefaultLogger implements Logger {
    private boolean debug = false;
    private static final String TAG = "[SCRIPTLIB]";

    @Override
    public void log(String msg) {
        if (debug) {
            Log.d(TAG, msg);
        }
    }

    @Override
    public void warn(String msg) {
        Log.w(TAG, msg);
    }

    @Override
    public final void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public final boolean getDebug() {
        return debug;
    }
}
