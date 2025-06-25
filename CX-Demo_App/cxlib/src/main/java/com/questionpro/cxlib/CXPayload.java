package com.questionpro.cxlib;

import android.util.Log;

import com.questionpro.cxlib.CXGlobalInfo;
import com.questionpro.cxlib.model.TouchPoint;

import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;


/**
 * Created by Dattakunde on 14/04/16.
 */
public class CXPayload {

    protected static JSONObject getPayloadJSON(TouchPoint touchPoint) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("firstName", touchPoint.getFirstName());
            jsonObject.put("lastName", touchPoint.getLastName());
            jsonObject.put("transactionLanguage", touchPoint.getTransactionLanguage());
            jsonObject.put("mobile", touchPoint.getMobile());
            jsonObject.put("segmentCode", touchPoint.getSegmentCode());
            //String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            //jsonObject.put("transactionDate", date);
            if(null == touchPoint.getEmail()){
                jsonObject.put("email", CXGlobalInfo.getInstance().getUUID()+"@questionpro.com");
            }else
                jsonObject.put("email", touchPoint.getEmail());
            //jsonObject.put("surveyID", touchPoint.getTouchPointID());
            jsonObject.put("showAsDialog",touchPoint.showAsDialog());
            jsonObject.put("themeColor", touchPoint.getThemeColor());
            jsonObject.put("accessToken", touchPoint.getAccessToken());
            jsonObject.put("apiBaseUrl", touchPoint.getApiBaseUrl());
            jsonObject.put("port", touchPoint.getPort());
            jsonObject.put("customVariables", touchPoint.getCustomVariables());
            Log.d("Datta", "Payload json: "+jsonObject.toString());

            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }
}
