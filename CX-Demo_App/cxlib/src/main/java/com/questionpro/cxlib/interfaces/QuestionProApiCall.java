package com.questionpro.cxlib.interfaces;

import org.json.JSONObject;

public interface QuestionProApiCall {
    void onSuccess(String  surveyUrl);
    void onError(JSONObject error);
}
