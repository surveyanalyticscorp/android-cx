package com.questionpro.cxlib;

import com.questionpro.cxlib.model.Intercept;

import org.json.JSONObject;

interface IQuestionProApiCallback {
    void OnApiCallbackFailed(JSONObject error);

    void onApiCallbackSuccess(Intercept intercept, String surveyUrl);
}


interface IQuestionProRulesCallback {
    void onTimeSpendSatisfied(int  interceptId);
    void onViewCountRuleSatisfied(int interceptId);
}