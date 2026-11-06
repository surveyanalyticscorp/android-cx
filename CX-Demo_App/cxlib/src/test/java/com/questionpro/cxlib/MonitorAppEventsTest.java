package com.questionpro.cxlib;

import android.content.Context;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

import com.questionpro.cxlib.model.InterceptRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class MonitorAppEventsTest {

    private MonitorAppEvents monitor;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();

        // Reset MonitorAppEvents singleton
        Field instance = MonitorAppEvents.class.getDeclaredField("mMonitorEvents");
        instance.setAccessible(true);
        instance.set(null, null);

        // Reset SharedPreferenceManager singleton
        Field spmInstance = SharedPreferenceManager.class.getDeclaredField("instance");
        spmInstance.setAccessible(true);
        spmInstance.set(null, null);

        monitor = MonitorAppEvents.getInstance();
    }

    // -------------------------------------------------------------------------
    // singleton
    // -------------------------------------------------------------------------

    @Test
    public void getInstance_returnsSameInstance() {
        assertSame(MonitorAppEvents.getInstance(), MonitorAppEvents.getInstance());
    }

    // -------------------------------------------------------------------------
    // appSessionStarted — timer fires callback after delay
    // -------------------------------------------------------------------------

    @Test
    public void appSessionStarted_validRule_firesCallbackAfterDelay() throws Exception {
        AtomicBoolean fired = new AtomicBoolean(false);
        InterceptRule rule = new InterceptRule();
        rule.value = "1"; // 1 second

        monitor.appSessionStarted(42, rule, new IQuestionProRulesCallback() {
            @Override public void onTimeSpendSatisfied(int interceptId) { fired.set(true); }
            @Override public void onViewCountRuleSatisfied(int interceptId) {}
        });

        assertFalse("callback should not fire before delay", fired.get());

        // Advance Robolectric's main looper
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertTrue("callback should fire after delay elapsed", fired.get());
    }

    @Test
    public void appSessionStarted_passesCorrectInterceptId() throws Exception {
        AtomicInteger receivedId = new AtomicInteger(-1);
        InterceptRule rule = new InterceptRule();
        rule.value = "1";

        monitor.appSessionStarted(99, rule, new IQuestionProRulesCallback() {
            @Override public void onTimeSpendSatisfied(int interceptId) { receivedId.set(interceptId); }
            @Override public void onViewCountRuleSatisfied(int interceptId) {}
        });

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertEquals(99, receivedId.get());
    }

    @Test
    public void appSessionStarted_nullRuleValue_doesNotSchedule() throws Exception {
        AtomicBoolean fired = new AtomicBoolean(false);
        InterceptRule rule = new InterceptRule();
        rule.value = null;

        monitor.appSessionStarted(1, rule, new IQuestionProRulesCallback() {
            @Override public void onTimeSpendSatisfied(int interceptId) { fired.set(true); }
            @Override public void onViewCountRuleSatisfied(int interceptId) {}
        });

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertFalse("null value should not schedule any runnable", fired.get());
    }

    @Test
    public void appSessionStarted_nonNumericRuleValue_doesNotSchedule() throws Exception {
        AtomicBoolean fired = new AtomicBoolean(false);
        InterceptRule rule = new InterceptRule();
        rule.value = "not-a-number";

        monitor.appSessionStarted(1, rule, new IQuestionProRulesCallback() {
            @Override public void onTimeSpendSatisfied(int interceptId) { fired.set(true); }
            @Override public void onViewCountRuleSatisfied(int interceptId) {}
        });

        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertFalse(fired.get());
    }

    // -------------------------------------------------------------------------
    // stopAllTimers — clears runnableMap and cancels callbacks
    // -------------------------------------------------------------------------

    @Test
    public void stopAllTimers_preventsCallbackFromFiring() throws Exception {
        AtomicBoolean fired = new AtomicBoolean(false);
        InterceptRule rule = new InterceptRule();
        rule.value = "1";

        monitor.appSessionStarted(5, rule, new IQuestionProRulesCallback() {
            @Override public void onTimeSpendSatisfied(int interceptId) { fired.set(true); }
            @Override public void onViewCountRuleSatisfied(int interceptId) {}
        });

        monitor.stopAllTimers();
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        assertFalse("timer should be cancelled after stopAllTimers", fired.get());
    }

    @Test
    public void stopAllTimers_clearsRunnableMap() throws Exception {
        InterceptRule rule = new InterceptRule();
        rule.value = "5";

        monitor.appSessionStarted(10, rule, new IQuestionProRulesCallback() {
            @Override public void onTimeSpendSatisfied(int interceptId) {}
            @Override public void onViewCountRuleSatisfied(int interceptId) {}
        });

        monitor.stopAllTimers();

        Field mapField = MonitorAppEvents.class.getDeclaredField("runnableMap");
        mapField.setAccessible(true);
        Map<?, ?> map = (Map<?, ?>) mapField.get(monitor);
        assertTrue("runnableMap should be empty after stopAllTimers", map.isEmpty());
    }

    @Test
    public void stopAllTimers_withNoTimers_doesNotThrow() {
        monitor.stopAllTimers(); // should not crash
    }
}
