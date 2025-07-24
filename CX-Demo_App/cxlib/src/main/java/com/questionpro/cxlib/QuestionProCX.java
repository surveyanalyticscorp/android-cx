package com.questionpro.cxlib;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.dataconnect.CXApiHandler;
import com.questionpro.cxlib.enums.InterceptCondition;
import com.questionpro.cxlib.enums.InterceptRuleType;
import com.questionpro.cxlib.enums.InterceptType;
import com.questionpro.cxlib.interfaces.IQuestionProCallback;
import com.questionpro.cxlib.interfaces.IQuestionProApiCallback;
import com.questionpro.cxlib.interfaces.IQuestionProRulesCallback;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.model.InterceptRule;
import com.questionpro.cxlib.model.TouchPoint;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.interaction.InteractionActivity;
import com.questionpro.cxlib.util.CXUtils;
import com.questionpro.cxlib.util.DateTimeUtils;
import com.questionpro.cxlib.util.SharedPreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
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

    private IQuestionProCallback questionProCallback;

    private SharedPreferenceManager preferenceManager = null;

    private static final HashMap<Integer, Set<String>> interceptSatisfiedRules = new HashMap<>();

    public QuestionProCX(){
    }

    public static QuestionProCX getInstance(){
        if(mInstance == null){
            mInstance = new QuestionProCX();
        }
        return mInstance;
    }

    public synchronized void init(Context context, TouchPoint touchPoint){
        try {
            appContext = context;
            initialize();
            CXGlobalInfo.getInstance().savePayLoad(touchPoint);
            //new CXApiHandler(activity, this).getIntercept();
        }catch (Exception e){
            Log.e(LOG_TAG, "Error in initialization: "+e.getMessage());
        }
    }

    public synchronized void init(Context context, TouchPoint touchPoint, IQuestionProCallback callback){
        appContext = context;
        questionProCallback = callback;
        try {
            CXUtils.printLog("Datta","Initialising the SDK");
            initialize();
            CXGlobalInfo.getInstance().savePayLoad(touchPoint);
        }catch (Exception e){
            callback.onInitializationFailure(e.getMessage());
        }
    }

    /**
     * @param tagName : TagName or screen name which is set while configuring the intercept rules.
     */
    public void setScreenVisited(String tagName){
        MonitorAppEvents.getInstance().setTagNameCheckRules(tagName, preferenceManager, QuestionProCX.this);
    }

    public void clearSession(){
        CXUtils.printLog("Datta","Clearing session.");
        isSessionAlive = false;
        MonitorAppEvents.getInstance().stopAllTimers();
        interceptSatisfiedRules.clear();
        preferenceManager.resetPreferences();
    }

    protected void initialize() throws Exception{
        //mActivity = new WeakReference<>(activity);

        preferenceManager = new SharedPreferenceManager(appContext);
        if (appContext instanceof Application) {
            ((Application) appContext).registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks());
        } else if (appContext.getApplicationContext() instanceof Application) {
            ((Application) appContext.getApplicationContext()).registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks());
        }

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
            if(questionProCallback != null) {
                questionProCallback.onInitializationSuccess(surveyUrl);
            }
            setUpIntercept();
        }
    }

    @Override
    public void OnApiCallbackFailed(JSONObject error) {
        CXUtils.printLog("Datta", "Error in initialization: "+error.toString());
        if(questionProCallback != null) {
            questionProCallback.onInitializationFailure(error.toString());
        }
    }

    private void setUpIntercept(){
        try{
            JSONObject interceptObj =new JSONObject(preferenceManager.getIntercepts());
            JSONArray interceptArray = interceptObj.getJSONArray("intercepts");
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
        }catch (Exception e){}

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
            int surveyId = preferenceManager.getInterceptSurveyId(interceptId);
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
            long prevTime = preferenceManager.getLaunchedInterceptTime(appContext, interceptId);
            boolean isSleepTimeOverForIntercept = CXUtils.isSleepTimeOver(prevTime);
            Log.d("Datta","Does sleep time over for "+interceptId+" Intercept "+isSleepTimeOverForIntercept);

            if(isSleepTimeOverForIntercept) {
                Intercept intercept = preferenceManager.getInterceptById(interceptId);

                Set<String> temp = interceptSatisfiedRules.get(interceptId);
                assert temp != null;
                CXUtils.printLog("Datta", interceptId + " Satisfied intercepts: " + temp);

                if (intercept.condition.equals(InterceptCondition.OR.name())) {
                    launchFeedbackSurvey(intercept);
                } else if (intercept.interceptRule.size() == temp.size()) {
                    launchFeedbackSurvey(intercept);
                }
            }else{
                CXUtils.printLog("Datta","The survey was already answered for ongoing session: "+interceptId);
            }
        }catch (Exception e){}
    }

    /*private void showProgress(){
        progressDialog = new ProgressDialog(mActivity.get());
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }*/

    public void handleError(JSONObject response) throws JSONException {
        if(null != progressDialog && progressDialog.isShowing()){
            progressDialog.cancel();
        }
        if(response.has("error") && response.getJSONObject("error").has("message")) {
            final String errorMessage = "Error: " + response.getJSONObject("error").getString("message");
            /*final Activity activity = mActivity.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }*/
            Toast.makeText(appContext, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private synchronized void launchFeedbackSurvey(Intercept intercept){
        CXUtils.printLog("Datta",isSessionAlive +" Running activity count: "+runningActivities);
        if(intercept.type.equals(InterceptType.SURVEY_URL.name())){
            new CXApiHandler(appContext, this).getInterceptSurvey(intercept);
        }else {
            if (runningActivities == 0 && isSessionAlive) {
                Intent intent = new Intent(appContext, InteractionActivity.class);
                intent.putExtra("INTERCEPT", intercept);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                appContext.startActivity(intent);
            }
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
