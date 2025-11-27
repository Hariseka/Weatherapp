package com.aniketjain.weatherapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;

public class LanguageManager {
    private static final String PREF_NAME = "language_pref";
    private static final String KEY_LANGUAGE = "selected_language";

    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_INDONESIAN = "id";

    public static void setLanguage(Context context, String languageCode) {
        // Simpan pilihan bahasa
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();

        // Terapkan bahasa
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);

        context.createConfigurationContext(config);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_INDONESIAN);
    }

    public static void applyLanguage(Context context) {
        String languageCode = getLanguage(context);
        setLanguage(context, languageCode);
    }

    public static String getLanguageName(String languageCode) {
        switch (languageCode) {
            case LANGUAGE_ENGLISH:
                return "English";
            case LANGUAGE_INDONESIAN:
                return "Bahasa Indonesia";
            default:
                return "Bahasa Indonesia";
        }
    }
}