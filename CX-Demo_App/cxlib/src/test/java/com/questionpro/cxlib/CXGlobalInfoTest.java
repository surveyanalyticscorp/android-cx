package com.questionpro.cxlib;

import com.questionpro.cxlib.enums.Platform;
import com.questionpro.cxlib.model.TouchPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class CXGlobalInfoTest {

    private CXGlobalInfo info;

    @Before
    public void setUp() throws Exception {
        // Reset singleton and static fields
        Field instance = CXGlobalInfo.class.getDeclaredField("ourInstance");
        instance.setAccessible(true);
        instance.set(null, null);

        Field uuid = CXGlobalInfo.class.getDeclaredField("UUID");
        uuid.setAccessible(true);
        uuid.set(null, null);

        Field apiKey = CXGlobalInfo.class.getDeclaredField("apiKey");
        apiKey.setAccessible(true);
        apiKey.set(null, null);

        Field payload = CXGlobalInfo.class.getDeclaredField("payload");
        payload.setAccessible(true);
        payload.set(null, null);

        info = CXGlobalInfo.getInstance();
    }

    // -------------------------------------------------------------------------
    // setApiKey / getApiKey
    // -------------------------------------------------------------------------

    @Test
    public void setApiKey_validKey_stored() {
        info.setApiKey("test-key-123");
        assertEquals("test-key-123", info.getApiKey());
    }

    @Test
    public void setApiKey_trimsWhitespace() {
        info.setApiKey("  my-key  ");
        assertEquals("my-key", info.getApiKey());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setApiKey_null_throwsIllegalArgument() {
        info.setApiKey(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setApiKey_empty_throwsIllegalArgument() {
        info.setApiKey("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void setApiKey_whitespaceOnly_throwsIllegalArgument() {
        info.setApiKey("   ");
    }

    // -------------------------------------------------------------------------
    // setUUID / getUUID
    // -------------------------------------------------------------------------

    @Test
    public void setUUID_stored_andRetrieved() {
        info.setUUID("some-uuid");
        assertEquals("some-uuid", info.getUUID());
    }

    // -------------------------------------------------------------------------
    // getDataCenter — reads from stored payload JSON
    // -------------------------------------------------------------------------

    @Test
    public void getDataCenter_validPayload_returnsDataCenter() throws Exception {
        setPayload("{\"dataCenter\":\"US\",\"platform\":\"ANDROID\"}");
        assertEquals("US", CXGlobalInfo.getDataCenter());
    }

    @Test
    public void getDataCenter_missingKey_returnsEmpty() throws Exception {
        setPayload("{\"platform\":\"ANDROID\"}");
        assertEquals("", CXGlobalInfo.getDataCenter());
    }

    @Test
    public void getDataCenter_nullPayload_returnsEmpty() {
        // payload is null (reset in @Before)
        assertEquals("", CXGlobalInfo.getDataCenter());
    }

    // -------------------------------------------------------------------------
    // getApiPayload — strips keys, adds isManualSurvey
    // -------------------------------------------------------------------------

    @Test
    public void getApiPayload_stripsUnwantedKeys() throws Exception {
        setPayload("{\"showAsDialog\":true,\"themeColor\":\"#fff\",\"type\":\"PROMPT\",\"dataCenter\":\"US\",\"foo\":\"bar\"}");
        String result = CXGlobalInfo.getApiPayload(42L);
        assertFalse(result.contains("showAsDialog"));
        assertFalse(result.contains("themeColor"));
        assertFalse(result.contains("type"));
        assertFalse(result.contains("dataCenter"));
        assertTrue(result.contains("isManualSurvey"));
        assertTrue(result.contains("bar")); // foo key kept
    }

    @Test
    public void getApiPayload_nullPayload_returnsEmpty() {
        assertEquals("", CXGlobalInfo.getApiPayload(1L));
    }

    // -------------------------------------------------------------------------
    // updateCXPayloadWithSurveyId
    // -------------------------------------------------------------------------

    @Test
    public void updateCXPayloadWithSurveyId_addsField() throws Exception {
        setPayload("{\"platform\":\"ANDROID\"}");
        CXGlobalInfo.updateCXPayloadWithSurveyId(99L);
        String result = CXGlobalInfo.getApiPayload(99L);
        // After update the stored payload should contain surveyId
        assertTrue(result.contains("99") || result.contains("surveyId"));
    }

    // -------------------------------------------------------------------------
    // singleton
    // -------------------------------------------------------------------------

    @Test
    public void getInstance_returnsSameInstance() {
        assertSame(CXGlobalInfo.getInstance(), CXGlobalInfo.getInstance());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private void setPayload(String json) throws Exception {
        Field payload = CXGlobalInfo.class.getDeclaredField("payload");
        payload.setAccessible(true);
        payload.set(null, json);
    }
}
