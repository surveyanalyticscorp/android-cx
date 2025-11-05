package com.questionpro.cxlib;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.questionpro.cxlib.enums.ConfigType;

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
import java.util.Objects;
import java.util.Set;

/**
 * Created by Dattakunde on 14/04/16.
 */
public class QuestionProCX implements IQuestionProApiCallback, IQuestionProRulesCallback {
    private static final String LOG_TAG="QuestionProCX";
    private static int runningActivities;
    private static boolean isSessionAlive = false;
    private ProgressDialog progressDialog;

    private static Context appContext;

    private static QuestionProCX mInstance = null;

    private IQuestionProInitCallback questionProInitCallback;
    private IQuestionProCallback questionProCallback;

    private static final HashMap<Integer, Set<String>> interceptSatisfiedRules = new HashMap<>();
    public QuestionProCX(){
    }

    public synchronized static QuestionProCX getInstance(){
        if(mInstance == null){
            mInstance = new QuestionProCX();
        }
        return mInstance;
    }

    /**
     * Initializes the SDK
     */
    public synchronized void init(Context context, TouchPoint touchPoint, IQuestionProInitCallback callback){
        appContext = context;
        questionProInitCallback = callback;

        if (appContext instanceof Application) {
            ((Application) appContext).registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks());
        } else if (appContext.getApplicationContext() instanceof Application) {
            ((Application) appContext.getApplicationContext()).registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks());
        }

        if(touchPoint == null){
            if(callback != null) {
                callback.onInitializationFailure("TouchPoint object is null.");
            }
            return;
        }

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    CXUtils.printLog("Datta","Initialising the SDK");
                    CXGlobalInfo.getInstance().savePayLoad(touchPoint);
                    initialize();
                }catch (Exception e){
                    callback.onInitializationFailure(e.getMessage());
                }
            }
        }, 3000);
    }

    private synchronized void launchFeedbackSurvey(long surveyId){
        if (runningActivities == 0) {
            try {
                Intent intent = new Intent(appContext, InteractionActivity.class);
                intent.putExtra("SURVEY_ID", surveyId);
                if (!(appContext instanceof Activity)) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                appContext.startActivity(intent);
            }catch (Exception e){
                Log.e("QuestionPro", "Failed to launch activity", e);
            }
        }
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
        MonitorAppEvents.getInstance().setTagNameCheckRules(tagName, appContext, QuestionProCX.this);
    }

    public void setDataMappings(HashMap<String, String> customDataMappings){
        SharedPreferenceManager.getInstance(appContext).saveCustomDataMappings(customDataMappings);
    }

    public void closeSurveyWindow(){
        if (runningActivities > 0) {
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
            CXGlobalInfo.getInstance().setApiKey(apiKey);
            CXGlobalInfo.getInstance().setAppPackage(appContext.getPackageName());
            CXGlobalInfo.getInstance().setUUID(CXUtils.getUniqueDeviceId(appContext));
            CXGlobalInfo.getInstance().setInitialized(true);
        }

        fetchInterceptSettings();
    }

    protected void fetchInterceptSettings(){
        if(!isSessionAlive) {
            isSessionAlive = true;
            new CXApiHandler(appContext, this).getIntercept();
        }
    }

    @Override
    public void onApiCallbackSuccess(Intercept intercept, String surveyUrl) {
        if(null != intercept && intercept.type.equals(InterceptType.SURVEY_URL.name())) {
            new CXApiHandler(appContext, this).submitFeedback(intercept, "MATCHED");
            if(questionProCallback != null) {
                questionProCallback.getSurveyUrl(surveyUrl);
            }
        }else{
            CXUtils.printLog("Datta", "Initialization API response: "+surveyUrl);
            if(questionProInitCallback != null) {
                questionProInitCallback.onInitializationSuccess(surveyUrl);
            }
            setUpIntercept();
        }
    }

    @Override
    public void OnApiCallbackFailed(JSONObject error) {
        CXUtils.printLog("Datta", "Error in initialization: "+error.toString());
        if(questionProInitCallback != null) {
            questionProInitCallback.onInitializationFailure(error.toString());
        }
    }

    private void setUpIntercept(){
        try{
            JSONObject projectObj =new JSONObject(SharedPreferenceManager.getInstance(appContext).getProject());
            JSONArray interceptArray = projectObj.getJSONArray("intercepts");
            for(int i = 0; i < interceptArray.length(); i++){
                JSONObject jsonObject = interceptArray.getJSONObject(i);
                //setUpTimeSpendIntercept(obj);
                Intercept intercept = Intercept.fromJSON(jsonObject);
                for(InterceptRule rule: intercept.interceptRule){
                    if(rule.name.equals(InterceptRuleType.TIME_SPENT.name())){
                        MonitorAppEvents.getInstance().appSessionStarted(intercept.id, rule, QuestionProCX.this);
                    } else if(rule.name.equals(InterceptRuleType.DAY.name())){
                        checkDayRule(rule, intercept.id);
                    } else if(rule.name.equals(InterceptRuleType.DATE.name())){
                        checkDateRule(rule, intercept.id);
                    }
                }
            }
        }catch (Exception e){e.printStackTrace();}

    }

    private void checkDateRule(InterceptRule rule, int interceptId){
        if(!CXUtils.isEmpty(rule.value)) {
            String[] dates = rule.value.split(",");
            for(String date: dates) {
                if (Integer.parseInt(date) == Integer.parseInt(DateTimeUtils.getCurrentDayOfMonth())) {
                    Set<String> interceptRules = new HashSet<>();
                    if (interceptSatisfiedRules.containsKey(interceptId)) {
                        interceptRules.addAll(interceptSatisfiedRules.get(interceptId));
                    }
                    interceptRules.add(InterceptRuleType.DATE.name());

                    interceptSatisfiedRules.put(interceptId, interceptRules);
                    checkAllRulesForIntercept(interceptId);
                }
            }
        }
    }

    private void checkDayRule(InterceptRule rule, int interceptId){
        if(!CXUtils.isEmpty(rule.value)) {
            String[] days = rule.value.split(",");
            for (String day : days) {
                if (day.equalsIgnoreCase(DateTimeUtils.getCurrentDayOfWeek())) {
                    Set<String> interceptRules = new HashSet<>();
                    if (interceptSatisfiedRules.containsKey(interceptId)) {
                        interceptRules.addAll(interceptSatisfiedRules.get(interceptId));
                    }
                    interceptRules.add(InterceptRuleType.DAY.name());

                    interceptSatisfiedRules.put(interceptId, interceptRules);
                    checkAllRulesForIntercept(interceptId);
                }
            }
        }
    }

    @Override
    public void onViewCountRuleSatisfied(int interceptId) {
        Set<String> interceptRules = new HashSet<>();
        if(interceptSatisfiedRules.containsKey(interceptId)) {
            interceptRules.addAll(Objects.requireNonNull(interceptSatisfiedRules.get(interceptId)));
        }
        interceptRules.add(InterceptRuleType.VIEW_COUNT.name());

        interceptSatisfiedRules.put(interceptId, interceptRules);
        checkAllRulesForIntercept(interceptId);
    }

    @Override
    public void onTimeSpendSatisfied(int interceptId) {
        try {
            CXUtils.printLog("Datta","Trigger the intercept as time is satisfied:"+interceptId);
            Set<String> interceptRules = new HashSet<>();
            if(interceptSatisfiedRules.containsKey(interceptId)) {
                interceptRules.addAll(Objects.requireNonNull(interceptSatisfiedRules.get(interceptId)));
            }
            interceptRules.add(InterceptRuleType.TIME_SPENT.name());

            interceptSatisfiedRules.put(interceptId, interceptRules);

            checkAllRulesForIntercept(interceptId);
        }catch (Exception e){}
    }

    private void checkAllRulesForIntercept(int interceptId){
        try {
            Intercept intercept = SharedPreferenceManager.getInstance(appContext).getInterceptById(interceptId);
            /** TODO add check if intercept is null**/
            if(shouldSurveyLaunch(intercept)) {
                Set<String> temp = interceptSatisfiedRules.get(interceptId);
                assert temp != null;
                CXUtils.printLog("Datta", interceptId + " Satisfied intercepts: " + temp);

                if (intercept.condition.equals(InterceptCondition.OR.name())) {
                    launchFeedbackSurvey(intercept);
                } else if (intercept.interceptRule.size() == temp.size()) {
                    launchFeedbackSurvey(intercept);
                }
            }
        }catch (Exception e){}
    }

    private boolean shouldSurveyLaunch(Intercept intercept){
        boolean doesSurveyAlreadyLaunched = SharedPreferenceManager.getInstance(appContext).isSurveyAlreadyLaunched(intercept.id);
        boolean allowMultipleResponse = intercept.interceptSettings.allowMultipleResponse;
        return allowMultipleResponse || !doesSurveyAlreadyLaunched;
    }


    private synchronized void launchFeedbackSurvey(Intercept intercept){
        CXUtils.printLog("Datta",isSessionAlive +" Running activity count: "+runningActivities);
        if(intercept.type.equals(InterceptType.SURVEY_URL.name())){
            new CXApiHandler(appContext, this).getInterceptSurvey(intercept);
        } else if (runningActivities == 0 && isSessionAlive) {
            int triggerDelay = intercept.interceptSettings.triggerDelayInSeconds * 1000;
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Intent intent = new Intent(appContext, InteractionActivity.class);
                        intent.putExtra("INTERCEPT", intercept);
                        if (!(appContext instanceof Activity)) {
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        }
                        appContext.startActivity(intent);
                    }catch (Exception e){
                        Log.e("QuestionPro", "Failed to launch activity", e);
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
        if (runningActivities == 0) {
            //CXPayloadWorker.appWentToForeground(activity);
        }
        runningActivities++;
    }

    public void onStop(Activity activity){
        CXUtils.printLog("Datta","Interaction Activity onStop");
        try {
            ActivityLifecycleManager.activityStopped(activity);
            runningActivities--;
            if (runningActivities < 0) {
                Log.e(LOG_TAG,"Incorrect number of running Activities encountered. Resetting to 0. Did you make sure to call Apptentive.onStart() and Apptentive.onStop() in all your Activities?");
                runningActivities = 0;
            }
            // If there are no running activities, wake the thread so it can stop immediately and gracefully.
            if (runningActivities == 0) {
                //CXPayloadWorker.appWentToBackground();
            }

        } catch (Exception e) {
            Log.w(LOG_TAG,"Error stopping Apptentive Activity.", e);
        }
    }

    public void cleanup() {
        if (appContext instanceof Application) {
            ((Application) appContext).unregisterActivityLifecycleCallbacks(new ActivityLifecycleCallbacks());
        } else if (appContext.getApplicationContext() instanceof Application) {
            ((Application) appContext.getApplicationContext()).unregisterActivityLifecycleCallbacks(new ActivityLifecycleCallbacks());
        }
    }
}
