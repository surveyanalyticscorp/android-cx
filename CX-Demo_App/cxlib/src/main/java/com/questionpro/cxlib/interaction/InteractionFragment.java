package com.questionpro.cxlib.interaction;


import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.questionpro.cxlib.R;
import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.model.CXInteraction;
import com.questionpro.cxlib.util.CXUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class InteractionFragment extends Fragment implements MyWebChromeClient.ProgressListener{
    private ProgressBar progressBar;
    private WebView webView;
    private String url = "";
    private CXInteraction cxInteraction;

    public InteractionFragment(){
        super(R.layout.cx_webview_fullscreen);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This callback will only be called when MyFragment is at least Started.
        OnBackPressedCallback callback = new OnBackPressedCallback(true ) {
            @Override
            public void handleOnBackPressed() {
                getParentFragmentManager().popBackStack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        //int someInt = requireArguments().getInt("some_int");
        //cxInteraction =(CXInteraction) requireArguments().getSerializableExtra(CXConstants.CX_INTERACTION_CONTENT);
        cxInteraction = (CXInteraction) requireArguments().getSerializable(CXConstants.CX_INTERACTION_CONTENT);
        url = cxInteraction.url;

        try {
            RelativeLayout container = (RelativeLayout) view.findViewById(R.id.topBar);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                    convertDpToPixel(40));
            params.setMargins(0, convertDpToPixel(30), 0, 0);
            container.setLayoutParams(params);
        }catch (Exception e){e.printStackTrace();}

        progressBar =(ProgressBar) view.findViewById(R.id.progressBar);

        webView = (WebView)view.findViewById(R.id.surveyWebView);
        webView.setWebViewClient(new CXWebViewClient());
        webView.setWebChromeClient(new MyWebChromeClient(this));
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.clearCache(true);
        webView.getSettings().setUserAgentString("AndroidWebView");

        if(url==null || CXUtils.isEmpty(url)){
            //getActivity().finish();
            getParentFragmentManager().popBackStack();
        } else{
            webView.loadUrl(url);
        }

        ImageButton closeButton = (ImageButton)view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //getActivity().finish();
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private int convertDpToPixel(int dp){
        Resources r = getActivity().getResources();
        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                r.getDisplayMetrics()
        );
        return px;
    }

    @Override
    public void onUpdateProgress(int progressValue) {
        if(progressBar != null){
            progressBar.setProgress(progressValue);
            if(progressValue == 100){
                progressBar.setVisibility(View.INVISIBLE);
            }
        }
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
                            //getActivity().finish();
                            getParentFragmentManager().popBackStack();
                        }
                        return false;
                }
            }
            return super.shouldOverrideKeyEvent(view, event);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            if(url.contains("#autoClose")){
                runTimer();
            }
        }

    }

    private static final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private void runTimer() {
        Runnable task = new Runnable() {
            public void run() {
                //getActivity().finish();
                getParentFragmentManager().popBackStack();
            }
        };
        worker.schedule(task, 5, TimeUnit.SECONDS);
    }
}
