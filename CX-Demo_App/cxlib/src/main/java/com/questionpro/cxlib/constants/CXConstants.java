package com.questionpro.cxlib.constants;


import android.content.Context;

import com.questionpro.cxlib.init.CXGlobalInfo;
import com.questionpro.cxlib.enums.DataCenter;

public class CXConstants {

    private static final String CX_TRANSACTION_SURVEY_URL = "/a/api/v2/cx/transactions/survey-url";
    private static final String SURVEYS_URL = "/a/api/v2/surveys/";
    private static final String GET_MOBILE_INTERCEPTS = "/api/v1/visitor/mobile";
    private static final String CX_INTERCEPT_SURVEY_URL = "/api/v1/data-mapping/mobile/survey-url";
    private static final String SUBMIT_SURVEY_FEEDBACK = "/api/v1/visitor/mobile/survey-feedback";
    public static final String PREF_NAME="questionpro_cx";
    //public static final String PREF_KEY_API_KEY="cx_pref_api_key";
    public static final String MANIFEST_KEY_API_KEY="cx_manifest_api_key";
    public static final String PREF_KEY_APP_ACTIVITY_STATE_QUEUE="cx_key_app_activity_state_queue";
    //public static final String PREF_KEY_PAYLOAD="cx_pref_key_payload";
    public static final String CX_INTERACTION_CONTENT="cx_interaction_content";


    public static String getSurveyUrl(Context context) {
        try {
            String dataCenter = CXGlobalInfo.getDataCenter();
            return getBaseUrl(dataCenter) + CX_INTERCEPT_SURVEY_URL;
            //return "https://cx-intercept-staging-api.questionpro.com" + CX_INTERCEPT_SURVEY_URL;
        }catch (Exception e){
            e.printStackTrace();
        }
        return "";
    }

    public static String getInterceptsUrl(){
        String dataCenter = CXGlobalInfo.getDataCenter();
        return getBaseUrl(dataCenter) + GET_MOBILE_INTERCEPTS;
    }

    public static String getFeedbackUrl(){
        String dataCenter = CXGlobalInfo.getDataCenter();
        return getBaseUrl(dataCenter) + SUBMIT_SURVEY_FEEDBACK;
    }

    private static String getBaseUrl(String dataCenter){
        if(DataCenter.EU.name().equalsIgnoreCase(dataCenter))
            return "https://intercept-api.questionpro.eu";
        if(DataCenter.CA.name().equalsIgnoreCase(dataCenter))
            return "https://intercept-api.questionpro.ca";
        if(DataCenter.SG.name().equalsIgnoreCase(dataCenter))
            return "https://intercept-api.questionpro.sg";
        if(DataCenter.AU.name().equalsIgnoreCase(dataCenter))
            return "https://intercept-api.questionpro.au";
        if(DataCenter.AE.name().equalsIgnoreCase(dataCenter))
            return "https://intercept-api.questionpro.ae";
        if(DataCenter.SA.name().equalsIgnoreCase(dataCenter))
            return "https://api.surveyanalytics.com";
        if(DataCenter.KSA.name().equalsIgnoreCase(dataCenter))
            return "https://intercept-api.questionpro.com";

        return "https://intercept-api.questionpro.com";
    }

    public static class JSONUploadFields{
        public static final String UDID = "udid";
        public static final String SURVEY_ID = "surveyID";
    }

    public static class JSONResponseFields{
        public static final String STATUS = "status";
        public static final String RESPONSE = "response";
        public static final String PROJECT = "project";
        public static final String VISITOR = "visitor";
        public static final String CX_SURVEY_URL = "surveyURL";
        public static final String CORE_SURVEY_URL = "url";
        public static final String IS_DIALOG="isDialog";
        public static final String THEME_COLOR="themeColor";
        public static final String ID = "id";
        public static final String MESSAGE="message";
        public static final String EMPTY="empty";
    }

}
