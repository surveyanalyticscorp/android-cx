package com.questionpro.cxlib.interfaces;

public interface IQuestionProInitCallback {
    void onInitializationSuccess(String message);
    void onInitializationFailure(String error);

    /**
     * Called when a runtime error occurs inside the SDK (e.g. survey failed to load).
     * Override to forward to your crash reporting tool (Crashlytics, Sentry, etc.).
     * Default is a no-op so existing implementations are not broken.
     */
    default void onError(int interceptId, String errorMessage) {}
}
