package com.questionpro.cxlib;

import android.os.Handler;
import android.os.Looper;

import com.questionpro.cxlib.enums.InterceptRuleType;
import com.questionpro.cxlib.interfaces.IQuestionProRulesCallback;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.model.InterceptRule;
import com.questionpro.cxlib.util.SharedPreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

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

    protected void appSessionStarted(final int interceptId, InterceptRule rule, final IQuestionProRulesCallback rulesCallback) {
        handler = new Handler(Looper.getMainLooper());
        long delay = Long.parseLong(rule.value);
        eventRunnable = new Runnable() {
            @Override
            public void run() {
                // Trigger your event here
                rulesCallback.onTimeSpendSatisfied(interceptId);
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

    protected void setTagNameCheckRules(String tagName, SharedPreferenceManager preferenceManager, final IQuestionProRulesCallback rulesCallback){
        if(preferenceManager != null) {
            int viewCountForTag = preferenceManager.updateViewCountForTag(tagName);
            //Log.d("Datta", "View count for tag name: "+tagName+" is: "+viewCountForTag);
            try {
                JSONObject interceptObj = new JSONObject(preferenceManager.getIntercepts());
                JSONArray interceptArray = interceptObj.getJSONArray("intercepts");
                for (int i = 0; i < interceptArray.length(); i++) {
                    JSONObject jsonObject = interceptArray.getJSONObject(i);
                    Intercept intercept = Intercept.fromJSON(jsonObject);
                    //Log.d("Datta", "Intercept Id for tag "+tagName+" : "+intercept.id);
                    for (InterceptRule rule : intercept.interceptRule) {
                        if (rule.name.equals(InterceptRuleType.VIEW_COUNT.name()) &&
                                rule.key.equals(tagName) &&
                                Integer.parseInt(rule.value) == viewCountForTag) {
                            //Log.d("Datta", "Key of intercept "+ rule.key+" : "+rule.value);
                            rulesCallback.onViewCountRuleSatisfied(intercept.id);
                            preferenceManager.resetViewCountForTag(tagName);
                        }
                    }
                }
            } catch (Exception ignored) {

            }
        }else{
            // handle the else case
        }
    }
}
