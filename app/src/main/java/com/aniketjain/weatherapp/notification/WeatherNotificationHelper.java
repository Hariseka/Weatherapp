package com.aniketjain.weatherapp.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.aniketjain.weatherapp.HomeActivity;
import com.aniketjain.weatherapp.R;

public class WeatherNotificationHelper {
    private static final String CHANNEL_ID = "weather_channel";
    private static final String CHANNEL_NAME = "Weather Notifications";
    private static final int NOTIFICATION_ID = 1001;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Daily weather forecast notifications");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void sendWeatherNotification(Context context, String cityName,
                                               String temperature, String condition,
                                               String customMessage) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, flags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.clear_day)
                .setContentTitle("Cuaca Hari Ini - " + cityName)
                .setContentText(temperature + " • " + condition)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(customMessage + "\n\n" +
                                "📍 " + cityName + "\n" +
                                "🌡️ Suhu: " + temperature + "\n" +
                                "☁️ Kondisi: " + condition))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    // Overload method untuk backward compatibility
    public static void sendWeatherNotification(Context context, String cityName,
                                               String temperature, String condition) {
        sendWeatherNotification(context, cityName, temperature, condition,
                "Apapun cuacanya, semangat beraktivitas hari ini! 💪");
    }
}