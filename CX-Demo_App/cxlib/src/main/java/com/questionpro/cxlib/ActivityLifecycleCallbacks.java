package com.questionpro.cxlib;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

public class ActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks{

    private boolean isInForeground = false;
    private int activityStartCount = 0;

    @Override
    public void onActivityStarted(Activity activity) {
        activityStartCount++;
        if (!isInForeground && activityStartCount > 0) {
            isInForeground = true;
            // App moved to foreground
            Log.d("Datta","App moved to foreground...");
            try {
                QuestionProCX.getInstance().fetchInterceptSettings();
            }catch (Exception e){}
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        activityStartCount--;

        if (activityStartCount == 0) {
            isInForeground = false;
            // App moved to background
            QuestionProCX.getInstance().clearSession();
            Log.d("Datta","App moved to background...");
        }
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}

}
