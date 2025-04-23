package com.questionpro.cxlib.model;

import org.json.JSONObject;

import java.io.Serializable;

public class InterceptRule implements Serializable {

    public String key;
    public String name;
    public String operand;
    public String value;
    public String rangeValue;
    public String variable;
    public String type;

    public static InterceptRule fromJSON(JSONObject interceptJson) throws Exception{
        InterceptRule interceptRule = new InterceptRule();
        interceptRule.key = interceptJson.getString("key");
        interceptRule.name = interceptJson.getString("name");
        interceptRule.operand = interceptJson.getString("operand");
        interceptRule.value = interceptJson.getString("value");
        interceptRule.rangeValue = interceptJson.getString("rangeValues");
        interceptRule.variable = interceptJson.getString("variable");
        interceptRule.type = interceptJson.getString("type");

        return interceptRule;
    }
}
