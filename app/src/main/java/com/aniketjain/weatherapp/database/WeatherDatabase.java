package com.aniketjain.weatherapp.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {WeatherEntity.class}, version = 1, exportSchema = false)
public abstract class WeatherDatabase extends RoomDatabase {

    private static WeatherDatabase instance;
    private static final String DATABASE_NAME = "weather_database";

    public abstract WeatherDao weatherDao();

    public static synchronized WeatherDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            WeatherDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public static void cleanExpiredCache(Context context) {
        new Thread(() -> {
            WeatherDatabase db = getInstance(context);
            long expireTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
            db.weatherDao().deleteExpiredWeather(expireTime);
        }).start();
    }
}