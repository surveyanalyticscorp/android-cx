package com.questionpro.cxlib.model;

import org.json.JSONObject;

import java.io.Serializable;

public class InterceptRule implements Serializable {
    private static final long serialVersionUID = 1L;

    public String key;
    public String name;
    public String operand;
    public String value;
    public String rangeValue;
    public String variable;
    public String type;

    public static InterceptRule fromJSON(JSONObject interceptJson) throws Exception{
        InterceptRule interceptRule = new InterceptRule();
        interceptRule.key = interceptJson.optString("key", "");
        interceptRule.name = interceptJson.optString("name", "");
        interceptRule.operand = interceptJson.optString("operand", "");
        interceptRule.value = interceptJson.optString("value", "");
        interceptRule.rangeValue = interceptJson.optString("rangeValues", "");
        interceptRule.variable = interceptJson.optString("variable", "");
        interceptRule.type = interceptJson.optString("type", "");

        return interceptRule;
    }
}
