package com.questionpro.cxlib.interfaces;

public interface IQuestionProInitCallback {
    void onSuccess(String  message);
    void onFailed(String  error);

    void getSurveyUrl(String surveyUrl);
}
