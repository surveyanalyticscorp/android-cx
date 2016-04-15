package com.questionpro.cxlib.dataconnect;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sachinsable on 14/04/16.
 */
public class CXResponseContent {

    private String url="";

    public static CXResponseContent fromJSON(JSONObject jsonObject) throws JSONException{
        CXResponseContent cxResponseContent = new CXResponseContent();
        cxResponseContent.url = jsonObject.getString("surveyURL");
        return  cxResponseContent;
    }


}
