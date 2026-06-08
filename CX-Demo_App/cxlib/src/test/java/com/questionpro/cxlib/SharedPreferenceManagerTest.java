package com.questionpro.cxlib;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;

import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashMap;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class SharedPreferenceManagerTest {

    private SharedPreferenceManager prefs;

    @Before
    public void setUp() throws Exception {
        // SharedPreferenceManager.instance is a static singleton. Robolectric resets the
        // Android environment (new Application) between test classes, but Java statics are
        // NOT reset. If another test class ran first, the stale singleton holds a reference
        // to the old Robolectric context — clearing SharedPreferences via the new context
        // has no effect on it. Nulling the field forces re-creation with the current context.
        Field instanceField = SharedPreferenceManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        Context context = ApplicationProvider.getApplicationContext();
        prefs = SharedPreferenceManager.getInstance(context);
        prefs.resetPreferences();
        context.getSharedPreferences("Intercepts", Context.MODE_PRIVATE).edit().clear().apply();
    }

    // --- view count ---

    @Test
    public void updateViewCount_incrementsCorrectly() {
        assertEquals(1, prefs.updateViewCountForTag("home"));
        assertEquals(2, prefs.updateViewCountForTag("home"));
        assertEquals(3, prefs.updateViewCountForTag("home"));
    }

    @Test
    public void updateViewCount_differentTags_areIndependent() {
        prefs.updateViewCountForTag("home");
        prefs.updateViewCountForTag("home");
        prefs.updateViewCountForTag("cart");

        assertEquals(2, prefs.updateViewCountForTag("home") - 1); // was 2, now 3 → subtract op
        assertEquals(1, prefs.updateViewCountForTag("cart") - 1); // was 1, now 2 → subtract op
    }

    @Test
    public void updateViewCount_stopsAtMaxCap() {
        // Simulate count already at cap
        for (int i = 0; i < 10_001; i++) {
            prefs.updateViewCountForTag("spamTag");
        }
        // After cap it should not exceed MAX_VIEW_COUNT (10_000)
        int result = prefs.updateViewCountForTag("spamTag");
        assertEquals(10_000, result);
    }

    @Test
    public void resetViewCount_resetsToZero() {
        prefs.updateViewCountForTag("home");
        prefs.updateViewCountForTag("home");
        prefs.resetViewCountForTag("home");
        assertEquals(1, prefs.updateViewCountForTag("home"));
    }

    // --- survey launch tracking ---

    @Test
    public void isSurveyAlreadyLaunched_falseBeforeLaunch() {
        assertFalse(prefs.isSurveyAlreadyLaunched(101));
    }

    @Test
    public void isSurveyAlreadyLaunched_trueAfterSave() {
        prefs.saveInterceptIdForLaunchedSurvey(101, System.currentTimeMillis());
        assertTrue(prefs.isSurveyAlreadyLaunched(101));
    }

    @Test
    public void isSurveyAlreadyLaunched_differentInterceptsAreIndependent() {
        prefs.saveInterceptIdForLaunchedSurvey(101, System.currentTimeMillis());
        assertFalse(prefs.isSurveyAlreadyLaunched(202));
    }

    // --- custom data mappings ---

    @Test
    public void saveAndGetCustomDataMappings_roundTrip() {
        HashMap<String, String> data = new HashMap<>();
        data.put("userName", "Alice");
        data.put("accountType", "premium");
        prefs.saveCustomDataMappings(data);

        String json = prefs.getCustomDataMappings();
        assertNotNull(json);
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("premium"));
    }

    @Test
    public void saveCustomDataMappings_mergesWithExisting() {
        HashMap<String, String> first = new HashMap<>();
        first.put("userName", "Alice");
        prefs.saveCustomDataMappings(first);

        HashMap<String, String> second = new HashMap<>();
        second.put("accountType", "premium");
        prefs.saveCustomDataMappings(second);

        String json = prefs.getCustomDataMappings();
        assertTrue(json.contains("Alice"));
        assertTrue(json.contains("premium"));
    }

    @Test
    public void saveCustomDataMappings_overwritesExistingKey() {
        HashMap<String, String> first = new HashMap<>();
        first.put("userName", "Alice");
        prefs.saveCustomDataMappings(first);

        HashMap<String, String> second = new HashMap<>();
        second.put("userName", "Bob");
        prefs.saveCustomDataMappings(second);

        String json = prefs.getCustomDataMappings();
        assertFalse(json.contains("Alice"));
        assertTrue(json.contains("Bob"));
    }

    // --- visitor UUID ---

    @Test
    public void saveAndGetVisitorUUID_roundTrip() {
        prefs.saveVisitorsUUID("uuid-1234");
        assertEquals("uuid-1234", prefs.getVisitorsUUID());
    }

    // --- reset ---

    @Test
    public void resetPreferences_clearsViewCounts() {
        prefs.updateViewCountForTag("home");
        prefs.resetPreferences();
        assertEquals(1, prefs.updateViewCountForTag("home")); // starts from 0 again
    }
}
