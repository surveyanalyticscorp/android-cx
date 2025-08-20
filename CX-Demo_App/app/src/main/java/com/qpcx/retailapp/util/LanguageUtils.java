package com.qpcx.retailapp.util;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.LocaleList;
import android.util.Log;

import java.util.Locale;

public class LanguageUtils {
    public static Context wrapContextWithLocale(Context context, String languageCode) {
        Locale newLocale = new Locale(languageCode);
        Locale.setDefault(newLocale);

        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(newLocale); // update config with new locale

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(new LocaleList(newLocale));
            return context.createConfigurationContext(configuration);
        } else {
            return context.createConfigurationContext(configuration);
        }
    }
}
