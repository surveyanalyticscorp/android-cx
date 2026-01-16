package com.questionpro.cxlib;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SharedPreferenceManager {
    protected static final String PREF_NAME="questionpro_cx";
    private static final String SURVEY_MAP_KEY = "survey_id_timestamp_map";
    private static SharedPreferenceManager instance;
    private final Context context;

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

    private void saveSurveyMap(Map<Long, Long> surveyMap) {
        JSONObject jsonObject = new JSONObject();
        for (Map.Entry<Long, Long> entry : surveyMap.entrySet()) {
            try {
                jsonObject.put(String.valueOf(entry.getKey()), entry.getValue());
            } catch (JSONException e) {
                // Handle exception if needed
            }
        }
        getPrefs().edit().putString(SURVEY_MAP_KEY, jsonObject.toString()).apply();
    }

    private Map<Long, Long> getSurveyMap() {
        String jsonString = getPrefs().getString(SURVEY_MAP_KEY, null);
        Map<Long, Long> map = new HashMap<>();
        if (jsonString != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    map.put(Long.parseLong(key), jsonObject.getLong(key));
                }
            } catch (JSONException e) {
                // Handle exception if needed
            }
        }
        return map;
    }

    protected void putSurveyTimestamp(long surveyId) {
        long currentTime = System.currentTimeMillis();
        Map<Long, Long> map = getSurveyMap();
        map.put(surveyId, currentTime);
        saveSurveyMap(map);
    }

    protected Long getSurveyTimestamp(long surveyId) {
        Map<Long, Long> map = getSurveyMap();
        return map.get(surveyId);
    }

}
