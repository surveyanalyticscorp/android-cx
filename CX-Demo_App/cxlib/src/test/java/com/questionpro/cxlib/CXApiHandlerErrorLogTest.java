package com.questionpro.cxlib;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.questionpro.cxlib.enums.DataCenter;
import com.questionpro.cxlib.model.TouchPoint;

import org.json.JSONObject;
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
public class CXApiHandlerErrorLogTest {

    private Context context;
    private CXApiHandler handler;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();

        Field spmInstance = SharedPreferenceManager.class.getDeclaredField("instance");
        spmInstance.setAccessible(true);
        spmInstance.set(null, null);

        // initialise CXGlobalInfo with a minimal TouchPoint so getPlatform() doesn't NPE
        TouchPoint tp = new TouchPoint.Builder(DataCenter.US, "test-key").build();
        CXGlobalInfo.getInstance().savePayLoad(tp);

        handler = new CXApiHandler(context);
    }

    // --- helpers ---

    private JSONObject buildPayload(String message, int httpStatus, String path,
                                    Exception exception, String errorType) throws Exception {
        Method m = CXApiHandler.class.getDeclaredMethod(
                "buildErrorLogPayload", String.class, int.class, String.class, Exception.class, String.class);
        m.setAccessible(true);
        String json = (String) m.invoke(handler, message, httpStatus, path, exception, errorType);
        return new JSONObject(json);
    }

    private String extractReason(JSONObject response) throws Exception {
        Method m = CXApiHandler.class.getDeclaredMethod("extractReason", JSONObject.class);
        m.setAccessible(true);
        return (String) m.invoke(handler, response);
    }

    // --- buildErrorLogPayload ---

    @Test
    public void buildErrorLogPayload_allFields_presentInOutput() throws Exception {
        JSONObject payload = buildPayload("Survey failed", 404, "/api/v1/test", null, "HttpError");

        assertEquals("Survey failed", payload.getString("message"));
        assertEquals(404, payload.getInt("httpStatus"));
        assertEquals("/api/v1/test", payload.getString("path"));
        assertEquals("N/A", payload.getString("stacktrace"));

        JSONObject ctx = payload.getJSONObject("context");
        assertFalse(ctx.getString("platform").isEmpty());
        assertEquals("HttpError", ctx.getString("errorType"));
        assertFalse(ctx.getString("sdkVersion").isEmpty());
    }

    @Test
    public void buildErrorLogPayload_withException_stacktraceIncluded() throws Exception {
        Exception e = new RuntimeException("Something went wrong");
        JSONObject payload = buildPayload("Runtime failure", 500, "/api/v1/survey", e, "Exception");

        String stacktrace = payload.getString("stacktrace");
        assertTrue(stacktrace.contains("RuntimeException"));
        assertTrue(stacktrace.contains("Something went wrong"));
        assertTrue(stacktrace.contains("at "));
    }

    @Test
    public void buildErrorLogPayload_nullMessage_defaultsToUnknown() throws Exception {
        JSONObject payload = buildPayload(null, 500, "/api/v1/survey", null, "Exception");
        assertEquals("Unknown error", payload.getString("message"));
    }

    @Test
    public void buildErrorLogPayload_nullPath_defaultsToUnknown() throws Exception {
        JSONObject payload = buildPayload("Error", 500, null, null, "Exception");
        assertEquals("Unknown", payload.getString("path"));
    }

    @Test
    public void buildErrorLogPayload_nullErrorType_defaultsToUnknown() throws Exception {
        JSONObject payload = buildPayload("Error", 500, "/api/v1/test", null, null);
        assertEquals("Unknown", payload.getJSONObject("context").getString("errorType"));
    }

    @Test
    public void buildErrorLogPayload_zeroHttpStatus_allowed() throws Exception {
        JSONObject payload = buildPayload("No network", 0, "/api/v1/test", null, "NetworkError");
        assertEquals(0, payload.getInt("httpStatus"));
    }

    @Test
    public void buildErrorLogPayload_platformReflectsTouchPoint() throws Exception {
        JSONObject payload = buildPayload("err", 500, "/test", null, "Exception");
        // platform must be a non-empty string derived from the TouchPoint (not hardcoded)
        assertFalse(payload.getJSONObject("context").getString("platform").isEmpty());
    }

    // --- stackTraceToString ---

    @Test
    public void stackTraceToString_singleException_containsClassNameAndFrames() throws Exception {
        Method m = CXApiHandler.class.getDeclaredMethod("stackTraceToString", Exception.class);
        m.setAccessible(true);

        IllegalStateException e = new IllegalStateException("bad state");
        String result = (String) m.invoke(handler, e);

        assertTrue(result.contains("IllegalStateException"));
        assertTrue(result.contains("bad state"));
        assertTrue(result.contains("at "));
    }

    // --- extractReason ---

    @Test
    public void extractReason_errorIsString_returnsString() throws Exception {
        JSONObject response = new JSONObject("{\"success\":false,\"error\":\"Project not found. Invalid app key\",\"errorCode\":\"NOT_FOUND\"}");
        assertEquals("Project not found. Invalid app key", extractReason(response));
    }

    @Test
    public void extractReason_errorIsObject_returnsNestedMessage() throws Exception {
        JSONObject response = new JSONObject("{\"error\":{\"message\":\"Unauthorized\"}}");
        assertEquals("Unauthorized", extractReason(response));
    }

    @Test
    public void extractReason_noErrorKey_fallsBackToMessage() throws Exception {
        JSONObject response = new JSONObject("{\"message\":\"Something went wrong\"}");
        assertEquals("Something went wrong", extractReason(response));
    }

    @Test
    public void extractReason_nullResponse_returnsUnknown() throws Exception {
        assertEquals("Unknown error", extractReason(null));
    }

    @Test
    public void extractReason_emptyJson_returnsUnknown() throws Exception {
        assertEquals("Unknown error", extractReason(new JSONObject()));
    }

    @Test
    public void extractReason_errorObjectMissingMessage_returnsUnknown() throws Exception {
        JSONObject response = new JSONObject("{\"error\":{\"code\":404}}");
        // error is a JSONObject but has no "message" key → falls through to unknown
        assertEquals("Unknown error", extractReason(response));
    }

    // --- logError queues payload when server is unreachable ---

    @Test
    public void logError_serverUnreachable_payloadQueuedInSharedPreferences() throws Exception {
        SharedPreferenceManager prefs = SharedPreferenceManager.getInstance(context);

        // drain any pre-existing queue
        prefs.drainErrorLogQueue();

        // logError fires a background POST to a real URL that will fail in a unit test environment
        handler.logError("Test error", 500, "/api/v1/test", null, "Exception");

        // wait long enough for both attempts + 2s retry delay to complete
        Thread.sleep(5000);

        // the failed POST should have been queued
        assertEquals(1, prefs.drainErrorLogQueue().length());
    }
}
