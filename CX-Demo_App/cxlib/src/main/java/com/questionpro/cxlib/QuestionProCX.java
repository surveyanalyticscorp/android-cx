package com.questionpro.cxlib;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.questionpro.cxlib.enums.Platform;
import com.questionpro.cxlib.enums.VisitorStatus;
import com.questionpro.cxlib.interfaces.IQuestionProInitCallback;
import com.questionpro.cxlib.interfaces.IQuestionProCallback;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.model.InterceptRule;
import com.questionpro.cxlib.model.TouchPoint;
import com.questionpro.cxlib.util.CXUtils;
import com.questionpro.cxlib.util.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Dattakunde on 14/04/16.
 */
public class QuestionProCX implements IQuestionProApiCallback, IQuestionProRulesCallback {
    private static final String LOG_TAG="QuestionProCX";
    private static final AtomicInteger runningActivities = new AtomicInteger(0);
    private static boolean isSessionAlive = false;
    private static boolean isInitialised = false;

    private static Context appContext;

    private IQuestionProInitCallback questionProInitCallback;
    private IQuestionProCallback questionProCallback;
    private ActivityLifecycleCallbacks activityLifecycleCallbacks;

    private static final ConcurrentHashMap<Integer, Set<String>> interceptSatisfiedRules = new ConcurrentHashMap<>();

    private QuestionProCX(){
    }

    private static class Holder {
        static final QuestionProCX INSTANCE = new QuestionProCX();
    }

    public static QuestionProCX getInstance(){
        return Holder.INSTANCE;
    }

    /**
     * Initializes the SDK
     */
    public synchronized void init(Context context, TouchPoint touchPoint, IQuestionProInitCallback callback){
        appContext = context.getApplicationContext();
        questionProInitCallback = callback;

        if(touchPoint == null){
            if(callback != null) {
                callback.onInitializationFailure("TouchPoint object is null.");
            }
            return;
        }

        activityLifecycleCallbacks = new ActivityLifecycleCallbacks(getStartActivityCount(touchPoint));
        ((Application) appContext).registerActivityLifecycleCallbacks(activityLifecycleCallbacks);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    CXUtils.printLog("Datta","Initialising the SDK");
                    CXGlobalInfo.getInstance().savePayLoad(touchPoint);
                    initialize();
                }catch (Exception e){
                    String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    new CXApiHandler(appContext).logError(msg, 500, "/init", e, "InitException");
                    callback.onInitializationFailure(msg);
                }
            }
        }, 2000);
    }

    private synchronized void launchFeedbackSurvey(long surveyId){
        if (runningActivities.get() == 0) {
            try {
                Intent intent = new Intent(appContext, InteractionActivity.class);
                intent.putExtra("SURVEY_ID", surveyId);
                if (!(appContext instanceof Activity)) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                appContext.startActivity(intent);
            }catch (Exception e){
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Log.e("QuestionPro", "Failed to launch activity", e);
                new CXApiHandler(appContext).logError(msg, 500, "/launchSurvey", e, "ActivityLaunchException");
            }
        }
    }

    IQuestionProInitCallback getInitCallback() {
        return questionProInitCallback;
    }

    /**
     * Returns the survey URL via callback
     */
    public void getSurveyUrl(IQuestionProCallback questionProCallback){
        this.questionProCallback = questionProCallback;
    }

    /**
     * @param tagName : TagName or screen name which is set while configuring the intercept rules.
     */
    public void setScreenVisited(String tagName){
        if(isInitialised) {
            MonitorAppEvents.getInstance().setTagNameCheckRules(tagName, appContext, QuestionProCX.this);
        }
    }

    public void setDataMappings(HashMap<String, String> customDataMappings){
        SharedPreferenceManager.getInstance(appContext).saveCustomDataMappings(customDataMappings);
    }

    public void closeSurveyWindow(){
        if (runningActivities.get() > 0) {
            if (InteractionActivity.currentActivity != null && !InteractionActivity.currentActivity.isFinishing()) {
                InteractionActivity.currentActivity.finish();
            }
        }
    }

    /**
     * Clears the session and resets preferences
     */
    protected void clearSession(){
        Log.i("QuestionPro","Clearing session.");
        isSessionAlive = false;
        isInitialised = false;
        MonitorAppEvents.getInstance().stopAllTimers();
        interceptSatisfiedRules.clear();
        SharedPreferenceManager.getInstance(appContext).resetPreferences();
    }

    protected void initialize() throws Exception{
        //mActivity = new WeakReference<>(activity);

        //final Context appContext = activity.getApplicationContext();
        ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
        Bundle metaData = ai.metaData;

        if (metaData != null) {
            String apiKey = metaData.getString(CXConstants.MANIFEST_KEY_API_KEY);
            CXUtils.printLog(LOG_TAG, "API key: " + apiKey);
            if(!CXUtils.isEmpty(apiKey))
                CXGlobalInfo.getInstance().setApiKey(apiKey);
        }
        if (CXUtils.isEmpty(CXGlobalInfo.getInstance().getApiKey())) {
            throw new IllegalStateException("API key not found. Set it via AndroidManifest meta-data or TouchPoint.Builder.");
        }

        CXGlobalInfo.getInstance().setUUID(CXUtils.getUniqueDeviceId(appContext));

        fetchInterceptSettings();
    }

    protected void fetchInterceptSettings(){
        if(!isSessionAlive) {
            isSessionAlive = true;
            new CXApiHandler(appContext, this).getIntercept();
        }
    }

    protected void refreshInterceptSettings(final String tagName){
        new CXApiHandler(appContext, new IQuestionProApiCallback() {
            @Override
            public void OnApiCallbackFailed(JSONObject error) {
                String reason = error.optString("error", error.toString());
                Log.e("QuestionPro", "Error in fetching intercept settings: " + reason);
                new CXApiHandler(appContext).logError(reason, 0, CXConstants.PATH_INTERCEPTS, null, "RefreshFailed");
            }

            @Override
            public void onApiCallbackSuccess(Intercept intercept, String surveyUrl) {
                setScreenVisited(tagName);
            }
        }).getIntercept();
    }

    // Called only for getIntercept() — initialization path
    @Override
    public void onApiCallbackSuccess(Intercept intercept, String surveyUrl) {
        isInitialised = true;
        CXUtils.printLog("Datta", "Initialization API response: " + surveyUrl);
        if (questionProInitCallback != null) {
            questionProInitCallback.onInitializationSuccess(surveyUrl);
        }
        setUpIntercept();
    }

    // Called only for getIntercept() — initialization path
    @Override
    public void OnApiCallbackFailed(JSONObject error) {
        String reason = error.optString("error", error.toString());
        Log.e(LOG_TAG, "Intercept fetch failed during init: " + reason);
        new CXApiHandler(appContext).logError(reason, 0, CXConstants.PATH_INTERCEPTS, null, "InitFailed");
        if (questionProInitCallback != null) {
            questionProInitCallback.onInitializationFailure(reason);
        }
    }

    private void setUpIntercept(){
        try{
            String projectJson = SharedPreferenceManager.getInstance(appContext).getProject();
            if (projectJson != null && !projectJson.trim().isEmpty()) {
                JSONObject projectObj = new JSONObject(projectJson);
                JSONArray interceptArray = projectObj.getJSONArray("intercepts");
                for (int i = 0; i < interceptArray.length(); i++) {
                    JSONObject jsonObject = interceptArray.getJSONObject(i);
                    //setUpTimeSpendIntercept(obj);
                    Intercept intercept = Intercept.fromJSON(jsonObject);
                    //CXUtils.printLog("Datta","checkShouldShowSampling: " +checkShouldShowSampling(intercept));
                    if(checkShouldShowSampling(intercept)) {
                        for (InterceptRule rule : intercept.interceptRule) {
                            if (rule.name.equals(InterceptRuleType.TIME_SPENT.name())) {
                                MonitorAppEvents.getInstance().appSessionStarted(intercept.id, rule, QuestionProCX.this);
                            } else if (rule.name.equals(InterceptRuleType.DAY.name())) {
                                checkDayRule(rule, intercept.id);
                            } else if (rule.name.equals(InterceptRuleType.DATE.name())) {
                                checkDateRule(rule, intercept.id);
                            }
                        }
                    }
                }
            }else{
                Log.w("QuestionProCX", "Project JSON is null or empty. Fetching the intercept settings...");
                fetchInterceptSettings();
            }
        }catch (Exception e){e.printStackTrace();}
    }

    protected boolean checkShouldShowSampling(Intercept intercept){
        // 1. Server-provided status takes priority (set on previous sessions or fetched fresh)
        if (!CXUtils.isEmpty(intercept.interceptMetadata.visitorStatus)) {
            return !intercept.interceptMetadata.visitorStatus.equals(VisitorStatus.EXCLUDED.name());
        }

        // 2. Check locally cached status from this session to avoid re-computing and re-sending feedback
        String cachedStatus = SharedPreferenceManager.getInstance(appContext).getVisitorStatusForIntercept(intercept.id);
        if (!CXUtils.isEmpty(cachedStatus)) {
            return !cachedStatus.equals(VisitorStatus.EXCLUDED.name());
        }

        // 3. First time this session: compute sampling decision and cache it
        int samplingRate = intercept.interceptSettings.samplingRate;
        if (samplingRate >= 100) {
            return true;
        }

        int matchedCount = intercept.interceptMetadata.matchedCount;
        int excludedCount = intercept.interceptMetadata.excludedCount;
        int total = matchedCount + excludedCount;
        boolean isIncluded = total == 0 ? (samplingRate > 0) : (matchedCount * 100 / total) < samplingRate;

        if (!isIncluded) {
            SharedPreferenceManager.getInstance(appContext)
                    .saveVisitorStatusForIntercept(intercept.id, VisitorStatus.EXCLUDED.name());
            new CXApiHandler(appContext).excludedFeedback(intercept);
        }
        return isIncluded;
    }

    private void markRuleSatisfied(int interceptId, String ruleType) {
        synchronized (interceptSatisfiedRules) {
            Set<String> existingSet = interceptSatisfiedRules.get(interceptId);
            Set<String> updatedSet = (existingSet != null) ? new HashSet<>(existingSet) : new HashSet<>();
            updatedSet.add(ruleType);
            interceptSatisfiedRules.put(interceptId, updatedSet);
        }
        checkAllRulesForIntercept(interceptId);
    }

    private void checkDateRule(InterceptRule rule, int interceptId){
        if(!CXUtils.isEmpty(rule.value)) {
            String[] dates = rule.value.split(",");
            for(String date: dates) {
                if (Integer.parseInt(date) == Integer.parseInt(DateTimeUtils.getCurrentDayOfMonth())) {
                    markRuleSatisfied(interceptId, InterceptRuleType.DATE.name());
                }
            }
        }
    }

    private void checkDayRule(InterceptRule rule, int interceptId){
        if(!CXUtils.isEmpty(rule.value)) {
            String[] days = rule.value.split(",");
            for (String day : days) {
                if (day.equalsIgnoreCase(DateTimeUtils.getCurrentDayOfWeek())) {
                    markRuleSatisfied(interceptId, InterceptRuleType.DAY.name());
                }
            }
        }
    }

    @Override
    public void onViewCountRuleSatisfied(int interceptId) {
        markRuleSatisfied(interceptId, InterceptRuleType.VIEW_COUNT.name());
    }

    @Override
    public void onTimeSpendSatisfied(int interceptId) {
        CXUtils.printLog("Datta", "Trigger the intercept as time is satisfied: " + interceptId);
        markRuleSatisfied(interceptId, InterceptRuleType.TIME_SPENT.name());
    }

    private void checkAllRulesForIntercept(int interceptId){
        try {
            Intercept intercept = SharedPreferenceManager.getInstance(appContext).getInterceptById(interceptId);
            if (intercept == null) return;
            if(shouldSurveyLaunch(intercept)) {
                Set<String> temp;
                synchronized (interceptSatisfiedRules) {
                    Set<String> existing = interceptSatisfiedRules.get(interceptId);
                    temp = (existing != null) ? new HashSet<>(existing) : null;
                }
                if (temp == null) return;
                CXUtils.printLog("Datta", interceptId + " Satisfied intercepts: " + temp);

                if (intercept.condition.equals(InterceptCondition.OR.name())) {
                    launchFeedbackSurvey(intercept);
                } else if (intercept.interceptRule.size() == temp.size()) {
                    launchFeedbackSurvey(intercept);
                }
            }
        }catch (Exception e){
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            Log.e(LOG_TAG, "Error checking rules for intercept: " + interceptId, e);
            new CXApiHandler(appContext).logError(msg, 500, "/checkRules", e, "RuleCheckException");
        }
    }

    private boolean shouldSurveyLaunch(Intercept intercept){
        boolean doesSurveyAlreadyLaunched = SharedPreferenceManager.getInstance(appContext).isSurveyAlreadyLaunched(intercept.id);
        boolean allowMultipleResponse = intercept.interceptSettings.allowMultipleResponse;
        return allowMultipleResponse || !doesSurveyAlreadyLaunched;
    }


    private synchronized void launchFeedbackSurvey(Intercept intercept){
        CXUtils.printLog("Datta",isSessionAlive +" Running activity count: "+runningActivities.get());
        if(InterceptType.SURVEY_URL.name().equals(intercept.type)){
            new CXApiHandler(appContext, new IQuestionProApiCallback() {
                @Override
                public void onApiCallbackSuccess(Intercept interceptResult, String surveyUrl) {
                    new CXApiHandler(appContext).submitFeedback(interceptResult, VisitorStatus.MATCHED.name());
                    if (questionProCallback != null) {
                        questionProCallback.getSurveyUrl(surveyUrl);
                    }
                }
                @Override
                public void OnApiCallbackFailed(JSONObject error) {
                    String reason = error.optString("error", error.toString());
                    Log.e(LOG_TAG, "Failed to fetch survey URL for SURVEY_URL intercept: " + reason);
                    new CXApiHandler(appContext).logError(reason, 0, CXConstants.PATH_INTERCEPT_SURVEY, null, "HttpError");
                }
            }).getInterceptSurvey(intercept);
        } else {
            int triggerDelay = intercept.interceptSettings.triggerDelayInSeconds * 1000;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (runningActivities.get() == 0 && isSessionAlive){
                            runningActivities.incrementAndGet();
                            Intent intent = new Intent(appContext, InteractionActivity.class);
                            intent.putExtra("INTERCEPT", intercept);
                            if (!(appContext instanceof Activity)) {
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            }
                            appContext.startActivity(intent);
                        }
                    }catch (Exception e){
                        String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                        Log.e("QuestionPro", "Failed to launch activity", e);
                        new CXApiHandler(appContext).logError(msg, 500, "/launchSurvey", e, "ActivityLaunchException");
                    }
                }
            }, triggerDelay);
        }
    }

    /** Use this function while launching the survey Activity. Check if feedback activity is already running */
    public void onStart(Activity activity){
        //init(activity);
        CXUtils.printLog("Datta","Interaction Activity onStart");
        ActivityLifecycleManager.activityStarted(activity);
        if (runningActivities.get() == 0) {
            //CXPayloadWorker.appWentToForeground(activity);
        }
    }

    public void onStop(Activity activity){
        CXUtils.printLog("Datta","Interaction Activity onStop");
        try {
            ActivityLifecycleManager.activityStopped(activity);
            if (runningActivities.decrementAndGet() < 0) {
                Log.e(LOG_TAG,"Incorrect number of running Activities encountered. Resetting to 0. Did you make sure to call Apptentive.onStart() and Apptentive.onStop() in all your Activities?");
                runningActivities.set(0);
            }
            // If there are no running activities, wake the thread so it can stop immediately and gracefully.
            if (runningActivities.get() == 0) {
                //CXPayloadWorker.appWentToBackground();
            }

        } catch (Exception e) {
            Log.w(LOG_TAG,"Error stopping Apptentive Activity.", e);
        }
    }

    public void cleanup() {
        if (activityLifecycleCallbacks != null && appContext != null) {
            ((Application) appContext).unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
            activityLifecycleCallbacks = null;
        }
    }

    private int getStartActivityCount(TouchPoint touchPoint){
        if(touchPoint.getPlatform().equals(Platform.FLUTTER)){
            return 1;
        }

        if(touchPoint.getPlatform().equals(Platform.REACT_NATIVE)){
            return 1;
        }
        return 0;
    }
}
