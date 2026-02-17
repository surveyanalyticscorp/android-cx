package com.questionpro.cxlib.model;

import org.json.JSONObject;

import java.io.Serializable;

public class InterceptMetadata implements Serializable {
        public int matchedCount;
        public int excludedCount;
        public String visitorStatus;


    public static InterceptMetadata fromJSON(JSONObject metadataJson){
        InterceptMetadata interceptMetadata = new InterceptMetadata();
        try {
            if (metadataJson.has("matchedCount")) {
                interceptMetadata.matchedCount = metadataJson.getInt("matchedCount");
            }
            if (metadataJson.has("excludedCount")) {
                interceptMetadata.excludedCount = metadataJson.getInt("excludedCount");
            }
            if (metadataJson.has("visitorStatus")) {
                interceptMetadata.visitorStatus = metadataJson.getString("visitorStatus");
            }
        }catch (Exception e){}
        return interceptMetadata;
    }

}
