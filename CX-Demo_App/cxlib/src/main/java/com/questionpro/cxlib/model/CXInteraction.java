package com.questionpro.cxlib.model;

import android.util.Log;

import com.questionpro.cxlib.constants.CXConstants;

import org.json.JSONObject;

import java.io.Serializable;

public class CXInteraction implements Serializable{
    public String url="";
    public boolean isDialog;
    public String themeColor;

    public static CXInteraction fromJSON(JSONObject jsonObject) {
        CXInteraction cxInteraction = new CXInteraction();
        try {
            if (jsonObject.has(CXConstants.JSONResponseFields.CX_SURVEY_URL)) {
                cxInteraction.url = jsonObject.getString(CXConstants.JSONResponseFields.CX_SURVEY_URL);
            }
            if(jsonObject.has(CXConstants.JSONResponseFields.CORE_SURVEY_URL)){
                cxInteraction.url = jsonObject.getString(CXConstants.JSONResponseFields.CORE_SURVEY_URL);
            }
            if (jsonObject.has(CXConstants.JSONResponseFields.IS_DIALOG)) {
                cxInteraction.isDialog = jsonObject.getBoolean(CXConstants.JSONResponseFields.IS_DIALOG);
            }

            if(jsonObject.has(CXConstants.JSONResponseFields.THEME_COLOR)){
                cxInteraction.themeColor = jsonObject.getString(CXConstants.JSONResponseFields.THEME_COLOR);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return cxInteraction;
    }

    public static JSONObject toJSON(CXInteraction cxInteraction){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(CXConstants.JSONResponseFields.CX_SURVEY_URL, cxInteraction.url);
            jsonObject.put(CXConstants.JSONResponseFields.IS_DIALOG, cxInteraction.isDialog);
            jsonObject.put(CXConstants.JSONResponseFields.THEME_COLOR, cxInteraction.themeColor);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return jsonObject;
    }
}
