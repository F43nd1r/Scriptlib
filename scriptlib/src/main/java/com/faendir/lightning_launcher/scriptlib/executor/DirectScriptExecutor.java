package com.faendir.lightning_launcher.scriptlib.executor;

import android.content.Context;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import com.faendir.lightning_launcher.scriptlib.ExceptionHandler;
import com.faendir.lightning_launcher.scriptlib.Logger;
import com.faendir.lightning_launcher.scriptlib.ResultCallback;
import com.faendir.lightning_launcher.scriptlib.exception.FailureException;
import com.trianguloy.llscript.repository.aidl.Failure;
import com.trianguloy.llscript.repository.aidl.ILightningService;
import com.trianguloy.llscript.repository.aidl.IResultCallback;
import com.trianguloy.llscript.repository.aidl.Script;
import org.apache.commons.text.StringEscapeUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 25.07.2016.
 *
 * @author F43nd1r
 */

public class DirectScriptExecutor implements Executor<String> {
    private String code;
    private final Map<String, String> variables;
    private int codeRes;

    public DirectScriptExecutor(@NonNull String code) {
        this.code = code;
        variables = new HashMap<>();
    }

    public DirectScriptExecutor(@RawRes int codeRes) {
        this.codeRes = codeRes;
        variables = new HashMap<>();
        code = null;
    }

    public DirectScriptExecutor putVariable(@NonNull String name, @Nullable String value) {
        variables.put(name, value);
        return this;
    }

    public DirectScriptExecutor putVariables(@NonNull Map<String, String> variables) {
        this.variables.putAll(variables);
        return this;
    }

    @Override
    public void execute(@NonNull Context context, @NonNull ILightningService lightningService,
                        @NonNull final ExceptionHandler exceptionHandler, @NonNull final Logger logger, @NonNull final ResultCallback<String> listener) {
        if (code == null) {
            code = Script.rawResourceToString(context, codeRes);
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            builder.append("var ").append(entry.getKey()).append(" = ")
                    .append(entry.getValue() == null ? "null" : "\"" + StringEscapeUtils.escapeJava(entry.getValue()) + "\"").append(";\n");
        }
        builder.append(code);
        try {
            lightningService.runScriptForResult(builder.toString(), new IResultCallback.Stub() {
                @Override
                public void onResult(String result) {
                    logger.log("Result received");
                    listener.onResult(result);
                }

                @Override
                public void onFailure(Failure failure) {
                    exceptionHandler.onException(new FailureException(failure));
                }
            });
        } catch (RemoteException e) {
            exceptionHandler.onException(e);
        }
    }
}
