package com.aniketjain.weatherapp.database;

import android.content.Context;
import android.util.Log;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WeatherRepository {
    private static final String TAG = "WeatherRepository";
    private final WeatherDao weatherDao;
    private final ExecutorService executorService;

    public WeatherRepository(Context context) {
        WeatherDatabase database = WeatherDatabase.getInstance(context);
        weatherDao = database.weatherDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void insertWeather(WeatherEntity weather, OnWeatherSavedListener listener) {
        executorService.execute(() -> {
            try {
                long id = weatherDao.insertWeather(weather);
                weather.setId((int) id);
                Log.d(TAG, "Weather data saved with ID: " + id);

                if (listener != null) {
                    listener.onSuccess(weather);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving weather: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    public void getWeatherByCity(String cityName, OnWeatherLoadedListener listener) {
        executorService.execute(() -> {
            try {
                WeatherEntity weather = weatherDao.getWeatherByCity(cityName);

                if (weather != null && weather.isValid()) {
                    Log.d(TAG, "Valid cached weather found for: " + cityName);
                    if (listener != null) {
                        listener.onSuccess(weather);
                    }
                } else {
                    Log.d(TAG, "No valid cached weather for: " + cityName);
                    if (listener != null) {
                        listener.onNotFound();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading weather: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    public void getWeatherByCoordinates(double lat, double lon, OnWeatherLoadedListener listener) {
        executorService.execute(() -> {
            try {
                WeatherEntity weather = weatherDao.getWeatherByCoordinates(lat, lon);

                if (weather != null && weather.isValid()) {
                    Log.d(TAG, "Valid cached weather found for coordinates");
                    if (listener != null) {
                        listener.onSuccess(weather);
                    }
                } else {
                    Log.d(TAG, "No valid cached weather for coordinates");
                    if (listener != null) {
                        listener.onNotFound();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading weather: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    public void getRecentWeather(int limit, OnWeatherListLoadedListener listener) {
        executorService.execute(() -> {
            try {
                List<WeatherEntity> weatherList = weatherDao.getRecentWeather(limit);
                Log.d(TAG, "Loaded " + weatherList.size() + " recent weather records");

                if (listener != null) {
                    listener.onSuccess(weatherList);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading recent weather: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    public void deleteAll(OnDeleteListener listener) {
        executorService.execute(() -> {
            try {
                weatherDao.deleteAll();
                Log.d(TAG, "All weather data deleted");

                if (listener != null) {
                    listener.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting weather: " + e.getMessage());
                if (listener != null) {
                    listener.onError(e.getMessage());
                }
            }
        });
    }

    public interface OnWeatherSavedListener {
        void onSuccess(WeatherEntity weather);
        void onError(String error);
    }

    public interface OnWeatherLoadedListener {
        void onSuccess(WeatherEntity weather);
        void onNotFound();
        void onError(String error);
    }

    public interface OnWeatherListLoadedListener {
        void onSuccess(List<WeatherEntity> weatherList);
        void onError(String error);
    }

    public interface OnDeleteListener {
        void onSuccess();
        void onError(String error);
    }
}