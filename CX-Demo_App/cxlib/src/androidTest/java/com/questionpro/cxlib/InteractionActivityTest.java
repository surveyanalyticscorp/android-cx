package com.questionpro.cxlib;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.ImageViewCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.questionpro.cxlib.model.Intercept;
import com.questionpro.cxlib.model.InterceptSettings;
import com.questionpro.cxlib.model.WidgetSettings;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumentation tests for InteractionActivity.
 *
 * Each test launches the activity with a crafted Intent, then asserts the synchronous
 * UI state set up by applyWidgetSettings() / applyPromptPositionAndSize() in onCreate().
 *
 * The async network call (getInterceptSurveyDetails) always fails in the test
 * environment; its error dialog is posted to the main thread well after our
 * scenario.onActivity() assertions have already run.
 */
@RunWith(AndroidJUnit4.class)
public class InteractionActivityTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Build a minimal Intercept with no WidgetSettings. */
    private static Intercept buildIntercept(int id, String type) {
        Intercept intercept = new Intercept();
        intercept.id = id;
        intercept.type = type;
        intercept.surveyId = 0;
        intercept.interceptSettings = new InterceptSettings();
        return intercept;
    }

    /** Build an Intercept with the given WidgetSettings. */
    private static Intercept buildInterceptWithWidget(int id, String type, WidgetSettings ws) {
        Intercept intercept = buildIntercept(id, type);
        intercept.widgetSettings = ws;
        return intercept;
    }

    /**
     * Construct a WidgetSettings object directly (no JSON parsing).
     *
     * @param position "VERTICAL_HORIZONTAL" format, e.g. "BOTTOM_RIGHT", "CENTER_CENTER".
     *                 verticalPosition and horizontalPosition are derived from this value.
     */
    private static WidgetSettings makeWidgetSettings(String textColor, String iconColor,
                                                      String backgroundColor,
                                                      String position, String widgetTitle) {
        WidgetSettings ws = new WidgetSettings();
        ws.textColor         = textColor;
        ws.iconColor         = iconColor;
        ws.backgroundColor   = backgroundColor;
        ws.widgetTitle       = widgetTitle;
        ws.widgetWindowWidth  = 90;
        ws.widgetWindowHeight = 80;
        ws.position          = position != null ? position : "BOTTOM_RIGHT";

        if (position != null && position.contains("_")) {
            String[] parts = position.split("_", 2);
            ws.verticalPosition   = parts[0];
            ws.horizontalPosition = parts[1];
        } else {
            ws.verticalPosition   = "BOTTOM";
            ws.horizontalPosition = "RIGHT";
        }
        return ws;
    }

    private static ActivityScenario<InteractionActivity> launch(Intercept intercept) {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
                InteractionActivity.class);
        intent.putExtra("INTERCEPT", intercept);
        return ActivityScenario.launch(intent);
    }

    private static ActivityScenario<InteractionActivity> launchNoExtras() {
        return ActivityScenario.launch(
                new Intent(ApplicationProvider.getApplicationContext(),
                        InteractionActivity.class));
    }

    // -------------------------------------------------------------------------
    // Layout selection
    // -------------------------------------------------------------------------

    @Test
    public void promptType_usesDialogLayout() {
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildIntercept(1, "PROMPT"))) {
            scenario.onActivity(activity ->
                    assertNotNull("PROMPT type must inflate the dialog layout (dialogContent view)",
                            activity.findViewById(R.id.dialogContent)));
        }
    }

    @Test
    public void embedType_usesFullscreenLayout() {
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildIntercept(2, "EMBED"))) {
            scenario.onActivity(activity -> {
                assertNull("EMBED type must NOT inflate the dialog layout (no dialogContent)",
                        activity.findViewById(R.id.dialogContent));
                assertNotNull("EMBED type must inflate the fullscreen layout (surveyWebView)",
                        activity.findViewById(R.id.surveyWebView));
            });
        }
    }

    // -------------------------------------------------------------------------
    // Close button
    // -------------------------------------------------------------------------

    @Test
    public void closeButton_isVisible() {
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildIntercept(3, "PROMPT"))) {
            scenario.onActivity(activity -> {
                View closeButton = activity.findViewById(R.id.closeButton);
                assertNotNull("Close button must exist in the layout", closeButton);
                assertEquals("Close button must be visible",
                        View.VISIBLE, closeButton.getVisibility());
            });
        }
    }

    @Test
    public void closeButton_click_finishesActivity() {
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildIntercept(4, "PROMPT"))) {
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.closeButton).performClick();
                assertTrue("Activity must be finishing after close button click",
                        activity.isFinishing());
            });
        }
    }

    // -------------------------------------------------------------------------
    // Null intercept → error dialog
    // -------------------------------------------------------------------------

    @Test
    public void nullIntercept_showsErrorDialog() {
        // No INTERCEPT extra → initIntercept() calls showErrorDialog() without ever
        // calling setContentView(), so no survey views exist in the hierarchy.
        try (ActivityScenario<InteractionActivity> ignored = launchNoExtras()) {
            Espresso.onView(ViewMatchers.withText("Survey Id is null"))
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));
        }
    }

    // -------------------------------------------------------------------------
    // WidgetSettings — background color
    // -------------------------------------------------------------------------

    @Test
    public void backgroundColor_appliedToTopBar() {
        WidgetSettings ws = makeWidgetSettings("", "", "#FF0000", "BOTTOM_RIGHT", "");
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildInterceptWithWidget(5, "PROMPT", ws))) {
            scenario.onActivity(activity -> {
                View topBar = activity.findViewById(R.id.topBarContainer);
                assertNotNull(topBar);
                assertTrue("setBackgroundColor() must produce a ColorDrawable",
                        topBar.getBackground() instanceof ColorDrawable);
                assertEquals("topBarContainer background must match the configured color",
                        Color.parseColor("#FF0000"),
                        ((ColorDrawable) topBar.getBackground()).getColor());
            });
        }
    }

    @Test
    public void emptyBackgroundColor_doesNotCrash() {
        // An empty backgroundColor must be skipped silently; the XML default background survives.
        WidgetSettings ws = makeWidgetSettings("", "", "", "BOTTOM_RIGHT", "");
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildInterceptWithWidget(6, "PROMPT", ws))) {
            scenario.onActivity(activity ->
                    assertNotNull("topBarContainer must exist even when backgroundColor is empty",
                            activity.findViewById(R.id.topBarContainer)));
        }
    }

    // -------------------------------------------------------------------------
    // WidgetSettings — widget title
    // -------------------------------------------------------------------------

    @Test
    public void withWidgetTitle_titleVisibleAndPoweredByHidden() {
        WidgetSettings ws = makeWidgetSettings("", "", "", "BOTTOM_RIGHT", "Feedback");
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildInterceptWithWidget(7, "PROMPT", ws))) {
            scenario.onActivity(activity -> {
                TextView titleText = activity.findViewById(R.id.widgetTitleText);
                View poweredBy    = activity.findViewById(R.id.poweredByLayout);
                assertNotNull(titleText);
                assertNotNull(poweredBy);
                assertEquals("widgetTitleText must be VISIBLE when widgetTitle is set",
                        View.VISIBLE, titleText.getVisibility());
                assertEquals("poweredByLayout must be GONE when widgetTitle is set",
                        View.GONE, poweredBy.getVisibility());
                assertEquals("Feedback", titleText.getText().toString());
            });
        }
    }

    @Test
    public void noWidgetTitle_poweredByVisible() {
        WidgetSettings ws = makeWidgetSettings("", "", "", "BOTTOM_RIGHT", "");
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildInterceptWithWidget(8, "PROMPT", ws))) {
            scenario.onActivity(activity -> {
                View poweredBy = activity.findViewById(R.id.poweredByLayout);
                TextView titleText = activity.findViewById(R.id.widgetTitleText);
                assertNotNull(poweredBy);
                assertEquals("poweredByLayout must remain VISIBLE when widgetTitle is empty",
                        View.VISIBLE, poweredBy.getVisibility());
                if (titleText != null) {
                    assertEquals("widgetTitleText must remain GONE when widgetTitle is empty",
                            View.GONE, titleText.getVisibility());
                }
            });
        }
    }

    // -------------------------------------------------------------------------
    // WidgetSettings — text color and icon color
    // -------------------------------------------------------------------------

    @Test
    public void textColor_appliedToWidgetTitleText() {
        // iconColor must also be a valid hex: applyWidgetSettings() parses both inside the
        // same try block gated on textColor being non-empty.
        WidgetSettings ws = makeWidgetSettings("#0000FF", "#00FF00", "", "BOTTOM_RIGHT", "Title");
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildInterceptWithWidget(9, "PROMPT", ws))) {
            scenario.onActivity(activity -> {
                TextView titleText = activity.findViewById(R.id.widgetTitleText);
                assertNotNull(titleText);
                assertEquals("Title text color must match the configured textColor",
                        Color.parseColor("#0000FF"), titleText.getCurrentTextColor());
            });
        }
    }

    @Test
    public void iconColor_appliedToCloseButton() {
        WidgetSettings ws = makeWidgetSettings("#0000FF", "#FF00FF", "", "BOTTOM_RIGHT", "Title");
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildInterceptWithWidget(10, "PROMPT", ws))) {
            scenario.onActivity(activity -> {
                ImageButton closeButton = activity.findViewById(R.id.closeButton);
                assertNotNull(closeButton);
                android.content.res.ColorStateList tint =
                        ImageViewCompat.getImageTintList(closeButton);
                assertNotNull("ImageViewCompat tint must be set when iconColor is provided", tint);
                assertEquals("Close button tint must match the configured iconColor",
                        Color.parseColor("#FF00FF"), tint.getDefaultColor());
            });
        }
    }

    // -------------------------------------------------------------------------
    // WidgetSettings — PROMPT dialog position and size
    // -------------------------------------------------------------------------

    @Test
    public void promptPosition_center_gravityIsCenter() {
        WidgetSettings ws = makeWidgetSettings("", "", "", "CENTER_CENTER", "");
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildInterceptWithWidget(11, "PROMPT", ws))) {
            scenario.onActivity(activity -> {
                LinearLayout dialogContent = activity.findViewById(R.id.dialogContent);
                assertNotNull(dialogContent);
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) dialogContent.getLayoutParams();
                assertEquals("CENTER position must produce Gravity.CENTER",
                        Gravity.CENTER, params.gravity);
            });
        }
    }

    @Test
    public void promptSize_widthAndHeight_matchScreenPercentages() {
        WidgetSettings ws = makeWidgetSettings("", "", "", "CENTER_CENTER", "");
        ws.widgetWindowWidth  = 80;
        ws.widgetWindowHeight = 70;
        try (ActivityScenario<InteractionActivity> scenario =
                     launch(buildInterceptWithWidget(12, "PROMPT", ws))) {
            scenario.onActivity(activity -> {
                LinearLayout dialogContent = activity.findViewById(R.id.dialogContent);
                assertNotNull(dialogContent);
                android.util.DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                int expectedWidth  = (int)(dm.widthPixels  * 80 / 100.0);
                int expectedHeight = (int)(dm.heightPixels * 70 / 100.0);
                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) dialogContent.getLayoutParams();
                assertEquals("Dialog width must be 80% of screen width",
                        expectedWidth, params.width);
                assertEquals("Dialog height must be 70% of screen height",
                        expectedHeight, params.height);
            });
        }
    }
}
