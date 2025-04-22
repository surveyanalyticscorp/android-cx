package com.questionpro.cxlib.interfaces;

import com.questionpro.cxlib.model.Intercept;

import org.json.JSONObject;

public interface IQuestionProApiCallback {
    void OnApiCallbackFailed(JSONObject error);

    void onApiCallbackSuccess(Intercept intercept, String surveyUrl);
}
