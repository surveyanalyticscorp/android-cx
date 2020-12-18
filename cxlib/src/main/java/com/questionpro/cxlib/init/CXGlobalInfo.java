package com.questionpro.cxlib.init;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.dataconnect.CXPayload;
import com.questionpro.cxlib.model.CXInteraction;
import com.questionpro.cxlib.model.TouchPoint;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sachinsable on 12/04/16.
 */
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

    public static boolean isInteractionPending(Activity activity,TouchPoint touchPoint){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return  prefs.contains(touchPoint.getTouchPointID()+"");
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
    public static void setPayLoad(Activity activity, CXPayload cxPayload){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(CXConstants.PREF_KEY_PAYLOAD, cxPayload.getPayloadJSON().toString());
        ed.apply();
    }

    public static String getCXPayload(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(CXConstants.PREF_KEY_PAYLOAD,"");
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
