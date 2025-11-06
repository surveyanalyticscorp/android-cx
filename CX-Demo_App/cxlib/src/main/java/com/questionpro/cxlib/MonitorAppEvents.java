package com.questionpro.cxlib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.model.InterceptRule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MonitorAppEvents {

    private Handler handler;

    private static MonitorAppEvents mMonitorEvents = null;
    private final Map<Integer, Runnable> runnableMap = new HashMap<>();


    private MonitorAppEvents(){}
    public static MonitorAppEvents getInstance(){
        if(mMonitorEvents == null){
            mMonitorEvents = new MonitorAppEvents();
        }
        return mMonitorEvents;
    }

    protected void appSessionStarted(int interceptId, InterceptRule rule, IQuestionProRulesCallback rulesCallback) {
        handler = new Handler(Looper.getMainLooper());
        long delay = Long.parseLong(rule.value) * 1000;

        Runnable runnable = createTimerRunnable(interceptId,  rulesCallback);
        runnableMap.put(interceptId, runnable);
        handler.postDelayed(runnable, delay);

    }

    private Runnable createTimerRunnable(final int interceptId, final IQuestionProRulesCallback rulesCallback) {
        return new Runnable() {
            @Override
            public void run() {
                rulesCallback.onTimeSpendSatisfied(interceptId);
            }
        };
    }

    protected void stopAllTimers() {
        for (Runnable runnable : runnableMap.values()) {
            handler.removeCallbacks(runnable);
        }
        runnableMap.clear();
    }

    protected void setTagNameCheckRules(String tagName, Context context, final IQuestionProRulesCallback rulesCallback){
        int viewCountForTag = SharedPreferenceManager.getInstance(context).updateViewCountForTag(tagName);
        //Log.d("Datta", "View count for tag name: "+tagName+" is: "+viewCountForTag);
        try {
            String projectJson = SharedPreferenceManager.getInstance(context).getProject();
            if (projectJson != null && !projectJson.trim().isEmpty()) {
                JSONObject interceptObj = new JSONObject(projectJson);
                JSONArray interceptArray = interceptObj.getJSONArray("intercepts");
                for (int i = 0; i < interceptArray.length(); i++) {
                    JSONObject jsonObject = interceptArray.getJSONObject(i);
                    Intercept intercept = Intercept.fromJSON(jsonObject);
                    //Log.d("Datta", "Intercept Id for tag "+tagName+" : "+intercept.id);
                    for (InterceptRule rule : intercept.interceptRule) {
                        if (rule.name.equals(InterceptRuleType.VIEW_COUNT.name()) &&
                                rule.key.equals(tagName) &&
                                Integer.parseInt(rule.value) <= viewCountForTag) {
                            //Log.d("Datta", "Key of intercept "+ rule.key+" : "+rule.value);
                            rulesCallback.onViewCountRuleSatisfied(intercept.id);
                            SharedPreferenceManager.getInstance(context).resetViewCountForTag(tagName);
                        }
                    }
                }
            }else{
                Log.w("QuestionProCX", "Project JSON is null or empty. Fetching the intercept settings...");
                QuestionProCX.getInstance().refreshInterceptSettings(tagName);
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }
}
