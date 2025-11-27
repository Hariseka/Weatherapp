package com.aniketjain.weatherapp.url;

import android.util.Log;
import com.aniketjain.weatherapp.location.LocationCord;

public class URL {
    private static final String TAG = "URL";
    private String link;
    private static String city_url;
    private static String forecast_url;

    public URL() {
        // Validasi koordinat
        if (LocationCord.lat == null || LocationCord.lat.isEmpty() ||
                LocationCord.lon == null || LocationCord.lon.isEmpty()) {
            Log.e(TAG, "ERROR: Coordinates are empty!");
            Log.e(TAG, "Lat: '" + LocationCord.lat + "'");
            Log.e(TAG, "Lon: '" + LocationCord.lon + "'");
            link = "";
        } else {
            // Pakai CURRENT WEATHER API (cuaca saat ini)
            link = "https://api.openweathermap.org/data/2.5/weather?lat="
                    + LocationCord.lat
                    + "&lon=" + LocationCord.lon
                    + "&units=metric"                          // langsung Celsius
                    + "&appid=" + LocationCord.API_KEY;

            Log.d(TAG, "Generated Current Weather URL: " + link);
        }
    }

    public String getLink() {
        return link;
    }

    // URL untuk current weather berdasarkan nama kota
    public static void setCity_url(String cityName) {
        city_url = "https://api.openweathermap.org/data/2.5/weather?q="
                + cityName + "&appid=" + LocationCord.API_KEY;
        Log.d(TAG, "City Weather URL: " + city_url);
    }

    public static String getCity_url() {
        return city_url;
    }

    // URL untuk forecast berdasarkan nama kota
    public static void setCityForecast_url(String cityName) {
        forecast_url = "https://api.openweathermap.org/data/2.5/forecast?q="
                + cityName + "&appid=" + LocationCord.API_KEY;
        Log.d(TAG, "City Forecast URL: " + forecast_url);
    }

    public static String getCityForecast_url() {
        return forecast_url;
    }

    // URL untuk current weather berdasarkan koordinat
    public static String getCurrentWeatherUrl(double lat, double lon) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat="
                + lat + "&lon=" + lon + "&appid=" + LocationCord.API_KEY;
        Log.d(TAG, "Current Weather URL: " + url);
        return url;
    }

    // URL untuk forecast berdasarkan koordinat
    public static String getForecastUrl(double lat, double lon) {
        String url = "https://api.openweathermap.org/data/2.5/forecast?lat="
                + lat + "&lon=" + lon + "&appid=" + LocationCord.API_KEY;
        Log.d(TAG, "Forecast URL: " + url);
        return url;
    }
}