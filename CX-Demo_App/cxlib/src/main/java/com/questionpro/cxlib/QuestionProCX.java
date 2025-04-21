package com.questionpro.cxlib;

import android.app.Activity;
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
import com.questionpro.cxlib.interfaces.IQuestionProInitCallback;
import com.questionpro.cxlib.interfaces.QuestionProApiCallback;
import com.questionpro.cxlib.interfaces.QuestionProIntercepts;
import com.questionpro.cxlib.model.CXInteraction;
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
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Dattakunde on 14/04/16.
 */
public class QuestionProCX implements QuestionProApiCallback, QuestionProIntercepts {
    private static final String LOG_TAG="QuestionProCX";
    private static int runningActivities;
    private ProgressDialog progressDialog;

    private WeakReference<Activity> mActivity;

    private static QuestionProCX mInstance = null;

    private IQuestionProInitCallback questionProInitCallback;

    private SharedPreferenceManager preferenceManager = null;

    private static final HashMap<Integer, ArrayList<String>> interceptSatisfiedRules = new HashMap<>();

    public QuestionProCX(){
    }

    public static QuestionProCX getInstance(){
        if(mInstance == null){
            mInstance = new QuestionProCX();
        }
        return mInstance;
    }

    public synchronized void init(Activity activity, TouchPoint touchPoint){
        try {
            initialize(activity);
            CXGlobalInfo.getInstance().savePayLoad(touchPoint);
        }catch (Exception e){
            Log.e(LOG_TAG, "Error in initialization: "+e.getMessage());
        }
    }

    public synchronized void init(Activity activity, TouchPoint touchPoint, IQuestionProInitCallback callback){
        questionProInitCallback = callback;
        try {
            Log.d("Datta","Initialising the SDK");
            initialize(activity);
            CXGlobalInfo.getInstance().savePayLoad(touchPoint);

            new CXApiHandler(activity, this).getIntercept();
            //callback.onSuccess("QuestionPro SDK initialise successfully!");
        }catch (Exception e){
            callback.onFailed(e.getMessage());
        }
    }

    /**
     * @param tagName : TagName or screen name which is set while configuring the intercept rules.
     */
    public void setScreenVisited(String tagName){
        MonitorAppEvents.getInstance().setTagNameCheckRules(tagName, preferenceManager, QuestionProCX.this);
    }

    public void clearSession(){
        MonitorAppEvents.getInstance().stopTimer();
        interceptSatisfiedRules.clear();
        preferenceManager.resetPreferences();
    }

    private void initialize(Activity activity) throws Exception{
        mActivity = new WeakReference<>(activity);

        preferenceManager = new SharedPreferenceManager(activity);

        final Context appContext = activity.getApplicationContext();
        ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
        Bundle metaData = ai.metaData;

        if (metaData != null) {
            String apiKey = metaData.getString(CXConstants.MANIFEST_KEY_API_KEY);
            Log.d(LOG_TAG,"API key: "+apiKey);
            CXGlobalInfo.getInstance().setApiKey(apiKey);
            CXGlobalInfo.getInstance().setAppPackage(appContext.getPackageName());
            CXGlobalInfo.getInstance().setUUID(CXUtils.getUniqueDeviceId(activity));
            CXGlobalInfo.getInstance().setInitialized(true);
        }
    }

    @Override
    public void onSurveyUrlReceived(Intercept intercept, String surveyUrl) {
        if(null != intercept && intercept.type.equals(InterceptType.SURVEY_URL.name())) {
            questionProInitCallback.getSurveyUrl(surveyUrl);
        }else{
            Log.d("Datta", "Initialization API response: "+surveyUrl);
            questionProInitCallback.onSuccess(surveyUrl);
            setUpIntercept();
        }
    }

    @Override
    public void onError(JSONObject error) {
        //Log.d("Datta", "Error in initialization: "+error.toString());
        questionProInitCallback.onFailed(error.toString());
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
        //Log.d("Datta","Date: "+DateTimeUtils.getCurrentDayOfMonth());
        if(rule.value.equalsIgnoreCase(DateTimeUtils.getCurrentDayOfMonth())){
            ArrayList<String> interceptRules = new ArrayList<>();
            if(interceptSatisfiedRules.containsKey(interceptId)) {
                interceptRules.addAll(interceptSatisfiedRules.get(interceptId));
            }
            interceptRules.add(InterceptRuleType.DATE.name());

            interceptSatisfiedRules.put(interceptId, interceptRules);
            checkAllRulesForIntercept(interceptId);
        }
    }

    private void checkDayRule(InterceptRule rule, int interceptId){
        //Log.d("Datta","Day of week: "+DateTimeUtils.getCurrentDayOfWeek());
        if(rule.value.equalsIgnoreCase(DateTimeUtils.getCurrentDayOfWeek())){
            ArrayList<String> interceptRules = new ArrayList<>();
            if(interceptSatisfiedRules.containsKey(interceptId)) {
                interceptRules.addAll(interceptSatisfiedRules.get(interceptId));
            }
            interceptRules.add(InterceptRuleType.DAY.name());

            interceptSatisfiedRules.put(interceptId, interceptRules);
            checkAllRulesForIntercept(interceptId);
        }
    }

    @Override
    public void onViewCountRuleSatisfied(int interceptId) {
        ArrayList<String> interceptRules = new ArrayList<>();
        if(interceptSatisfiedRules.containsKey(interceptId)) {
            interceptRules.addAll(interceptSatisfiedRules.get(interceptId));
        }
        interceptRules.add(InterceptRuleType.VIEW_COUNT.name());

        interceptSatisfiedRules.put(interceptId, interceptRules);
        checkAllRulesForIntercept(interceptId);
    }

    @Override
    public void onTimeSpendSatisfied(int interceptId) {
        try {
            int surveyId = preferenceManager.getInterceptSurveyId(interceptId);
            Log.d("Datta","Trigger the intercept as time is satisfied:"+surveyId);
            ArrayList<String> interceptRules = new ArrayList<>();
            if(interceptSatisfiedRules.containsKey(interceptId)) {
                interceptRules.addAll(interceptSatisfiedRules.get(interceptId));
            }
            interceptRules.add(InterceptRuleType.TIME_SPENT.name());

            interceptSatisfiedRules.put(interceptId, interceptRules);

            checkAllRulesForIntercept(interceptId);
        }catch (Exception e){}
    }

    private void checkAllRulesForIntercept(int interceptId){
        try {
            if(!preferenceManager.getLaunchedSurveys().contains(String.valueOf(interceptId))) {

                Intercept intercept = preferenceManager.getInterceptById(interceptId);

                ArrayList<String> temp = interceptSatisfiedRules.get(interceptId);
                assert temp != null;
                Log.d("Datta", interceptId + " Satisfied intercepts: " + temp);

                if (intercept.condition.equals(InterceptCondition.OR.name())) {
                    launchFeedbackSurvey(intercept);
                } else if (intercept.interceptRule.size() == temp.size()) {
                    launchFeedbackSurvey(intercept);
                }
            }else{
                Log.d("Datta","The survey was already answered for ongoing session: "+interceptId);
            }

            /*Iterator it = interceptSatisfiedRules.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                System.out.println(pair.getKey() + " = " + pair.getValue());
                it.remove(); // avoids a ConcurrentModificationException
            }*/

        }catch (Exception e){}
    }

    private void showProgress(){
        progressDialog = new ProgressDialog(mActivity.get());
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void handleError(JSONObject response) throws JSONException {
        if(null != progressDialog && progressDialog.isShowing()){
            progressDialog.cancel();
        }
        if(response.has("error") && response.getJSONObject("error").has("message")) {
            final String errorMessage = "Error: " + response.getJSONObject("error").getString("message");
            final Activity activity = mActivity.get();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    /** Use this function while launching the survey Activity. Check if feedback activity is already running */
    public void onStart(Activity activity){
        //init(activity);
        Log.d("Datta","Interaction Activity onStart");
        ActivityLifecycleManager.activityStarted(activity);
        if (runningActivities == 0) {
            //CXPayloadWorker.appWentToForeground(activity);
        }
        runningActivities++;
    }


    public synchronized void launchFeedbackSurvey(long surveyId){
        /*showProgress();
        CXGlobalInfo.updateCXPayloadWithSurveyId(surveyId);
        CXPayloadWorker.appWentToForeground(mActivity.get());*/

        if(runningActivities == 0) {
            Intent intent = new Intent(mActivity.get(), InteractionActivity.class);
            intent.putExtra("SURVEY_ID", surveyId);
            mActivity.get().startActivity(intent);
        }
    }

    private synchronized void launchFeedbackSurvey(Intercept intercept){
        if(intercept.type.equals(InterceptType.SURVEY_URL.name())){
            new CXApiHandler(mActivity.get(), this).getInterceptSurvey(intercept);
        }else {
            if (runningActivities == 0) {
                preferenceManager.saveInterceptIdForLaunchedSurvey(String.valueOf(intercept.id));

                Intent intent = new Intent(mActivity.get(), InteractionActivity.class);
                intent.putExtra("INTERCEPT", intercept);
                mActivity.get().startActivity(intent);
            }
        }
    }

    public synchronized  void launchFeedbackScreen(Activity activity, CXInteraction cxInteraction){
        try {
            progressDialog.cancel();
            Intent intent = new Intent(activity, InteractionActivity.class);
            intent.putExtra(CXConstants.CX_INTERACTION_CONTENT, cxInteraction);
            activity.startActivity(intent);

           /* Bundle args=new Bundle();
            args.putSerializable(CXConstants.CX_INTERACTION_CONTENT, CXGlobalInfo.getInteraction(activity, touchPointID));
            activity.getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .add(android.R.id.content, InteractionFragment.class, args)
                    .addToBackStack(null)
                    .commit();

            CXGlobalInfo.clearInteraction(activity, touchPointID);*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onStop(Activity activity){
        Log.d("Datta","Interaction Activity onStop");
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
}
