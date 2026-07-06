package com.questionpro.cxlib;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

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

        handler = new CXApiHandler(context);
    }

    private JSONObject buildPayload(String message, int httpStatus, String path,
                                    Exception exception, String errorType) throws Exception {
        Method m = CXApiHandler.class.getDeclaredMethod(
                "buildErrorLogPayload", String.class, int.class, String.class, Exception.class, String.class);
        m.setAccessible(true);
        String json = (String) m.invoke(handler, message, httpStatus, path, exception, errorType);
        return new JSONObject(json);
    }

    @Test
    public void buildErrorLogPayload_allFields_presentInOutput() throws Exception {
        JSONObject payload = buildPayload("Survey failed", 404, "/api/v1/test", null, "HttpError");

        assertEquals("Survey failed", payload.getString("message"));
        assertEquals(404, payload.getInt("httpStatus"));
        assertEquals("/api/v1/test", payload.getString("path"));
        assertEquals("N/A", payload.getString("stacktrace"));

        JSONObject ctx = payload.getJSONObject("context");
        assertEquals("android", ctx.getString("platform"));
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

        JSONObject ctx = payload.getJSONObject("context");
        assertEquals("Unknown", ctx.getString("errorType"));
    }

    @Test
    public void buildErrorLogPayload_zeroHttpStatus_allowed() throws Exception {
        JSONObject payload = buildPayload("No network", 0, "/api/v1/test", null, "NetworkError");

        assertEquals(0, payload.getInt("httpStatus"));
    }

    @Test
    public void stackTraceToString_singleException_containsClassName() throws Exception {
        Method m = CXApiHandler.class.getDeclaredMethod("stackTraceToString", Exception.class);
        m.setAccessible(true);

        IllegalStateException e = new IllegalStateException("bad state");
        String result = (String) m.invoke(handler, e);

        assertTrue(result.contains("IllegalStateException"));
        assertTrue(result.contains("bad state"));
        assertTrue(result.contains("at "));
    }
}
