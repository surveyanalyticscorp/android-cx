package com.questionpro.cxlib.interaction;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.fragment.app.FragmentActivity;

import com.questionpro.cxlib.R;
import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.model.CXInteraction;
import com.questionpro.cxlib.util.CXUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InteractionActivity extends FragmentActivity implements MyWebChromeClient.ProgressListener {
    private final String LOG_TAG="InteractionActivity";
    private ProgressBar progressBar;
    private WebView webView;
    private String url = "";
    private CXInteraction cxInteraction;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cxInteraction =(CXInteraction) getIntent().getSerializableExtra(CXConstants.CX_INTERACTION_CONTENT);
        url = cxInteraction.url;
        if(cxInteraction.isDialog) {
            setContentView(R.layout.cx_webview_dialog);
            RelativeLayout container = (RelativeLayout) findViewById(R.id.topBar);
            if(!cxInteraction.themeColor.isEmpty()) {
                container.setBackgroundColor(Color.parseColor(cxInteraction.themeColor));
            }
        } else {
            setContentView(R.layout.cx_webview_fullscreen);
            try {
                RelativeLayout container = (RelativeLayout) findViewById(R.id.topBar);
                if(!cxInteraction.themeColor.isEmpty()) {
                    container.setBackgroundColor(Color.parseColor(cxInteraction.themeColor));
                }
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        CXUtils.convertDpToPixel(this, 40));
                container.setLayoutParams(params);
            }catch (Exception e){e.printStackTrace();}
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
        webView.setWebChromeClient(new MyWebChromeClient(this));
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.clearCache(true);
        webView.getSettings().setUserAgentString("AndroidWebView");
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        /*int density = (int)getResources().getDisplayMetrics().density;
        webView.getSettings().setTextZoom(100 * density);*/
        webView.getSettings().setTextZoom(90);

        if(url==null || CXUtils.isEmpty(url)){
            finish();
        } else{
            webView.loadUrl(url);
        }
    }

    @Override
    public void onUpdateProgress(int progressValue) {
        if(progressBar != null){
            progressBar.setProgress(progressValue);
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
            if(url.contains("#autoClose") || !url.contains("questionpro")){
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
