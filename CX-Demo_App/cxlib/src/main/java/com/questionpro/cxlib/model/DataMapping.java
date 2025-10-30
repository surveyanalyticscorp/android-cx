package com.questionpro.cxlib.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class DataMapping implements Serializable {
    public String variable;
    public String displayName;

    public static ArrayList<DataMapping> fromJSON(JSONArray dataMappingsJsonArray) throws JSONException {
        ArrayList<DataMapping> dataMappings = new ArrayList<>();

        for(int i = 0; i < dataMappingsJsonArray.length(); i++){
            JSONObject jsonObject = dataMappingsJsonArray.getJSONObject(i);
            DataMapping dataMapping = new DataMapping();
            if(jsonObject.has("variable") && !jsonObject.isNull("variable")){
                dataMapping.variable = jsonObject.getString("variable");
            }
            if(jsonObject.has("displayName") && !jsonObject.isNull("displayName")){
                dataMapping.displayName = jsonObject.getString("displayName");
            }
            dataMappings.add(dataMapping);
        }
        return dataMappings;
    }
}
