package com.questionpro.cxlib.dataconnect;

import android.app.Activity;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;


import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.interfaces.QuestionProApiCallback;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.util.CXUtils;
import com.questionpro.cxlib.util.SharedPreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

public class CXApiHandler {

    private final Activity mActivity;
    private final QuestionProApiCallback mQuestionProApiCall;

    public CXApiHandler(Activity activity, QuestionProApiCallback call){
        this.mActivity = activity;
        mQuestionProApiCall = call;
    }

    public void getInterceptSurvey(final Intercept intercept){
        ExecutorService myExecutor = Executors.newSingleThreadExecutor();
        final Handler handler = new Handler(Looper.getMainLooper());
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    if (!CXUtils.isNetworkConnectionPresent(mActivity)) {
                        mQuestionProApiCall.onError(new JSONObject().put("error", new JSONObject().put("message", "No internet connection.")));
                        return;
                    }
                    getSurveyUrl(intercept);
                }catch (Exception e){
                    try {
                        mQuestionProApiCall.onError(new JSONObject().put("error", e.getMessage()));
                    } catch (JSONException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

    public void getIntercept(){
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
                    getInterceptConfigurations();
                }catch (JSONException e){
                    try {
                        mQuestionProApiCall.onError(new JSONObject().put("error", e.getMessage()));
                    } catch (JSONException ex) {
                        throw new RuntimeException(ex);
                    }
                }

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
                mQuestionProApiCall.onSuccess("SDK is Initialised");
            }else if(response != null && response.isRejectedPermanently()){
                JSONObject jsonObject = new JSONObject(response.getContent());
                mQuestionProApiCall.onError(jsonObject);
            }else
                mQuestionProApiCall.onError(new JSONObject().put("error","Error in fetching the intercept settings"));
        }catch (Exception e){
            mQuestionProApiCall.onError(new JSONObject());
        }
    }


    private void getSurveyUrl(Intercept intercept){
        try {
            String payload = CXGlobalInfo.getInterceptApiPayload(intercept);
            //JSONObject payloadObj = new JSONObject(payload);
            CXHttpResponse response = CXUploadClient.uploadforCX(mActivity, payload);
            if (response != null) {
                QuestionProCX questionProCX = new QuestionProCX();
                if (response.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(response.getContent());
                    if (jsonObject.has(CXConstants.JSONResponseFields.CX_SURVEY_URL)) {
                        mQuestionProApiCall.onSuccess(jsonObject.getString(CXConstants.JSONResponseFields.CX_SURVEY_URL));
                    }else{
                        mQuestionProApiCall.onError(jsonObject);
                    }
                } else if (response.isRejectedPermanently() || response.isBadPayload()) {
                    //Log.v("Rejected json:", response.getContent());
                    JSONObject jsonObject = new JSONObject(response.getContent());
                    if (jsonObject.has("response")) {
                        mQuestionProApiCall.onError(jsonObject.getJSONObject("response"));
                    }else if(jsonObject.has("message")){
                        mQuestionProApiCall.onError(jsonObject);
                    }else{
                        mQuestionProApiCall.onError(new JSONObject());
                    }
                } else {
                    mQuestionProApiCall.onError(new JSONObject());
                }
            }
        }catch (Exception e){
            mQuestionProApiCall.onError(new JSONObject());
        }
    }
}
