package com.questionpro.cxlib;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.questionpro.cxlib.interfaces.QuestionProIntercepts;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MonitorAppEvents {

    private Handler handler;
    private Runnable eventRunnable;
    private static MonitorAppEvents mMonitorEvents = null;

    private MonitorAppEvents(){}
    public static MonitorAppEvents getInstance(){
        if(mMonitorEvents == null){
            mMonitorEvents = new MonitorAppEvents();
        }
        return mMonitorEvents;
    }

    protected void appSessionStarted(final long delay, final int interceptId, final QuestionProIntercepts intercepts) {
        handler = new Handler(Looper.getMainLooper());

        eventRunnable = new Runnable() {
            @Override
            public void run() {
                // Trigger your event here
                //triggerMyEvent(surveyId);
                intercepts.onTimeSpendSatisfied(interceptId);
            }
        };
        handler.postDelayed(eventRunnable, delay); // 5 minutes in milliseconds
    }

    private void triggerMyEvent(int surveyId) {
        Log.d("Datta", "Trigger the Event...."+surveyId);
        QuestionProCX.getInstance().launchFeedbackSurvey(surveyId);
    }

    protected void stopTimer() {
        if (eventRunnable != null) {
            handler.removeCallbacks(eventRunnable);
            eventRunnable = null;
        }
    }


}
