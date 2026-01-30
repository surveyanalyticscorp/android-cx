package com.questionpro.cxlib.dataconnect;

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

    public static JSONObject getPayloadJSON(TouchPoint touchPoint) throws JSONException{
        JSONObject jsonObject = new JSONObject();
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        jsonObject.put("transactionDate", date);
        /*if(null == touchPoint.getEmail()){
            jsonObject.put("email", CXGlobalInfo.getInstance().getUUID()+"@questionpro.com");
        }else
            jsonObject.put("email", touchPoint.getEmail());*/
        jsonObject.put("dataCenter", touchPoint.getDataCenter());
        jsonObject.put("platform", touchPoint.getPlatform());
        //Log.d("Datta", "Payload json: "+jsonObject.toString());

        return jsonObject;
    }
}
