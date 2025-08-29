package com.questionpro.cxlib;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.fragment.app.FragmentActivity;

import com.questionpro.cxlib.enums.ConfigType;
import com.questionpro.cxlib.interaction.MyWebChromeClient;
import com.questionpro.cxlib.model.Intercept;
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
    //private ProgressDialog progressDialog;
    private ProgressDialog customProgressDialog;

    private WebView webView;
    private Intercept intercept;

    private SharedPreferenceManager preferenceManager;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new SharedPreferenceManager(this);

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
            getInterceptSurveyDetails();
        }else{
            showErrorDialog("Survey Id is null");
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
            showErrorDialog("Survey Id is null");
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
        progressBar =(ProgressBar) findViewById(R.id.progressBar);
        webView = (WebView)findViewById(R.id.surveyWebView);

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

    private void getInterceptSurveyDetails(){
        try {
            customProgressDialog = new ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT);
            customProgressDialog.setMessage("Please wait.");
            customProgressDialog.setCancelable(false);
            customProgressDialog.show();

            new CXApiHandler(this, this).getInterceptSurvey(intercept);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void getSurveyDetails(long surveyId){
        try {
            customProgressDialog = new ProgressDialog(this, ProgressDialog.THEME_HOLO_LIGHT);
            customProgressDialog.setMessage("Please wait.");
            customProgressDialog.setCancelable(false);
            customProgressDialog.show();

            new CXApiHandler(this, this).getSurvey(surveyId);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void OnApiCallbackFailed(JSONObject response) {
        if(null != customProgressDialog && customProgressDialog.isShowing()){
            customProgressDialog.dismiss();
        }
        try {
            String errorMessage = "Something went wrong. Unable to load the survey.";
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
        preferenceManager.saveInterceptIdForLaunchedSurvey(this,
                intercept.id, CXUtils.getCurrentLocalTimeInMillis());
        new CXApiHandler(InteractionActivity.this, this).submitFeedback(intercept, "LAUNCHED");

        webView.loadUrl(url);
    }

    private void showErrorDialog(String errorMsg){
        AlertDialog.Builder builder = new AlertDialog.Builder(InteractionActivity.this, AlertDialog.THEME_HOLO_LIGHT);
        builder.setMessage(errorMsg);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
        if(progressBar != null){
            progressBar.setProgress(progressValue);
            if(progressValue >= 20){
                if(null != customProgressDialog && customProgressDialog.isShowing()){
                    customProgressDialog.dismiss();
                }
            }
            if(progressValue == 100){
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        QuestionProCX.getInstance().onStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        QuestionProCX.getInstance().onStop(this);
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
            if(url.contains("#autoClose") || !url.contains("questionpro") || url.contains("exitsurvey")){
                runTimer();
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
