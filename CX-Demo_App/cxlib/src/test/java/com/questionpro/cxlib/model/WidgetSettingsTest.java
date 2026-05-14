package com.questionpro.cxlib.model;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class WidgetSettingsTest {

    // --- fromJSON: field parsing ---

    @Test
    public void fromJSON_parsesAllFields() throws Exception {
        JSONObject json = new JSONObject()
                .put("textColor", "#ffffff")
                .put("backgroundColor", "#1B87E6")
                .put("widgetTitle", "Rate us")
                .put("position", "BOTTOM_RIGHT")
                .put("widgetWindowHeight", 40)
                .put("widgetWindowWidth", 45);

        WidgetSettings ws = WidgetSettings.fromJSON(json);

        assertEquals("#ffffff", ws.textColor);
        assertEquals("#1B87E6", ws.backgroundColor);
        assertEquals("Rate us", ws.widgetTitle);
        assertEquals("BOTTOM_RIGHT", ws.position);
        assertEquals(40, ws.widgetWindowHeight);
        assertEquals(45, ws.widgetWindowWidth);
    }

    // --- position splitting ---

    @Test
    public void fromJSON_position_BOTTOM_RIGHT() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject().put("position", "BOTTOM_RIGHT"));
        assertEquals("BOTTOM", ws.verticalPosition);
        assertEquals("RIGHT",  ws.horizontalPosition);
    }

    @Test
    public void fromJSON_position_BOTTOM_LEFT() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject().put("position", "BOTTOM_LEFT"));
        assertEquals("BOTTOM", ws.verticalPosition);
        assertEquals("LEFT",   ws.horizontalPosition);
    }

    @Test
    public void fromJSON_position_TOP_RIGHT() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject().put("position", "TOP_RIGHT"));
        assertEquals("TOP",   ws.verticalPosition);
        assertEquals("RIGHT", ws.horizontalPosition);
    }

    @Test
    public void fromJSON_position_TOP_LEFT() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject().put("position", "TOP_LEFT"));
        assertEquals("TOP",  ws.verticalPosition);
        assertEquals("LEFT", ws.horizontalPosition);
    }

    @Test
    public void fromJSON_position_CENTER_CENTER() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject().put("position", "CENTER_CENTER"));
        assertEquals("CENTER", ws.verticalPosition);
        assertEquals("CENTER", ws.horizontalPosition);
    }

    @Test
    public void fromJSON_position_BOTTOM_CENTER() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject().put("position", "BOTTOM_CENTER"));
        assertEquals("BOTTOM", ws.verticalPosition);
        assertEquals("CENTER", ws.horizontalPosition);
    }

    @Test
    public void fromJSON_position_TOP_CENTER() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject().put("position", "TOP_CENTER"));
        assertEquals("TOP",    ws.verticalPosition);
        assertEquals("CENTER", ws.horizontalPosition);
    }

    @Test
    public void fromJSON_position_CENTER_LEFT() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject().put("position", "CENTER_LEFT"));
        assertEquals("CENTER", ws.verticalPosition);
        assertEquals("LEFT",   ws.horizontalPosition);
    }

    @Test
    public void fromJSON_position_CENTER_RIGHT() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject().put("position", "CENTER_RIGHT"));
        assertEquals("CENTER", ws.verticalPosition);
        assertEquals("RIGHT",  ws.horizontalPosition);
    }

    // --- defaults when fields are missing ---

    @Test
    public void fromJSON_missingFields_usesDefaults() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject());

        assertEquals("",             ws.textColor);
        assertEquals("",             ws.backgroundColor);
        assertEquals("",             ws.widgetTitle);
        assertEquals("BOTTOM_RIGHT", ws.position);
        assertEquals(80,             ws.widgetWindowHeight);
        assertEquals(90,             ws.widgetWindowWidth);
        assertEquals("BOTTOM",       ws.verticalPosition);
        assertEquals("RIGHT",        ws.horizontalPosition);
    }

    // --- edge cases ---

    @Test
    public void fromJSON_positionWithoutUnderscore_fallsBackToDefault() throws Exception {
        WidgetSettings ws = WidgetSettings.fromJSON(new JSONObject().put("position", "UNKNOWN"));
        // No underscore — falls back to default split result
        assertEquals("BOTTOM", ws.verticalPosition);
        assertEquals("RIGHT",  ws.horizontalPosition);
    }

    @Test
    public void fromJSON_widgetWindowPercentages_boundaryValues() throws Exception {
        WidgetSettings ws100 = WidgetSettings.fromJSON(
                new JSONObject().put("widgetWindowHeight", 100).put("widgetWindowWidth", 100));
        assertEquals(100, ws100.widgetWindowHeight);
        assertEquals(100, ws100.widgetWindowWidth);

        WidgetSettings ws0 = WidgetSettings.fromJSON(
                new JSONObject().put("widgetWindowHeight", 0).put("widgetWindowWidth", 0));
        assertEquals(0, ws0.widgetWindowHeight);
        assertEquals(0, ws0.widgetWindowWidth);
    }
}
