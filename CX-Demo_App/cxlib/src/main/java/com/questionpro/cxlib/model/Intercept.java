package com.questionpro.cxlib.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class Intercept implements Serializable {
    public int id;
    public String type;
    public String condition;
    public int surveyId;
    public int ruleGroupId;

    public InterceptSettings interceptSettings = new InterceptSettings();
    public ArrayList<InterceptRule> interceptRule = new ArrayList<>();


    public static Intercept fromJSON(JSONObject interceptJson) throws Exception{
        Intercept intercept=new Intercept();
        intercept.id = interceptJson.getInt("id");
        intercept.surveyId = interceptJson.getInt("surveyId");
        intercept.ruleGroupId = interceptJson.getInt("ruleGroupId");
        intercept.type = interceptJson.getString("type");
        intercept.condition = interceptJson.getString("condition");
        if(interceptJson.has("settings") && !interceptJson.isNull("settings")){
            intercept.interceptSettings = InterceptSettings.fromJSON(interceptJson.getJSONObject("settings"));
        }
        JSONArray interceptRule = interceptJson.getJSONArray("rules");
        for(int i = 0; i < interceptRule.length(); i++) {
            intercept.interceptRule.add(InterceptRule.fromJSON(interceptRule.getJSONObject(i)));
        }

        return intercept;
    }
}
