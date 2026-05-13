package com.questionpro.cxlib.model;

import org.json.JSONObject;

import java.io.Serializable;

public class WidgetSettings implements Serializable {
    private static final long serialVersionUID = 1L;

    public String textColor;
    public String iconColor;
    public String backgroundColor;
    public String widgetTitle;
    public String position;           // e.g. "BOTTOM_RIGHT"
    public int widgetWindowHeight;    // % of screen height (PROMPT only)
    public int widgetWindowWidth;     // % of screen width  (PROMPT only)

    // Derived from position — set during fromJSON()
    // verticalPosition   : "TOP"  | "CENTER" | "BOTTOM"
    // horizontalPosition : "LEFT" | "CENTER" | "RIGHT"
    public String verticalPosition;
    public String horizontalPosition;

    public static WidgetSettings fromJSON(JSONObject json) {
        WidgetSettings settings = new WidgetSettings();
        settings.textColor = json.optString("textColor", "");
        settings.iconColor = json.optString("iconColor", "");
        settings.backgroundColor = json.optString("backgroundColor", "");
        settings.widgetTitle = json.optString("widgetTitle", "");
        settings.position = json.optString("position", "BOTTOM_RIGHT");
        settings.widgetWindowHeight = json.optInt("widgetWindowHeight", 80);
        settings.widgetWindowWidth = json.optInt("widgetWindowWidth", 90);

        if (settings.position != null && settings.position.contains("_")) {
            String[] parts = settings.position.split("_", 2);
            settings.verticalPosition   = parts[0]; // TOP | CENTER | BOTTOM
            settings.horizontalPosition = parts[1]; // LEFT | CENTER | RIGHT
        } else {
            settings.verticalPosition   = "BOTTOM";
            settings.horizontalPosition = "RIGHT";
        }

        return settings;
    }
}
