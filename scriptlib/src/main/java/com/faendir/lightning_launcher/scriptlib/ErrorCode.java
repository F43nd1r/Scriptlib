package com.faendir.lightning_launcher.scriptlib;

/**
 * Created by Lukas on 29.01.2016.
 * Returned to caller in the case of an error
 */
public enum ErrorCode {
    /**
     * a problem reported by repository importer
     * e.g. LL is not installed
     */
    LAUNCHER_PROBLEM,
    /**
     * Repository importer is not installed.
     * By default a dialog asking to install is already shown!
     * Change this with {@link ScriptManager#askForRepositoryImporterInstallationIfMissing(boolean)}
     */
    NO_IMPORTER,
    /**
     * Security Exception encountered.
     * This can probably be fixed by uninstalling the app and the reinstalling it.
     */
    SECURITY_EXCEPTION,
    /**
     * User didn't grant permission
     * By default a toast is already shown!
     * Change this with {@link ScriptManager#toastIfPermissionNotGranted(boolean)}
     */
    PERMISSION_DENIED,
    /**
     * Importer rejected the input
     */
    INVALID_INPUT

}
