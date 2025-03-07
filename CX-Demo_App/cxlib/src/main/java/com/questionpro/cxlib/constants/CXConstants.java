package com.questionpro.cxlib.constants;


import android.content.Context;

import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.model.DataCenter;

import org.json.JSONException;
import org.json.JSONObject;

public class CXConstants {

    private static final String CX_TRANSACTION_SURVEY_URL = "/a/api/v2/cx/transactions/survey-url";
    private static final String SURVEYS_URL = "/a/api/v2/surveys/";
    public static final String PREF_NAME="questionpro_cx";
    //public static final String PREF_KEY_API_KEY="cx_pref_api_key";
    public static final String MANIFEST_KEY_API_KEY="cx_manifest_api_key";
    public static final String PREF_KEY_APP_ACTIVITY_STATE_QUEUE="cx_key_app_activity_state_queue";
    //public static final String PREF_KEY_PAYLOAD="cx_pref_key_payload";
    public static final String CX_INTERACTION_CONTENT="cx_interaction_content";


    public static String getUrl(Context context, String surveyId) {
        try {
            //String type = CXGlobalInfo.getType(context);
            String dataCenter = CXGlobalInfo.getDataCenter(context);
            return getBaseUrl(dataCenter) + CX_TRANSACTION_SURVEY_URL;
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    private static String getBaseUrl(String dataCenter){
        if(DataCenter.EU.name().equalsIgnoreCase(dataCenter))
            return "https://api.questionpro.eu";
        if(DataCenter.CA.name().equalsIgnoreCase(dataCenter))
            return "https://api.questionpro.ca";
        if(DataCenter.SG.name().equalsIgnoreCase(dataCenter))
            return "https://api.questionpro.sg";
        if(DataCenter.AU.name().equalsIgnoreCase(dataCenter))
            return "https://api.questionpro.au";
        if(DataCenter.AE.name().equalsIgnoreCase(dataCenter))
            return "https://api.questionpro.ae";
        if(DataCenter.SA.name().equalsIgnoreCase(dataCenter))
            return "https://api.surveyanalytics.com";
        if(DataCenter.KSA.name().equalsIgnoreCase(dataCenter))
            return "https://api.questionprosa.com";

        return "https://api.questionpro.com";
    }

    public static class JSONUploadFields{
        public static final String UDID = "udid";
        public static final String SURVEY_ID = "surveyID";
    }

    public static class JSONResponseFields{
        public static final String STATUS = "status";
        public static final String RESPONSE = "response";
        public static final String CX_SURVEY_URL = "surveyURL";
        public static final String CORE_SURVEY_URL = "url";
        public static final String IS_DIALOG="isDialog";
        public static final String THEME_COLOR="themeColor";
        public static final String ID = "id";
        public static final String MESSAGE="message";
        public static final String EMPTY="empty";
    }
}
