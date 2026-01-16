package com.questionpro.cxlib;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.questionpro.cxlib.R;
import com.questionpro.cxlib.interaction.MyWebChromeClient;
import com.questionpro.cxlib.interfaces.QuestionProApiCall;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InteractionActivity extends FragmentActivity implements MyWebChromeClient.ProgressListener, QuestionProApiCall {
    private final String LOG_TAG="InteractionActivity";
    private ProgressBar progressBar;
    //private ProgressDialog progressDialog;
    private ProgressDialog customProgressDialog;

    private WebView webView;
    private String url = "";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QuestionProCX.registerInteractionActivity(this);
        init();

        CXInteraction interactionSerializable = (CXInteraction)getIntent().getSerializableExtra(CXConstants.CX_INTERACTION_CONTENT);
        if(interactionSerializable != null){
            launchSurvey(interactionSerializable.url);
        }
        //getSurveyDetails();
    }

    private void init(){
        if(CXGlobalInfo.isShowDialog(this)) {
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

        View topBar = findViewById(R.id.topBar);
        if(!CXUtils.isEmpty(CXGlobalInfo.getThemeColour(this)))
            topBar.setBackgroundColor(Color.parseColor(CXGlobalInfo.getThemeColour(this)));

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
        webView.addJavascriptInterface(new WebAppInterface(), "Android");


        /*int density = (int)getResources().getDisplayMetrics().density;
        webView.getSettings().setTextZoom(100 * density);*/
        webView.getSettings().setTextZoom(90);

    }

    public class WebAppInterface {
        @JavascriptInterface
        public void onMessage(String message) {
            //Log.d("Datta","Web interface: "+message);
            if(isValidWebUrl(message))
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(message)));
            else
                Toast.makeText(InteractionActivity.this, message,Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidWebUrl(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return Patterns.WEB_URL.matcher(text).matches();
    }

    private void launchSurvey(String url){
        webView.loadUrl(url);
    }

    @Override
    public void onSuccess(final String surveyUrl) {
        CXUtils.printLogs("Url: "+surveyUrl);
        if(surveyUrl==null || CXUtils.isEmpty(surveyUrl)){
            finish();
        } else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    launchSurvey(surveyUrl);
                }
            });
        }
    }

    @Override
    public void onError(JSONObject response) {
        if(null != customProgressDialog && customProgressDialog.isShowing()){
            customProgressDialog.dismiss();
        }
        try {
            String errorMessage = "Something went wrong. Unable to load the survey.";
            if (response.has("error") && response.getJSONObject("error").has("message")) {
                errorMessage = "Error: " + response.getJSONObject("error").getString("message");
            }
            final String finalErrorMessage = errorMessage;
            runOnUiThread(new Runnable() {
                public void run() {
                    showErrorDialog(finalErrorMessage);
                }
            });
        }catch (Exception e){}
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

    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private void runTimer() {
        Runnable task = new Runnable() {
            public void run() {
                finish();
            }
        };
        worker.schedule(task, 5, TimeUnit.SECONDS);
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
        if(!CXGlobalInfo.isShowDialog(this)){
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferenceManager.getInstance(InteractionActivity.this)
                .putSurveyTimestamp(CXGlobalInfo.getInstance().getSurveyIDFromPayload());
        QuestionProCX.unregisterInteractionActivity();
    }
}
