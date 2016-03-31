package com.questionpro.cxlib;

import android.app.Activity;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Created by sachinsable on 29/03/16.
 */
public class CXUtils {
    public static String getUniqueDeviceId(Activity activity) {
        String device_id = Settings.Secure.getString(
                activity.getContentResolver(), Settings.Secure.ANDROID_ID);
        if ((device_id == null) || (device_id.equals("9774d56d682e549c"))
                || (device_id.length() < 15)) {
            device_id = new BigInteger(64, new SecureRandom()).toString(16);
        }
        Log.d("device_id", device_id);
        return device_id;
    }

    public static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is,
                "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}
