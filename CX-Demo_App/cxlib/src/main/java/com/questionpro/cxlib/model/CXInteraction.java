package com.questionpro.cxlib.model;

import com.questionpro.cxlib.constants.CXConstants;

import org.json.JSONObject;

import java.io.Serializable;

public class CXInteraction implements Serializable{

    public String url="";
    public boolean isDialog = true;

    public static CXInteraction fromJSON(JSONObject jsonObject) {
        CXInteraction cxInteraction = new CXInteraction();
        try {


            if (jsonObject.has(CXConstants.JSONResponseFields.SURVEY_URL)) {
                cxInteraction.url = jsonObject.getString(CXConstants.JSONResponseFields.SURVEY_URL);
            }
            if (jsonObject.has(CXConstants.JSONResponseFields.IS_DIALOG)) {
                cxInteraction.isDialog = true;//jsonObject.getBoolean(CXConstants.JSONResponseFields.IS_DIALOG);
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
            jsonObject.put(CXConstants.JSONResponseFields.SURVEY_URL, cxInteraction.url);
            jsonObject.put(CXConstants.JSONResponseFields.IS_DIALOG, cxInteraction.isDialog);

        }
        catch (Exception e){
            e.printStackTrace();
        }
        return jsonObject;
    }


}
