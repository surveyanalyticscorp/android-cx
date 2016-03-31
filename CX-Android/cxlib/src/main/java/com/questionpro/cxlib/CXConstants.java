package com.questionpro.cxlib;

/**
 * Created by sachinsable on 29/03/16.
 */
public class CXConstants {
    public static final String CX_URL = "http://192.168.1.25/a/api/questionpro.cx.mobileTouchpoint?apiKey=";

    public static String globalDialogPromptTitleText="CX Feeback";
    public static  String globalDialogPromptMessageText="Would you like to give us some feedback?";
    public static String globalDialogPromptPositiveText="Yes";
    public static String globalDialogPromptNegativeText="No";
    public static String getCXUploadURL(String apiKey){
        return CX_URL + apiKey;
    }


    public static class JSONUploadFields{
        public static final String UDID = "udid";
        public static final String TOUCH_POINT_ID = "touchPointID";
    }


    public static class JSONResponseFields{
        public static final String STATUS = "status";
        public static final String RESPONSE = "response";
        public static final String SURVEY_URL = "surveyURL";
        public static final String ID = "id";
        public static final String MESSAGE="message";
        public static final String EMPTY="empty";



    }
}
