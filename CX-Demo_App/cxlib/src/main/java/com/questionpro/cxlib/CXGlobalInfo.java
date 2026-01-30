package com.questionpro.cxlib;

import static com.questionpro.cxlib.CXConstants.JSONUploadFields.SURVEY_ID;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.questionpro.cxlib.dataconnect.CXPayload;
import com.questionpro.cxlib.enums.Platform;
import com.questionpro.cxlib.model.DataMapping;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.model.InterceptSettings;
import com.questionpro.cxlib.model.TouchPoint;
import com.questionpro.cxlib.util.CXUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class CXGlobalInfo {
    private static String UUID;
    private static String apiKey = null;
    private static String payload;

    private static CXGlobalInfo ourInstance;

    public static CXGlobalInfo getInstance() {
        if(ourInstance == null){
            ourInstance = new CXGlobalInfo();
        }
        return ourInstance;
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

    public void setUUID(String uuid){
        CXGlobalInfo.UUID = uuid;
    }

    public String getUUID(){
        return CXGlobalInfo.UUID;
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

    @NonNull
    public static String getDataCenter(){
        try{
            JSONObject payloadObj = new JSONObject(getStoredPayload());
            return payloadObj.getString("dataCenter");
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    protected Platform getPlatform(){
        try{
            JSONObject payloadObj = new JSONObject(getStoredPayload());
            String platformStr = payloadObj.getString("platform");
            return Platform.valueOf(platformStr);
        }catch (Exception e){
            e.printStackTrace();
        }
        return Platform.ANDROID;
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
    public static String getApiPayload(long surveyId){
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

    protected HashMap<String, String> getInterceptApiPayload(Context mContext){
        try{
            HashMap<String, String> headers = new HashMap<>();
            headers.put("x-app-key",CXGlobalInfo.getInstance().getApiKey());
            headers.put("visitor-id",SharedPreferenceManager.getInstance(mContext).getVisitorsUUID());
            headers.put("package-name", mContext.getPackageName());
            headers.put("x-platform", getPlatformType());
            headers.put("x-device-id", CXUtils.getUniqueDeviceId(mContext));
            return headers;
        }catch (Exception e){e.printStackTrace();}
        return new HashMap<>();
    }

    protected String getSurveyApiPayload(Intercept intercept, Context context){
        try {
            JSONObject payloadObj = new JSONObject();
            payloadObj.put("packageName", context.getPackageName());
            payloadObj.put("visitedUserId",SharedPreferenceManager.getInstance(context).getVisitorsUUID());
            payloadObj.put("interceptId",intercept.id);
            payloadObj.put("surveyId",intercept.surveyId);
            if(intercept.interceptSettings != null) {
                InterceptSettings interceptSettings = intercept.interceptSettings;
                if(interceptSettings.autoLanguageSelection){
                    setAppLanguage(payloadObj, context);
                }
            }
            setCustomVariable(payloadObj, intercept, context);

            return payloadObj.toString();
        }catch (Exception e){e.printStackTrace();}
        return "";
    }

    private void setAppLanguage(JSONObject requestObj, Context context) throws JSONException{
        requestObj.put("surveyLanguage", CXUtils.getAppLanguage(context));
    }

    private void setCustomVariable(JSONObject requestObj, Intercept intercept, Context context){
        try {
            JSONArray customVars = getCustomVariablesFromPayload();
            ArrayList<DataMapping> dataMappings = intercept.dataMappings;
            String dataMappingPref = SharedPreferenceManager.getInstance(context).getCustomDataMappings();
            if(CXUtils.isEmpty(dataMappingPref) || dataMappings.isEmpty()){
                requestObj.put("data",customVars);
                return;
            }
            JSONObject dataMappingObj = new JSONObject(dataMappingPref);
            for (DataMapping dataMapping : dataMappings) {
                String value = getValueIgnoreCase(dataMappingObj, dataMapping.displayName.trim());
                if(null != value){
                    JSONObject customVar = new JSONObject();
                    //String value = dataMappingObj.getString(dataMapping.displayName.trim());
                    customVar.put("variableName",dataMapping.variable);
                    customVar.put("value",value);
                    customVars.put(customVar);
                }
            }
            requestObj.put("data",customVars);
        }catch (Exception e){e.printStackTrace();}
    }

    private static boolean hasKeyIgnoreCase(JSONObject jsonObject, String key) {
        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String currentKey = it.next();
            if (currentKey.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private String getValueIgnoreCase(JSONObject jsonObject, String key) throws JSONException {
        for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
            String currentKey = it.next();
            if (currentKey.equalsIgnoreCase(key)) {
                return jsonObject.getString(currentKey);
            }
        }
        return null;
    }

    private JSONArray getCustomVariablesFromPayload(){
        try{
            JSONObject payloadObj = new JSONObject(getStoredPayload());
            if (payloadObj.has("customVariables")) {
                String inputString = payloadObj.getString("customVariables").replace("{","").replace("}","");
                String[] pairs = inputString.split(",");
                return getCustomVars(pairs);
            }
        }catch (Exception e){e.printStackTrace();}
        return new JSONArray();
    }

    @NonNull
    private JSONArray getCustomVars(String[] pairs) throws JSONException {
        JSONArray customVars = new JSONArray();
        for (String pair : pairs) {
            JSONObject customVar = new JSONObject();
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = "custom"+keyValue[0].trim();
                String value = keyValue[1].trim();
                customVar.put("variableName",key);
                customVar.put("value",value);
                customVars.put(customVar);
            }
        }
        return customVars;
    }

    private String getPlatformType(){
        if(getPlatform().equals(Platform.REACT_NATIVE))
            return "react-native";
        else if (getPlatform().equals(Platform.IOS))
            return "ios";
        else if (getPlatform().equals(Platform.FLUTTER))
            return "flutter";
        else
            return "android";
    }
}
