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

    private static final String LOG_TAG = "CXApiHandler";

    private final Context mContext;
    private IQuestionProApiCallback mQuestionProApiCall;
    private IInteractionCallback iInteractionCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CXApiHandler(Context context, IQuestionProApiCallback call){
        this.mContext = context;
        mQuestionProApiCall = call;
    }

    public CXApiHandler(Context context, IInteractionCallback call){
        this.mContext = context;
        iInteractionCallback = call;
    }

    // For fire-and-forget calls (submitFeedback, excludedFeedback) that need no callback
    public CXApiHandler(Context context){
        this.mContext = context;
    }

    private void postSuccess(final Intercept intercept, final String surveyUrl) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                if(iInteractionCallback != null)
                    iInteractionCallback.onSurveyUrlReady(intercept, surveyUrl);
                else if(mQuestionProApiCall != null)
                    mQuestionProApiCall.onApiCallbackSuccess(intercept, surveyUrl);
            }
        });
    }

    private void postFailure(final JSONObject error) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                if(iInteractionCallback != null)
                    iInteractionCallback.onSurveyUrlFailed(error);
                else if(mQuestionProApiCall != null)
                    mQuestionProApiCall.OnApiCallbackFailed(error);
            }
        });
    }

    public void getInterceptSurvey(final Intercept intercept){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!CXUtils.isNetworkConnectionPresent(mContext)) {
                        postFailure(buildError("No internet connection."));
                        return;
                    }
                    getInterceptSurveyUrl(intercept);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "getInterceptSurvey failed", e);
                    postFailure(buildError(e.getMessage()));
                }
            }
        });
        executor.shutdown();
    }

    public void getSurvey(final long surveyId){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!CXUtils.isNetworkConnectionPresent(mContext)) {
                        postFailure(buildError("No internet connection."));
                        return;
                    }
                    getSurveyUrl(surveyId);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "getSurvey failed", e);
                    postFailure(buildError(e.getMessage()));
                }
            }
        });
        executor.shutdown();
    }

    public void getIntercept(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!CXUtils.isNetworkConnectionPresent(mContext)) {
                        postFailure(buildError("No internet connection."));
                        return;
                    }
                    getInterceptConfigurations();
                } catch (Exception e) {
                    // Catches JSONException, IOException, NullPointerException, etc.
                    Log.e(LOG_TAG, "getIntercept failed", e);
                    postFailure(buildError(e.getMessage()));
                }
            }
        });
        executor.shutdown();
    }

    protected void submitFeedback(final Intercept intercept, final String type){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String payload = getSurveyFeedbackApiPayload(intercept, type);
                    if (payload.isEmpty()) {
                        Log.w(LOG_TAG, "submitFeedback: empty payload, skipping.");
                        return;
                    }

                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("x-app-key", CXGlobalInfo.getInstance().getApiKey());
                    headers.put("package-name", mContext.getPackageName());
                    headers.put("visitor-id", SharedPreferenceManager.getInstance(mContext).getVisitorsUUID());

                    URL url = new URL(CXConstants.getSurveyFeedbackUrl());
                    CXHttpResponse response = CXUploadClient.uploadCXApi(url, headers, payload);

                    if (response == null || !response.isSuccessful()) {
                        Log.w(LOG_TAG, "submitFeedback: non-success response code: " +
                                (response != null ? response.getCode() : "null"));
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "submitFeedback failed", e);
                }
            }
        });
        executor.shutdown();
    }

    protected void excludedFeedback(final Intercept intercept){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String payload = getExcludedFeedbackApiPayload(intercept);
                    if (payload.isEmpty()) {
                        Log.w(LOG_TAG, "excludedFeedback: empty payload, skipping.");
                        return;
                    }

                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("x-app-key", CXGlobalInfo.getInstance().getApiKey());
                    headers.put("package-name", mContext.getPackageName());
                    headers.put("visitor-id", SharedPreferenceManager.getInstance(mContext).getVisitorsUUID());

                    URL url = new URL(CXConstants.getExcludedFeedbackUrl());
                    CXHttpResponse response = CXUploadClient.uploadCXApi(url, headers, payload);

                    if (response == null || !response.isSuccessful()) {
                        Log.w(LOG_TAG, "excludedFeedback: non-success response code: " +
                                (response != null ? response.getCode() : "null"));
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "excludedFeedback failed", e);
                }
            }
        });
        executor.shutdown();
    }

    private void getInterceptConfigurations() throws Exception {
        URL url = new URL(CXConstants.getInterceptsUrl());
        CXHttpResponse response = CXUploadClient.getCxApi(url, CXGlobalInfo.getInstance().getInterceptApiHeaders(mContext));

        if (response.isSuccessful()) {
            String content = response.getContent();
            if (CXUtils.isEmpty(content)) {
                postFailure(buildError("Empty response body from intercepts API."));
                return;
            }
            JSONObject jsonObject = new JSONObject(content);
            if (jsonObject.has(CXConstants.JSONResponseFields.PROJECT)) {
                JSONObject projectJson = jsonObject.getJSONObject(CXConstants.JSONResponseFields.PROJECT);
                SharedPreferenceManager.getInstance(mContext).saveProject(projectJson.toString());
            }
            if (jsonObject.has(CXConstants.JSONResponseFields.VISITOR)) {
                String uuid = jsonObject.getJSONObject(CXConstants.JSONResponseFields.VISITOR).optString("uuid", "");
                if (!CXUtils.isEmpty(uuid)) {
                    SharedPreferenceManager.getInstance(mContext).saveVisitorsUUID(uuid);
                }
            }
            postSuccess(null, "SDK is Initialised");
        } else if (response.isRejectedPermanently()) {
            String content = response.getContent();
            postFailure(CXUtils.isEmpty(content) ? buildError("Request rejected (4xx).") : new JSONObject(content));
        } else {
            Log.w(LOG_TAG, "getInterceptConfigurations: unexpected response code " + response.getCode());
            postFailure(buildError("Unexpected error fetching intercept settings. Code: " + response.getCode()));
        }
    }

    private void getInterceptSurveyUrl(Intercept intercept) throws Exception {
        String payload = CXGlobalInfo.getInstance().getSurveyApiPayload(intercept, mContext);

        HashMap<String, String> headers = new HashMap<>();
        headers.put("x-app-key", CXGlobalInfo.getInstance().getApiKey());
        headers.put("package-name", mContext.getPackageName());

        URL url = new URL(CXConstants.getInterceptSurveyUrl(mContext));
        CXHttpResponse response = CXUploadClient.uploadCXApi(url, headers, payload);

        String content = response.getContent();

        if (response.isSuccessful()) {
            if (CXUtils.isEmpty(content)) {
                postFailure(buildError("Empty response body from survey URL API."));
                return;
            }
            JSONObject jsonObject = new JSONObject(content);
            if (jsonObject.has(CXConstants.JSONResponseFields.CX_SURVEY_URL)) {
                postSuccess(intercept, jsonObject.getString(CXConstants.JSONResponseFields.CX_SURVEY_URL));
            } else {
                Log.w(LOG_TAG, "getInterceptSurveyUrl: surveyURL key missing in response.");
                postFailure(jsonObject);
            }
        } else if (response.isRejectedPermanently() || response.isBadPayload()) {
            Log.w(LOG_TAG, "getInterceptSurveyUrl: rejected with code " + response.getCode());
            if (CXUtils.isEmpty(content)) {
                postFailure(buildError("Request rejected. Code: " + response.getCode()));
                return;
            }
            JSONObject jsonObject = new JSONObject(content);
            if (jsonObject.has("response")) {
                postFailure(jsonObject.getJSONObject("response"));
            } else {
                postFailure(jsonObject);
            }
        } else {
            Log.w(LOG_TAG, "getInterceptSurveyUrl: unexpected response code " + response.getCode());
            postFailure(buildError("Unexpected error fetching survey URL. Code: " + response.getCode()));
        }
    }

    private void getSurveyUrl(long surveyId) throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charSet=UTF-8");
        headers.put("api-key", CXGlobalInfo.getInstance().getApiKey());

        URL url = new URL(CXConstants.getSurveyUrl(surveyId));
        CXHttpResponse response = CXUploadClient.getCxApi(url, headers);

        String content = response.getContent();

        if (response.isSuccessful()) {
            if (CXUtils.isEmpty(content)) {
                postFailure(buildError("Empty response body from survey API."));
                return;
            }
            JSONObject jsonObject = new JSONObject(content);
            if (jsonObject.has(CXConstants.JSONResponseFields.RESPONSE)) {
                JSONObject responseObj = jsonObject.getJSONObject(CXConstants.JSONResponseFields.RESPONSE);
                String surveyUrl = responseObj.optString(CXConstants.JSONResponseFields.CORE_SURVEY_URL, "");
                if (CXUtils.isEmpty(surveyUrl)) {
                    postFailure(buildError("Survey URL not found in response."));
                } else {
                    postSuccess(null, surveyUrl);
                }
            } else {
                Log.w(LOG_TAG, "getSurveyUrl: 'response' key missing.");
                postFailure(jsonObject);
            }
        } else if (response.isRejectedPermanently() || response.isBadPayload()) {
            Log.w(LOG_TAG, "getSurveyUrl: rejected with code " + response.getCode());
            if (CXUtils.isEmpty(content)) {
                postFailure(buildError("Request rejected. Code: " + response.getCode()));
                return;
            }
            JSONObject jsonObject = new JSONObject(content);
            if (jsonObject.has("response")) {
                postFailure(jsonObject.getJSONObject("response"));
            } else {
                postFailure(jsonObject);
            }
        } else {
            Log.w(LOG_TAG, "getSurveyUrl: unexpected response code " + response.getCode());
            postFailure(buildError("Unexpected error fetching survey URL. Code: " + response.getCode()));
        }
    }

    // --- Payload builders ---

    private String getSurveyFeedbackApiPayload(Intercept intercept, String surveyType){
        try {
            JSONObject payloadObj = new JSONObject();
            payloadObj.put("interceptId", intercept.id);
            payloadObj.put("ruleGroupId", intercept.ruleGroupId);
            payloadObj.put("surveyId", intercept.surveyId);
            payloadObj.put("surveyType", surveyType);
            return payloadObj.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to build survey feedback payload", e);
        }
        return "";
    }

    private String getExcludedFeedbackApiPayload(Intercept intercept){
        try {
            JSONArray jsonArray = new JSONArray();
            JSONObject payloadObj = new JSONObject();
            payloadObj.put("interceptId", intercept.id);
            payloadObj.put("ruleGroupId", intercept.ruleGroupId);
            payloadObj.put("surveyId", intercept.surveyId);
            payloadObj.put("surveyType", VisitorStatus.EXCLUDED.name());
            jsonArray.put(payloadObj);
            return jsonArray.toString();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to build excluded feedback payload", e);
        }
        return "";
    }

    private JSONObject buildError(String message) {
        try {
            return new JSONObject().put("error", message != null ? message : "Unknown error");
        } catch (JSONException e) {
            return new JSONObject();
        }
    }
}
