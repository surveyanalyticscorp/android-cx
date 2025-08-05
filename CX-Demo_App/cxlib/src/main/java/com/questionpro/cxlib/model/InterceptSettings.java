package com.questionpro.cxlib.model;

import org.json.JSONObject;

import java.io.Serializable;

public class InterceptSettings implements Serializable {
    boolean allowMultipleResponse;
    public boolean autoLanguageSelection;
    int triggerDelayInSeconds;

    public static InterceptSettings fromJSON(JSONObject settingsJson) throws Exception{
        InterceptSettings settings = new InterceptSettings();
        if(settingsJson.has("allowMultipleResponse")) {
            settings.allowMultipleResponse = settingsJson.getBoolean("allowMultipleResponse");
        }

        if(settingsJson.has("autoLanguageSelection")) {
            settings.autoLanguageSelection = settingsJson.getBoolean("autoLanguageSelection");
        }

        if(settingsJson.has("triggerDelayInSeconds")){
            settings.triggerDelayInSeconds = settingsJson.getInt("triggerDelayInSeconds");
        }

        return settings;
    }
}
