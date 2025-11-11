package com.questionpro.cxlib;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.questionpro.cxlib.util.CXUtils;

public class ActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks{

    private boolean isInForeground = true;
    private int activityStartCount = 1;

    @Override
    public void onActivityStarted(Activity activity) {
        if (!isInForeground && activityStartCount == 0) {
            isInForeground = true;
            // App moved to foreground
            CXUtils.printLog("Datta","App moved to foreground...");
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        QuestionProCX.getInstance().fetchInterceptSettings();
                    }catch (Exception e){e.printStackTrace();}
                }
            }, 1500);
        }
        activityStartCount++;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        activityStartCount--;
        if (activityStartCount < 0) {
            activityStartCount = 0; // Safety check
        }

        if (activityStartCount == 0) {
            isInForeground = false;
            // App moved to background
            CXUtils.printLog("Datta","App moved to background...");
            QuestionProCX.getInstance().clearSession();
        }
    }

    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}

}
