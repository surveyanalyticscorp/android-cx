package com.questionpro.cxlib.model;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class InterceptParsingTest {

    // --- Intercept.fromJSON ---

    @Test
    public void intercept_parsesRequiredFields() throws Exception {
        JSONObject json = buildMinimalIntercept(42, "PROMPT", "AND");
        Intercept intercept = Intercept.fromJSON(json);

        assertEquals(42,       intercept.id);
        assertEquals("PROMPT", intercept.type);
        assertEquals("AND",    intercept.condition);
    }

    @Test
    public void intercept_missingId_defaultsToZero() throws Exception {
        JSONObject json = buildMinimalIntercept(0, "PROMPT", "OR");
        json.remove("id");
        Intercept intercept = Intercept.fromJSON(json);
        assertEquals(0, intercept.id);
    }

    @Test
    public void intercept_missingType_defaultsToEmpty() throws Exception {
        JSONObject json = buildMinimalIntercept(1, "PROMPT", "OR");
        json.remove("type");
        Intercept intercept = Intercept.fromJSON(json);
        assertEquals("", intercept.type);
    }

    @Test
    public void intercept_missingCondition_defaultsToOR() throws Exception {
        JSONObject json = buildMinimalIntercept(1, "PROMPT", "OR");
        json.remove("condition");
        Intercept intercept = Intercept.fromJSON(json);
        assertEquals("OR", intercept.condition);
    }

    @Test
    public void intercept_parsesWidgetSettings_whenPresent() throws Exception {
        JSONObject json = buildMinimalIntercept(1, "PROMPT", "OR");
        json.put("widgetSettings", new JSONObject()
                .put("textColor", "#ffffff")
                .put("backgroundColor", "#000000")
                .put("position", "TOP_LEFT")
                .put("widgetWindowHeight", 50)
                .put("widgetWindowWidth", 60));

        Intercept intercept = Intercept.fromJSON(json);

        assertNotNull(intercept.widgetSettings);
        assertEquals("#ffffff", intercept.widgetSettings.textColor);
        assertEquals("TOP",     intercept.widgetSettings.verticalPosition);
        assertEquals("LEFT",    intercept.widgetSettings.horizontalPosition);
    }

    @Test
    public void intercept_widgetSettingsNull_whenAbsent() throws Exception {
        Intercept intercept = Intercept.fromJSON(buildMinimalIntercept(1, "PROMPT", "OR"));
        assertNull(intercept.widgetSettings);
    }

    @Test
    public void intercept_parsesInterceptRules() throws Exception {
        JSONObject json = buildMinimalIntercept(1, "PROMPT", "OR");
        json.getJSONArray("rules").put(new JSONObject()
                .put("name", "TIME_SPENT")
                .put("value", "30")
                .put("key", "")
                .put("operand", "")
                .put("rangeValues", "")
                .put("variable", "")
                .put("type", ""));

        Intercept intercept = Intercept.fromJSON(json);

        assertEquals(1,            intercept.interceptRule.size());
        assertEquals("TIME_SPENT", intercept.interceptRule.get(0).name);
        assertEquals("30",         intercept.interceptRule.get(0).value);
    }

    // --- InterceptRule.fromJSON ---

    @Test
    public void interceptRule_missingFields_defaultToEmpty() throws Exception {
        InterceptRule rule = InterceptRule.fromJSON(new JSONObject());
        assertEquals("", rule.name);
        assertEquals("", rule.key);
        assertEquals("", rule.value);
        assertEquals("", rule.operand);
        assertEquals("", rule.rangeValue);
        assertEquals("", rule.variable);
        assertEquals("", rule.type);
    }

    // --- InterceptSettings.fromJSON ---

    @Test
    public void interceptSettings_defaults() throws Exception {
        InterceptSettings settings = InterceptSettings.fromJSON(new JSONObject());
        assertFalse(settings.allowMultipleResponse);
        assertFalse(settings.autoLanguageSelection);
        assertEquals(0,   settings.triggerDelayInSeconds);
        assertEquals(100, settings.samplingRate);
        assertFalse(settings.autoCloseOnCompletion);
    }

    @Test
    public void interceptSettings_parsesAllFields() throws Exception {
        JSONObject json = new JSONObject()
                .put("allowMultipleResponse",  true)
                .put("autoLanguageSelection",  true)
                .put("triggerDelayInSeconds",  10)
                .put("samplingRate",           75)
                .put("autoCloseOnCompletion",  true);

        InterceptSettings settings = InterceptSettings.fromJSON(json);

        assertTrue(settings.allowMultipleResponse);
        assertTrue(settings.autoLanguageSelection);
        assertEquals(10, settings.triggerDelayInSeconds);
        assertEquals(75, settings.samplingRate);
        assertTrue(settings.autoCloseOnCompletion);
    }

    // --- InterceptMetadata.fromJSON ---

    @Test
    public void interceptMetadata_defaults_whenEmpty() {
        InterceptMetadata meta = InterceptMetadata.fromJSON(new JSONObject());
        assertEquals(0,    meta.matchedCount);
        assertEquals(0,    meta.excludedCount);
        assertNull(meta.visitorStatus);
    }

    @Test
    public void interceptMetadata_parsesAllFields() throws Exception {
        JSONObject json = new JSONObject()
                .put("matchedCount",  5)
                .put("excludedCount", 2)
                .put("visitorStatus", "MATCHED");

        InterceptMetadata meta = InterceptMetadata.fromJSON(json);

        assertEquals(5,         meta.matchedCount);
        assertEquals(2,         meta.excludedCount);
        assertEquals("MATCHED", meta.visitorStatus);
    }

    // --- DataMapping.fromJSON ---

    @Test
    public void dataMapping_parsesVariableAndDisplayName() throws Exception {
        JSONArray array = new JSONArray().put(
                new JSONObject().put("variable", "q1_name").put("displayName", "userName"));

        java.util.ArrayList<DataMapping> mappings = DataMapping.fromJSON(array);

        assertEquals(1,         mappings.size());
        assertEquals("q1_name", mappings.get(0).variable);
        assertEquals("userName",mappings.get(0).displayName);
    }

    @Test
    public void dataMapping_emptyArray_returnsEmptyList() throws Exception {
        assertTrue(DataMapping.fromJSON(new JSONArray()).isEmpty());
    }

    // --- helpers ---

    private JSONObject buildMinimalIntercept(int id, String type, String condition) throws Exception {
        return new JSONObject()
                .put("id",        id)
                .put("surveyId",  100)
                .put("ruleGroupId", 1)
                .put("type",      type)
                .put("condition", condition)
                .put("rules",     new JSONArray());
    }
}
