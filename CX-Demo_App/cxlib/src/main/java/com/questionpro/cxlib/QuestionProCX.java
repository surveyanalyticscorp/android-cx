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
import com.questionpro.cxlib.dataconnect.CXPayload;
import com.questionpro.cxlib.dataconnect.CXPayloadWorker;
import com.questionpro.cxlib.interaction.InteractionFragment;
import com.questionpro.cxlib.model.CXInteraction;
import com.questionpro.cxlib.model.TouchPoint;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.interaction.InteractionActivity;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Created by Dattakunde on 14/04/16.
 */
public class QuestionProCX {
    private static final String LOG_TAG="QuestionProCX";
    private static int runningActivities;
    private static ProgressDialog progressDialog;

    private static WeakReference<Activity> mActivity;

    public QuestionProCX(){
    }

    private static void init(Activity activity){
        mActivity = new WeakReference<>(activity);
        final Context appContext = activity.getApplicationContext();
        try {
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
        }catch (Exception e){
            Log.e(LOG_TAG,"Unexpected error while reading application info."+ e.getMessage());
        }

        /*if (!CXGlobalInfo.initialized) {
            SharedPreferences prefs = appContext.getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
            // First, Get the api key, and figure out if app is debuggable.
            String apiKey = prefs.getString(CXConstants.PREF_KEY_API_KEY, null);

            try {
                ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
                Bundle metaData = ai.metaData;
                if (metaData != null) {
                    *//*if (apiKey == null) {
                        apiKey = metaData.getString(CXConstants.MANIFEST_KEY_API_KEY);
                        Log.d(LOG_TAG,"Saving API key for the first time: "+apiKey);
                        prefs.edit().putString(CXConstants.PREF_KEY_API_KEY, apiKey).apply();
                    } else {
                        Log.d(LOG_TAG,"Using cached API Key: "+apiKey);
                    }*//*
                    apiKey = metaData.getString(CXConstants.MANIFEST_KEY_API_KEY);
                    Log.d(LOG_TAG,"API key: "+apiKey);
                    prefs.edit().putString(CXConstants.PREF_KEY_API_KEY, apiKey).apply();
                }
            } catch (Exception e) {
                Log.e(LOG_TAG,"Unexpected error while reading application info."+ e.getMessage());
            }
            String errorString = "No CX api key specified. Please make sure you have specified your api key in your AndroidManifest.xml";
            if(CXUtils.isEmpty(apiKey)){
                Log.e("QuestionProCX", errorString);
            }
            CXGlobalInfo.apiKey = apiKey;
            // Grab app info we need to access later on.
            CXGlobalInfo.appPackage = appContext.getPackageName();
            CXGlobalInfo.UUID = CXUtils.getUniqueDeviceId(activity);
            CXGlobalInfo.initialized = true;
        }*/
    }

    private static void showProgress(){
        progressDialog = new ProgressDialog(mActivity.get());
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    public void onError(JSONObject response) throws JSONException {
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

    public static void onStart(Activity activity){
        //init(activity);
        ActivityLifecycleManager.activityStarted(activity);
        if (runningActivities == 0) {
            CXPayloadWorker.appWentToForeground(activity);
        }
        runningActivities++;
    }


    public static synchronized void init(Activity activity, TouchPoint touchPoint){
        init(activity);
        CXGlobalInfo.getInstance().savePayLoad(touchPoint);
    }

    public static synchronized void launchFeedbackSurvey(long surveyId){
        //showProgress();
        CXGlobalInfo.updateCXPayloadWithSurveyId(surveyId);
        CXPayloadWorker.appWentToForeground(mActivity.get());

        /*Intent intent = new Intent(mActivity.get(), InteractionActivity.class);
        intent.putExtra("SURVEY_ID", surveyId);
        mActivity.get().startActivity(intent);*/
    }

    public static void onStop(Activity activity){
        try {
            ActivityLifecycleManager.activityStopped(activity);
            runningActivities--;
            if (runningActivities < 0) {
                Log.e(LOG_TAG,"Incorrect number of running Activities encountered. Resetting to 0. Did you make sure to call Apptentive.onStart() and Apptentive.onStop() in all your Activities?");
                runningActivities = 0;
            }
            // If there are no running activities, wake the thread so it can stop immediately and gracefully.
            if (runningActivities == 0) {
                CXPayloadWorker.appWentToBackground();

            }

        } catch (Exception e) {
            Log.w(LOG_TAG,"Error stopping Apptentive Activity.", e);

        }
    }
    public static synchronized  void launchFeedbackScreen(Activity activity, CXInteraction cxInteraction){
        try {
            if(progressDialog != null && progressDialog.isShowing())
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
}
