package com.questionpro.cxlib.interfaces;

public interface IQuestionProCallback {
    void onInitializationSuccess(String  message);
    void onInitializationFailure(String  error);

    void getSurveyUrl(String surveyUrl);
}
