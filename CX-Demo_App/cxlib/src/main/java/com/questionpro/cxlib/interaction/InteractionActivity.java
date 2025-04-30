package com.questionpro.cxlib.interaction;

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

import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.R;
import com.questionpro.cxlib.dataconnect.CXApiHandler;
import com.questionpro.cxlib.enums.InterceptType;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.interfaces.IQuestionProApiCallback;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.util.CXUtils;
import com.questionpro.cxlib.util.SharedPreferenceManager;

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

        Serializable surveyIdSerializable = getIntent().getSerializableExtra("INTERCEPT");
        if (surveyIdSerializable != null) {
            intercept = (Intercept) surveyIdSerializable;
            CXGlobalInfo.updateCXPayloadWithSurveyId(intercept.surveyId);

            init();

            getSurveyDetails();
        }else{
            showErrorDialog("Survey Id is null");
        }
        preferenceManager = new SharedPreferenceManager(this);
    }

    private void init(){
        if(intercept.type.equals(InterceptType.PROMPT.name())) {
            setContentView(R.layout.cx_webview_dialog);
           
        } else {
            setContentView(R.layout.cx_webview_fullscreen);
        }
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
    
    private void launchSurvey(String url){
        preferenceManager.saveInterceptIdForLaunchedSurvey(this,
                intercept.id, CXUtils.getCurrentLocalTimeInMillis());
        new CXApiHandler(InteractionActivity.this, this).submitFeedback(intercept, "LAUNCHED");

        webView.loadUrl(url);
    }

    private void getSurveyDetails(){
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
        if(intercept != null && !intercept.type.equals(InterceptType.SURVEY_URL.name())) {
            CXUtils.printLog("Datta", "Url: " + surveyUrl);
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
        }
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
