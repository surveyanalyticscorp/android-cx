package com.questionpro.cxlib;

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
    private static final String PREF_NAME_INTERCEPTS="Intercepts";
    private final String KEY_PROJECTS = "Projects";
    private final String KEY_VISITORS_UUID = "visitors_uuid";
    private final String KEY_LAUNCHED_SURVEYS = "launched_surveys";
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

    private SharedPreferences getPrefs() {
        return this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    void saveProject(String project){
        interceptStr = project;
        getPrefs().edit().putString(KEY_PROJECTS, project).apply();
    }

    String getProject(){
        if(CXUtils.isEmpty(interceptStr)){
            interceptStr = getPrefs().getString(KEY_PROJECTS, "");
        }
        return interceptStr;
    }

    void saveVisitorsUUID(String uuid){
        getPrefs().edit().putString(KEY_VISITORS_UUID, uuid).apply();
    }

    String getVisitorsUUID(){
        return getPrefs().getString(KEY_VISITORS_UUID, "");
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

    void saveCustomDataMappings(HashMap<Integer, String> customDataMappings){
        Gson gson = new Gson();
        String json = gson.toJson(customDataMappings);
        getPrefs().edit().putString(CXConstants.CUSTOM_DATA_MAPPINGS, json).apply();
    }

    String getCustomDataMappings(){
        return getPrefs().getString(CXConstants.CUSTOM_DATA_MAPPINGS, null);
    }

    void resetPreferences(){
        interceptStr = null;
        getPrefs().edit().clear().apply();
    }


    void saveInterceptIdForLaunchedSurvey(int interceptId, long time){
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME_INTERCEPTS, Context.MODE_PRIVATE);
        Map<Integer, Long> myMap = getLaunchedSurveysMap(prefs);

        myMap.put(interceptId, time);
        Gson gson = new Gson();
        String json = gson.toJson(myMap);
        prefs.edit().putString(KEY_LAUNCHED_SURVEYS,json).apply();
    }

    long getLaunchedInterceptTime(int interceptId){
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME_INTERCEPTS, Context.MODE_PRIVATE);
        Map<Integer, Long> myMap = getLaunchedSurveysMap(prefs);
        if (myMap.containsKey(interceptId))
            return myMap.get(interceptId) != null ? myMap.get(interceptId) : 0;
        return 0;
    }

    boolean isSurveyAlreadyLaunched(int interceptId){
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME_INTERCEPTS, Context.MODE_PRIVATE);
        return getLaunchedSurveysMap(prefs).containsKey(interceptId);
    }

    private Map<Integer, Long> getLaunchedSurveysMap(SharedPreferences prefs){
        String json = prefs.getString(KEY_LAUNCHED_SURVEYS, null);
        if (json != null) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<Integer, Long>>(){}.getType();
                return gson.fromJson(json, type);
            } catch (Exception e) {
                return new HashMap<>();
            }
        }
        return new HashMap<>();
    }
}
