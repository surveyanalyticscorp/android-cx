package com.questionpro.cxlib.dataconnect;

import android.util.Log;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.init.CXGlobalInfo;
import java.text.SimpleDateFormat;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;
import java.util.Locale;


/**
 * Created by sachinsable on 14/04/16.
 */
public class CXPayload {
    private long touchPointID;
    public CXPayload(long touchPointID){
        this.touchPointID = touchPointID;
    }

    public  JSONObject getPayloadJSON() {
      try {
          JSONObject jsonObject = new JSONObject();

          jsonObject.put("firstName", "");
          jsonObject.put("lastName", "");
          jsonObject.put("transactionLanguage", "");
          jsonObject.put("mobile", "");
          jsonObject.put("segmentCode", "S1");
          String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

          jsonObject.put("transactionDate", date);

          jsonObject.put("email", CXGlobalInfo.UUID+"@questionpro.com");
          jsonObject.put("surveyID", touchPointID+"");
          Log.d("nehal json",jsonObject.toString());

          return jsonObject;
      }
      catch (JSONException e){
          e.printStackTrace();
      }
        return new JSONObject();
    }
}
