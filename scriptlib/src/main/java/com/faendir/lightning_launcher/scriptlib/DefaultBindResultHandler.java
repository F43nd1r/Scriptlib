package com.faendir.lightning_launcher.scriptlib;

/**
 * Created on 01.08.2016.
 *
 * @author F43nd1r
 */

public class DefaultBindResultHandler implements ResultCallback<BindResult> {
    private final ResponseManager responseManager;

    public DefaultBindResultHandler(ResponseManager responseManager) {
        this.responseManager = responseManager;
    }

    @Override
    public void onResult(BindResult result) {
        switch (result){
            case OK:
                break;
            case PERMISSION_NOT_GRANTED:
                responseManager.permissionNotGranted();
                break;
            case REPOSITORY_IMPORTER_MISSING:
                responseManager.noImporter();
                break;
            case REPOSITORY_IMPORTER_OUTDATED:
                responseManager.outdatedImporter();
                break;
        }
    }
}
