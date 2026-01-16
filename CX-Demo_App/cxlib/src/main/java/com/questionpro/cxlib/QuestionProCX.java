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

import com.questionpro.cxlib.interfaces.QuestionProCallback;
import com.questionpro.cxlib.model.TouchPoint;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.Map;


/**
 * Created by Dattakunde on 14/04/16.
 */
public class QuestionProCX {
    private static final String LOG_TAG="QuestionProCX";
    private static int runningActivities;
    private static ProgressDialog progressDialog;

    private static WeakReference<Activity> mActivity;
    private static WeakReference<InteractionActivity> interactionActivityRef;

    private static QuestionProCX mInstance = null;

    private QuestionProCallback questionProCallback;


    public QuestionProCX(){
    }

    public static QuestionProCX getInstance(){
        if(mInstance == null){
            mInstance = new QuestionProCX();
        }
        return mInstance;
    }


    private void init(Activity activity){
        mActivity = new WeakReference<>(activity);
        final Context appContext = activity.getApplicationContext();
        try {
            ApplicationInfo ai = appContext.getPackageManager().getApplicationInfo(appContext.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = ai.metaData;
            if (metaData != null) {
                String apiKey = metaData.getString(CXConstants.MANIFEST_KEY_API_KEY);
                CXUtils.printLogs("API key: "+apiKey);
                CXGlobalInfo.getInstance().setApiKey(apiKey);
                CXGlobalInfo.getInstance().setAppPackage(appContext.getPackageName());
                CXGlobalInfo.getInstance().setUUID(CXUtils.getUniqueDeviceId(activity));
                CXGlobalInfo.getInstance().setInitialized(true);
            }
        }catch (Exception e){
            Log.e(LOG_TAG,"Unexpected error while reading application info."+ e.getMessage());
        }
    }

    protected static void registerInteractionActivity(InteractionActivity activity) {
        interactionActivityRef = new WeakReference<>(activity);
    }

    protected static void unregisterInteractionActivity() {
        if (interactionActivityRef != null) {
            interactionActivityRef.clear();
            interactionActivityRef = null;
        }
    }

    private void showProgress(){
        progressDialog = new ProgressDialog(mActivity.get());
        progressDialog.setMessage("loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    protected void handleTokenExpiry(){
        questionProCallback.refreshToken();
        QuestionProCX.getInstance().launchFeedbackSurvey(CXGlobalInfo.getInstance().getSurveyIDFromPayload());
    }

    protected void onError(JSONObject response) throws JSONException {
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

    private void onStart(Activity activity){
        //init(activity);
        ActivityLifecycleManager.activityStarted(activity);
        if (runningActivities == 0) {
            CXPayloadWorker.appWentToForeground(activity);
        }
        runningActivities++;
    }


    public synchronized void init(Activity activity, TouchPoint touchPoint, QuestionProCallback callback){
        this.questionProCallback = callback;
        init(activity);
        CXGlobalInfo.getInstance().savePayLoad(touchPoint);

        //Log.d("Datta","ClientModule new access token received: " + getAccessToken());
        //Log.d("Datta","ClientModule encrypted data received: " + getEncryptedData("Encrypt this data and send back"));
        //Log.d("Datta","API decrypted data received: " + getDecryptedModuleData());
    }

    protected String getAccessToken(){
        return questionProCallback.refreshToken();
    }

    protected Map.Entry<String, Map<String, String>> getEncryptedData(String dataToEncrypt){
        return questionProCallback.encryptData(dataToEncrypt);
    }

    protected String getDecryptedModuleData(String encryptedData, Map<String, String> headers) {
        // Using AbstractMap.SimpleEntry to represent Pair in Java
        // Alternatively, you could create a custom data class/record for apiResponse
        Map.Entry<String, Map<String, String>> apiResponse =
                new AbstractMap.SimpleEntry<>(encryptedData, headers);

        return questionProCallback.decryptedData(apiResponse);
    }

    public synchronized void launchFeedbackSurvey(long surveyId){
        //showProgress();
        if(checkIfSurveyInRestingPeriod(surveyId)){
            Log.d("Datta","Survey launch aborted due to resting period.");
            if(null != progressDialog && progressDialog.isShowing()){
                progressDialog.cancel();
            }
            return;
        }
        CXGlobalInfo.updateCXPayloadWithSurveyId(surveyId);
        CXPayloadWorker.appWentToForeground(mActivity.get());
    }

    private boolean checkIfSurveyInRestingPeriod(long surveyId){
        int storedRestingPeriod = CXGlobalInfo.getInstance().getRestingPeriodFromPayload();
        Log.d("Datta","Resting period stored in payload: " + storedRestingPeriod);
        Long lastSurveyTime = SharedPreferenceManager.getInstance(mActivity.get()).getSurveyTimestamp(surveyId);
        if(lastSurveyTime != null){
            long currentTime = System.currentTimeMillis();
            long diffInMillis = currentTime - lastSurveyTime;
            Log.d("Datta","Time since survey answered in Hours: " + diffInMillis / (1000 * 60 * 60));
            long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);
            if(diffInDays < storedRestingPeriod){
                Log.d("Datta","Survey is in resting period. Days left: " + (storedRestingPeriod - diffInDays));
                return true;
            }
        }
        return false;
    }

    public void closeSurveyWindow(){
        if (interactionActivityRef != null) {
            InteractionActivity activity = interactionActivityRef.get();
            if (activity != null && !activity.isFinishing()) {
                activity.finish();
            }
        }
    }

    private void onStop(Activity activity){
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
    protected synchronized  void launchSurveyScreen(Activity activity, CXInteraction cxInteraction){
        try {
            if(progressDialog != null && progressDialog.isShowing())
                progressDialog.cancel();
            Intent intent = new Intent(activity, InteractionActivity.class);
            intent.putExtra(CXConstants.CX_INTERACTION_CONTENT, cxInteraction);
            activity.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
