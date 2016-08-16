package com.faendir.lightning_launcher.scriptlib.executor;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.WorkerThread;

import com.faendir.lightning_launcher.scriptlib.ServiceManager;
import com.trianguloy.llscript.repository.aidl.Script;

import org.apache.commons.lang3.StringEscapeUtils;

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

    private static String loadCode(@NonNull ServiceManager serviceManager, @RawRes int codeRes) {
        String code = Script.rawResourceToString(serviceManager.getContext(), codeRes);
        if (code == null) {
            code = "return null;";
        }
        return code;
    }

    public DirectScriptExecutor putVariable(@NonNull String name, @Nullable String value) {
        variables.put(name, value);
        return this;
    }

    public DirectScriptExecutor putVariables(@NonNull Map<String, String> variables) {
        this.variables.putAll(variables);
        return this;
    }


    @WorkerThread
    @Override
    public String execute(@NonNull ServiceManager serviceManager) {
        if (code == null) {
            code = Script.rawResourceToString(serviceManager.getContext(), codeRes);
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            builder.append("var ").append(entry.getKey()).append(" = ")
                    .append(entry.getValue() == null ? "null" : "\"" + StringEscapeUtils.escapeJava(entry.getValue()) + "\"").append(";\n");
        }
        builder.append(code);
        return serviceManager.runScriptForResult(builder.toString());
    }
}
