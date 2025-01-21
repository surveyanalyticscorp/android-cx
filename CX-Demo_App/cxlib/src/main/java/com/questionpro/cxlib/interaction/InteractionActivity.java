package com.questionpro.cxlib.interaction;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.questionpro.cxlib.R;
import com.questionpro.cxlib.dataconnect.CXApiHandler;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.interfaces.QuestionProApiCall;
import com.questionpro.cxlib.model.CXInteraction;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InteractionActivity extends FragmentActivity implements MyWebChromeClient.ProgressListener, QuestionProApiCall {
    private final String LOG_TAG="InteractionActivity";
    private ProgressBar progressBar;
    private ProgressDialog progressDialog;
    private WebView webView;
    private String url = "";
    private CXInteraction cxInteraction;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //cxInteraction =(CXInteraction) getIntent().getSerializableExtra(CXConstants.CX_INTERACTION_CONTENT);
        //url = cxInteraction.url;

        init();

        getSurveyDetails();

        //launchSurvey();
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
        webView.loadUrl(url);
    }

    private void getSurveyDetails(){
        try {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("QuestionPro");
            progressDialog.setMessage("Loading survey...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            long surveyId = (Long) getIntent().getSerializableExtra("SURVEY_ID");

            CXGlobalInfo.updateCXPayloadWithSurveyId(surveyId);
            new CXApiHandler(this).execute();
        }catch (Exception e){

        }
    }

    @Override
    public void onSuccess(final String surveyUrl) {
        Log.d("Datta","Urllll: "+surveyUrl);
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
        if(null != progressDialog && progressDialog.isShowing()){
            progressDialog.cancel();
        }
        try {
            if (response.has("error") && response.getJSONObject("error").has("message")) {
                final String errorMessage = "Error: " + response.getJSONObject("error").getString("message");
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(InteractionActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }catch (Exception e){}
    }

    @Override
    public void onUpdateProgress(int progressValue) {
        if(progressBar != null){
            progressBar.setProgress(progressValue);
            if(progressValue >= 20){
                if(null != progressDialog && progressDialog.isShowing()){
                    progressDialog.cancel();
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
            Log.d("Datta","onPageFinished url:"+url);
            if(url.contains("#autoClose") || !url.contains("questionpro") || url.contains("exitsurvey")){
                runTimer();
            }
        }

    }

    @Override
    public void onBackPressed() {
        if(!cxInteraction.isDialog){
            super.onBackPressed();
        }
    }
}
