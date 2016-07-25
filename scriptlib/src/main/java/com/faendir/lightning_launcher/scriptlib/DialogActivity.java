package com.faendir.lightning_launcher.scriptlib;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created on 06.07.2016.
 *
 * @author F43nd1r
 */

public class DialogActivity extends Activity {
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_POSITIVE_BUTTON_TEXT = "positiveText";
    private static final String EXTRA_NEGATIVE_BUTTON_TEXT = "negativeText";
    private static final String EXTRA_RESULT_RECEIVER = "resultReceiver";
    private static final String EXTRA_THEME = "theme";

    private ResultReceiver resultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String title = intent.getStringExtra(EXTRA_TITLE);
        String message = intent.getStringExtra(EXTRA_MESSAGE);
        String positiveText = intent.getStringExtra(EXTRA_POSITIVE_BUTTON_TEXT);
        String negativeText = intent.getStringExtra(EXTRA_NEGATIVE_BUTTON_TEXT);
        resultReceiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
        int theme = intent.getIntExtra(EXTRA_THEME, 0);
        setTheme(theme);
        setTitle(title);
        setContentView(R.layout.activity_dialog);
        ((TextView) findViewById(R.id.text_message)).setText(message);
        ((Button) findViewById(R.id.button_positive)).setText(positiveText);
        ((Button) findViewById(R.id.button_negative)).setText(negativeText);
    }

    public void onClick(View v) {
        sendResult(v.getId() == R.id.button_positive);
        finish();
    }

    private void sendResult(boolean positive) {
        resultReceiver.send(positive ? AlertDialog.BUTTON_POSITIVE : AlertDialog.BUTTON_NEGATIVE, null);
    }

    @Override
    public void onBackPressed() {
        sendResult(false);
        super.onBackPressed();
    }

    public static class Builder {
        private final Context context;
        private final Intent intent;
        private ResultReceiver resultReceiver;

        public Builder(Context context, int themeResId) {
            this.context = context;
            intent = new Intent(context, DialogActivity.class);
            intent.putExtra(EXTRA_THEME, themeResId);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
        }

        public Builder setTitle(String title) {
            intent.putExtra(EXTRA_TITLE, title);
            return this;
        }

        public Builder setTitle(@StringRes int titleId) {
            return setTitle(context.getString(titleId));
        }

        public Builder setMessage(@StringRes int messageId) {
            return setMessage(context.getString(messageId));
        }

        public Builder setMessage(String message) {
            intent.putExtra(EXTRA_MESSAGE, message);
            return this;
        }

        public Builder setButtons(@StringRes int positive, @StringRes int negative, ResultReceiver listener) {
            return setButtons(context.getString(positive), context.getString(negative), listener);
        }

        public Builder setButtons(String positive, String negative, ResultReceiver listener) {
            intent.putExtra(EXTRA_POSITIVE_BUTTON_TEXT, positive);
            intent.putExtra(EXTRA_NEGATIVE_BUTTON_TEXT, negative);
            resultReceiver = listener;
            return this;
        }

        public void show() {
            intent.putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
            context.startActivity(intent);
        }
    }
}
