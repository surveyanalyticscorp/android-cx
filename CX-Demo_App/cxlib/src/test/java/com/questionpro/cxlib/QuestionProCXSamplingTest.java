package com.questionpro.cxlib;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.questionpro.cxlib.enums.VisitorStatus;
import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.model.InterceptMetadata;
import com.questionpro.cxlib.model.InterceptSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class QuestionProCXSamplingTest {

    private static final int INTERCEPT_ID = 101;

    private QuestionProCX cx;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();

        // Reset the SharedPreferenceManager singleton so it re-binds to the current
        // Robolectric context. Without this, a stale instance from a previous test class
        // (holding a reference to a different Robolectric Application) would read from
        // and write to a different SharedPreferences store than the one we clear here.
        Field spmInstance = SharedPreferenceManager.class.getDeclaredField("instance");
        spmInstance.setAccessible(true);
        spmInstance.set(null, null);

        cx = QuestionProCX.getInstance();

        // Inject appContext into the QuestionProCX singleton without triggering init().
        Field appContextField = QuestionProCX.class.getDeclaredField("appContext");
        appContextField.setAccessible(true);
        appContextField.set(null, context);

        // Clear both SharedPreference stores used by SharedPreferenceManager.
        context.getSharedPreferences(SharedPreferenceManager.PREF_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
        context.getSharedPreferences("Intercepts", Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Build an intercept for sampling tests. */
    private Intercept buildSamplingIntercept(int samplingRate,
                                             String visitorStatus,
                                             int matchedCount,
                                             int excludedCount) {
        Intercept intercept = new Intercept();
        intercept.id = INTERCEPT_ID;

        InterceptSettings settings = new InterceptSettings();
        settings.samplingRate = samplingRate;
        intercept.interceptSettings = settings;

        InterceptMetadata meta = new InterceptMetadata();
        meta.visitorStatus = visitorStatus;
        meta.matchedCount  = matchedCount;
        meta.excludedCount = excludedCount;
        intercept.interceptMetadata = meta;

        return intercept;
    }

    /** Build an intercept for launch-eligibility tests. */
    private Intercept buildLaunchIntercept(int id, boolean allowMultipleResponse) {
        Intercept intercept = new Intercept();
        intercept.id = id;

        InterceptSettings settings = new InterceptSettings();
        settings.allowMultipleResponse = allowMultipleResponse;
        intercept.interceptSettings = settings;

        intercept.interceptMetadata = new InterceptMetadata();
        return intercept;
    }

    /** Reflectively invoke the private shouldSurveyLaunch(Intercept) method. */
    private boolean invokeShouldSurveyLaunch(Intercept intercept) throws Exception {
        Method m = QuestionProCX.class.getDeclaredMethod("shouldSurveyLaunch", Intercept.class);
        m.setAccessible(true);
        return (boolean) m.invoke(cx, intercept);
    }

    // -------------------------------------------------------------------------
    // checkShouldShowSampling — visitorStatus branch
    // -------------------------------------------------------------------------

    @Test
    public void sampling_visitorStatus_excluded_returnsFalse() {
        Intercept intercept = buildSamplingIntercept(100, VisitorStatus.EXCLUDED.name(), 0, 0);
        assertFalse(cx.checkShouldShowSampling(intercept));
    }

    @Test
    public void sampling_visitorStatus_matched_returnsTrue() {
        // Any non-EXCLUDED status → true, regardless of samplingRate
        Intercept intercept = buildSamplingIntercept(0, VisitorStatus.MATCHED.name(), 0, 0);
        assertTrue(cx.checkShouldShowSampling(intercept));
    }

    @Test
    public void sampling_visitorStatus_launched_returnsTrue() {
        Intercept intercept = buildSamplingIntercept(0, VisitorStatus.LAUNCHED.name(), 0, 0);
        assertTrue(cx.checkShouldShowSampling(intercept));
    }

    // -------------------------------------------------------------------------
    // checkShouldShowSampling — rate-based (visitorStatus absent / empty)
    // -------------------------------------------------------------------------

    @Test
    public void sampling_rate100_nullVisitorStatus_alwaysIncluded() {
        Intercept intercept = buildSamplingIntercept(100, null, 0, 0);
        assertTrue(cx.checkShouldShowSampling(intercept));
    }

    @Test
    public void sampling_rate100_emptyVisitorStatus_alwaysIncluded() {
        Intercept intercept = buildSamplingIntercept(100, "", 0, 0);
        assertTrue(cx.checkShouldShowSampling(intercept));
    }

    @Test
    public void sampling_rate100_withHighMatchCount_alwaysIncluded() {
        // samplingRate >= 100 short-circuits — counts don't matter
        Intercept intercept = buildSamplingIntercept(100, null, 999, 1);
        assertTrue(cx.checkShouldShowSampling(intercept));
    }

    @Test
    public void sampling_rate0_noHistory_excluded() {
        // Math: (0 * 100) / (0 + 0 + 1) = 0; 0 < 0 → false
        Intercept intercept = buildSamplingIntercept(0, null, 0, 0);
        assertFalse(cx.checkShouldShowSampling(intercept));
    }

    @Test
    public void sampling_rate50_noHistory_included() {
        // Math: (0 * 100) / 1 = 0; 0 < 50 → true
        Intercept intercept = buildSamplingIntercept(50, null, 0, 0);
        assertTrue(cx.checkShouldShowSampling(intercept));
    }

    @Test
    public void sampling_rate50_balancedHistory_included() {
        // Math: (50 * 100) / (50 + 50 + 1) = 5000 / 101 = 49; 49 < 50 → true
        Intercept intercept = buildSamplingIntercept(50, null, 50, 50);
        assertTrue(cx.checkShouldShowSampling(intercept));
    }

    @Test
    public void sampling_rate50_highMatchRatio_excluded() {
        // Math: (100 * 100) / (100 + 0 + 1) = 10000 / 101 = 99; 99 < 50 → false
        Intercept intercept = buildSamplingIntercept(50, null, 100, 0);
        assertFalse(cx.checkShouldShowSampling(intercept));
    }

    @Test
    public void sampling_rateBoundary99_noHistory_included() {
        // Math: 0 / 1 = 0; 0 < 99 → true
        Intercept intercept = buildSamplingIntercept(99, null, 0, 0);
        assertTrue(cx.checkShouldShowSampling(intercept));
    }

    @Test
    public void sampling_rate1_allMatched_excluded() {
        // Math: (99 * 100) / (99 + 0 + 1) = 9900 / 100 = 99; 99 < 1 → false
        Intercept intercept = buildSamplingIntercept(1, null, 99, 0);
        assertFalse(cx.checkShouldShowSampling(intercept));
    }

    // -------------------------------------------------------------------------
    // shouldSurveyLaunch (private — accessed via reflection)
    // -------------------------------------------------------------------------

    @Test
    public void shouldLaunch_multipleAllowed_noPriorLaunch_returnsTrue() throws Exception {
        Intercept intercept = buildLaunchIntercept(201, true);
        assertTrue(invokeShouldSurveyLaunch(intercept));
    }

    @Test
    public void shouldLaunch_multipleAllowed_withPriorLaunch_returnsTrue() throws Exception {
        // allowMultipleResponse=true → always launch, even if already launched once
        Intercept intercept = buildLaunchIntercept(202, true);
        SharedPreferenceManager.getInstance(context)
                .saveInterceptIdForLaunchedSurvey(202, System.currentTimeMillis());
        assertTrue(invokeShouldSurveyLaunch(intercept));
    }

    @Test
    public void shouldLaunch_singleResponse_noPriorLaunch_returnsTrue() throws Exception {
        Intercept intercept = buildLaunchIntercept(203, false);
        assertTrue(invokeShouldSurveyLaunch(intercept));
    }

    @Test
    public void shouldLaunch_singleResponse_withPriorLaunch_returnsFalse() throws Exception {
        // allowMultipleResponse=false and already launched → do not show again
        Intercept intercept = buildLaunchIntercept(204, false);
        SharedPreferenceManager.getInstance(context)
                .saveInterceptIdForLaunchedSurvey(204, System.currentTimeMillis());
        assertFalse(invokeShouldSurveyLaunch(intercept));
    }

    @Test
    public void shouldLaunch_differentInterceptsAreIndependent() throws Exception {
        // Marking intercept 205 launched must not block intercept 206
        SharedPreferenceManager.getInstance(context)
                .saveInterceptIdForLaunchedSurvey(205, System.currentTimeMillis());
        Intercept other = buildLaunchIntercept(206, false);
        assertTrue(invokeShouldSurveyLaunch(other));
    }
}
