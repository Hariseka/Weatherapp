package com.aniketjain.weatherapp.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "weather_cache")
public class WeatherEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String cityName;
    private double latitude;
    private double longitude;
    private String temperature;
    private String minTemperature;
    private String maxTemperature;
    private String condition;
    private String description;
    private String pressure;
    private String windSpeed;
    private String humidity;
    private long timestamp;
    private String weatherJson;

    public WeatherEntity() {
    }

    public WeatherEntity(String cityName, double latitude, double longitude,
                         String temperature, String minTemperature, String maxTemperature,
                         String condition, String description, String pressure,
                         String windSpeed, String humidity, long timestamp, String weatherJson) {
        this.cityName = cityName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.temperature = temperature;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.condition = condition;
        this.description = description;
        this.pressure = pressure;
        this.windSpeed = windSpeed;
        this.humidity = humidity;
        this.timestamp = timestamp;
        this.weatherJson = weatherJson;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(String minTemperature) {
        this.minTemperature = minTemperature;
    }

    public String getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(String maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPressure() {
        return pressure;
    }

    public void setPressure(String pressure) {
        this.pressure = pressure;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getWeatherJson() {
        return weatherJson;
    }

    public void setWeatherJson(String weatherJson) {
        this.weatherJson = weatherJson;
    }

    // Helper method untuk cek apakah data masih valid (< 30 menit)
    public boolean isValid() {
        long currentTime = System.currentTimeMillis();
        long thirtyMinutes = 30 * 60 * 1000;
        return (currentTime - timestamp) < thirtyMinutes;
    }
}