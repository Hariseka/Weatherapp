package com.aniketjain.weatherapp.notification;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.aniketjain.weatherapp.location.LocationCord;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WeatherNotificationWorker extends Worker {
    private static final String TAG = "WeatherWorker";
    private static final String PREF_NAME = "weather_cache";
    private static final String KEY_CITY = "last_city";
    private static final String KEY_TEMP = "last_temp";
    private static final String KEY_CONDITION = "last_condition";
    private static final String KEY_CONDITION_CODE = "last_condition_code";

    public WeatherNotificationWorker(@NonNull Context context,
                                     @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Weather notification worker started");

        try {
            if (hasLocationPermission()) {
                boolean success = fetchCurrentLocationWeatherSync();
                if (!success) {
                    sendNotificationFromCache();
                }
            } else {
                Log.w(TAG, "No location permission, using cached data");
                sendNotificationFromCache();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in doWork: " + e.getMessage(), e);
            sendNotificationFromCache();
        }

        return Result.success();
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean fetchCurrentLocationWeatherSync() {
        FusedLocationProviderClient client = LocationServices
                .getFusedLocationProviderClient(getApplicationContext());

        try {
            // Use Tasks.await() to synchronously wait for location
            Location location = Tasks.await(client.getLastLocation(), 10, TimeUnit.SECONDS);

            if (location != null) {
                Log.d(TAG, "Location obtained: " + location.getLatitude() + ", " + location.getLongitude());

                String cityName = getCityName(location);
                Log.d(TAG, "City name: " + cityName);

                return fetchWeatherDataSync(cityName, location.getLatitude(), location.getLongitude());
            } else {
                Log.w(TAG, "Location is null");
                return false;
            }
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.e(TAG, "Error getting location: " + e.getMessage());
            return false;
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission denied: " + e.getMessage());
            return false;
        }
    }

    private String getCityName(Location location) {
        if (location == null) {
            Log.w(TAG, "Location is null in getCityName");
            return getLastKnownCity();
        }

        try {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

            if (!Geocoder.isPresent()) {
                Log.w(TAG, "Geocoder not available");
                return getLastKnownCity();
            }

            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                String city = address.getLocality();
                if (city == null || city.isEmpty()) {
                    city = address.getSubAdminArea();
                }
                if (city == null || city.isEmpty()) {
                    city = address.getAdminArea();
                }

                if (city != null && !city.isEmpty()) {
                    Log.d(TAG, "City found from Geocoder: " + city);
                    return city;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder IOException: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error in getCityName: " + e.getMessage());
        }

        return getLastKnownCity();
    }

    private String getLastKnownCity() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lastCity = prefs.getString(KEY_CITY, null);

        if (lastCity != null && !lastCity.equals("Your Location")) {
            Log.d(TAG, "Using last known city: " + lastCity);
            return lastCity;
        }

        Log.d(TAG, "No last known city, using default");
        return "Your Location";
    }

    private boolean fetchWeatherDataSync(String cityName, double lat, double lon) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
                "&lon=" + lon +
                "&appid=" + LocationCord.API_KEY +
                "&units=metric";

        Log.d(TAG, "Fetching weather for: " + cityName);

        final boolean[] success = {false};
        final Object lock = new Object();

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        JSONObject main = response.getJSONObject("main");
                        double temp = main.getDouble("temp");

                        JSONObject weather0 = response.getJSONArray("weather").getJSONObject(0);
                        int conditionCode = weather0.getInt("id");
                        String description = weather0.getString("description");

                        String temperature = Math.round(temp) + "°C";

                        Log.d(TAG, "Weather fetched - City: " + cityName +
                                ", Temp: " + temperature);

                        saveLastWeatherData(
                                getApplicationContext(),
                                cityName,
                                temperature,
                                description,
                                conditionCode
                        );

                        String customMessage = getWeatherMessage(conditionCode, temperature);
                        WeatherNotificationHelper.sendWeatherNotification(
                                getApplicationContext(),
                                cityName,
                                temperature,
                                description,
                                customMessage
                        );

                        success[0] = true;
                        Log.d(TAG, "Notification sent successfully");

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing weather data: " + e.getMessage());
                        success[0] = false;
                    }

                    synchronized (lock) {
                        lock.notify();
                    }
                },
                error -> {
                    Log.e(TAG, "Failed to fetch weather: " + error.toString());
                    success[0] = false;
                    synchronized (lock) {
                        lock.notify();
                    }
                }
        );

        jsonObjectRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                10000,
                0,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(jsonObjectRequest);

        // Wait for request to complete
        synchronized (lock) {
            try {
                lock.wait(15000); // Wait max 15 seconds
            } catch (InterruptedException e) {
                Log.e(TAG, "Wait interrupted: " + e.getMessage());
            }
        }

        return success[0];
    }

    private void sendNotificationFromCache() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String cityName = prefs.getString(KEY_CITY, null);
        String temperature = prefs.getString(KEY_TEMP, null);
        String condition = prefs.getString(KEY_CONDITION, null);
        int conditionCode = prefs.getInt(KEY_CONDITION_CODE, 800);

        if (cityName == null || temperature == null || condition == null) {
            Log.w(TAG, "No valid cached weather data available");
            return;
        }

        String customMessage = getWeatherMessage(conditionCode, temperature);

        WeatherNotificationHelper.sendWeatherNotification(
                getApplicationContext(),
                cityName,
                temperature,
                condition,
                customMessage
        );

        Log.d(TAG, "Notification sent from cache - City: " + cityName);
    }

    private String getWeatherMessage(int conditionCode, String temperature) {
        if (conditionCode >= 200 && conditionCode < 300) {
            return "⚡ Ada badai petir nih! Mending di rumah aja, rebahan sambil dengerin suara hujan~";
        } else if (conditionCode >= 300 && conditionCode < 400) {
            return "🌧 Gerimis-gerimis… Pas banget buat minum kopi hangat sambil baca buku!";
        } else if (conditionCode >= 500 && conditionCode < 600) {
            return "☔ Lagi hujan nih! Cuaca dingin, enaknya makan mie ayam… YUKKK! 🍜";
        } else if (conditionCode >= 600 && conditionCode < 700) {
            return "❄️ Wah salju turun! Langka banget di Indonesia, nikmatin momentnya ya! ☃️";
        } else if (conditionCode >= 700 && conditionCode < 800) {
            if (conditionCode == 701 || conditionCode == 741) {
                return "🌫 Berkabut nih, hati-hati di jalan ya! Nyalain lampu kendaraan~";
            } else {
                return "😷 Udara lagi ga bersih nih, pake masker kalo keluar ya!";
            }
        } else if (conditionCode == 800) {
            try {
                int temp = Integer.parseInt(temperature.replace("°C", "").trim());
                if (temp >= 32) {
                    return "🌞 Cerah banget hari ini! Panas banget sih, jangan lupa sunscreen & minum air putih yaa 😎💦";
                } else if (temp >= 28) {
                    return "☀️ Cerah dan hangat! Waktu yang pas buat jalan-jalan tapi jangan lupa sunscreen yaa 😎";
                } else {
                    return "🌤 Cerah dan adem! Cuaca sempurna buat beraktivitas hari ini! 💪";
                }
            } catch (NumberFormatException e) {
                return "☀️ Cerah banget hari ini! Waktu yang pas buat jalan-jalan tapi jangan lupa sunscreen yaa 😎";
            }
        } else if (conditionCode > 800 && conditionCode < 900) {
            if (conditionCode == 801 || conditionCode == 802) {
                return "⛅ Berawan dikit, tapi tetep adem kok! Enak buat aktivitas outdoor~";
            } else {
                return "☁️ Berawan tapi adem, enak buat rebahan sambil denger musik! 🎵";
            }
        } else if (conditionCode >= 900) {
            if (conditionCode == 905 || conditionCode == 951) {
                return "💨 Anginnya lumayan kencang, hati-hati pas naik motor yaa! 🏍️";
            } else {
                return "⚠️ Cuaca ekstrim nih, stay safe dan tetap waspada ya!";
            }
        }

        return "🌈 Apapun cuacanya, semangat beraktivitas hari ini! 💪";
    }

    public static void saveLastWeatherData(Context context, String cityName,
                                           String temperature, String condition, int conditionCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CITY, cityName)
                .putString(KEY_TEMP, temperature)
                .putString(KEY_CONDITION, condition)
                .putInt(KEY_CONDITION_CODE, conditionCode)
                .apply();

        Log.d(TAG, "Weather data saved - City: " + cityName +
                ", Temp: " + temperature +
                ", Condition: " + condition +
                ", Code: " + conditionCode);
    }
}