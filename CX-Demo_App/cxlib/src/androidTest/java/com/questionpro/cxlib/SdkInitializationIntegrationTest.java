package com.questionpro.cxlib;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.questionpro.cxlib.enums.DataCenter;
import com.questionpro.cxlib.interfaces.IQuestionProInitCallback;
import com.questionpro.cxlib.model.TouchPoint;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

/**
 * Integration tests that verify the SDK initialization flow with real API calls.
 *
 * Each test:
 *   1. Builds a TouchPoint with a real DataCenter + API key.
 *   2. Calls QuestionProCX.init() and waits for the async callback.
 *   3. Asserts that onInitializationSuccess() was fired (not onInitializationFailure).
 *
 * SETUP: Replace each "REPLACE_WITH_*" placeholder below with a valid API key
 * for that DataCenter before running.
 *
 * Run on a physical device or emulator with network access.
 */
@RunWith(AndroidJUnit4.class)
public class SdkInitializationIntegrationTest {

    // -----------------------------------------------------------------------
    // API keys — fill in before running
    // -----------------------------------------------------------------------
    private static final String API_KEY_US  = "058d9ebc-c80e-4969-8196-f4feb7aae5e6";
    private static final String API_KEY_EU  = "7904e566-cefa-4cf5-815d-d9f4a83d6b77";
    private static final String API_KEY_CA  = "REPLACE_WITH_CA_API_KEY";
    private static final String API_KEY_SG  = "REPLACE_WITH_SG_API_KEY";
    private static final String API_KEY_AU  = "REPLACE_WITH_AU_API_KEY";
    private static final String API_KEY_AE  = "REPLACE_WITH_AE_API_KEY";
    private static final String API_KEY_SA  = "REPLACE_WITH_SA_API_KEY";
    private static final String API_KEY_KSA = "REPLACE_WITH_KSA_API_KEY";

    // 2-second init delay + network round-trip + buffer
    private static final long TIMEOUT_SECONDS = 15;

    private Context context;

    // -----------------------------------------------------------------------
    // Setup / teardown
    // -----------------------------------------------------------------------

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        resetSdkState();
    }

    @After
    public void tearDown() throws Exception {
        // Unregister lifecycle callbacks while appContext is still set.
        QuestionProCX.getInstance().cleanup();
        resetSdkState();
    }

    /**
     * Clears all SDK singleton state so each test starts fresh.
     *
     * Without this:
     *  - isSessionAlive=true from a previous test would skip fetchInterceptSettings()
     *  - A stale API key / DataCenter from the previous test would pollute URL resolution
     */
    private void resetSdkState() throws Exception {
        setStaticField(QuestionProCX.class, "isSessionAlive", false);
        setStaticField(QuestionProCX.class, "isInitialised",  false);
        setStaticField(QuestionProCX.class, "appContext",     null);

        setStaticField(CXGlobalInfo.class, "ourInstance", null);
        setStaticField(CXGlobalInfo.class, "UUID",        null);
        setStaticField(CXGlobalInfo.class, "apiKey",      null);
        setStaticField(CXGlobalInfo.class, "payload",     null);
    }

    private void setStaticField(Class<?> clazz, String name, Object value) throws Exception {
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    // -----------------------------------------------------------------------
    // Core helper
    // -----------------------------------------------------------------------

    /**
     * Initializes the SDK with the given DataCenter and API key, then blocks
     * until onInitializationSuccess or onInitializationFailure is invoked.
     *
     * @return "success:<message>" | "failure:<error>" | "timeout"
     */
    private String initAndWait(DataCenter dataCenter, String apiKey) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>("timeout");

        TouchPoint touchPoint = new TouchPoint.Builder(dataCenter, apiKey).build();

        QuestionProCX.getInstance().init(context, touchPoint, new IQuestionProInitCallback() {
            @Override
            public void onInitializationSuccess(String message) {
                result.set("success:" + message);
                latch.countDown();
            }

            @Override
            public void onInitializationFailure(String error) {
                result.set("failure:" + error);
                latch.countDown();
            }
        });

        latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return result.get();
    }

    // -----------------------------------------------------------------------
    // DataCenter — US
    // -----------------------------------------------------------------------

    @Test
    public void init_us_dataCenter_callbackFires_withSuccess() throws Exception {
        assumeFalse("US API key not configured — skipping", API_KEY_US.startsWith("REPLACE_WITH"));
        String result = initAndWait(DataCenter.US, API_KEY_US);
        assertNotEquals("No callback received for US DataCenter within " + TIMEOUT_SECONDS + "s", "timeout", result);
        assertTrue("SDK init failed for US DataCenter. Response: " + result, result.startsWith("success:"));
    }

    // -----------------------------------------------------------------------
    // DataCenter — EU
    // -----------------------------------------------------------------------

    @Test
    public void init_eu_dataCenter_callbackFires_withSuccess() throws Exception {
        assumeFalse("EU API key not configured — skipping", API_KEY_EU.startsWith("REPLACE_WITH"));
        String result = initAndWait(DataCenter.EU, API_KEY_EU);
        assertNotEquals("No callback received for EU DataCenter within " + TIMEOUT_SECONDS + "s", "timeout", result);
        assertTrue("SDK init failed for EU DataCenter. Response: " + result, result.startsWith("success:"));
    }

    // -----------------------------------------------------------------------
    // DataCenter — CA
    // -----------------------------------------------------------------------

    @Test
    public void init_ca_dataCenter_callbackFires_withSuccess() throws Exception {
        assumeFalse("CA API key not configured — skipping", API_KEY_CA.startsWith("REPLACE_WITH"));
        String result = initAndWait(DataCenter.CA, API_KEY_CA);
        assertNotEquals("No callback received for CA DataCenter within " + TIMEOUT_SECONDS + "s", "timeout", result);
        assertTrue("SDK init failed for CA DataCenter. Response: " + result, result.startsWith("success:"));
    }

    // -----------------------------------------------------------------------
    // DataCenter — SG
    // -----------------------------------------------------------------------

    @Test
    public void init_sg_dataCenter_callbackFires_withSuccess() throws Exception {
        assumeFalse("SG API key not configured — skipping", API_KEY_SG.startsWith("REPLACE_WITH"));
        String result = initAndWait(DataCenter.SG, API_KEY_SG);
        assertNotEquals("No callback received for SG DataCenter within " + TIMEOUT_SECONDS + "s", "timeout", result);
        assertTrue("SDK init failed for SG DataCenter. Response: " + result, result.startsWith("success:"));
    }

    // -----------------------------------------------------------------------
    // DataCenter — AU
    // -----------------------------------------------------------------------

    @Test
    public void init_au_dataCenter_callbackFires_withSuccess() throws Exception {
        assumeFalse("AU API key not configured — skipping", API_KEY_AU.startsWith("REPLACE_WITH"));
        String result = initAndWait(DataCenter.AU, API_KEY_AU);
        assertNotEquals("No callback received for AU DataCenter within " + TIMEOUT_SECONDS + "s", "timeout", result);
        assertTrue("SDK init failed for AU DataCenter. Response: " + result, result.startsWith("success:"));
    }

    // -----------------------------------------------------------------------
    // DataCenter — AE
    // -----------------------------------------------------------------------

    @Test
    public void init_ae_dataCenter_callbackFires_withSuccess() throws Exception {
        assumeFalse("AE API key not configured — skipping", API_KEY_AE.startsWith("REPLACE_WITH"));
        String result = initAndWait(DataCenter.AE, API_KEY_AE);
        assertNotEquals("No callback received for AE DataCenter within " + TIMEOUT_SECONDS + "s", "timeout", result);
        assertTrue("SDK init failed for AE DataCenter. Response: " + result, result.startsWith("success:"));
    }

    // -----------------------------------------------------------------------
    // DataCenter — SA
    // -----------------------------------------------------------------------

    @Test
    public void init_sa_dataCenter_callbackFires_withSuccess() throws Exception {
        assumeFalse("SA API key not configured — skipping", API_KEY_SA.startsWith("REPLACE_WITH"));
        String result = initAndWait(DataCenter.SA, API_KEY_SA);
        assertNotEquals("No callback received for SA DataCenter within " + TIMEOUT_SECONDS + "s", "timeout", result);
        assertTrue("SDK init failed for SA DataCenter. Response: " + result, result.startsWith("success:"));
    }

    // -----------------------------------------------------------------------
    // DataCenter — KSA
    // -----------------------------------------------------------------------

    @Test
    public void init_ksa_dataCenter_callbackFires_withSuccess() throws Exception {
        assumeFalse("KSA API key not configured — skipping", API_KEY_KSA.startsWith("REPLACE_WITH"));
        String result = initAndWait(DataCenter.KSA, API_KEY_KSA);
        assertNotEquals("No callback received for KSA DataCenter within " + TIMEOUT_SECONDS + "s", "timeout", result);
        assertTrue("SDK init failed for KSA DataCenter. Response: " + result, result.startsWith("success:"));
    }

    // -----------------------------------------------------------------------
    // Error cases
    // -----------------------------------------------------------------------

    /**
     * An invalid API key must trigger onInitializationFailure, not success.
     * This confirms the server rejects bad credentials and the failure path works.
     */
    @Test
    public void init_invalidApiKey_us_triggersFailureCallback() throws Exception {
        String result = initAndWait(DataCenter.US, "invalid-key-00000000-0000");
        assertNotEquals("No callback received for invalid key test within " + TIMEOUT_SECONDS + "s", "timeout", result);
        assertTrue("Expected failure callback for invalid API key, got: " + result, result.startsWith("failure:"));
    }

    /**
     * Passing a null TouchPoint must fire onInitializationFailure immediately
     * without waiting for a network response.
     */
    @Test
    public void init_nullTouchPoint_triggersImmediateFailure() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>("timeout");

        QuestionProCX.getInstance().init(context, null, new IQuestionProInitCallback() {
            @Override
            public void onInitializationSuccess(String message) {
                result.set("success:" + message);
                latch.countDown();
            }

            @Override
            public void onInitializationFailure(String error) {
                result.set("failure:" + error);
                latch.countDown();
            }
        });

        // Null-TouchPoint path does not go through the 2s delayed Handler,
        // so a 3-second timeout is sufficient.
        latch.await(3, TimeUnit.SECONDS);
        assertTrue("Expected immediate failure for null TouchPoint, got: " + result.get(),
                result.get().startsWith("failure:"));
    }
}
