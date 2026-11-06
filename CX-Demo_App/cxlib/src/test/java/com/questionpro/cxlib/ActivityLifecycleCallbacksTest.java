package com.questionpro.cxlib;

import android.app.Activity;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class ActivityLifecycleCallbacksTest {

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();

        Field spmInstance = SharedPreferenceManager.class.getDeclaredField("instance");
        spmInstance.setAccessible(true);
        spmInstance.set(null, null);

        // Inject appContext so QuestionProCX doesn't crash on clearSession/fetchInterceptSettings
        Field appContextField = QuestionProCX.class.getDeclaredField("appContext");
        appContextField.setAccessible(true);
        appContextField.set(null, context);

        context.getSharedPreferences(SharedPreferenceManager.PREF_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
        context.getSharedPreferences("Intercepts", Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    private Activity buildActivity() {
        return Robolectric.buildActivity(Activity.class).create().get();
    }

    private boolean isInForeground(ActivityLifecycleCallbacks cb) throws Exception {
        Field f = ActivityLifecycleCallbacks.class.getDeclaredField("isInForeground");
        f.setAccessible(true);
        return (boolean) f.get(cb);
    }

    private int activityStartCount(ActivityLifecycleCallbacks cb) throws Exception {
        Field f = ActivityLifecycleCallbacks.class.getDeclaredField("activityStartCount");
        f.setAccessible(true);
        return (int) f.get(cb);
    }

    // -------------------------------------------------------------------------
    // onActivityStarted — count increment
    // -------------------------------------------------------------------------

    @Test
    public void onActivityStarted_incrementsCount() throws Exception {
        ActivityLifecycleCallbacks cb = new ActivityLifecycleCallbacks(0);
        // Start with isInForeground=true so fetch isn't triggered
        cb.onActivityStarted(buildActivity());
        assertEquals(1, activityStartCount(cb));
    }

    @Test
    public void onActivityStarted_multiple_accumulatesCount() throws Exception {
        ActivityLifecycleCallbacks cb = new ActivityLifecycleCallbacks(0);
        Activity a = buildActivity();
        cb.onActivityStarted(a);
        cb.onActivityStarted(a);
        cb.onActivityStarted(a);
        assertEquals(3, activityStartCount(cb));
    }

    // -------------------------------------------------------------------------
    // onActivityStopped — count decrement and background detection
    // -------------------------------------------------------------------------

    @Test
    public void onActivityStopped_decrementsCount() throws Exception {
        ActivityLifecycleCallbacks cb = new ActivityLifecycleCallbacks(2);
        cb.onActivityStopped(buildActivity());
        assertEquals(1, activityStartCount(cb));
    }

    @Test
    public void onActivityStopped_countsFloorAtZero() throws Exception {
        ActivityLifecycleCallbacks cb = new ActivityLifecycleCallbacks(0);
        cb.onActivityStopped(buildActivity());
        assertEquals(0, activityStartCount(cb));
    }

    @Test
    public void onActivityStopped_countReachesZero_setsBackgroundFlag() throws Exception {
        ActivityLifecycleCallbacks cb = new ActivityLifecycleCallbacks(1);
        cb.onActivityStopped(buildActivity());
        assertFalse("isInForeground should be false when count hits 0", isInForeground(cb));
    }

    @Test
    public void onActivityStopped_countAboveZero_remainsInForeground() throws Exception {
        ActivityLifecycleCallbacks cb = new ActivityLifecycleCallbacks(2);
        // Manually set isInForeground=true (default via constructor)
        Field f = ActivityLifecycleCallbacks.class.getDeclaredField("isInForeground");
        f.setAccessible(true);
        f.set(cb, true);

        cb.onActivityStopped(buildActivity());
        assertTrue("isInForeground should stay true while other activities are started", isInForeground(cb));
    }

    // -------------------------------------------------------------------------
    // foreground return detection (onActivityStarted when was background)
    // -------------------------------------------------------------------------

    @Test
    public void onActivityStarted_fromBackground_setsInForeground() throws Exception {
        ActivityLifecycleCallbacks cb = new ActivityLifecycleCallbacks(0);

        // Put into background state
        Field f = ActivityLifecycleCallbacks.class.getDeclaredField("isInForeground");
        f.setAccessible(true);
        f.set(cb, false);

        cb.onActivityStarted(buildActivity());
        assertTrue("app should be marked as in foreground after returning from background", isInForeground(cb));
    }

    @Test
    public void onActivityStarted_alreadyForeground_noForegroundReturnTrigger() throws Exception {
        // isInForeground=true (default), activityStartCount=1
        ActivityLifecycleCallbacks cb = new ActivityLifecycleCallbacks(1);

        Field f = ActivityLifecycleCallbacks.class.getDeclaredField("isInForeground");
        f.setAccessible(true);
        f.set(cb, true);

        // Should not throw even though fetchInterceptSettings would be a no-op
        cb.onActivityStarted(buildActivity());
        assertTrue(isInForeground(cb));
        assertEquals(2, activityStartCount(cb));
    }
}
