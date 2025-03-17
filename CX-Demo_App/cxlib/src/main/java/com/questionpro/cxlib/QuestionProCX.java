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
import com.questionpro.cxlib.dataconnect.CXPayloadWorker;
import com.questionpro.cxlib.interfaces.IQuestionProInitCallback;
import com.questionpro.cxlib.interfaces.QuestionProApiCallback;
import com.questionpro.cxlib.model.CXInteraction;
import com.questionpro.cxlib.model.TouchPoint;
import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.interaction.InteractionActivity;
import com.questionpro.cxlib.util.ApiNameEnum;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Created by Dattakunde on 14/04/16.
 */
public class QuestionProCX implements QuestionProApiCallback {
    private static final String LOG_TAG="QuestionProCX";
    private static int runningActivities;
    private static ProgressDialog progressDialog;

    private static WeakReference<Activity> mActivity;

    private static QuestionProCX mInstance = null;

    private IQuestionProInitCallback questionProInitCallback;

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

            new  CXApiHandler(activity, this).makeApiCall(ApiNameEnum.GET_INTERCEPTS);
            //callback.onSuccess("QuestionPro SDK initialise successfully!");
        }catch (Exception e){
            callback.onFailed(e.getMessage());
        }
    }

    private static void initialize(Activity activity) throws Exception{
        mActivity = new WeakReference<>(activity);

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
    public void onSuccess(String message) {
        //Log.d("Datta", "Initialization API response: "+message);
        questionProInitCallback.onSuccess(message);
    }

    @Override
    public void onError(JSONObject error) {
        //Log.d("Datta", "Error in initialization: "+error.toString());
        questionProInitCallback.onFailed(error.toString());
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

    public void onStart(Activity activity){
        //init(activity);
        ActivityLifecycleManager.activityStarted(activity);
        if (runningActivities == 0) {
            CXPayloadWorker.appWentToForeground(activity);
        }
        runningActivities++;
    }


    public synchronized void launchFeedbackSurvey(long surveyId){
        /*showProgress();
        CXGlobalInfo.updateCXPayloadWithSurveyId(surveyId);
        CXPayloadWorker.appWentToForeground(mActivity.get());*/

        Intent intent = new Intent(mActivity.get(), InteractionActivity.class);
        intent.putExtra("SURVEY_ID", surveyId);
        mActivity.get().startActivity(intent);
    }

    public void onStop(Activity activity){
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
}
