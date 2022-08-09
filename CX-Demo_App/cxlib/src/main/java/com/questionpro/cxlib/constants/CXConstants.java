package com.questionpro.cxlib.constants;


import com.questionpro.cxlib.model.Type;

import org.json.JSONException;
import org.json.JSONObject;

public class CXConstants {
    private static final String CX_URL = "https://api.questionpro.com/a/api/questionpro.cx.getSurveyURL?apiKey=";
    private static final String SURVEYS_URL = "https://api.questionpro.com/a/api/v2/surveys/";
    public static final String PREF_NAME="questionpro_cx";
    public static final String PREF_KEY_API_KEY="cx_pref_api_key";
    public static final String MANIFEST_KEY_API_KEY="cx_manifest_api_key";
    public static final String PREF_KEY_APP_ACTIVITY_STATE_QUEUE="cx_key_app_activity_state_queue";
    public static final String PREF_KEY_PAYLOAD="cx_pref_key_payload";
    public static final String PREF_KEY_URL="cx_pref_key_url";
    public static final String SURVEY_URL="survey_url";
    public static final String CX_INTERACTION_CONTENT="cx_interaction_content";
    public static final String EXTRA_TOUCH_POINT="touch_point";

    public static String globalDialogPromptTitleText="CX Feeback";
    public static  String globalDialogPromptMessageText="Would you like to give us some feedback?";
    public static String globalDialogPromptPositiveText="Yes";
    public static String globalDialogPromptNegativeText="No";

    public static String getUrl(String apiKey, String payload) {
        try {
            JSONObject payloadObj = new JSONObject(payload);
            if (Type.CUSTOMER_EXPERIENCE.toString().equals(payloadObj.getString("type"))) {
                return CX_URL + apiKey;
            } else {
                return SURVEYS_URL + payloadObj.getString("surveyID") + "?apiKey=" + apiKey;
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
        return "";
    }

    public static class JSONUploadFields{
        public static final String UDID = "udid";
        public static final String TOUCH_POINT_ID = "touchPointID";
    }

    public static class JSONResponseFields{
        public static final String STATUS = "status";
        public static final String RESPONSE = "response";
        public static final String CX_SURVEY_URL = "SurveyURL";
        public static final String CORE_SURVEY_URL = "url";
        public static final String IS_DIALOG="isDialog";
        public static final String THEME_COLOR="themeColor";
        public static final String ID = "id";
        public static final String MESSAGE="message";
        public static final String EMPTY="empty";
    }
}
