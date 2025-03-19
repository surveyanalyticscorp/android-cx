package com.questionpro.cxlib.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.model.Intercept;

import org.json.JSONArray;
import org.json.JSONObject;

public class SharedPreferenceManager {
    private SharedPreferences prefs;
    private final String INTERCEPT = "Intercepts";
    private final String APP_VIEW_COUNT = "app_view_count";
    private static String interceptStr;
    public SharedPreferenceManager(Activity activity){
        prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveIntercepts(String intercept){
        prefs.edit().putString(INTERCEPT, intercept).apply();
    }

    public String getIntercepts(){
        interceptStr = prefs.getString(INTERCEPT, "");
        return prefs.getString(INTERCEPT, "");
    }


    public Intercept getInterceptById(int interceptId) throws Exception{
        JSONArray interceptArray = new JSONObject(interceptStr).getJSONArray("intercepts");
        for(int i = 0; i < interceptArray.length(); i++){
            Intercept intercept = Intercept.fromJSON(interceptArray.getJSONObject(i));
            if(intercept.id == interceptId)
                return intercept;
        }
        return null;
    }
    public int getInterceptSurveyId(int interceptId) throws Exception{
        if(CXUtils.isEmpty(interceptStr)) {
            interceptStr = prefs.getString(INTERCEPT, "");
        }
        JSONArray interceptArray = new JSONObject(interceptStr).getJSONArray("intercepts");
        for(int i = 0; i < interceptArray.length(); i++){
            Intercept intercept = Intercept.fromJSON(interceptArray.getJSONObject(i));
            if(intercept.id == interceptId)
                return intercept.surveyId;
        }
        return -1;
    }

    public void updateAppViewCount(){
        prefs.edit().putInt(APP_VIEW_COUNT, getAppViewCount() + 1).apply();
    }

    public int getAppViewCount(){
        return prefs.getInt(APP_VIEW_COUNT,0);
    }

    public void resetAppViewCount(){
        prefs.edit().putInt(APP_VIEW_COUNT, 0).apply();
    }
}
