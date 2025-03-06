package com.questionpro.cxlib.init;

import static com.questionpro.cxlib.constants.CXConstants.JSONUploadFields.SURVEY_ID;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
//import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.dataconnect.CXPayload;
import com.questionpro.cxlib.model.TouchPoint;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class CXGlobalInfo {
    private static boolean initialized = false;
    private static String UUID;
    private static String appPackage;
    private static String apiKey = null;
    private static String payload;

    private static CXGlobalInfo ourInstance;

    public static CXGlobalInfo getInstance() {
        if(ourInstance == null){
            ourInstance = new CXGlobalInfo();
        }
        return ourInstance;
    }

    private CXGlobalInfo() {
    }


    /**
     * This function is used to save the payload in preferences at the time of initialization.
     */
    public void savePayLoad(TouchPoint touchPoint){
        CXGlobalInfo.payload = CXPayload.getPayloadJSON(touchPoint).toString();
    }

    public void setApiKey(String apiKey){
        CXGlobalInfo.apiKey = apiKey;
    }

    public String getApiKey(){
        return CXGlobalInfo.apiKey;
    }

    public void setAppPackage(String appPackage){
        CXGlobalInfo.appPackage = appPackage;
    }

    public void setUUID(String uuid){
        CXGlobalInfo.UUID = uuid;
    }

    public String getUUID(){
        return CXGlobalInfo.UUID;
    }

    public void setInitialized(boolean initialized){
        CXGlobalInfo.initialized = initialized;
    }

    /**
     * At the time of initialization we don't have surveyId in payload. add the surveyId
     * in payload and update preferences.
     */
    public static void updateCXPayloadWithSurveyId(long surveyId){
        try {
            JSONObject payloadObj = new JSONObject(getStoredPayload());
            payloadObj.put(SURVEY_ID, surveyId);

            CXGlobalInfo.payload = payloadObj.toString();
        }catch (Exception e){e.printStackTrace();}
    }

    /**
     * This function is used to get the type of Survey
     * @param context
     * @return
     */
    @NonNull
    public static String getType(Context context){
        try{
            JSONObject payloadObj = new JSONObject(getStoredPayload());
            return payloadObj.getString("type");
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    @NonNull
    public static String getDataCenter(Context context){
        try{
            JSONObject payloadObj = new JSONObject(getStoredPayload());
            return payloadObj.getString("dataCenter");
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    @NonNull
    public static boolean isShowDialog(Context context){
        try{
            JSONObject payloadObj = new JSONObject(getStoredPayload());
            return payloadObj.getBoolean("showAsDialog");
        }catch (Exception e){e.printStackTrace();}
        return false;
    }

    @NonNull
    public static String getThemeColour(Context context){
        try{
            //AppCompatActivity activity = (AppCompatActivity) context;
            JSONObject payloadObj = new JSONObject(getStoredPayload());
            return payloadObj.getString("themeColor");
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    /**
     * Get the payload in the form string at the time of fetching survey url.
     */
    private static String getStoredPayload(){
        return CXGlobalInfo.payload;
    }

    /**
     * Get the payload in the form string at the time of fetching survey url.
     * Have to remove unwanted key from the stored object.
     */
    public static String getApiPayload(Activity activity){
        try {
            //SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
            //JSONObject payloadObj = new JSONObject(prefs.getString(CXConstants.PREF_KEY_PAYLOAD, ""));
            JSONObject payloadObj = new JSONObject(CXGlobalInfo.payload);
            payloadObj.put("isManualSurvey", true);
            CXGlobalInfo.setCustomVariable(payloadObj);
            payloadObj.remove("showAsDialog");
            payloadObj.remove("themeColor");
            payloadObj.remove("type");
            payloadObj.remove("dataCenter");
            return payloadObj.toString();
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    private static void setCustomVariable(JSONObject payloadObj){
        try {
            if (payloadObj.has("customVariables")) {
                //String inputString = "key1=value1,key2=value2,key3=value3";
                String inputString = payloadObj.getString("customVariables").replace("{","").replace("}","");

                Map<String, String> myMap = new HashMap<>();
                String[] pairs = inputString.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        String key = "custom"+keyValue[0].trim();
                        String value = keyValue[1].trim();
                        //myMap.put(key, value);
                        payloadObj.put(key,value);
                    }
                }
                payloadObj.remove("customVariables");
                Log.d("Datta","playload: "+payloadObj);
            }
        }catch (Exception e){}
    }

    /*public static boolean isInteractionPending(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return  prefs.contains(activity.getLocalClassName());
    }*/

    /*public static boolean isInteractionPending(Activity activity,long touchPointID){
        //SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        //return  prefs.contains(touchPointID+"");
        //return interactions.containsKey(touchPointID);
    }*/

    /*public static void clearInteraction(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(activity.getLocalClassName()).apply();
    }*/

    /*public static void clearInteraction(Activity activity, long touchPointID){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(touchPointID+"").apply();
    }*/
    /*public static void clearPayload(Activity activity){
     *//*SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.remove(CXConstants.PREF_KEY_PAYLOAD).apply();*//*
    }*/

    /*public static void storeInteraction(Activity activity, String url){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(activity.getLocalClassName(), url);
        ed.apply();
    }*/
    /*public static void storeInteraction(Activity activity, long surveyID, CXInteraction cxInteraction){
        *//*SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putString(touchPointID+"", CXInteraction.toJSON(cxInteraction).toString());
        ed.apply();*//*
        interactions.put(surveyID, CXInteraction.toJSON(cxInteraction).toString());
    }*/
    /*public static String getInteraction(Activity activity){
        SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(activity.getLocalClassName(),"");
    }*/
    /*public static CXInteraction getInteraction(Activity activity, long surveyID) throws JSONException{
        *//*SharedPreferences prefs = activity.getApplicationContext().getSharedPreferences(CXConstants.PREF_NAME, Context.MODE_PRIVATE);
        JSONObject jsonObject  = new JSONObject(prefs.getString(touchPointId+"",""));*//*
        JSONObject jsonObject  = new JSONObject(interactions.get(surveyID));
        return CXInteraction.fromJSON(jsonObject);
    }*/

    /*public static long getSurveyIDFromPayload(String payload){
        try {
            JSONObject jsonObject = new JSONObject(payload);
            if(jsonObject.has(CXConstants.JSONUploadFields.SURVEY_ID)){
                return jsonObject.getLong(CXConstants.JSONUploadFields.SURVEY_ID);
            }
        }
        catch (Exception e){
            //eat it
        }
        return -1;
    }*/
}
