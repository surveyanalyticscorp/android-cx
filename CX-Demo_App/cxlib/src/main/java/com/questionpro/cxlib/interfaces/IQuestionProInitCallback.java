package com.questionpro.cxlib.interfaces;

public interface IQuestionProInitCallback {
    void onInitializationSuccess(String  message);
    void onInitializationFailure(String  error);
}
