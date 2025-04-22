package com.questionpro.cxlib.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.model.Intercept;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;


public class SharedPreferenceManager {
    private SharedPreferences prefs;
    private final String INTERCEPT = "Intercepts";
    private final String VISITORS_UUID = "visitors_uuid";
    private final String LAUNCHED_SURVEYS = "launched_surveys";
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

    public void saveVisitorsUUID(String uuid){
        prefs.edit().putString(VISITORS_UUID, uuid).apply();
    }

    public String getVisitorsUUID(){
        return prefs.getString(VISITORS_UUID, "");
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

    public int updateViewCountForTag(String tag){
        int updatedViewCount = getViewCountForTag(tag) + 1;
        prefs.edit().putInt(tag, updatedViewCount).apply();
        return updatedViewCount;
    }

    public void resetViewCountForTag(String tag){
        prefs.edit().putInt(tag, 0).apply();
    }
    private int getViewCountForTag(String tag){
        return prefs.getInt(tag,0);
    }

    public void saveInterceptIdForLaunchedSurvey(String interceptId){
        Set<String> interceptIds = getLaunchedSurveys();
        interceptIds.add(interceptId);

        prefs.edit().putStringSet(LAUNCHED_SURVEYS, interceptIds).apply();
    }

    public Set<String> getLaunchedSurveys(){
        return prefs.getStringSet(LAUNCHED_SURVEYS, new HashSet<String>());
    }
    protected boolean checkIfSurveyAlreadyLaunchedForIntercept(int interceptId){
        return false;
    }

    public void resetPreferences(){
        prefs.edit().clear().apply();
    }
}
