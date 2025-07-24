package com.questionpro.cxlib.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.model.Intercept;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SharedPreferenceManager {
    private SharedPreferences prefs;
    private final String INTERCEPT = "Intercepts";
    private final String VISITORS_UUID = "visitors_uuid";
    private final String LAUNCHED_SURVEYS = "launched_surveys";
    private static String interceptStr;
    public SharedPreferenceManager(Context context){
        prefs = context.getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
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

    public void saveInterceptIdForLaunchedSurvey(Activity activity, int interceptId, long time){
        Map<Integer, Long> myMap = new HashMap<>();
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences("Intercepts", Context.MODE_PRIVATE);
        String storedIntercepts = prefs.getString(LAUNCHED_SURVEYS, null);
        if (storedIntercepts != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<Integer, Long>>() {}.getType();
            myMap = gson.fromJson(storedIntercepts, type);
        }

        myMap.put(interceptId, time);
        Gson gson = new Gson();
        String json = gson.toJson(myMap);
        prefs.edit().putString(LAUNCHED_SURVEYS,json).apply();
    }

    public long getLaunchedInterceptTime(Context context, int interceptId){
        SharedPreferences prefs = context.getSharedPreferences("Intercepts", Context.MODE_PRIVATE);
        String json = prefs.getString(LAUNCHED_SURVEYS, null);

        //Log.d("Datta", interceptId+ " Getting Saved time: " + json);
        try {
            if (json != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<Integer, Long>>() {
                }.getType();
                Map<Integer, Long> myMap = gson.fromJson(json, type);


                if (myMap.containsKey(interceptId))
                    return myMap.get(interceptId);
            }
        }catch (Exception e){
            return 0;
        }
        return 0;
    }

    public void resetPreferences(){
        prefs.edit().clear().apply();
    }
}
