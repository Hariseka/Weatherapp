package com.aniketjain.weatherapp;

import static com.aniketjain.weatherapp.location.CityFinder.getCityNameUsingNetwork;
import static com.aniketjain.weatherapp.location.CityFinder.setLongitudeLatitude;
import static com.aniketjain.weatherapp.network.InternetConnectivity.isInternetConnected;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.aniketjain.weatherapp.adapter.CityAdapter;
import com.aniketjain.weatherapp.adapter.CityHistoryAdapter;
import com.aniketjain.weatherapp.adapter.DaysAdapter;
import com.aniketjain.weatherapp.data.CitiesData;
import com.aniketjain.weatherapp.database.WeatherDatabase;
import com.aniketjain.weatherapp.database.WeatherEntity;
import com.aniketjain.weatherapp.database.WeatherRepository;
import com.aniketjain.weatherapp.databinding.ActivityHomeBinding;
import com.aniketjain.weatherapp.location.LocationCord;
import com.aniketjain.weatherapp.model.City;
import com.aniketjain.weatherapp.notification.WeatherNotificationHelper;
import com.aniketjain.weatherapp.notification.WeatherNotificationWorker;
import com.aniketjain.weatherapp.toast.Toaster;
import com.aniketjain.weatherapp.update.UpdateUI;
import com.aniketjain.weatherapp.url.URL;
import com.aniketjain.weatherapp.utils.CityRecommendationHelper;
import com.aniketjain.weatherapp.utils.LanguageManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private final int WEATHER_FORECAST_APP_UPDATE_REQ_CODE = 101;
    private static final int LOCATION_PERMISSION_CODE = 1;
    private static final int NOTIFICATION_PERMISSION_CODE = 102;

    private String name, updated_at, description, temperature, min_temperature, max_temperature, pressure, wind_speed, humidity;
    private int condition;
    private long update_time, sunset, sunrise;
    private String city = "";
    private ActivityHomeBinding binding;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;

    private CityAdapter cityAdapter;
    private CityHistoryAdapter historyAdapter;
    private List<City> citiesList;
    private List<String> recentCities;
    private WeatherRepository weatherRepository;

    private static final String PREF_LAST_CITY = "last_searched_city";
    private static final String KEY_LAST_CITY_NAME = "city_name";
    private static final String KEY_LAST_CITY_LAT = "city_lat";
    private static final String KEY_LAST_CITY_LON = "city_lon";

    private boolean hasRequestedPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "=== onCreate START ===");

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        weatherRepository = new WeatherRepository(this);
        LanguageManager.applyLanguage(this);

        setupDailyNotification();
        WeatherDatabase.cleanExpiredCache(this);

        initializeSpeechRecognizer();
        setNavigationBarColor();
        checkUpdate();
        setRefreshLayoutColor();
        setupCityAutoComplete();
        setupCityHistory();
        listeners();

        // Request permissions first, then load data
        requestAllPermissions();

        Log.d(TAG, "=== onCreate END ===");
    }

    private void requestAllPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Check location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsToRequest);
            hasRequestedPermission = true;
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    LOCATION_PERMISSION_CODE
            );
        } else {
            Log.d(TAG, "All permissions already granted");
            getDataUsingNetwork();
        }
    }

    private void setupDailyNotification() {
        PeriodicWorkRequest weatherWorkRequest = new PeriodicWorkRequest.Builder(
                WeatherNotificationWorker.class,
                24, TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(this).enqueue(weatherWorkRequest);
        Log.d(TAG, "Daily weather notification scheduled");
    }

    private void initializeSpeechRecognizer() {
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            String spokenCity = matches.get(0).toUpperCase();
                            Log.d(TAG, "Voice input: " + spokenCity);
                            binding.layout.cityEt.setText(spokenCity);
                            searchCity(spokenCity);
                        }
                    }
                }
        );
    }

    private void setupCityHistory() {
        recentCities = CityRecommendationHelper.getRecentCities(this);

        if (recentCities != null && !recentCities.isEmpty()) {
            binding.layout.historySection.setVisibility(View.VISIBLE);

            historyAdapter = new CityHistoryAdapter(this, recentCities, cityName -> {
                Log.d(TAG, "History city clicked: " + cityName);
                searchCity(cityName);
            });

            binding.layout.historyRv.setLayoutManager(
                    new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            );
            binding.layout.historyRv.setAdapter(historyAdapter);
        } else {
            binding.layout.historySection.setVisibility(View.GONE);
        }
    }

    private void updateCityHistory() {
        recentCities = CityRecommendationHelper.getRecentCities(this);

        if (recentCities != null && !recentCities.isEmpty()) {
            binding.layout.historySection.setVisibility(View.VISIBLE);

            if (historyAdapter == null) {
                historyAdapter = new CityHistoryAdapter(this, recentCities, cityName -> {
                    searchCity(cityName);
                });
                binding.layout.historyRv.setAdapter(historyAdapter);
            } else {
                historyAdapter.notifyDataSetChanged();
            }
        }
    }

    private void setupCityAutoComplete() {
        Log.d(TAG, "=== setupCityAutoComplete START ===");

        citiesList = CitiesData.getIndonesianCities();
        Log.d(TAG, "Total cities loaded: " + citiesList.size());

        cityAdapter = new CityAdapter(this, citiesList);

        if (binding.layout.cityEt instanceof AutoCompleteTextView) {
            AutoCompleteTextView cityAutoComplete = (AutoCompleteTextView) binding.layout.cityEt;

            cityAutoComplete.setAdapter(cityAdapter);
            cityAutoComplete.setThreshold(1);

            cityAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
                City selectedCity = (City) parent.getItemAtPosition(position);

                if (selectedCity != null) {
                    LocationCord.lat = String.valueOf(selectedCity.getLat());
                    LocationCord.lon = String.valueOf(selectedCity.getLon());

                    new android.os.Handler().postDelayed(() -> {
                        checkCacheBeforeFetch(selectedCity.getName());
                    }, 100);

                    hideKeyboard(view);
                    cityAutoComplete.clearFocus();
                    cityAutoComplete.setText("");
                }
            });
        }

        Log.d(TAG, "=== setupCityAutoComplete END ===");
    }

    private void setNavigationBarColor() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.navBarColor));
        }
    }

    private void setUpDaysRecyclerView() {
        Log.d(TAG, "Setting up days RecyclerView");
        DaysAdapter daysAdapter = new DaysAdapter(this);

        // Set HORIZONTAL layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
        );

        binding.dayRv.setLayoutManager(layoutManager);
        binding.dayRv.setAdapter(daysAdapter);
        binding.dayRv.setHasFixedSize(true);

        Log.d(TAG, "RecyclerView setup complete with horizontal orientation");
    }

    @SuppressLint("ClickableViewAccessibility")
    private void listeners() {
        Log.d(TAG, "Setting up listeners");

        binding.layout.mainLayout.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard(view);
            return false;
        });

        binding.layout.searchBarIv.setOnClickListener(view -> {
            String cityName = binding.layout.cityEt.getText().toString();
            searchCity(cityName);
        });

        binding.layout.searchBarIv.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard(view);
            return false;
        });

        binding.layout.cityEt.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_GO) {
                searchCity(binding.layout.cityEt.getText().toString());
                hideKeyboard(textView);
                return true;
            }
            return false;
        });

        binding.layout.cityEt.setOnFocusChangeListener((view, b) -> {
            if (!b) {
                hideKeyboard(view);
            }
        });

        binding.mainRefreshLayout.setOnRefreshListener(() -> {
            checkConnection();
            binding.mainRefreshLayout.setRefreshing(false);
        });

        binding.layout.micSearchId.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan nama kota");

            try {
                speechRecognizerLauncher.launch(intent);
            } catch (Exception e) {
                Log.e(TAG, "Voice search error: " + e.getMessage());
                Toaster.errorToast(this, "Voice search not available");
            }
        });
    }

    private void setRefreshLayoutColor() {
        binding.mainRefreshLayout.setProgressBackgroundColorSchemeColor(
                getResources().getColor(R.color.textColor)
        );
        binding.mainRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.navBarColor)
        );
    }

    private void searchCity(String cityName) {
        Log.d(TAG, "=== searchCity START ===");

        if (cityName == null || cityName.trim().isEmpty()) {
            Toaster.errorToast(this, "Please enter the city name");
            return;
        }

        if (!isInternetConnected(this)) {
            Toaster.errorToast(this, "Please check your internet connection");
            return;
        }

        City foundCity = null;
        for (City city : citiesList) {
            if (city.getName().equalsIgnoreCase(cityName.trim())) {
                foundCity = city;
                break;
            }
        }

        if (foundCity != null) {
            LocationCord.lat = String.valueOf(foundCity.getLat());
            LocationCord.lon = String.valueOf(foundCity.getLon());

            saveLastSearchedCity(foundCity.getName(), LocationCord.lat, LocationCord.lon);

            City finalFoundCity = foundCity;
            new android.os.Handler().postDelayed(() -> {
                checkCacheBeforeFetch(finalFoundCity.getName());
            }, 100);

            binding.layout.cityEt.setText("");
        } else {
            setLatitudeLongitudeUsingCity(cityName);
        }
    }

    private void checkCacheBeforeFetch(String cityName) {
        weatherRepository.getWeatherByCity(cityName, new WeatherRepository.OnWeatherLoadedListener() {
            @Override
            public void onSuccess(WeatherEntity weather) {
                runOnUiThread(() -> {
                    loadWeatherFromCache(weather);
                    Toaster.successToast(HomeActivity.this, "Data dari cache");
                });
            }

            @Override
            public void onNotFound() {
                getTodayWeatherInfo(cityName);
            }

            @Override
            public void onError(String error) {
                getTodayWeatherInfo(cityName);
            }
        });
    }

    private void loadWeatherFromCache(WeatherEntity weather) {
        name = weather.getCityName();
        temperature = weather.getTemperature();
        min_temperature = weather.getMinTemperature();
        max_temperature = weather.getMaxTemperature();
        description = weather.getDescription();
        pressure = weather.getPressure();
        wind_speed = weather.getWindSpeed();
        humidity = weather.getHumidity();

        updateUI();
        hideProgressBar();
        setUpDaysRecyclerView();
    }

    private void saveWeatherToDatabase(String cityName) {
        if (LocationCord.lat == null || LocationCord.lon == null) {
            Log.w(TAG, "Cannot save weather - invalid coordinates");
            return;
        }

        try {
            WeatherEntity weather = new WeatherEntity(
                    cityName,
                    Double.parseDouble(LocationCord.lat),
                    Double.parseDouble(LocationCord.lon),
                    temperature != null ? temperature : "N/A",
                    min_temperature != null ? min_temperature : "N/A",
                    max_temperature != null ? max_temperature : "N/A",
                    String.valueOf(condition),
                    description != null ? description : "No description",
                    pressure != null ? pressure : "0",
                    wind_speed != null ? wind_speed : "0",
                    humidity != null ? humidity : "0",
                    System.currentTimeMillis(),
                    ""
            );

            weatherRepository.insertWeather(weather, new WeatherRepository.OnWeatherSavedListener() {
                @Override
                public void onSuccess(WeatherEntity weather) {
                    Log.d(TAG, "Weather saved to database");
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Failed to save weather: " + error);
                }
            });
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing coordinates for database: " + e.getMessage());
        }
    }

    private void getDataUsingNetwork() {
        Log.d(TAG, "=== getDataUsingNetwork START ===");

        SharedPreferences lastCityPrefs = getSharedPreferences(PREF_LAST_CITY, MODE_PRIVATE);
        String lastCityName = lastCityPrefs.getString(KEY_LAST_CITY_NAME, null);
        String lastCityLat = lastCityPrefs.getString(KEY_LAST_CITY_LAT, null);
        String lastCityLon = lastCityPrefs.getString(KEY_LAST_CITY_LON, null);

        if (lastCityName != null && lastCityLat != null && lastCityLon != null) {
            Log.d(TAG, "Loading last searched city: " + lastCityName);
            LocationCord.lat = lastCityLat;
            LocationCord.lon = lastCityLon;
            city = lastCityName;

            checkCacheBeforeFetch(lastCityName);
            getDeviceLocationForNotification();
            return;
        }

        getDeviceLocation();
    }

    private void getDeviceLocation() {
        if (!checkLocationPermission()) {
            Log.w(TAG, "Location permission not granted");
            Toaster.errorToast(this, "Location permission required");
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

        try {
            client.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    setLongitudeLatitude(location);
                    city = getCityNameUsingNetwork(this, location);

                    if (city != null && !city.isEmpty()) {
                        getTodayWeatherInfo(city);
                        saveDeviceLocationForNotification(city, location);
                    } else {
                        Toaster.errorToast(this, "Unable to get city name");
                        hideProgressBar();
                    }
                } else {
                    Log.w(TAG, "Location is null");
                    Toaster.errorToast(this, "Unable to get location. Please search for a city.");
                    hideProgressBar();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to get location: " + e.getMessage());
                Toaster.errorToast(this, "Failed to get location");
                hideProgressBar();
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
            Toaster.errorToast(this, "Location permission denied");
            hideProgressBar();
        }
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void getDeviceLocationForNotification() {
        if (!checkLocationPermission()) {
            return;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);

        try {
            client.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    String deviceCity = getCityNameFromLocation(location);
                    Log.d(TAG, "Device location for notification: " + deviceCity);
                    saveDeviceLocationForNotification(deviceCity, location);
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception in notification location: " + e.getMessage());
        }
    }

    private void saveDeviceLocationForNotification(String cityName, android.location.Location location) {
        String deviceLat = String.valueOf(location.getLatitude());
        String deviceLon = String.valueOf(location.getLongitude());

        String properCityName = getCityNameFromLocation(location);
        fetchWeatherForNotification(properCityName, deviceLat, deviceLon);
    }

    private String getCityNameFromLocation(android.location.Location location) {
        try {
            android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
            java.util.List<android.location.Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1
            );
            if (addresses != null && !addresses.isEmpty()) {
                String city = addresses.get(0).getLocality();
                if (city == null) city = addresses.get(0).getSubAdminArea();
                if (city == null) city = addresses.get(0).getAdminArea();
                if (city != null) return city;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting city name: " + e.getMessage());
        }
        return "Your Location";
    }

    @SuppressLint("DefaultLocale")
    private void fetchWeatherForNotification(String cityName, String lat, String lon) {
        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
                "&lon=" + lon +
                "&appid=" + LocationCord.API_KEY +
                "&units=metric";

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        org.json.JSONObject main = response.getJSONObject("main");
                        double temp = main.getDouble("temp");

                        org.json.JSONObject weather0 = response.getJSONArray("weather").getJSONObject(0);
                        int conditionCode = weather0.getInt("id");
                        String description = weather0.getString("description");

                        String temperature = String.valueOf(Math.round(temp));

                        WeatherNotificationWorker.saveLastWeatherData(
                                this,
                                cityName,
                                temperature + "°C",
                                description,
                                conditionCode
                        );

                        Log.d(TAG, "Weather saved for notification - City: " + cityName +
                                ", Temp: " + temperature + "°C, Condition: " + conditionCode);

                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "Error parsing weather for notification: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Failed to fetch weather for notification")
        );

        requestQueue.add(jsonObjectRequest);
    }

    private void saveLastSearchedCity(String cityName, String lat, String lon) {
        SharedPreferences prefs = getSharedPreferences(PREF_LAST_CITY, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_LAST_CITY_NAME, cityName)
                .putString(KEY_LAST_CITY_LAT, lat)
                .putString(KEY_LAST_CITY_LON, lon)
                .apply();
        Log.d(TAG, "Last city saved: " + cityName);
    }

    private void setLatitudeLongitudeUsingCity(String cityName) {
        URL.setCity_url(cityName);
        String url = URL.getCity_url();

        RequestQueue requestQueue = Volley.newRequestQueue(HomeActivity.this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        LocationCord.lat = response.getJSONObject("coord").getString("lat");
                        LocationCord.lon = response.getJSONObject("coord").getString("lon");

                        getTodayWeatherInfo(cityName);
                        binding.layout.cityEt.setText("");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> Toaster.errorToast(this, "Please enter the correct city name")
        );
        requestQueue.add(jsonObjectRequest);
    }

    @SuppressLint("DefaultLocale")
    private void getTodayWeatherInfo(String name) {
        Log.d(TAG, "=== getTodayWeatherInfo START for: " + name + " ===");

        // Validate coordinates
        if (LocationCord.lat == null || LocationCord.lat.trim().isEmpty() ||
                LocationCord.lon == null || LocationCord.lon.trim().isEmpty()) {
            Log.e(TAG, "Invalid coordinates - lat: " + LocationCord.lat + ", lon: " + LocationCord.lon);
            Toaster.errorToast(this, "Location data not available");
            hideProgressBar();
            return;
        }

        try {
            double latDouble = Double.parseDouble(LocationCord.lat);
            double lonDouble = Double.parseDouble(LocationCord.lon);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid coordinate format: " + e.getMessage());
            Toaster.errorToast(this, "Invalid location coordinates");
            hideProgressBar();
            return;
        }

        URL url = new URL();
        String apiUrl = url.getLink();

        if (apiUrl == null || apiUrl.isEmpty()) {
            Toaster.errorToast(this, "API URL not configured");
            hideProgressBar();
            return;
        }

        Log.d(TAG, "API URL: " + apiUrl);
        runOnUiThread(() -> binding.progress.setVisibility(View.VISIBLE));

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                apiUrl,
                null,
                response -> {
                    try {
                        // Check response code
                        if (response.has("cod")) {
                            String cod = String.valueOf(response.get("cod"));
                            if (!cod.equals("200")) {
                                String message = response.has("message")
                                        ? response.getString("message")
                                        : "Unknown error";
                                Toaster.errorToast(this, "API error: " + message);
                                hideProgressBar();
                                return;
                            }
                        }

                        this.name = name;

                        // Get timestamp
                        update_time = response.getLong("dt");
                        updated_at = new SimpleDateFormat("EEEE hh:mm a", Locale.ENGLISH)
                                .format(new Date(update_time * 1000));
                        Log.d(TAG, "Updated at: " + updated_at);

                        // Get main weather data
                        org.json.JSONObject main = response.getJSONObject("main");
                        double temp = main.getDouble("temp");
                        double tempMin = main.getDouble("temp_min");
                        double tempMax = main.getDouble("temp_max");
                        int pressureVal = main.getInt("pressure");
                        int humidityVal = main.getInt("humidity");

                        // Get wind data
                        org.json.JSONObject wind = response.getJSONObject("wind");
                        double windSpeedVal = wind.getDouble("speed");

                        // Get sunrise/sunset
                        org.json.JSONObject sys = response.getJSONObject("sys");
                        sunrise = sys.getLong("sunrise");
                        sunset = sys.getLong("sunset");

                        // Get weather condition
                        org.json.JSONObject weather0 = response.getJSONArray("weather").getJSONObject(0);
                        condition = weather0.getInt("id");
                        description = weather0.getString("description");

                        // Format all values
                        temperature = String.valueOf(Math.round(temp));
                        min_temperature = String.valueOf(Math.round(tempMin));
                        max_temperature = String.valueOf(Math.round(tempMax));
                        pressure = String.valueOf(pressureVal);
                        wind_speed = String.format(Locale.getDefault(), "%.1f", windSpeedVal * 3.6);
                        humidity = String.valueOf(humidityVal);

                        Log.d(TAG, "Weather Data - Temp: " + temperature + "°C, Condition: " + condition + ", Desc: " + description);

                        // Update UI
                        updateUI();
                        hideProgressBar();
                        setUpDaysRecyclerView();

                        Toaster.successToast(this, "Weather updated for " + name);

                        // Save data
                        saveLastSearchedCity(name, LocationCord.lat, LocationCord.lon);
                        saveWeatherToDatabase(name);
                        CityRecommendationHelper.addRecentCity(this, name);
                        updateCityHistory();

                    } catch (org.json.JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage(), e);
                        e.printStackTrace();
                        hideProgressBar();
                        Toaster.errorToast(this, "Error parsing weather data");
                    }
                },
                error -> {
                    Log.e(TAG, "Weather API error: " + error.toString());
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        Log.e(TAG, "Status code: " + statusCode);

                        if (statusCode == 401) {
                            Toaster.errorToast(this, "Invalid API key");
                        } else if (statusCode == 404) {
                            Toaster.errorToast(this, "Location not found");
                        } else if (statusCode == 429) {
                            Toaster.errorToast(this, "API limit exceeded");
                        } else {
                            Toaster.errorToast(this, "Failed to fetch weather data");
                        }
                    } else {
                        Toaster.errorToast(this, "Network error");
                    }
                    hideProgressBar();
                }
        );

        jsonObjectRequest.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                15000,
                com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(jsonObjectRequest);
    }

    @SuppressLint("SetTextI18n")
    private void updateUI() {
        try {
            // City name
            binding.layout.nameTv.setText(name != null ? name : "Unknown");

            // Updated time with translation
            String translatedDate = updated_at != null ? translate(updated_at) : "";
            binding.layout.updatedAtTv.setText(translatedDate);

            // Weather icon
            int iconId = getResources().getIdentifier(
                    UpdateUI.getIconID(condition, update_time, sunrise, sunset),
                    "drawable",
                    getPackageName()
            );
            if (iconId != 0) {
                binding.layout.conditionIv.setImageResource(iconId);
            } else {
                Log.w(TAG, "Icon not found, using default");
                binding.layout.conditionIv.setImageResource(R.drawable.clear_day);
            }

            // Weather description
            binding.layout.conditionDescTv.setText(description != null ? description : "No data");

            // Main temperature - FIXED: Ensure temperature is set
            String tempDisplay = (temperature != null && !temperature.isEmpty()) ? temperature + "°C" : "0°C";
            binding.layout.tempTv.setText(tempDisplay);
            Log.d(TAG, "Temperature displayed: " + tempDisplay);

            // Min/Max temperatures
            binding.layout.minTempTv.setText((min_temperature != null ? min_temperature : "0") + "°C");
            binding.layout.maxTempTv.setText((max_temperature != null ? max_temperature : "0") + "°C");

            // Weather details
            binding.layout.pressureTv.setText((pressure != null ? pressure : "0") + " mb");
            binding.layout.windTv.setText((wind_speed != null ? wind_speed : "0") + " km/h");
            binding.layout.humidityTv.setText((humidity != null ? humidity : "0") + "%");

            Log.d(TAG, "UI Update Complete - Temp: " + tempDisplay + ", Desc: " + description);
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
        }
    }

    private String translate(String dayToTranslate) {
        String[] dayToTranslateSplit = dayToTranslate.split(" ");
        dayToTranslateSplit[0] = UpdateUI.TranslateDay(dayToTranslateSplit[0].trim(), getApplicationContext());
        return dayToTranslateSplit[0].concat(" " + dayToTranslateSplit[1]);
    }

    private void hideProgressBar() {
        binding.progress.setVisibility(View.GONE);
        binding.layout.mainLayout.setVisibility(View.VISIBLE);
    }

    private void hideMainLayout() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.layout.mainLayout.setVisibility(View.GONE);
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void checkConnection() {
        if (!isInternetConnected(this)) {
            hideMainLayout();
            Toaster.errorToast(this, "Please check your internet connection");
        } else {
            hideProgressBar();
            getDataUsingNetwork();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_language) {
            showLanguageDialog();
            return true;
        } else if (id == R.id.action_clear_cache) {
            clearCache();
            return true;
        } else if (id == R.id.action_clear_history) {
            clearHistory();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLanguageDialog() {
        String[] languages = {"Bahasa Indonesia", "English"};
        String currentLang = LanguageManager.getLanguage(this);
        int selectedIndex = currentLang.equals("id") ? 0 : 1;

        new AlertDialog.Builder(this)
                .setTitle("Pilih Bahasa / Select Language")
                .setSingleChoiceItems(languages, selectedIndex, (dialog, which) -> {
                    String langCode = which == 0 ? "id" : "en";
                    LanguageManager.setLanguage(this, langCode);
                    recreate();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearCache() {
        weatherRepository.deleteAll(new WeatherRepository.OnDeleteListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() ->
                        Toaster.successToast(HomeActivity.this, "Cache cleared"));
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toaster.errorToast(HomeActivity.this, "Failed to clear cache"));
            }
        });
    }

    private void clearHistory() {
        CityRecommendationHelper.clearHistory(this);
        binding.layout.historySection.setVisibility(View.GONE);
        Toaster.successToast(this, "History cleared");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toaster.successToast(this, "Permission Granted");
                getDataUsingNetwork();
            } else {
                Toaster.errorToast(this, "Permission Denied");
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkConnection();
        updateCityHistory();

        // Update device location untuk notifikasi setiap app dibuka
        getDeviceLocationForNotification();
    }

    private void checkUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            this,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build(),
                            WEATHER_FORECAST_APP_UPDATE_REQ_CODE
                    );
                } catch (IntentSender.SendIntentException e) {
                    Toaster.errorToast(this, "Update Failed");
                }
            }
        });
    }
}