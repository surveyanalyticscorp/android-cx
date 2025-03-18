package com.questionpro.cxlib.dataconnect;

import android.app.Activity;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;


import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.interfaces.QuestionProApiCallback;
import com.questionpro.cxlib.model.CXInteraction;
import com.questionpro.cxlib.util.ApiNameEnum;
import com.questionpro.cxlib.util.CXUtils;
import com.questionpro.cxlib.util.SharedPreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;

public class CXApiHandler {

    private final Activity mActivity;
    private final QuestionProApiCallback mQuestionProApiCall;

    public CXApiHandler(Activity activity, QuestionProApiCallback call){
        this.mActivity = activity;
        mQuestionProApiCall = call;
    }

    public void makeApiCall(final ApiNameEnum apiName){
        ExecutorService myExecutor = Executors.newSingleThreadExecutor();
        final Handler handler = new Handler(Looper.getMainLooper());
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // Background Task (Simulating heavy work)
                try {
                    if (!CXUtils.isNetworkConnectionPresent(mActivity)) {
                        mQuestionProApiCall.onError(new JSONObject().put("error", new JSONObject().put("message", "No internet connection.")));
                        return;
                    }
                    if(apiName == ApiNameEnum.GET_SURVEY) {
                        getSurveyUrl();
                    } else if (apiName == ApiNameEnum.GET_INTERCEPTS) {
                        getInterceptConfigurations();
                    }
                }catch (JSONException e){e.printStackTrace();}

                // Switch to UI thread
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void getInterceptConfigurations(){
        try {
            CXHttpResponse response = CXUploadClient.getCxApi(mActivity);
            if (response != null && response.isSuccessful()) {
                JSONObject jsonObject = new JSONObject(response.getContent());
                if (jsonObject.has(CXConstants.JSONResponseFields.PROJECT)) {
                    JSONObject responseJson = jsonObject.getJSONObject(CXConstants.JSONResponseFields.PROJECT);
                    SharedPreferenceManager sharedPreferenceManager=new SharedPreferenceManager(mActivity);
                    sharedPreferenceManager.saveIntercepts(responseJson.toString());
                }
            }
            mQuestionProApiCall.onSuccess("SDK in Initialised");
            //mQuestionProApiCall.onError(new JSONObject().put("error","Error in fetching the intercept settings"));
        }catch (Exception e){
            mQuestionProApiCall.onError(new JSONObject());
        }
    }


    private void getSurveyUrl(){
        try {
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
                    mQuestionProApiCall.onError(new JSONObject());
                }
            }
        }catch (Exception e){
            mQuestionProApiCall.onError(new JSONObject());
            e.printStackTrace();
        }
    }
}
