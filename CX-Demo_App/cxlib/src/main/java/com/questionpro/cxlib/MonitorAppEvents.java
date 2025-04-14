package com.questionpro.cxlib;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.questionpro.cxlib.interfaces.QuestionProIntercepts;
import com.questionpro.cxlib.model.InterceptRule;

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

    protected void appSessionStarted(final int interceptId, InterceptRule rule, final QuestionProIntercepts intercepts) {
        handler = new Handler(Looper.getMainLooper());
        long delay = Long.parseLong(rule.value);
        eventRunnable = new Runnable() {
            @Override
            public void run() {
                // Trigger your event here
                intercepts.onTimeSpendSatisfied(interceptId);
            }
        };
        handler.postDelayed(eventRunnable, delay * 1000); // 5 minutes in milliseconds
    }

    protected void stopTimer() {
        if (eventRunnable != null) {
            handler.removeCallbacks(eventRunnable);
            eventRunnable = null;
        }
    }


}
