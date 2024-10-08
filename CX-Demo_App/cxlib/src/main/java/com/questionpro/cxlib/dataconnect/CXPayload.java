package com.questionpro.cxlib.dataconnect;

import android.util.Log;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.init.CXGlobalInfo;
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

    public static JSONObject getPayloadJSON(TouchPoint touchPoint) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("firstName", touchPoint.getFirstName());
            jsonObject.put("lastName", touchPoint.getLastName());
            jsonObject.put("transactionLanguage", touchPoint.getTransactionLanguage());
            jsonObject.put("mobile", touchPoint.getMobile());
            jsonObject.put("segmentCode", touchPoint.getSegmentCode());
            String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            jsonObject.put("transactionDate", date);
            if(null == touchPoint.getEmail()){
                jsonObject.put("email", CXGlobalInfo.getInstance().getUUID()+"@questionpro.com");
            }else
                jsonObject.put("email", touchPoint.getEmail());
            //jsonObject.put("surveyID", touchPoint.getTouchPointID());
            jsonObject.put("showAsDialog",touchPoint.showAsDialog());
            jsonObject.put("themeColor", touchPoint.getThemeColor());
            jsonObject.put("type", touchPoint.getType());
            jsonObject.put("dataCenter", touchPoint.getDataCenter());
            jsonObject.put("custom1", touchPoint.getCustomVariable1());
            jsonObject.put("custom2", touchPoint.getCustomVariable2());
            jsonObject.put("custom3", touchPoint.getCustomVariable3());
            Log.d("Datta", "Payload json: "+jsonObject.toString());

            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }
}
