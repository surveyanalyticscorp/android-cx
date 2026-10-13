package com.questionpro.cxlib.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;


public class CXUtils {
    public static final String PSEUDO_ISO8601_DATE_FORMAT_MILLIS = "yyyy-MM-dd HH:mm:ss.SSSZ"; // 2011-01-01 11:59:59.123-0800 or 2011-01-01 11:59:59.23-0800


    public static String getUniqueDeviceId(Context context) {
        @SuppressLint("HardwareIds")
        String device_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if ((device_id == null) || (device_id.equals("9774d56d682e549c")) || (device_id.length() < 15)) {
            device_id = new BigInteger(64, new SecureRandom()).toString(16);
        }
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

    public static boolean isEmpty(CharSequence charSequence) {
        return charSequence == null || charSequence.length() == 0;
    }
    public static boolean isNetworkConnectionPresent(Context appContext) {
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null;
    }

    @SuppressLint("SimpleDateFormat")
    public static String dateToIso8601String(long millis) {
        return dateToString(new SimpleDateFormat(PSEUDO_ISO8601_DATE_FORMAT_MILLIS), new Date(millis));
    }

    public static String dateToString(DateFormat format, Date date) {
        return format.format(date);
    }

    public static void lockOrientation(Activity activity) {
        Display display = ((WindowManager) activity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int tempOrientation = activity.getResources().getConfiguration().orientation;
        int orientation = 0;
        switch(tempOrientation)
        {
            case Configuration.ORIENTATION_LANDSCAPE:
                if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_90)
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                else
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                if(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_270)
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                else
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        }
        //activity.setRequestedOrientation(orientation);
    }

    public static void unlockOrientation(Activity activity)
    {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }


    public static int convertDpToPixel(Activity activity, int dp){
        Resources r = activity.getResources();
        int px = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                r.getDisplayMetrics()
        );
        return px;
    }


    public static long getCurrentLocalTimeInMillis(){
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault()); // Get current time in default time zone
        return calendar.getTimeInMillis();
    }

    public static void printLog(String tag, String message){
        //Log.d(tag, message);
    }

    public static String getAppLanguage(Context context) {
        Locale locale;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = context.getResources().getConfiguration().getLocales().get(0);
        } else {
            locale = context.getResources().getConfiguration().locale;
        }
        //CXUtils.printLog("Datta","App Language: "+locale.toLanguageTag());
        //return locale.getLanguage(); // e.g., "en", "hi", "fr"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return locale.toLanguageTag();
        } else {
            return locale.getLanguage();
        }
    }

}
