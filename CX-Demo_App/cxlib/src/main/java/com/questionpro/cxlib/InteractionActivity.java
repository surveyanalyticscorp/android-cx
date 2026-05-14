package com.questionpro.cxlib;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.FragmentActivity;

import com.questionpro.cxlib.enums.ConfigType;
import com.questionpro.cxlib.enums.VisitorStatus;
import com.questionpro.cxlib.interaction.MyWebChromeClient;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.model.WidgetSettings;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InteractionActivity extends FragmentActivity implements
        MyWebChromeClient.ProgressListener,
        IQuestionProApiCallback {
    private final String LOG_TAG="InteractionActivity";
    private ProgressBar progressBar;
    private ProgressBar loadingSpinner;

    private WebView webView;
    private Intercept intercept;

    //private SharedPreferenceManager preferenceManager;
    protected static InteractionActivity currentActivity = null;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //preferenceManager = new SharedPreferenceManager(this);

        /*if(CXGlobalInfo.getConfigType().equals(ConfigType.INTERCEPT.name())) {
            initIntercept();
        }else{
            initSurveys();
        }*/
        initIntercept();
    }

    private void initIntercept(){
        Serializable surveyIdSerializable = getIntent().getSerializableExtra("INTERCEPT");
        if (surveyIdSerializable != null) {
            intercept = (Intercept) surveyIdSerializable;
            CXGlobalInfo.updateCXPayloadWithSurveyId(intercept.surveyId);
            if(intercept.type.equals(InterceptType.PROMPT.name())) {
                setContentView(R.layout.cx_webview_dialog);
            } else {
                setContentView(R.layout.cx_webview_fullscreen);
            }
            setupWebview();
            applyWidgetSettings();
            getInterceptSurveyDetails();
        }else{
            showErrorDialog(getString(R.string.cx_error_survey_id_null));
        }
    }

    private void initSurveys(){
        setContentView(R.layout.cx_webview_dialog);
        setupWebview();

        Serializable surveyIdSerializable = getIntent().getSerializableExtra("SURVEY_ID");
        if (surveyIdSerializable != null) {
            long surveyId = (Long) surveyIdSerializable;
            CXGlobalInfo.updateCXPayloadWithSurveyId(surveyId);

            getSurveyDetails(surveyId);
        }else{
            showErrorDialog(getString(R.string.cx_error_survey_id_null));
        }
    }
    private void setupWebview(){
        ImageButton closeButton = (ImageButton)findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //CXUtils.lockOrientation(this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        loadingSpinner = (ProgressBar) findViewById(R.id.loadingSpinner);
        webView = (WebView) findViewById(R.id.surveyWebView);

        webView.setWebViewClient(new CXWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient(InteractionActivity.this));
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.clearCache(true);
        webView.getSettings().setUserAgentString("AndroidWebView");
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setBackgroundColor(Color.WHITE);

        /*int density = (int)getResources().getDisplayMetrics().density;
        webView.getSettings().setTextZoom(100 * density);*/
        webView.getSettings().setTextZoom(90);
    }

    private void applyWidgetSettings() {
        if (intercept == null || intercept.widgetSettings == null) {
            applyDefaultPromptSize();
            return;
        }
        WidgetSettings ws = intercept.widgetSettings;

        // --- TopBar: applies to both PROMPT and FULL_SCREEN ---
        View topBarContainer = findViewById(R.id.topBarContainer);
        if (topBarContainer != null && !CXUtils.isEmpty(ws.backgroundColor)) {
            try { topBarContainer.setBackgroundColor(Color.parseColor(ws.backgroundColor)); }
            catch (IllegalArgumentException e) { Log.w(LOG_TAG, "Invalid backgroundColor: " + ws.backgroundColor); }
        }

        View poweredByLayout = findViewById(R.id.poweredByLayout);
        TextView widgetTitleText = findViewById(R.id.widgetTitleText);
        ImageButton closeButton = findViewById(R.id.closeButton);

        if (!CXUtils.isEmpty(ws.widgetTitle)) {
            if (poweredByLayout != null) poweredByLayout.setVisibility(View.GONE);
            if (widgetTitleText != null) {
                widgetTitleText.setVisibility(View.VISIBLE);
                widgetTitleText.setText(ws.widgetTitle);
            }
        }

        if (!CXUtils.isEmpty(ws.textColor)) {
            try {
                int textColor = Color.parseColor(ws.textColor);
                int iconColor = Color.parseColor(ws.iconColor);
                if (widgetTitleText != null) widgetTitleText.setTextColor(textColor);
                if (closeButton != null) ImageViewCompat.setImageTintList(closeButton, android.content.res.ColorStateList.valueOf(iconColor));
            } catch (IllegalArgumentException e) { Log.w(LOG_TAG, "Invalid textColor: " + ws.textColor); }
        }

        // --- Size & position: PROMPT only ---
        if (InterceptType.PROMPT.name().equals(intercept.type)) {
            applyPromptPositionAndSize(ws);
        }
    }

    private void applyDefaultPromptSize() {
        if (!InterceptType.PROMPT.name().equals(intercept.type))
            return;

        LinearLayout dialogContent = findViewById(R.id.dialogContent);
        if (dialogContent == null)
            return;

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int height = (int) (dm.heightPixels * 0.7);
        int width = (int) (dm.widthPixels * 0.9);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                width, height);
        params.gravity = Gravity.CENTER;
        dialogContent.setLayoutParams(params);
    }

    private void applyPromptPositionAndSize(WidgetSettings ws) {
        LinearLayout dialogContent = findViewById(R.id.dialogContent);
        if (dialogContent == null) return;

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width  = (ws.widgetWindowWidth  > 0 && ws.widgetWindowWidth  <= 100)
                ? (int) (dm.widthPixels  * ws.widgetWindowWidth  / 100.0)
                : FrameLayout.LayoutParams.MATCH_PARENT;
        int height = (ws.widgetWindowHeight > 0 && ws.widgetWindowHeight <= 100)
                ? (int) (dm.heightPixels * ws.widgetWindowHeight / 100.0)
                : FrameLayout.LayoutParams.WRAP_CONTENT;

        int verticalGravity;
        switch (ws.verticalPosition == null ? "" : ws.verticalPosition) {
            case "TOP":
                verticalGravity = Gravity.TOP;
                break;
            case "BOTTOM":
                verticalGravity = Gravity.BOTTOM;
                break;
            default:
                verticalGravity = Gravity.CENTER_VERTICAL;
                break; // CENTER
        }

        int horizontalGravity;
        switch (ws.horizontalPosition == null ? "" : ws.horizontalPosition) {
            case "LEFT":
                horizontalGravity = Gravity.START;
                break;
            case "RIGHT":
                horizontalGravity = Gravity.END;
                break;
            default:
                horizontalGravity = Gravity.CENTER_HORIZONTAL;
                break; // CENTER
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.gravity = verticalGravity | horizontalGravity;

        // InteractionActivity uses Theme.Translucent.NoTitleBar, so its window draws
        // behind the system status bar (top) and navigation bar (bottom). For TOP and
        // BOTTOM positions we defer layout until WindowInsets are available, then add
        // the exact system-bar margin so the dialog content is never clipped behind them.
        final String vPos = ws.verticalPosition == null ? "" : ws.verticalPosition;
        if ("TOP".equals(vPos) || "BOTTOM".equals(vPos)) {
            ViewCompat.setOnApplyWindowInsetsListener(dialogContent, (view, insets) -> {
                Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                if ("TOP".equals(vPos)) {
                    params.topMargin = sysBars.top;
                } else {
                    params.bottomMargin = sysBars.bottom;
                }
                view.setLayoutParams(params);
                ViewCompat.setOnApplyWindowInsetsListener(view, null); // one-shot
                return insets;
            });
            ViewCompat.requestApplyInsets(dialogContent);
        }

        dialogContent.setLayoutParams(params);
    }

    private void getInterceptSurveyDetails(){
        try {
            loadingSpinner.setVisibility(View.VISIBLE);
            new CXApiHandler(this, this).getInterceptSurvey(intercept);
        }catch (Exception e){
            Log.e(LOG_TAG, "Failed to fetch intercept survey details", e);
        }
    }

    private void getSurveyDetails(long surveyId){
        try {
            loadingSpinner.setVisibility(View.VISIBLE);
            new CXApiHandler(this, this).getSurvey(surveyId);
        }catch (Exception e){
            Log.e(LOG_TAG, "Failed to fetch survey details", e);
        }
    }

    @Override
    public void OnApiCallbackFailed(JSONObject response) {
        loadingSpinner.setVisibility(View.GONE);
        try {
            String errorMessage = getString(R.string.cx_error_survey_load_failed);
            if (response.has("error") && response.getJSONObject("error").has("message")) {
                errorMessage = "Error: " + response.getJSONObject("error").getString("message");
            }else if(response.has("message")){
                errorMessage = "Error: " + response.getString("message");
            }
            final String finalErrorMessage = errorMessage;
            runOnUiThread(new Runnable() {
                public void run() {
                    showErrorDialog(finalErrorMessage);
                }
            });
        }catch (Exception e){}
    }

    @Override
    public void onApiCallbackSuccess(Intercept intercept, final String surveyUrl) {
        CXUtils.printLog("Datta", "Survey url: " + surveyUrl);
        if(intercept != null && !intercept.type.equals(InterceptType.SURVEY_URL.name())) {
            if (surveyUrl == null || CXUtils.isEmpty(surveyUrl)) {
                finish();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        launchSurvey(surveyUrl);
                    }
                });
            }
        }else{
            if (surveyUrl == null || CXUtils.isEmpty(surveyUrl)) {
                finish();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl(surveyUrl);
                    }
                });
            }
        }
    }

    private void launchSurvey(String url){
        SharedPreferenceManager.getInstance(this).saveInterceptIdForLaunchedSurvey(
                intercept.id, CXUtils.getCurrentLocalTimeInMillis());
        new CXApiHandler(InteractionActivity.this, this).submitFeedback(intercept, VisitorStatus.LAUNCHED.name());

        webView.loadUrl(url);
    }

    private void showErrorDialog(String errorMsg){
        AlertDialog.Builder builder = new AlertDialog.Builder(InteractionActivity.this, AlertDialog.THEME_HOLO_LIGHT);
        builder.setMessage(errorMsg);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.cx_dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InteractionActivity.this.finish();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    @Override
    public void onUpdateProgress(int progressValue) {
        try {
            if (progressBar != null) {
                progressBar.setProgress(progressValue);
                if (progressValue >= 40) {
                    loadingSpinner.setVisibility(View.GONE);
                }
                if (progressValue == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentActivity = this;
        QuestionProCX.getInstance().onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        QuestionProCX.getInstance().onStop(this);
        if (currentActivity == this) {
            currentActivity = null;
        }
    }

    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private void runTimer() {
        Runnable task = new Runnable() {
            public void run() {
                finish();
            }
        };
        worker.schedule(task, 4, TimeUnit.SECONDS);
    }
    private class CXWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_BACK:
                        if (view.canGoBack()) {
                            view.goBack();
                        } else {
                           finish();
                        }
                        return false;
                }
            }
            return super.shouldOverrideKeyEvent(view, event);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            if(intercept.interceptSettings.autoCloseOnCompletion) {
                if (url.contains("#autoClose") || !url.contains("questionpro") || url.contains("exitsurvey")) {
                    runTimer();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(intercept.type.equals(InterceptType.EMBED.name())){
            super.onBackPressed();
        }
    }
}
