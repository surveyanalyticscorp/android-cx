package com.questionpro.cxlib;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.questionpro.cxlib.enums.DataCenter;
import com.questionpro.cxlib.enums.Platform;
import com.questionpro.cxlib.model.TouchPoint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Verifies the SDK initialization flow for all available DataCenters.
 *
 * The tests are grouped into four layers that mirror the initialization path:
 *
 *   1. TouchPoint.Builder  — DataCenter and API key are stored correctly.
 *   2. Payload generation  — CXGlobalInfo.savePayLoad() serializes the DataCenter
 *                            into the JSON payload so it can be read back later.
 *   3. URL routing         — CXConstants resolves the correct regional endpoint
 *                            for every DataCenter.
 *   4. SDK init validation — initialize() enforces the presence of an API key.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class DataCenterInitializationTest {

    private Context context;

    // -----------------------------------------------------------------------
    // Expected endpoint constants — mirrors CXConstants private logic.
    // -----------------------------------------------------------------------

    // Intercept API base URLs
    private static final String INTERCEPT_US  = "https://intercept-api.questionpro.com";
    private static final String INTERCEPT_EU  = "https://intercept-api.questionpro.eu";
    private static final String INTERCEPT_CA  = "https://intercept-api.questionpro.ca";
    private static final String INTERCEPT_SG  = "https://intercept-api.questionpro.sg";
    private static final String INTERCEPT_AU  = "https://intercept-api.questionpro.au";
    private static final String INTERCEPT_AE  = "https://intercept-api.questionpro.ae";
    private static final String INTERCEPT_SA  = "https://api.surveyanalytics.com";
    // KSA shares the same intercept base URL as US
    private static final String INTERCEPT_KSA = "https://intercept-api.questionpro.com";

    // Main API base URLs (used for survey URL resolution)
    private static final String API_US  = "https://api.questionpro.com";
    private static final String API_EU  = "https://api.questionpro.eu";
    private static final String API_CA  = "https://api.questionpro.ca";
    private static final String API_SG  = "https://api.questionpro.sg";
    private static final String API_AU  = "https://api.questionpro.au";
    private static final String API_AE  = "https://api.questionpro.ae";
    private static final String API_SA  = "https://api.surveyanalytics.com";
    private static final String API_KSA = "https://api.questionprosa.com";

    // -----------------------------------------------------------------------
    // Setup / teardown
    // -----------------------------------------------------------------------

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        resetCXGlobalInfo();
    }

    /**
     * Clears all static state in CXGlobalInfo so each test starts from scratch.
     * Without this, payload from a previous test would bleed into the next one.
     */
    private void resetCXGlobalInfo() throws Exception {
        Field instanceField = CXGlobalInfo.class.getDeclaredField("ourInstance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        Field uuidField = CXGlobalInfo.class.getDeclaredField("UUID");
        uuidField.setAccessible(true);
        uuidField.set(null, null);

        Field apiKeyField = CXGlobalInfo.class.getDeclaredField("apiKey");
        apiKeyField.setAccessible(true);
        apiKeyField.set(null, null);

        Field payloadField = CXGlobalInfo.class.getDeclaredField("payload");
        payloadField.setAccessible(true);
        payloadField.set(null, null);
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /** Saves the given DataCenter + API key as the active payload in CXGlobalInfo. */
    private void activateDataCenter(DataCenter dc, String apiKey) throws Exception {
        TouchPoint tp = new TouchPoint.Builder(dc, apiKey).build();
        CXGlobalInfo.getInstance().savePayLoad(tp);
    }

    // -----------------------------------------------------------------------
    // 1. TouchPoint.Builder — DataCenter and API key storage
    // -----------------------------------------------------------------------

    @Test
    public void touchPointBuilder_us_storesDataCenter() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.US, "key-us").build();
        assertEquals(DataCenter.US, tp.getDataCenter());
    }

    @Test
    public void touchPointBuilder_eu_storesDataCenter() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.EU, "key-eu").build();
        assertEquals(DataCenter.EU, tp.getDataCenter());
    }

    @Test
    public void touchPointBuilder_ca_storesDataCenter() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.CA, "key-ca").build();
        assertEquals(DataCenter.CA, tp.getDataCenter());
    }

    @Test
    public void touchPointBuilder_sg_storesDataCenter() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.SG, "key-sg").build();
        assertEquals(DataCenter.SG, tp.getDataCenter());
    }

    @Test
    public void touchPointBuilder_au_storesDataCenter() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.AU, "key-au").build();
        assertEquals(DataCenter.AU, tp.getDataCenter());
    }

    @Test
    public void touchPointBuilder_ae_storesDataCenter() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.AE, "key-ae").build();
        assertEquals(DataCenter.AE, tp.getDataCenter());
    }

    @Test
    public void touchPointBuilder_sa_storesDataCenter() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.SA, "key-sa").build();
        assertEquals(DataCenter.SA, tp.getDataCenter());
    }

    @Test
    public void touchPointBuilder_ksa_storesDataCenter() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.KSA, "key-ksa").build();
        assertEquals(DataCenter.KSA, tp.getDataCenter());
    }

    @Test
    public void touchPointBuilder_storesApiKey() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.US, "my-api-key").build();
        assertEquals("my-api-key", tp.getApiKey());
    }

    @Test
    public void touchPointBuilder_withoutApiKey_storesNullApiKey() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.US).build();
        assertNull(tp.getApiKey());
    }

    @Test
    public void touchPointBuilder_defaultPlatformIsAndroid() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.US, "key").build();
        assertEquals(Platform.ANDROID, tp.getPlatform());
    }

    @Test
    public void touchPointBuilder_setPlatform_storesPlatform() {
        TouchPoint tp = new TouchPoint.Builder(DataCenter.EU, "key")
                .setPlatform(Platform.FLUTTER)
                .build();
        assertEquals(Platform.FLUTTER, tp.getPlatform());
    }

    // -----------------------------------------------------------------------
    // 2. Payload generation — CXGlobalInfo.getDataCenter() after savePayLoad
    // -----------------------------------------------------------------------

    @Test
    public void savePayload_us_dataCenterSerializedToPayload() throws Exception {
        activateDataCenter(DataCenter.US, "key-us");
        assertEquals("US", CXGlobalInfo.getDataCenter());
    }

    @Test
    public void savePayload_eu_dataCenterSerializedToPayload() throws Exception {
        activateDataCenter(DataCenter.EU, "key-eu");
        assertEquals("EU", CXGlobalInfo.getDataCenter());
    }

    @Test
    public void savePayload_ca_dataCenterSerializedToPayload() throws Exception {
        activateDataCenter(DataCenter.CA, "key-ca");
        assertEquals("CA", CXGlobalInfo.getDataCenter());
    }

    @Test
    public void savePayload_sg_dataCenterSerializedToPayload() throws Exception {
        activateDataCenter(DataCenter.SG, "key-sg");
        assertEquals("SG", CXGlobalInfo.getDataCenter());
    }

    @Test
    public void savePayload_au_dataCenterSerializedToPayload() throws Exception {
        activateDataCenter(DataCenter.AU, "key-au");
        assertEquals("AU", CXGlobalInfo.getDataCenter());
    }

    @Test
    public void savePayload_ae_dataCenterSerializedToPayload() throws Exception {
        activateDataCenter(DataCenter.AE, "key-ae");
        assertEquals("AE", CXGlobalInfo.getDataCenter());
    }

    @Test
    public void savePayload_sa_dataCenterSerializedToPayload() throws Exception {
        activateDataCenter(DataCenter.SA, "key-sa");
        assertEquals("SA", CXGlobalInfo.getDataCenter());
    }

    @Test
    public void savePayload_ksa_dataCenterSerializedToPayload() throws Exception {
        activateDataCenter(DataCenter.KSA, "key-ksa");
        assertEquals("KSA", CXGlobalInfo.getDataCenter());
    }

    @Test
    public void savePayload_apiKeyFromTouchPoint_storedInGlobalInfo() throws Exception {
        activateDataCenter(DataCenter.US, "abc-123");
        assertEquals("abc-123", CXGlobalInfo.getInstance().getApiKey());
    }

    @Test
    public void savePayload_nullApiKeyInTouchPoint_doesNotOverrideExistingKey() throws Exception {
        // Pre-set a key via setApiKey, then save a TouchPoint with no API key;
        // the previously set key must survive.
        CXGlobalInfo.getInstance().setApiKey("pre-set-key");
        TouchPoint tp = new TouchPoint.Builder(DataCenter.EU).build(); // no API key
        CXGlobalInfo.getInstance().savePayLoad(tp);
        assertEquals("pre-set-key", CXGlobalInfo.getInstance().getApiKey());
    }

    // -----------------------------------------------------------------------
    // 3. URL routing — intercept endpoints per DataCenter
    //    CXConstants.getInterceptsUrl() must resolve to the regional base URL.
    // -----------------------------------------------------------------------

    @Test
    public void interceptsUrl_us_resolvesToUsEndpoint() throws Exception {
        activateDataCenter(DataCenter.US, "key-us");
        assertTrue(CXConstants.getInterceptsUrl().startsWith(INTERCEPT_US));
    }

    @Test
    public void interceptsUrl_eu_resolvesToEuEndpoint() throws Exception {
        activateDataCenter(DataCenter.EU, "key-eu");
        assertTrue(CXConstants.getInterceptsUrl().startsWith(INTERCEPT_EU));
    }

    @Test
    public void interceptsUrl_ca_resolvesToCaEndpoint() throws Exception {
        activateDataCenter(DataCenter.CA, "key-ca");
        assertTrue(CXConstants.getInterceptsUrl().startsWith(INTERCEPT_CA));
    }

    @Test
    public void interceptsUrl_sg_resolvesToSgEndpoint() throws Exception {
        activateDataCenter(DataCenter.SG, "key-sg");
        assertTrue(CXConstants.getInterceptsUrl().startsWith(INTERCEPT_SG));
    }

    @Test
    public void interceptsUrl_au_resolvesToAuEndpoint() throws Exception {
        activateDataCenter(DataCenter.AU, "key-au");
        assertTrue(CXConstants.getInterceptsUrl().startsWith(INTERCEPT_AU));
    }

    @Test
    public void interceptsUrl_ae_resolvesToAeEndpoint() throws Exception {
        activateDataCenter(DataCenter.AE, "key-ae");
        assertTrue(CXConstants.getInterceptsUrl().startsWith(INTERCEPT_AE));
    }

    @Test
    public void interceptsUrl_sa_resolvesToSaEndpoint() throws Exception {
        activateDataCenter(DataCenter.SA, "key-sa");
        assertTrue(CXConstants.getInterceptsUrl().startsWith(INTERCEPT_SA));
    }

    @Test
    public void interceptsUrl_ksa_resolvesToKsaEndpoint() throws Exception {
        activateDataCenter(DataCenter.KSA, "key-ksa");
        assertTrue(CXConstants.getInterceptsUrl().startsWith(INTERCEPT_KSA));
    }

    // -----------------------------------------------------------------------
    // 4. URL routing — feedback / survey endpoints per DataCenter
    //    getSurveyFeedbackUrl() and getExcludedFeedbackUrl() share the same
    //    intercept base URL. getSurveyUrl() uses the main API base URL.
    // -----------------------------------------------------------------------

    @Test
    public void feedbackUrl_eu_resolvesToEuEndpoint() throws Exception {
        activateDataCenter(DataCenter.EU, "key-eu");
        assertTrue(CXConstants.getSurveyFeedbackUrl().startsWith(INTERCEPT_EU));
        assertTrue(CXConstants.getExcludedFeedbackUrl().startsWith(INTERCEPT_EU));
    }

    @Test
    public void feedbackUrl_ca_resolvesToCaEndpoint() throws Exception {
        activateDataCenter(DataCenter.CA, "key-ca");
        assertTrue(CXConstants.getSurveyFeedbackUrl().startsWith(INTERCEPT_CA));
        assertTrue(CXConstants.getExcludedFeedbackUrl().startsWith(INTERCEPT_CA));
    }

    @Test
    public void feedbackUrl_sa_resolvesToSaEndpoint() throws Exception {
        activateDataCenter(DataCenter.SA, "key-sa");
        assertTrue(CXConstants.getSurveyFeedbackUrl().startsWith(INTERCEPT_SA));
        assertTrue(CXConstants.getExcludedFeedbackUrl().startsWith(INTERCEPT_SA));
    }

    @Test
    public void surveyUrl_us_resolvesToUsMainApiEndpoint() throws Exception {
        activateDataCenter(DataCenter.US, "key-us");
        assertTrue(CXConstants.getSurveyUrl(123L).startsWith(API_US));
    }

    @Test
    public void surveyUrl_eu_resolvesToEuMainApiEndpoint() throws Exception {
        activateDataCenter(DataCenter.EU, "key-eu");
        assertTrue(CXConstants.getSurveyUrl(123L).startsWith(API_EU));
    }

    @Test
    public void surveyUrl_ca_resolvesToCaMainApiEndpoint() throws Exception {
        activateDataCenter(DataCenter.CA, "key-ca");
        assertTrue(CXConstants.getSurveyUrl(123L).startsWith(API_CA));
    }

    @Test
    public void surveyUrl_sg_resolvesToSgMainApiEndpoint() throws Exception {
        activateDataCenter(DataCenter.SG, "key-sg");
        assertTrue(CXConstants.getSurveyUrl(123L).startsWith(API_SG));
    }

    @Test
    public void surveyUrl_au_resolvesToAuMainApiEndpoint() throws Exception {
        activateDataCenter(DataCenter.AU, "key-au");
        assertTrue(CXConstants.getSurveyUrl(123L).startsWith(API_AU));
    }

    @Test
    public void surveyUrl_ae_resolvesToAeMainApiEndpoint() throws Exception {
        activateDataCenter(DataCenter.AE, "key-ae");
        assertTrue(CXConstants.getSurveyUrl(123L).startsWith(API_AE));
    }

    @Test
    public void surveyUrl_sa_resolvesToSaMainApiEndpoint() throws Exception {
        activateDataCenter(DataCenter.SA, "key-sa");
        assertTrue(CXConstants.getSurveyUrl(123L).startsWith(API_SA));
    }

    @Test
    public void surveyUrl_ksa_resolvesToKsaMainApiEndpoint() throws Exception {
        activateDataCenter(DataCenter.KSA, "key-ksa");
        assertTrue(CXConstants.getSurveyUrl(123L).startsWith(API_KSA));
    }

    // -----------------------------------------------------------------------
    // 5. SDK init validation — API key enforcement in initialize()
    // -----------------------------------------------------------------------

    /**
     * initialize() must throw IllegalStateException when no API key is available
     * via either the TouchPoint builder or the manifest meta-data.
     *
     * The Robolectric Application manifest does not contain cx_manifest_api_key,
     * and we deliberately do not set an API key in CXGlobalInfo, so the SDK has
     * no valid key and must reject the call.
     */
    @Test
    public void initialize_missingApiKey_throwsIllegalStateException() throws Exception {
        // Set up the minimum required state for initialize() to reach the key check.
        // Do NOT call savePayLoad with an API key — the CXGlobalInfo.apiKey remains null.
        TouchPoint tp = new TouchPoint.Builder(DataCenter.US).build(); // no API key
        CXGlobalInfo.getInstance().savePayLoad(tp);

        // Inject appContext into the QuestionProCX singleton.
        Field appContextField = QuestionProCX.class.getDeclaredField("appContext");
        appContextField.setAccessible(true);
        appContextField.set(null, context);

        try {
            QuestionProCX.getInstance().initialize();
            fail("Expected IllegalStateException when API key is missing");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("API key not found"));
        }
    }

    /**
     * initialize() must NOT throw when a valid API key is provided via the
     * TouchPoint builder. The call will proceed asynchronously to fetch intercepts;
     * we only verify that the synchronous validation step passes without exception.
     */
    @Test
    public void initialize_withApiKey_doesNotThrowDuringValidation() throws Exception {
        activateDataCenter(DataCenter.US, "valid-api-key");

        Field appContextField = QuestionProCX.class.getDeclaredField("appContext");
        appContextField.setAccessible(true);
        appContextField.set(null, context);

        // Should pass validation without throwing; the async network call will
        // be a no-op in the test environment.
        try {
            QuestionProCX.getInstance().initialize();
        } catch (IllegalStateException e) {
            fail("initialize() must not throw IllegalStateException when API key is set: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // 6. CXGlobalInfo — API key management
    // -----------------------------------------------------------------------

    @Test
    public void setApiKey_nullValue_throwsIllegalArgumentException() {
        try {
            CXGlobalInfo.getInstance().setApiKey(null);
            fail("Expected IllegalArgumentException for null API key");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void setApiKey_emptyString_throwsIllegalArgumentException() {
        try {
            CXGlobalInfo.getInstance().setApiKey("");
            fail("Expected IllegalArgumentException for empty API key");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void setApiKey_blankString_throwsIllegalArgumentException() {
        try {
            CXGlobalInfo.getInstance().setApiKey("   ");
            fail("Expected IllegalArgumentException for blank API key");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void setApiKey_validKey_trimsWhitespaceAndStores() {
        CXGlobalInfo.getInstance().setApiKey("  my-key  ");
        assertEquals("my-key", CXGlobalInfo.getInstance().getApiKey());
    }

    @Test
    public void dataCenterChange_updatesUrlsAccordingly() throws Exception {
        // First init with US.
        activateDataCenter(DataCenter.US, "key-us");
        String usInterceptUrl = CXConstants.getInterceptsUrl();

        // Re-init with EU (simulates a different build variant or test environment).
        resetCXGlobalInfo();
        activateDataCenter(DataCenter.EU, "key-eu");
        String euInterceptUrl = CXConstants.getInterceptsUrl();

        assertNotEquals("EU intercept URL must differ from US", usInterceptUrl, euInterceptUrl);
        assertTrue(euInterceptUrl.startsWith(INTERCEPT_EU));
    }
}
