package com.questionpro.cxlib;

import android.content.Context;
import android.os.Looper;

import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;

import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.CXConstants;
import com.questionpro.cxlib.CXGlobalInfo;
import com.questionpro.cxlib.IQuestionProApiCallback;

import com.questionpro.cxlib.dataconnect.CXHttpResponse;
import com.questionpro.cxlib.dataconnect.CXUploadClient;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONException;
import org.json.JSONObject;

class CXApiHandler {

    private final Context mContext;
    private final IQuestionProApiCallback mQuestionProApiCall;

    public CXApiHandler(Context context, IQuestionProApiCallback call){
        this.mContext = context;
        mQuestionProApiCall = call;
    }

    public void getInterceptSurvey(final Intercept intercept){
        ExecutorService myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    if (!CXUtils.isNetworkConnectionPresent(mContext)) {
                        mQuestionProApiCall.OnApiCallbackFailed(new JSONObject().put("error", new JSONObject().put("message", "No internet connection.")));
                        return;
                    }
                    getInterceptSurveyUrl(intercept);
                }catch (Exception e){
                    try {
                        mQuestionProApiCall.OnApiCallbackFailed(new JSONObject().put("error", e.getMessage()));
                    } catch (JSONException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

    public void getSurvey(final long surveyId){
        ExecutorService myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    if (!CXUtils.isNetworkConnectionPresent(mContext)) {
                        mQuestionProApiCall.OnApiCallbackFailed(new JSONObject().put("error", new JSONObject().put("message", "No internet connection.")));
                        return;
                    }
                    getSurveyUrl(surveyId);
                }catch (Exception e){
                    try {
                        mQuestionProApiCall.OnApiCallbackFailed(new JSONObject().put("error", e.getMessage()));
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
                try {
                    if (!CXUtils.isNetworkConnectionPresent(mContext)) {
                        mQuestionProApiCall.OnApiCallbackFailed(new JSONObject().put("error", new JSONObject().put("message", "No internet connection.")));
                        return;
                    }
                    getInterceptConfigurations();
                }catch (JSONException e){
                    try {
                        mQuestionProApiCall.OnApiCallbackFailed(new JSONObject().put("error", e.getMessage()));
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

    public void submitFeedback(final Intercept intercept, final String type){
        ExecutorService myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String payload = getSurveyFeedbackApiPayload(intercept, type);

                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("x-app-key", CXGlobalInfo.getInstance().getApiKey());
                    headers.put("package-name", mContext.getPackageName());
                    headers.put("visitor-id", SharedPreferenceManager.getInstance(mContext).getVisitorsUUID());

                    URL url = new URL(CXConstants.getFeedbackUrl());

                    CXHttpResponse response = CXUploadClient.uploadCXApi(url, headers, payload);

                    if (response != null) {
                        JSONObject jsonObject = new JSONObject(response.getContent());
                    }
                }catch (Exception e){}
            }
        });
    }

    private void getInterceptConfigurations(){
        try {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("x-app-key",CXGlobalInfo.getInstance().getApiKey());
            headers.put("visitor-id",SharedPreferenceManager.getInstance(mContext).getVisitorsUUID());
            headers.put("package-name", mContext.getPackageName());
            headers.put("x-platform", getPlatformType());
            headers.put("x-device-id", CXUtils.getUniqueDeviceId(mContext));

            java.net.URL url = new URL(CXConstants.getInterceptsUrl());
            CXHttpResponse response = CXUploadClient.getCxApi(url, headers);

            if (response.isSuccessful()) {
                JSONObject jsonObject = new JSONObject(response.getContent());
                if (jsonObject.has(CXConstants.JSONResponseFields.PROJECT)) {
                    JSONObject projectJson = jsonObject.getJSONObject(CXConstants.JSONResponseFields.PROJECT);
                    SharedPreferenceManager.getInstance(mContext).saveProject(projectJson.toString());
                }
                SharedPreferenceManager.getInstance(mContext).saveVisitorsUUID(jsonObject.getJSONObject(CXConstants.JSONResponseFields.VISITOR).getString("uuid"));
                mQuestionProApiCall.onApiCallbackSuccess(null, "SDK is Initialised");
            }else if(response.isRejectedPermanently()){
                JSONObject jsonObject = new JSONObject(response.getContent());
                mQuestionProApiCall.OnApiCallbackFailed(jsonObject);
            }else
                mQuestionProApiCall.OnApiCallbackFailed(new JSONObject().put("error","Error in fetching the intercept settings"));
        }catch (Exception e){
            mQuestionProApiCall.OnApiCallbackFailed(new JSONObject());
        }
    }


    private void getInterceptSurveyUrl(Intercept intercept){
        try {
            String payload = CXGlobalInfo.getInterceptApiPayload(intercept, mContext);

            HashMap<String, String> headers = new HashMap<>();
            headers.put("x-app-key",CXGlobalInfo.getInstance().getApiKey());
            headers.put("package-name", mContext.getPackageName());

            URL url = new URL(CXConstants.getInterceptSurveyUrl(mContext));

            CXHttpResponse response = CXUploadClient.uploadCXApi(url, headers, payload);

            if (response != null) {
                QuestionProCX questionProCX = new QuestionProCX();
                if (response.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(response.getContent());
                    if (jsonObject.has(CXConstants.JSONResponseFields.CX_SURVEY_URL)) {
                        mQuestionProApiCall.onApiCallbackSuccess(intercept, jsonObject.getString(CXConstants.JSONResponseFields.CX_SURVEY_URL));
                    }else{
                        mQuestionProApiCall.OnApiCallbackFailed(jsonObject);
                    }
                } else if (response.isRejectedPermanently() || response.isBadPayload()) {
                    //Log.v("Rejected json:", response.getContent());
                    JSONObject jsonObject = new JSONObject(response.getContent());
                    if (jsonObject.has("response")) {
                        mQuestionProApiCall.OnApiCallbackFailed(jsonObject.getJSONObject("response"));
                    }else if(jsonObject.has("message")){
                        mQuestionProApiCall.OnApiCallbackFailed(jsonObject);
                    }else{
                        mQuestionProApiCall.OnApiCallbackFailed(new JSONObject());
                    }
                } else {
                    mQuestionProApiCall.OnApiCallbackFailed(new JSONObject());
                }
            }
        }catch (Exception e){
            mQuestionProApiCall.OnApiCallbackFailed(new JSONObject());
        }
    }

    private void getSurveyUrl(long surveyId){
        try {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json; charSet=UTF-8");
            headers.put("api-key", CXGlobalInfo.getInstance().getApiKey());

            URL url = new URL(CXConstants.getSurveyUrl(surveyId));
            CXHttpResponse response = CXUploadClient.getCxApi(url, headers);

            if (response.isSuccessful()) {
                JSONObject jsonObject = new JSONObject(response.getContent());
                if (jsonObject.has(CXConstants.JSONResponseFields.RESPONSE)) {
                    JSONObject responseObj = jsonObject.getJSONObject(CXConstants.JSONResponseFields.RESPONSE);
                    String surveyUrl = responseObj.getString(CXConstants.JSONResponseFields.CORE_SURVEY_URL);
                    mQuestionProApiCall.onApiCallbackSuccess(null, surveyUrl);
                }else{
                    mQuestionProApiCall.OnApiCallbackFailed(jsonObject);
                }
            } else if (response.isRejectedPermanently() || response.isBadPayload()) {
                //Log.v("Rejected json:", response.getContent());
                JSONObject jsonObject = new JSONObject(response.getContent());
                if (jsonObject.has("response")) {
                    mQuestionProApiCall.OnApiCallbackFailed(jsonObject.getJSONObject("response"));
                }else if(jsonObject.has("message")){
                    mQuestionProApiCall.OnApiCallbackFailed(jsonObject);
                }else{
                    mQuestionProApiCall.OnApiCallbackFailed(new JSONObject());
                }
            } else {
                mQuestionProApiCall.OnApiCallbackFailed(new JSONObject());
            }
        }catch (Exception e){
            mQuestionProApiCall.OnApiCallbackFailed(new JSONObject());
        }
    }

    private String getSurveyFeedbackApiPayload(Intercept intercept, String surveyType){
        try {
            JSONObject payloadObj = new JSONObject();
            payloadObj.put("interceptId",intercept.id);
            payloadObj.put("ruleGroupId", intercept.ruleGroupId);
            payloadObj.put("surveyId",intercept.surveyId);
            payloadObj.put("surveyType",surveyType);

            return payloadObj.toString();
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    private String getPlatformType(){
        return "1";
    }
}
