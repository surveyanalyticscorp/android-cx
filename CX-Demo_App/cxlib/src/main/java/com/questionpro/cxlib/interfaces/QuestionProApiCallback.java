package com.questionpro.cxlib.interfaces;

import org.json.JSONObject;

public interface QuestionProApiCallback {
    void onSuccess(String  surveyUrl);
    void onError(JSONObject error);
}
