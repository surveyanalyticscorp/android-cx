package com.questionpro.cxlib.interfaces;

import com.questionpro.cxlib.model.CXInteraction;

import org.json.JSONObject;

public interface QuestionProApiCall {
    void onSuccess(String  surveyUrl);
    void onError(JSONObject error);
}
