package com.aniketjain.weatherapp.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface WeatherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertWeather(WeatherEntity weather);

    @Update
    void updateWeather(WeatherEntity weather);

    @Query("SELECT * FROM weather_cache WHERE cityName = :cityName LIMIT 1")
    WeatherEntity getWeatherByCity(String cityName);

    @Query("SELECT * FROM weather_cache WHERE latitude = :lat AND longitude = :lon LIMIT 1")
    WeatherEntity getWeatherByCoordinates(double lat, double lon);

    @Query("SELECT * FROM weather_cache ORDER BY timestamp DESC")
    List<WeatherEntity> getAllWeather();

    @Query("SELECT * FROM weather_cache ORDER BY timestamp DESC LIMIT :limit")
    List<WeatherEntity> getRecentWeather(int limit);

    @Query("DELETE FROM weather_cache WHERE id = :id")
    void deleteWeather(int id);

    @Query("DELETE FROM weather_cache")
    void deleteAll();

    @Query("DELETE FROM weather_cache WHERE timestamp < :expireTime")
    void deleteExpiredWeather(long expireTime);

    @Query("SELECT COUNT(*) FROM weather_cache")
    int getWeatherCount();
}