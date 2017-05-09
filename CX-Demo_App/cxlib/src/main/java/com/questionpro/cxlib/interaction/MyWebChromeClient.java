package com.questionpro.cxlib.interaction;

import android.app.Activity;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class MyWebChromeClient extends WebChromeClient {
    public interface ProgressListener {
        public void onUpdateProgress(int progressValue);
    }

    private ProgressListener mListener;

    public MyWebChromeClient(ProgressListener listener) {
        mListener = listener;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        mListener.onUpdateProgress(newProgress);
        super.onProgressChanged(view, newProgress);
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        return super.onConsoleMessage(consoleMessage);

    }

    @Override
    public void onCloseWindow(WebView window) {
        super.onCloseWindow(window);
        if(mListener instanceof Activity){
            ((Activity)mListener).finish();
        }
    }

}
