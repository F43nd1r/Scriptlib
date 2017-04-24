package com.faendir.lightning_launcher.scriptlib;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Created on 18.06.2016.
 *
 * @author F43nd1r
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class ScriptManager {

    static Logger logger = new DefaultLogger();
    private final ServiceManager2 serviceManager;
    private final Context context;

    public ScriptManager(Context context) {
        serviceManager = new ServiceManager2(context);
        this.context = context;
    }

    public AsyncExecutorService getAsyncExecutorService() {
        return getAsyncExecutorService(new BaseExceptionHandler(context));
    }

    public AsyncExecutorService getAsyncExecutorService(ExceptionHandler exceptionHandler) {
        return new AsyncExecutorService(context, serviceManager, exceptionHandler, logger);
    }

    /**
     * enables extensive logging
     *
     * @return this instance
     */
    public ScriptManager enableDebug() {
        logger.setDebug(true);
        return this;
    }

    /**
     * replace the built in Logger with a custom one
     *
     * @param logger the logger which should replace the current one
     */
    public void replaceLogger(@NonNull Logger logger) {
        ScriptManager.logger.log("Replacing Logger...");
        ScriptManager.logger = logger;
        ScriptManager.logger.log("Logger replaced");
    }

}
