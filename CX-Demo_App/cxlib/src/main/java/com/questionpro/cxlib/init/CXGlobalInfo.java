package com.questionpro.cxlib.init;

import static com.questionpro.cxlib.constants.CXConstants.JSONUploadFields.SURVEY_ID;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.questionpro.cxlib.dataconnect.CXPayload;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.model.InterceptSettings;
import com.questionpro.cxlib.model.TouchPoint;
import com.questionpro.cxlib.util.CXUtils;
import com.questionpro.cxlib.util.SharedPreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
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
    public void savePayLoad(TouchPoint touchPoint) throws JSONException {
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
    /*@NonNull
    public static String getType(Context context){
        try{
            JSONObject payloadObj = new JSONObject(getStoredPayload());
            return payloadObj.getString("type");
        }catch (Exception e){e.printStackTrace();}
        return "";
    }*/

    @NonNull
    public static String getDataCenter(){
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
            JSONObject payloadObj = new JSONObject(CXGlobalInfo.payload);
            payloadObj.put("isManualSurvey", true);
            payloadObj.remove("showAsDialog");
            payloadObj.remove("themeColor");
            payloadObj.remove("type");
            payloadObj.remove("dataCenter");
            return payloadObj.toString();
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    public static String getInterceptApiPayload(Intercept intercept, Context context){
        try {
            JSONObject payloadObj = new JSONObject();
            payloadObj.put("packageName", context.getPackageName());
            payloadObj.put("visitedUserId",new SharedPreferenceManager(context).getVisitorsUUID());
            payloadObj.put("interceptId",intercept.id);
            payloadObj.put("surveyId",intercept.surveyId);
            if(intercept.interceptSettings != null) {
                InterceptSettings interceptSettings = intercept.interceptSettings;
                if(interceptSettings.autoLanguageSelection){
                    setAppLanguage(payloadObj, context);
                }
            }
            CXGlobalInfo.setCustomVariable(payloadObj);

            return payloadObj.toString();
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    private static void setAppLanguage(JSONObject requestObj, Context context) throws JSONException{
        requestObj.put("surveyLanguage", CXUtils.getAppLanguage(context));
    }

    private static void setCustomVariable(JSONObject requestObj){
        try {
            JSONObject payloadObj = new JSONObject(CXGlobalInfo.payload);
            if (payloadObj.has("customVariables")) {
                //String inputString = "key1=value1,key2=value2,key3=value3";
                String inputString = payloadObj.getString("customVariables").replace("{","").replace("}","");

                Map<String, String> myMap = new HashMap<>();
                String[] pairs = inputString.split(",");
                JSONArray customVars = new JSONArray();
                for (String pair : pairs) {
                    JSONObject customVar = new JSONObject();
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        String key = "custom"+keyValue[0].trim();
                        String value = keyValue[1].trim();
                        //myMap.put(key, value);
                        customVar.put("variableName",key);
                        customVar.put("value",value);
                        customVars.put(customVar);
                    }
                }
                requestObj.put("data",customVars);
            }
        }catch (Exception e){}
    }
}
