package com.questionpro.cxlib;

import android.content.Context;
import android.os.Looper;

import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.util.Log;

import com.questionpro.cxlib.QuestionProCX;
import com.questionpro.cxlib.CXConstants;
import com.questionpro.cxlib.CXGlobalInfo;
import com.questionpro.cxlib.IQuestionProApiCallback;

import com.questionpro.cxlib.dataconnect.CXHttpResponse;
import com.questionpro.cxlib.dataconnect.CXUploadClient;
import com.questionpro.cxlib.enums.Platform;
import com.questionpro.cxlib.enums.VisitorStatus;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class CXApiHandler {

    private final Context mContext;
    private final IQuestionProApiCallback mQuestionProApiCall;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CXApiHandler(Context context, IQuestionProApiCallback call){
        this.mContext = context;
        mQuestionProApiCall = call;
    }

    private void postSuccess(final Intercept intercept, final String surveyUrl) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                mQuestionProApiCall.onApiCallbackSuccess(intercept, surveyUrl);
            }
        });
    }

    private void postFailure(final JSONObject error) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                mQuestionProApiCall.OnApiCallbackFailed(error);
            }
        });
    }

    public void getInterceptSurvey(final Intercept intercept){
        ExecutorService myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    if (!CXUtils.isNetworkConnectionPresent(mContext)) {
                        postFailure(new JSONObject().put("error", new JSONObject().put("message", "No internet connection.")));
                        return;
                    }
                    getInterceptSurveyUrl(intercept);
                }catch (Exception e){
                    try {
                        postFailure(new JSONObject().put("error", e.getMessage()));
                    } catch (JSONException ex) {
                        Log.e("CXApiHandler", "Failed to build error payload", ex);
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
                        postFailure(new JSONObject().put("error", new JSONObject().put("message", "No internet connection.")));
                        return;
                    }
                    getSurveyUrl(surveyId);
                }catch (Exception e){
                    try {
                        postFailure(new JSONObject().put("error", e.getMessage()));
                    } catch (JSONException ex) {
                        Log.e("CXApiHandler", "Failed to build error payload", ex);
                    }
                }
            }
        });
    }

    public void getIntercept(){
        ExecutorService myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!CXUtils.isNetworkConnectionPresent(mContext)) {
                        postFailure(new JSONObject().put("error", new JSONObject().put("message", "No internet connection.")));
                        return;
                    }
                    getInterceptConfigurations();
                }catch (JSONException e){
                    try {
                        postFailure(new JSONObject().put("error", e.getMessage()));
                    } catch (JSONException ex) {
                        Log.e("CXApiHandler", "Failed to build error payload", ex);
                    }
                }
            }
        });
    }

    protected void submitFeedback(final Intercept intercept, final String type){
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

                    URL url = new URL(CXConstants.getSurveyFeedbackUrl());

                    CXHttpResponse response = CXUploadClient.uploadCXApi(url, headers, payload);

                    if (response != null) {
                        JSONObject jsonObject = new JSONObject(response.getContent());
                        //Log.d("Datta", "Survey feedback response: "+jsonObject.toString());
                    }
                }catch (Exception e){}
            }
        });
    }

    protected void excludedFeedback(final Intercept intercept){
        ExecutorService myExecutor = Executors.newSingleThreadExecutor();
        myExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String payload = getExcludedFeedbackApiPayload(intercept);

                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("x-app-key", CXGlobalInfo.getInstance().getApiKey());
                    headers.put("package-name", mContext.getPackageName());
                    headers.put("visitor-id", SharedPreferenceManager.getInstance(mContext).getVisitorsUUID());

                    URL url = new URL(CXConstants.getExcludedFeedbackUrl());

                    CXHttpResponse response = CXUploadClient.uploadCXApi(url, headers, payload);

                    if (response != null) {
                        JSONObject jsonObject = new JSONObject(response.getContent());
                        //Log.d("Datta", "Excluded feedback response: "+jsonObject.toString());
                    }
                }catch (Exception e){}
            }
        });
    }

    private void getInterceptConfigurations(){
        try {
            java.net.URL url = new URL(CXConstants.getInterceptsUrl());
            CXHttpResponse response = CXUploadClient.getCxApi(url, CXGlobalInfo.getInstance().getInterceptApiHeaders(mContext));

            if (response.isSuccessful()) {
                JSONObject jsonObject = new JSONObject(response.getContent());
                if (jsonObject.has(CXConstants.JSONResponseFields.PROJECT)) {
                    JSONObject projectJson = jsonObject.getJSONObject(CXConstants.JSONResponseFields.PROJECT);
                    SharedPreferenceManager.getInstance(mContext).saveProject(projectJson.toString());
                }
                SharedPreferenceManager.getInstance(mContext).saveVisitorsUUID(jsonObject.getJSONObject(CXConstants.JSONResponseFields.VISITOR).getString("uuid"));
                postSuccess(null, "SDK is Initialised");
            } else if (response.isRejectedPermanently()) {
                postFailure(new JSONObject(response.getContent()));
            } else {
                postFailure(new JSONObject().put("error", "Error in fetching the intercept settings"));
            }
        }catch (Exception e){
            postFailure(new JSONObject());
        }
    }


    private void getInterceptSurveyUrl(Intercept intercept){
        try {
            String payload = CXGlobalInfo.getInstance().getSurveyApiPayload(intercept, mContext);

            HashMap<String, String> headers = new HashMap<>();
            headers.put("x-app-key",CXGlobalInfo.getInstance().getApiKey());
            headers.put("package-name", mContext.getPackageName());

            URL url = new URL(CXConstants.getInterceptSurveyUrl(mContext));

            CXHttpResponse response = CXUploadClient.uploadCXApi(url, headers, payload);

            if (response != null) {
                if (response.isSuccessful()) {
                    JSONObject jsonObject = new JSONObject(response.getContent());
                    if (jsonObject.has(CXConstants.JSONResponseFields.CX_SURVEY_URL)) {
                        postSuccess(intercept, jsonObject.getString(CXConstants.JSONResponseFields.CX_SURVEY_URL));
                    } else {
                        postFailure(jsonObject);
                    }
                } else if (response.isRejectedPermanently() || response.isBadPayload()) {
                    JSONObject jsonObject = new JSONObject(response.getContent());
                    if (jsonObject.has("response")) {
                        postFailure(jsonObject.getJSONObject("response"));
                    } else if (jsonObject.has("message")) {
                        postFailure(jsonObject);
                    } else {
                        postFailure(new JSONObject());
                    }
                } else {
                    postFailure(new JSONObject());
                }
            }
        }catch (Exception e){
            postFailure(new JSONObject());
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
                    postSuccess(null, surveyUrl);
                } else {
                    postFailure(jsonObject);
                }
            } else if (response.isRejectedPermanently() || response.isBadPayload()) {
                JSONObject jsonObject = new JSONObject(response.getContent());
                if (jsonObject.has("response")) {
                    postFailure(jsonObject.getJSONObject("response"));
                } else if (jsonObject.has("message")) {
                    postFailure(jsonObject);
                } else {
                    postFailure(new JSONObject());
                }
            } else {
                postFailure(new JSONObject());
            }
        }catch (Exception e){
            postFailure(new JSONObject());
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

    private String getExcludedFeedbackApiPayload (Intercept intercept){
        try {
            JSONArray jsonArray = new JSONArray();
            JSONObject payloadObj = new JSONObject();
            payloadObj.put("interceptId",intercept.id);
            payloadObj.put("ruleGroupId", intercept.ruleGroupId);
            payloadObj.put("surveyId",intercept.surveyId);
            payloadObj.put("surveyType", VisitorStatus.EXCLUDED.name());

            jsonArray.put(payloadObj);
            return jsonArray.toString();
        }catch (Exception e){e.printStackTrace();}
        return "";
    }
}
