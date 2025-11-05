package com.questionpro.cxlib;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

class SharedPreferenceManager {
    protected static final String PREF_NAME="questionpro_cx";
    private final String INTERCEPT = "Intercepts";
    private final String VISITORS_UUID = "visitors_uuid";
    private final String LAUNCHED_SURVEYS = "launched_surveys";
    private static String interceptStr;
    private final Context context;
    private static SharedPreferenceManager instance;


    private SharedPreferenceManager(Context context){
        this.context = context.getApplicationContext();
    }

    protected static synchronized SharedPreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferenceManager(context);
        }
        return instance;
    }

    protected SharedPreferences getPrefs() {
        return context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    void saveProject(String intercept){
        interceptStr = intercept;
        getPrefs().edit().putString(INTERCEPT, intercept).apply();
    }

    String getProject(){
        if(CXUtils.isEmpty(interceptStr))
            interceptStr = getPrefs().getString(INTERCEPT, "");
        return interceptStr;
    }

    void saveVisitorsUUID(String uuid){
        getPrefs().edit().putString(VISITORS_UUID, uuid).apply();
    }

    String getVisitorsUUID(){
        return getPrefs().getString(VISITORS_UUID, "");
    }

    Intercept getInterceptById(int interceptId) throws Exception{
        JSONArray interceptArray = new JSONObject(getProject()).getJSONArray("intercepts");
        for(int i = 0; i < interceptArray.length(); i++){
            Intercept intercept = Intercept.fromJSON(interceptArray.getJSONObject(i));
            if(intercept.id == interceptId)
                return intercept;
        }
        return null;
    }
    int getInterceptSurveyId(int interceptId) throws Exception{
        JSONArray interceptArray = new JSONObject(getProject()).getJSONArray("intercepts");
        for(int i = 0; i < interceptArray.length(); i++){
            Intercept intercept = Intercept.fromJSON(interceptArray.getJSONObject(i));
            if(intercept.id == interceptId)
                return intercept.surveyId;
        }
        return -1;
    }

    int updateViewCountForTag(String tag){
        int updatedViewCount = getViewCountForTag(tag) + 1;
        getPrefs().edit().putInt(tag, updatedViewCount).apply();
        return updatedViewCount;
    }

    void resetViewCountForTag(String tag){
        getPrefs().edit().putInt(tag, 0).apply();
    }
    private int getViewCountForTag(String tag){
        return getPrefs().getInt(tag,0);
    }

    void saveInterceptIdForLaunchedSurvey(int interceptId, long time){
        Map<Integer, Long> myMap = new HashMap<>();
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences("Intercepts", Context.MODE_PRIVATE);
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

    long getLaunchedInterceptTime(int interceptId){
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

    boolean isSurveyAlreadyLaunched(int interceptId){
        SharedPreferences prefs = context.getSharedPreferences("Intercepts", Context.MODE_PRIVATE);
        String json = prefs.getString(LAUNCHED_SURVEYS, null);
        try {
            if (json != null) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<Integer, Long>>() {
                }.getType();
                Map<Integer, Long> myMap = gson.fromJson(json, type);
                return myMap.containsKey(interceptId);
            }
        }catch (Exception e){
            return false;
        }
        return false;
    }

    void saveCustomDataMappings(HashMap<Integer, String> customDataMappings){
        Gson gson = new Gson();
        String json = gson.toJson(customDataMappings);
        getPrefs().edit().putString(CXConstants.CUSTOM_DATA_MAPPINGS, json).apply();
    }

    String getCustomDataMappings(){
        return getPrefs().getString(CXConstants.CUSTOM_DATA_MAPPINGS, null);
    }

    void resetPreferences(){
        getPrefs().edit().clear().apply();
    }
}
