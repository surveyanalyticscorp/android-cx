package com.questionpro.cxlib;

import com.questionpro.cxlib.model.Intercept;

import org.json.JSONObject;

/**
 * Callback used exclusively by InteractionActivity to receive survey URL results
 * from CXApiHandler. Kept separate from IQuestionProApiCallback so the Activity
 * is not coupled to the SDK-internal callback contract.
 */
interface IInteractionCallback {
    void onSurveyUrlReady(Intercept intercept, String surveyUrl);
    void onSurveyUrlFailed(JSONObject errorMessage);
}
