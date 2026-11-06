package com.questionpro.cxlib;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.questionpro.cxlib.util.CXUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class CXUtilsTest {

    // -------------------------------------------------------------------------
    // isEmpty
    // -------------------------------------------------------------------------

    @Test
    public void isEmpty_null_returnsTrue() {
        assertTrue(CXUtils.isEmpty(null));
    }

    @Test
    public void isEmpty_emptyString_returnsTrue() {
        assertTrue(CXUtils.isEmpty(""));
    }

    @Test
    public void isEmpty_nonEmptyString_returnsFalse() {
        assertFalse(CXUtils.isEmpty("hello"));
    }

    @Test
    public void isEmpty_whitespace_returnsFalse() {
        assertFalse(CXUtils.isEmpty(" "));
    }

    // -------------------------------------------------------------------------
    // isNetworkConnectionPresent
    // -------------------------------------------------------------------------

    @Test
    public void isNetworkConnectionPresent_robolectricContext_doesNotCrash() {
        Context ctx = ApplicationProvider.getApplicationContext();
        // Just verify no NPE; Robolectric may or may not stub a network
        CXUtils.isNetworkConnectionPresent(ctx);
    }

    // -------------------------------------------------------------------------
    // getUniqueDeviceId
    // -------------------------------------------------------------------------

    @Test
    public void getUniqueDeviceId_returnsNonNullNonEmpty() {
        Context ctx = ApplicationProvider.getApplicationContext();
        String id = CXUtils.getUniqueDeviceId(ctx);
        assertNotNull(id);
        assertFalse(id.isEmpty());
    }

    @Test
    public void getUniqueDeviceId_notKnownBadId() {
        Context ctx = ApplicationProvider.getApplicationContext();
        String id = CXUtils.getUniqueDeviceId(ctx);
        assertNotEquals("9774d56d682e549c", id);
    }

    // -------------------------------------------------------------------------
    // getAppLanguage
    // -------------------------------------------------------------------------

    @Test
    public void getAppLanguage_returnsNonNullNonEmpty() {
        Context ctx = ApplicationProvider.getApplicationContext();
        String lang = CXUtils.getAppLanguage(ctx);
        assertNotNull(lang);
        assertFalse(lang.isEmpty());
    }

    // -------------------------------------------------------------------------
    // convertStreamToString
    // -------------------------------------------------------------------------

    @Test
    public void convertStreamToString_simpleInput_returnsContent() throws Exception {
        String input = "hello world";
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        String result = CXUtils.convertStreamToString(is);
        assertTrue(result.contains("hello world"));
    }

    @Test
    public void convertStreamToString_emptyStream_returnsEmpty() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        String result = CXUtils.convertStreamToString(is);
        assertEquals("", result);
    }

    // -------------------------------------------------------------------------
    // dateToIso8601String
    // -------------------------------------------------------------------------

    @Test
    public void dateToIso8601String_epochZero_returnsFormattedString() {
        String result = CXUtils.dateToIso8601String(0L);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
