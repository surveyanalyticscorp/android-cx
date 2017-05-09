package com.questionpro.cxlib.dataconnect;

import com.questionpro.cxlib.constants.CXConstants;
import com.questionpro.cxlib.init.CXGlobalInfo;

import org.json.JSONException;
import org.json.JSONObject;

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
          jsonObject.put(CXConstants.JSONUploadFields.UDID, CXGlobalInfo.UUID);
          jsonObject.put(CXConstants.JSONUploadFields.TOUCH_POINT_ID, touchPointID);
          return jsonObject;
      }
      catch (JSONException e){
          e.printStackTrace();
      }
        return new JSONObject();
    }
}
