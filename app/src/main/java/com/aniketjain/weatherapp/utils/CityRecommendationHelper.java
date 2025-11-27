package com.aniketjain.weatherapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class CityRecommendationHelper {
    private static final String PREF_NAME = "city_history";
    private static final String KEY_RECENT_CITIES = "recent_cities";
    private static final int MAX_RECENT = 5;

    // Kota-kota populer default
    private static final String[] DEFAULT_POPULAR = {
            "Jakarta", "Surabaya", "Bandung", "Medan", "Semarang",
            "Makassar", "Palembang", "Tangerang", "Depok", "Yogyakarta"
    };

    public static void addRecentCity(Context context, String cityName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        List<String> recentCities = getRecentCities(context);

        // Hapus jika sudah ada (untuk pindahkan ke depan)
        recentCities.remove(cityName);

        // Tambahkan di depan
        recentCities.add(0, cityName);

        // Batasi ukuran
        if (recentCities.size() > MAX_RECENT) {
            recentCities = recentCities.subList(0, MAX_RECENT);
        }

        // Simpan sebagai string dengan separator
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recentCities.size(); i++) {
            sb.append(recentCities.get(i));
            if (i < recentCities.size() - 1) {
                sb.append(",");
            }
        }
        prefs.edit().putString(KEY_RECENT_CITIES, sb.toString()).apply();

        // Update popularitas
        updatePopularCities(context, cityName);
    }

    public static List<String> getRecentCities(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String citiesStr = prefs.getString(KEY_RECENT_CITIES, "");

        List<String> cities = new ArrayList<>();
        if (!citiesStr.isEmpty()) {
            String[] cityArray = citiesStr.split(",");
            for (String city : cityArray) {
                if (!city.trim().isEmpty()) {
                    cities.add(city.trim());
                }
            }
        }

        return cities;
    }

    private static void updatePopularCities(Context context, String cityName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Load counter
        int count = prefs.getInt(cityName + "_count", 0);
        count++;
        prefs.edit().putInt(cityName + "_count", count).apply();
    }

    public static List<String> getPopularCities(Context context) {
        // Return default popular cities
        List<String> popular = new ArrayList<>();
        for (String city : DEFAULT_POPULAR) {
            popular.add(city);
        }
        return popular;
    }

    public static List<String> getRecommendations(Context context, String query) {
        List<String> recommendations = new ArrayList<>();

        // Jika query kosong, tampilkan recent + popular
        if (query == null || query.trim().isEmpty()) {
            recommendations.addAll(getRecentCities(context));

            List<String> popular = getPopularCities(context);
            for (String city : popular) {
                if (!recommendations.contains(city) && recommendations.size() < 8) {
                    recommendations.add(city);
                }
            }
        } else {
            // Filter berdasarkan query
            String lowerQuery = query.toLowerCase();

            // Prioritaskan recent cities yang cocok
            for (String city : getRecentCities(context)) {
                if (city.toLowerCase().startsWith(lowerQuery)) {
                    recommendations.add(city);
                }
            }

            // Tambahkan popular cities yang cocok
            for (String city : getPopularCities(context)) {
                if (city.toLowerCase().startsWith(lowerQuery) &&
                        !recommendations.contains(city)) {
                    recommendations.add(city);
                }
            }
        }

        // Hapus duplikat sambil pertahankan urutan
        return new ArrayList<>(new LinkedHashSet<>(recommendations));
    }

    public static void clearHistory(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }
}