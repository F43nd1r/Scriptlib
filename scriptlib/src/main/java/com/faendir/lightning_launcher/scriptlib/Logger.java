package com.faendir.lightning_launcher.scriptlib;

/**
 * Created on 28.07.2016.
 *
 * @author F43nd1r
 */
public interface Logger {
    void log(String msg);

    void warn(String msg);

    void setDebug(boolean debug);

    boolean getDebug();
}
