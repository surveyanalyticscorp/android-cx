package com.questionpro.cxlib.dataconnect;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;

import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.interfaces.QuestionProApiCall;
import com.questionpro.cxlib.model.CXInteraction;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONObject;

import java.net.URI;

public class CXApiHandler extends AsyncTask<String, String, String> {

    private Activity mActivity;
    private final QuestionProApiCall mQuestionProApiCall;
    private ProgressDialog progressDialog;
    public CXApiHandler(Activity activity){
        this.mActivity = activity;
        this.mQuestionProApiCall = (QuestionProApiCall)activity;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... strings) {
        /*try {
            makeApiCall();
        }catch (Exception e){
            mQuestionProApiCall.onError("Error occurred.....");
        }*/
        return makeApiCall();

    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
    }

    private String makeApiCall(){
        try {
            if (!CXUtils.isNetworkConnectionPresent(mActivity)) {
                Log.d("Datta", "Can't send payloads. No network connection.");
                return "No network connection.";
            }

            String payload = CXGlobalInfo.getApiPayload(mActivity);
            //JSONObject payloadObj = new JSONObject(payload);
            CXHttpResponse response = CXUploadClient.uploadforCX(mActivity, payload);
            if (response != null) {
                QuestionProCX questionProCX = new QuestionProCX();
                if (response.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(response.getContent());
                    if (jsonObject.has(CXConstants.JSONResponseFields.RESPONSE)) {
                        JSONObject responseJson = jsonObject.getJSONObject(CXConstants.JSONResponseFields.RESPONSE);
                        //responseJson.put(CXConstants.JSONResponseFields.IS_DIALOG,CXGlobalInfo.isShowDialog(contextRef.get()));
                        responseJson.put(CXConstants.JSONResponseFields.THEME_COLOR, CXGlobalInfo.getThemeColour(mActivity));
                        CXInteraction cxInteraction = CXInteraction.fromJSON(responseJson);

                        if (!cxInteraction.url.equalsIgnoreCase("Empty") && URI.create(cxInteraction.url).isAbsolute()) {
                            mQuestionProApiCall.onSuccess(cxInteraction.url);
                        } else {

                            mQuestionProApiCall.onError(responseJson);
                        }
                    }
                    //Log.d(LOG_TAG,"Payload submission successful" + response.getContent());

                } else if (response.isRejectedPermanently() || response.isBadPayload()) {
                    Log.v("Rejected json:", response.getContent());
                    JSONObject jsonObject = new JSONObject(response.getContent());
                    if (jsonObject.has("response")) {
                        //questionProCX.onError(jsonObject.getJSONObject("response"));
                        mQuestionProApiCall.onError(jsonObject.getJSONObject("response"));
                    }
                } else if (response.isRejectedTemporarily()) {
                    Log.d("Datta", "Unable to send JSON");
                }
            }
        }catch (Exception e){e.printStackTrace();}
        return  "";
    }
}
