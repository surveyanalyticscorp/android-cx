package com.questionpro.cxlib;

import android.content.Context;
import android.os.Looper;

import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.util.Log;

import com.questionpro.cxlib.CXConstants;
import com.questionpro.cxlib.CXGlobalInfo;
import com.questionpro.cxlib.IQuestionProApiCallback;

import com.questionpro.cxlib.dataconnect.CXHttpResponse;
import com.questionpro.cxlib.dataconnect.CXUploadClient;
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

    // For fire-and-forget calls (submitFeedback, excludedFeedback, logError) that need no callback
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
                else
                    Log.w(LOG_TAG, "postSuccess: no callback registered, result dropped.");
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
                else
                    Log.w(LOG_TAG, "postFailure: no callback registered. Error: " + error);
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
                    logError(e.getMessage(), 500, CXConstants.PATH_INTERCEPT_SURVEY, e, "Exception");
                    postFailure(buildError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
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
                    logError(e.getMessage(), 500, CXConstants.PATH_SURVEY, e, "Exception");
                    postFailure(buildError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
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
                    Log.e(LOG_TAG, "getIntercept failed", e);
                    logError(e.getMessage(), 500, CXConstants.PATH_INTERCEPTS, e, "Exception");
                    postFailure(buildError(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
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
                        int code = response != null ? response.getCode() : 500;
                        String msg = "submitFeedback failed with code: " + code;
                        Log.w(LOG_TAG, msg);
                        logError(msg, code, CXConstants.PATH_SURVEY_FEEDBACK, null, "HttpError");
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "submitFeedback failed", e);
                    logError(e.getMessage(), 500, CXConstants.PATH_SURVEY_FEEDBACK, e, "Exception");
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
                        int code = response != null ? response.getCode() : 500;
                        String msg = "excludedFeedback failed with code: " + code;
                        Log.w(LOG_TAG, msg);
                        logError(msg, code, CXConstants.PATH_EXCLUDED_FEEDBACK, null, "HttpError");
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "excludedFeedback failed", e);
                    logError(e.getMessage(), 500, CXConstants.PATH_EXCLUDED_FEEDBACK, e, "Exception");
                }
            }
        });
        executor.shutdown();
    }

    private void getInterceptConfigurations() throws Exception {
        flushErrorLogQueue();
        URL url = new URL(CXConstants.getInterceptsUrl());
        CXHttpResponse response = CXUploadClient.getCxApi(url, CXGlobalInfo.getInstance().getInterceptApiHeaders(mContext));

        if (response.isException()) {
            logError("Network error fetching intercept settings.", 503, CXConstants.PATH_INTERCEPTS, null, "NetworkError");
            postFailure(buildError("Network error fetching intercept settings."));
        } else if (response.isSuccessful()) {
            String content = response.getContent();
            if (CXUtils.isEmpty(content)) {
                logError("Empty response body from intercepts API.", 204, CXConstants.PATH_INTERCEPTS, null, "EmptyResponse");
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
            logError("Intercept settings request rejected.", response.getCode(), CXConstants.PATH_INTERCEPTS, null, "HttpError");
            postFailure(CXUtils.isEmpty(content) ? buildError("Request rejected (4xx).") : parseErrorBody(content));
        } else {
            String msg = "Unexpected error fetching intercept settings. Code: " + response.getCode();
            Log.w(LOG_TAG, msg);
            logError(msg, response.getCode(), CXConstants.PATH_INTERCEPTS, null, "HttpError");
            postFailure(buildError(msg));
        }
    }

    private void getInterceptSurveyUrl(Intercept intercept) throws Exception {
        String payload = CXGlobalInfo.getInstance().getSurveyApiPayload(intercept, mContext);

        HashMap<String, String> headers = new HashMap<>();
        headers.put("x-app-key", CXGlobalInfo.getInstance().getApiKey()+"12");
        headers.put("package-name", mContext.getPackageName());

        URL url = new URL(CXConstants.getInterceptSurveyUrl(mContext));
        CXHttpResponse response = CXUploadClient.uploadCXApi(url, headers, payload);

        String content = response.getContent();

        if (response.isException()) {
            logError("Network error fetching survey URL.", 503, CXConstants.PATH_INTERCEPT_SURVEY, null, "NetworkError");
            postFailure(buildError("Network error fetching survey URL."));
        } else if (response.isSuccessful()) {
            if (CXUtils.isEmpty(content)) {
                logError("Empty response body from survey URL API.", 204, CXConstants.PATH_INTERCEPT_SURVEY, null, "EmptyResponse");
                postFailure(buildError("Empty response body from survey URL API."));
                return;
            }
            JSONObject jsonObject = new JSONObject(content);
            if (jsonObject.has(CXConstants.JSONResponseFields.CX_SURVEY_URL)) {
                postSuccess(intercept, jsonObject.getString(CXConstants.JSONResponseFields.CX_SURVEY_URL));
            } else {
                Log.w(LOG_TAG, "getInterceptSurveyUrl: surveyURL key missing in response.");
                logError("surveyURL key missing in response.", 200, CXConstants.PATH_INTERCEPT_SURVEY, null, "ParseError");
                postFailure(jsonObject);
            }
        } else if (response.isRejectedPermanently() || response.isBadPayload()) {
            Log.w(LOG_TAG, "getInterceptSurveyUrl: rejected with code " + response.getCode());
            if (CXUtils.isEmpty(content)) {
                logError("Survey URL request rejected. Code: " + response.getCode(), response.getCode(), CXConstants.PATH_INTERCEPT_SURVEY, null, "HttpError");
                postFailure(buildError("Request rejected. Code: " + response.getCode()));
                return;
            }
            JSONObject errorJson = parseErrorBody(content);
            logError(extractReason(errorJson), response.getCode(), CXConstants.PATH_INTERCEPT_SURVEY, null, "HttpError");
            postFailure(errorJson.has("response") ? errorJson.getJSONObject("response") : errorJson);
        } else {
            String msg = "Unexpected error fetching survey URL. Code: " + response.getCode();
            Log.w(LOG_TAG, msg);
            logError(msg, response.getCode(), CXConstants.PATH_INTERCEPT_SURVEY, null, "HttpError");
            postFailure(buildError(msg));
        }
    }

    private void getSurveyUrl(long surveyId) throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json; charSet=UTF-8");
        headers.put("api-key", CXGlobalInfo.getInstance().getApiKey());

        URL url = new URL(CXConstants.getSurveyUrl(surveyId));
        CXHttpResponse response = CXUploadClient.getCxApi(url, headers);

        String content = response.getContent();

        if (response.isException()) {
            logError("Network error fetching survey URL.", 503, CXConstants.PATH_SURVEY, null, "NetworkError");
            postFailure(buildError("Network error fetching survey URL."));
        } else if (response.isSuccessful()) {
            if (CXUtils.isEmpty(content)) {
                logError("Empty response body from survey API.", 204, CXConstants.PATH_SURVEY, null, "EmptyResponse");
                postFailure(buildError("Empty response body from survey API."));
                return;
            }
            JSONObject jsonObject = new JSONObject(content);
            if (jsonObject.has(CXConstants.JSONResponseFields.RESPONSE)) {
                JSONObject responseObj = jsonObject.getJSONObject(CXConstants.JSONResponseFields.RESPONSE);
                String surveyUrl = responseObj.optString(CXConstants.JSONResponseFields.CORE_SURVEY_URL, "");
                if (CXUtils.isEmpty(surveyUrl)) {
                    logError("Survey URL not found in response.", 200, CXConstants.PATH_SURVEY, null, "ParseError");
                    postFailure(buildError("Survey URL not found in response."));
                } else {
                    postSuccess(null, surveyUrl);
                }
            } else {
                Log.w(LOG_TAG, "getSurveyUrl: 'response' key missing.");
                logError("'response' key missing in survey API response.", 200, CXConstants.PATH_SURVEY, null, "ParseError");
                postFailure(jsonObject);
            }
        } else if (response.isRejectedPermanently() || response.isBadPayload()) {
            Log.w(LOG_TAG, "getSurveyUrl: rejected with code " + response.getCode());
            if (CXUtils.isEmpty(content)) {
                logError("Survey request rejected. Code: " + response.getCode(), response.getCode(), CXConstants.PATH_SURVEY, null, "HttpError");
                postFailure(buildError("Request rejected. Code: " + response.getCode()));
                return;
            }
            JSONObject errorJson = parseErrorBody(content);
            logError(extractReason(errorJson), response.getCode(), CXConstants.PATH_SURVEY, null, "HttpError");
            postFailure(errorJson.has("response") ? errorJson.getJSONObject("response") : errorJson);
        } else {
            String msg = "Unexpected error fetching survey URL. Code: " + response.getCode();
            Log.w(LOG_TAG, msg);
            logError(msg, response.getCode(), CXConstants.PATH_SURVEY, null, "HttpError");
            postFailure(buildError(msg));
        }
    }

    // --- Error logging ---

    void logError(String message, int httpStatus, String path, Exception exception, String errorType) {
        final String payload = buildErrorLogPayload(message, httpStatus, path, exception, errorType);
        if (payload.isEmpty()) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (postErrorLog(payload)) return;

                // retry once after 2 seconds
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                if (postErrorLog(payload)) return;

                // both attempts failed — queue for next init
                SharedPreferenceManager.getInstance(mContext).enqueueErrorLog(payload);
                Log.w(LOG_TAG, "Error log queued for retry on next SDK init.");
            }
        });
        executor.shutdown();
    }

    private boolean postErrorLog(String payload) {
        try {
            HashMap<String, String> headers = new HashMap<>();
            headers.put("x-app-key", CXGlobalInfo.getInstance().getApiKey());
            headers.put("package-name", mContext.getPackageName());
            headers.put("visitor-id", SharedPreferenceManager.getInstance(mContext).getVisitorsUUID());
            headers.put("Content-Type", "application/json");

            URL url = new URL(CXConstants.getErrorLogsUrl());
            CXHttpResponse response = CXUploadClient.uploadCXApi(url, headers, payload);
            return response != null && response.isSuccessful();
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error log POST failed", e);
            return false;
        }
    }

    private void flushErrorLogQueue() {
        JSONArray queue = SharedPreferenceManager.getInstance(mContext).drainErrorLogQueue();
        if (queue.length() == 0) return;

        Log.d(LOG_TAG, "Flushing " + queue.length() + " queued error log(s).");
        for (int i = 0; i < queue.length(); i++) {
            try {
                String payload = queue.getString(i);
                if (!postErrorLog(payload)) {
                    // server still down — re-queue and stop to avoid hammering
                    SharedPreferenceManager.getInstance(mContext).enqueueErrorLog(payload);
                    Log.w(LOG_TAG, "Error log flush failed at index " + i + ", re-queued remaining.");
                    break;
                }
            } catch (Exception ignored) {}
        }
    }

    private String buildErrorLogPayload(String message, int httpStatus, String path, Exception exception, String errorType) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("message", message != null ? message : "Unknown error");
            payload.put("httpStatus", httpStatus);
            payload.put("path", path != null ? path : "Unknown");
            payload.put("stacktrace", exception != null ? stackTraceToString(exception) : "N/A");

            JSONObject context = new JSONObject();
            context.put("platform", CXGlobalInfo.getInstance().getPlatform().name().toLowerCase());
            context.put("sdkVersion", BuildConfig.SDK_VERSION);
            context.put("errorType", errorType != null ? errorType : "Unknown");

            payload.put("context", context);
            return payload.toString();
        } catch (Exception e) {
            Log.w(LOG_TAG, "Failed to build error log payload", e);
            return "";
        }
    }

    private String extractReason(JSONObject response) {
        try {
            if (response != null) {
                if (response.has("error")) {
                    Object error = response.get("error");
                    if (error instanceof JSONObject) {
                        String msg = ((JSONObject) error).optString("message", "");
                        if (!msg.isEmpty()) return msg;
                    } else {
                        String msg = error.toString();
                        if (!msg.isEmpty()) return msg;
                    }
                }
                if (response.has("message")) {
                    return response.getString("message");
                }
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "Failed to extract reason from error response", e);
        }
        return "Unknown error";
    }

    private String stackTraceToString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        return sb.toString();
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

    private JSONObject parseErrorBody(String content) {
        try {
            return new JSONObject(content);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error response body is not valid JSON: " + content);
            return buildError("Server error (non-JSON response).");
        }
    }
}
