package com.questionpro.cxlib.dataconnect;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.CXObject;
import com.questionpro.cxlib.util.CXUtils;
import com.questionpro.cxlib.R;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by sachinsable on 29/03/16.
 */
public class CXUploader {
    private Activity activity;
    public String UUID;
    public  String URL;
    private CXObject cxObject;
    private final String LOG_TAG="CXUploader";
    public CXUploader(Activity activity, CXObject cxObject){
        this.activity = activity;
        this.cxObject = cxObject;
        UUID = CXUtils.getUniqueDeviceId(activity);
    }


    public void doCXUpload(){
        CXUploadService cxUploadService = new CXUploadService();
        cxUploadService.execute();
    }

    private class CXUploadService extends AsyncTask<Void,Void,String> {
        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog= new ProgressDialog(activity);
            progressDialog.setTitle("CX");
            progressDialog.setMessage("Loading...");
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(CXConstants.JSONUploadFields.UDID, CXUploader.this.UUID);
                jsonObject.put(CXConstants.JSONUploadFields.TOUCH_POINT_ID, cxObject.touchPointID);
                String result = uploadforCX(jsonObject.toString());
                JSONObject resultJSON = new JSONObject(result);
                if (resultJSON.has(CXConstants.JSONResponseFields.STATUS)) {
                    JSONObject status = resultJSON.getJSONObject(CXConstants.JSONResponseFields.STATUS);
                    if (status.getInt(CXConstants.JSONResponseFields.ID) == 200 && resultJSON.has(CXConstants.JSONResponseFields.RESPONSE)) {
                        JSONObject response = resultJSON.getJSONObject(CXConstants.JSONResponseFields.RESPONSE);
                        if (response.has(CXConstants.JSONResponseFields.SURVEY_URL)) {
                            String url = response.getString(CXConstants.JSONResponseFields.SURVEY_URL);
                            if (!url.equalsIgnoreCase(CXConstants.JSONResponseFields.EMPTY)) {
                                URI uri=new URI(url);
                                if(uri.isAbsolute())
                                    return url;
                            }
                        }
                    }

                }
            }
            catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if(progressDialog.isShowing()){
                progressDialog.dismiss();
            }
            if(result!=null){
                showSurveyDialog(result);
            }
        }
    }
    public String uploadforCX(String payload) {
        HttpURLConnection urlConnection = null;
        try {
            java.net.URL uRL = new URL(CXConstants.getCXUploadURL(cxObject.apiKey));
            urlConnection = (HttpURLConnection) uRL.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/json; charSet=UTF-8");
            urlConnection.setConnectTimeout(30000);
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setFixedLengthStreamingMode(payload.length());
            OutputStream os = urlConnection.getOutputStream();
            os.write(payload.getBytes("UTF-8"));
            os.close();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String s = CXUtils.convertStreamToString(in);
            in.close();
            return s;


        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    Dialog webViewDialog;
    ProgressBar progressBar;
    public void showSurveyDialog(final String url){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                activity);

        // set title
        alertDialogBuilder.setTitle(cxObject.getDialogPromptTitleText()!=null?
                cxObject.getDialogPromptTitleText() : CXConstants.globalDialogPromptTitleText );
        alertDialogBuilder.setCancelable(false);
        // set dialog message
        alertDialogBuilder
                .setMessage(cxObject.getDialogPromptMessageText()!=null?
                        cxObject.getDialogPromptMessageText(): CXConstants.globalDialogPromptMessageText )
                .setCancelable(false)
                .setPositiveButton(cxObject.getDialogPromptPositiveText()!=null?
                        cxObject.getDialogPromptPositiveText(): CXConstants.globalDialogPromptPositiveText,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        webViewDialog= new Dialog(activity);
                        webViewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        webViewDialog.getWindow().setBackgroundDrawableResource(
                                R.color.dialog_bg_color);
                        webViewDialog.setCancelable(false);
                        View layoutView = activity.getLayoutInflater().inflate(R.layout.cx_webview_dialog,null);
                        progressBar =(ProgressBar) layoutView.findViewById(R.id.progressBar);
                        final WebView webView = (WebView)layoutView.findViewById(R.id.surveyWebView);
                        webView.setWebViewClient(new CXWebViewClient());
                        webView.setWebChromeClient(new CXWebChromeClient());
                        webView.setVerticalScrollBarEnabled(false);
                        webView.setHorizontalScrollBarEnabled(false);
                        webView.getSettings().setJavaScriptEnabled(true);
                        webView.getSettings().setLoadWithOverviewMode(true);
                        webView.getSettings().setUseWideViewPort(true);
                        webView.clearCache(true);
                        webView.getSettings().setUserAgentString("AndroidWebView");
                        if (Build.VERSION.SDK_INT >= 19) {
                            // chromium, enable hardware acceleration
                            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                        } else {
                            // older android version, disable hardware acceleration
                            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                        }

                        webView.loadUrl(url);
                        ImageButton closeButton = (ImageButton)layoutView.findViewById(R.id.closeButton);
                        closeButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                webViewDialog.dismiss();
                            }
                        });

                        webViewDialog.setContentView(layoutView);

                        webViewDialog.show();
                    }
                })
                .setNegativeButton(cxObject.getDialogPromptNegativeText()!=null?
                        cxObject.getDialogPromptNegativeText(): CXConstants.globalDialogPromptNegativeText,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();




    }

    private class CXWebViewClient extends WebViewClient{


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
                            if(webViewDialog!=null && webViewDialog.isShowing()){
                                webViewDialog.dismiss();
                            }
                        }
                        return false;

                }


            }
            return super.shouldOverrideKeyEvent(view, event);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
        }

    }


    private class CXWebChromeClient extends WebChromeClient{
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            Log.d(LOG_TAG, consoleMessage.message());
            if(consoleMessage.message()!=null && consoleMessage.message().equalsIgnoreCase("cx_thank_you_page")){
                runTimer();
                return true;
            }

            return super.onConsoleMessage(consoleMessage);

        }

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            webViewDialog.dismiss();
        }


    }


    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();

    private void runTimer() {

        Runnable task = new Runnable() {
            public void run() {
                if (webViewDialog != null && webViewDialog.isShowing()) {
                    webViewDialog.dismiss();
                }
            }
        };
        worker.schedule(task, 5, TimeUnit.SECONDS);

    }
}
