package com.questionpro.cxlib.init;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.dataconnect.CXPayload;
import com.questionpro.cxlib.model.CXInteraction;
import com.questionpro.cxlib.model.TouchPoint;

import org.json.JSONException;
import org.json.JSONObject;

public class CXGlobalInfo {
    public static boolean initialized = false;
    public static String UUID;
    public static String appDisplayName;
    public static String appPackage;
    public static String apiKey = null;


    private static CXGlobalInfo ourInstance;

    public static CXGlobalInfo getInstance() {
        if(ourInstance==null){
            ourInstance = new CXGlobalInfo();
        }
        return ourInstance;
    }

    private CXGlobalInfo() {
    }


    public static boolean isInteractionPending(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return  prefs.contains(activity.getLocalClassName());


    }
    public static boolean isInteractionPending(Activity activity,long touchPointID){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return  prefs.contains(touchPointID+"");
    }

    public static void clearInteraction(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(activity.getLocalClassName()).apply();
    }

    public static void clearInteraction(Activity activity, long touchPointID){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(touchPointID+"").apply();
    }
    public static void clearPayload(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(CXConstants.PREF_KEY_PAYLOAD).apply();
    }

    /**
     * This function is used to save the payload in preferences at the time of initialization.
     */
    public static void savePayLoad(Activity activity, TouchPoint touchPoint){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(CXConstants.PREF_KEY_PAYLOAD, CXPayload.getPayloadJSON(touchPoint).toString());
        ed.apply();
    }

    /**
     * At the time of initialization we don't have surveyId in payload. add the surveyId
     * in payload and update preferences.
     */
    public static void updateCXPayloadWithSurveyId(Activity activity, long surveyId){
        try {
            JSONObject payloadObj = new JSONObject(getStoredPayload(activity));
            payloadObj.put("surveyID", surveyId);

            SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(CXConstants.PREF_KEY_PAYLOAD, payloadObj.toString());
            ed.apply();
        }catch (Exception e){e.printStackTrace();}
    }

    /**
     * This function is used to get the type of Survey
     * @param context
     * @return
     */
    public static String getType(Context context){
        try{
            AppCompatActivity activity = (AppCompatActivity) context;
            JSONObject payloadObj = new JSONObject(getStoredPayload(activity));
            return payloadObj.getString("type");
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    public static String getDataCenter(Context context){
        try{
            AppCompatActivity activity = (AppCompatActivity) context;
            JSONObject payloadObj = new JSONObject(getStoredPayload(activity));
            return payloadObj.getString("dataCenter");
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    public static String isShowDialog(Context context){
        try{
            AppCompatActivity activity = (AppCompatActivity) context;
            JSONObject payloadObj = new JSONObject(getStoredPayload(activity));
            return payloadObj.getString("showAsDialog");
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    public static String getThemeColour(Context context){
        try{
            AppCompatActivity activity = (AppCompatActivity) context;
            JSONObject payloadObj = new JSONObject(getStoredPayload(activity));
            return payloadObj.getString("themeColor");
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    /**
     * Get the payload in the form string at the time of fetching survey url.
     */
    private static String getStoredPayload(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(CXConstants.PREF_KEY_PAYLOAD,"");
    }

    /**
     * Get the payload in the form string at the time of fetching survey url.
     * Have to remove unwanted key from the stored object.
     */
    public static String getApiPayload(Activity activity){
        try {
            SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
            JSONObject payloadObj = new JSONObject(prefs.getString(CXConstants.PREF_KEY_PAYLOAD, ""));
            payloadObj.put("isManualSurvey", true);
            payloadObj.remove("showAsDialog");
            payloadObj.remove("themeColor");
            payloadObj.remove("type");
            payloadObj.remove("dataCenter");
            return payloadObj.toString();
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    public static void storeInteraction(Activity activity, String url){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(activity.getLocalClassName(), url);
        ed.apply();
    }
    public static void storeInteraction(Activity activity, long touchPointID, CXInteraction cxInteraction){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(touchPointID+"", CXInteraction.toJSON(cxInteraction).toString());
        ed.apply();
    }
    public static String getInteraction(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(activity.getLocalClassName(),"");
    }
    public static CXInteraction getInteraction(Activity activity, long touchPointId) throws JSONException{
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        JSONObject jsonObject  = new JSONObject(prefs.getString(touchPointId+"",""));
        return CXInteraction.fromJSON(jsonObject);
    }

    public static long getTouchPointIDFromPayload(String payload){
        try {
            JSONObject jsonObject = new JSONObject(payload);
            if(jsonObject.has(CXConstants.JSONUploadFields.TOUCH_POINT_ID)){
                return jsonObject.getLong(CXConstants.JSONUploadFields.TOUCH_POINT_ID);
            }
        }
        catch (Exception e){
            //eat it
        }
        return -1;
    }
}
