package com.questionpro.cxlib;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.dataconnect.CXPayload;
import com.questionpro.cxlib.dataconnect.CXPayloadWorker;
import com.questionpro.cxlib.model.TouchPoint;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.interaction.InteractionActivity;
import com.questionpro.cxlib.util.CXUtils;

/**
 * Created by sachinsable on 14/04/16.
 */
public class QuestionProCX {
    private static final String LOG_TAG="QuestionProCX";
    private static int runningActivities;
    private static ProgressDialog progressDialog;

    private static void init(Activity activity){
        final Context appContext = activity.getApplicationContext();

        progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (!CXGlobalInfo.initialized) {
            SharedPreferences prefs = appContext.getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);

            // First, Get the api key, and figure out if app is debuggable.
            String apiKey = prefs.getString(CXConstants.PREF_KEY_API_KEY, null);

            try {
                ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
                Bundle metaData = ai.metaData;
                if (metaData != null) {
                    if (apiKey == null) {
                        apiKey = metaData.getString(CXConstants.MANIFEST_KEY_API_KEY);
                        Log.d(LOG_TAG,"Saving API key for the first time: %s"+apiKey);
                        prefs.edit().putString(CXConstants.PREF_KEY_API_KEY, apiKey).apply();
                    } else {
                        Log.d(LOG_TAG,"Using cached API Key: %s"+apiKey);
                    }
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
        }
    }


    public static void onStart(Activity activity){
        init(activity);
        ActivityLifecycleManager.activityStarted(activity);
        if (runningActivities == 0) {
            CXPayloadWorker.appWentToForeground(activity);

        }
        runningActivities++;
    }

    public static synchronized void engageTouchPoint(Activity activity, TouchPoint touchPoint){
        init(activity);
        if(!CXGlobalInfo.isInteractionPending(activity,touchPoint)){
            CXGlobalInfo.setPayLoad(activity, new CXPayload(touchPoint.getTouchPointID()));
            CXPayloadWorker.appWentToForeground(activity);
        } else{
            launchFeedbackScreen(activity,touchPoint.getTouchPointID());
        }
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
    public static synchronized  void launchFeedbackScreen(Activity activity, long touchPointID){
        try {
            progressDialog.cancel();

            Intent intent = new Intent(activity, InteractionActivity.class);
            intent.putExtra(CXConstants.CX_INTERACTION_CONTENT, CXGlobalInfo.getInteraction(activity, touchPointID));
            activity.startActivity(intent);
            CXGlobalInfo.clearInteraction(activity, touchPointID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
