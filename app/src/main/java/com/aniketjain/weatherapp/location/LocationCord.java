package com.aniketjain.weatherapp.location;

import android.util.Log;

public class LocationCord {
    private static final String TAG = "LocationCord";

    public static String lat = "";
    public static String lon = "";
    public final static String API_KEY = "68a540bf049a84ea7c6fd0ee7cd2b317";

    // Method untuk validasi koordinat
    public static boolean isValidCoordinates() {
        return lat != null && !lat.isEmpty() && lon != null && !lon.isEmpty();
    }

    // Method untuk set koordinat dengan validasi
    public static void setCoordinates(double latitude, double longitude) {
        lat = String.valueOf(latitude);
        lon = String.valueOf(longitude);
        Log.d(TAG, "Coordinates set - Lat: " + lat + ", Lon: " + lon);
    }

    // Method untuk reset koordinat
    public static void resetCoordinates() {
        lat = "";
        lon = "";
        Log.d(TAG, "Coordinates reset");
    }


}
