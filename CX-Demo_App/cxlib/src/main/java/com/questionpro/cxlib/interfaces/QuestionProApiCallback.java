package com.questionpro.cxlib.interfaces;

import com.questionpro.cxlib.model.Intercept;

import org.json.JSONObject;

public interface QuestionProApiCallback {
    void onError(JSONObject error);

    void onSurveyUrlReceived(Intercept intercept, String surveyUrl);
}
