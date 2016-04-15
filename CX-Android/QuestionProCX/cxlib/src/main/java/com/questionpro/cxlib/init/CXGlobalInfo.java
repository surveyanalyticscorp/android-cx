package com.questionpro.cxlib.init;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.dataconnect.CXPayload;
import com.questionpro.cxlib.dataconnect.TouchPoint;

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
        ed.remove(activity.getLocalClassName()).commit();
    }

    public static void clearInteraction(Activity activity, TouchPoint touchPoint){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(touchPoint.getTouchPointID()+"").commit();
    }
    public static void clearPayload(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(CXConstants.PREF_KEY_PAYLOAD).commit();
    }
    public static void setPayLoad(Activity activity, CXPayload cxPayload){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(CXConstants.PREF_KEY_PAYLOAD, cxPayload.getPayloadJSON().toString());
        ed.commit();
    }

    public static String getCXPayload(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(CXConstants.PREF_KEY_PAYLOAD,"");
    }

    public static void setSurveyURL(Activity activity, String url){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(activity.getLocalClassName(), url);
        ed.commit();
    }
    public static void setSurveyURL(Activity activity,long touchPointID, String url){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(touchPointID+"", url);
        ed.commit();
    }
    public static String getSurveyURL(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(activity.getLocalClassName(),"");
    }
    public static String getSurveyURL(Activity activity, TouchPoint touchPoint){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(touchPoint.getTouchPointID()+"","");
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
