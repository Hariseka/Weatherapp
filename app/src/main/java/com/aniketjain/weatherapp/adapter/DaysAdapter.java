package com.aniketjain.weatherapp.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.aniketjain.weatherapp.R;
import com.aniketjain.weatherapp.location.LocationCord;
import com.aniketjain.weatherapp.update.UpdateUI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DaysAdapter extends RecyclerView.Adapter<DaysAdapter.DayViewHolder> {

    private static final String TAG = "DaysAdapter";
    private final Context context;
    private final List<DayWeatherData> weatherList;

    public DaysAdapter(Context context) {
        this.context = context;
        this.weatherList = new ArrayList<>();
        fetchWeatherForecast();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.day_item_layout, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        if (position < weatherList.size()) {
            DayWeatherData weather = weatherList.get(position);
            holder.bind(weather, position, context);
        }
    }

    @Override
    public int getItemCount() {
        return weatherList.size();
    }

    private void fetchWeatherForecast() {
        if (LocationCord.lat == null || LocationCord.lon == null) {
            Log.e(TAG, "Invalid coordinates");
            return;
        }

        String url = "https://api.openweathermap.org/data/2.5/forecast?lat=" +
                LocationCord.lat + "&lon=" + LocationCord.lon +
                "&appid=" + LocationCord.API_KEY + "&units=metric";

        RequestQueue requestQueue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        processWeatherData(response);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Forecast API error: " + error.toString())
        );

        requestQueue.add(jsonObjectRequest);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void processWeatherData(JSONObject response) throws JSONException {
        JSONArray list = response.getJSONArray("list");
        weatherList.clear();

        String currentDate = "";
        int dayCount = 0;

        for (int i = 0; i < list.length() && dayCount < 7; i++) {
            JSONObject forecast = list.getJSONObject(i);
            long timestamp = forecast.getLong("dt");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String forecastDate = sdf.format(new Date(timestamp * 1000));

            SimpleDateFormat timeSdf = new SimpleDateFormat("HH", Locale.getDefault());
            int hour = Integer.parseInt(timeSdf.format(new Date(timestamp * 1000)));

            if (!forecastDate.equals(currentDate) && (hour >= 11 && hour <= 13)) {
                currentDate = forecastDate;

                JSONObject main = forecast.getJSONObject("main");
                int tempMin = (int) Math.round(main.getDouble("temp_min"));
                int tempMax = (int) Math.round(main.getDouble("temp_max"));
                int pressure = main.getInt("pressure");
                int humidity = main.getInt("humidity");

                JSONObject wind = forecast.getJSONObject("wind");
                double windSpeed = wind.getDouble("speed") * 3.6;

                JSONObject weather = forecast.getJSONArray("weather").getJSONObject(0);
                int conditionId = weather.getInt("id");

                long sunrise = response.getJSONObject("city").getLong("sunrise");
                long sunset = response.getJSONObject("city").getLong("sunset");

                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                String dayName = dayFormat.format(new Date(timestamp * 1000));

                if (dayCount == 0) {
                    dayName = "Hari Ini";
                }

                DayWeatherData dayWeather = new DayWeatherData(
                        dayName, tempMin, tempMax, conditionId, pressure,
                        windSpeed, humidity, timestamp, sunrise, sunset
                );

                weatherList.add(dayWeather);
                dayCount++;
            }
        }

        notifyDataSetChanged();
        Log.d(TAG, "Loaded " + weatherList.size() + " days forecast");
    }

    // ViewHolder
    public static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayTime, minTemp, maxTemp, pressure, wind, humidity;
        ImageView icon;
        ProgressBar progressBar;
        LinearLayout contentLayout, errorLayout;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTime = itemView.findViewById(R.id.day_time);
            minTemp = itemView.findViewById(R.id.day_min_temp);
            maxTemp = itemView.findViewById(R.id.day_max_temp);
            icon = itemView.findViewById(R.id.day_icon);
            pressure = itemView.findViewById(R.id.day_pressure);
            wind = itemView.findViewById(R.id.day_wind);
            humidity = itemView.findViewById(R.id.day_humidity);
            progressBar = itemView.findViewById(R.id.day_progress_bar);
            contentLayout = itemView.findViewById(R.id.day_relative_layout);
            errorLayout = itemView.findViewById(R.id.day_error_layout);
        }

        @SuppressLint("DefaultLocale")
        public void bind(DayWeatherData weather, int position, Context context) {
            progressBar.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);
            errorLayout.setVisibility(View.GONE);

            dayTime.setText(weather.dayName);
            minTemp.setText(weather.tempMin + "°C");
            maxTemp.setText(weather.tempMax + "°C");
            pressure.setText(weather.pressure + " mb");
            wind.setText(String.format("%.1f km/h", weather.windSpeed));
            humidity.setText(weather.humidity + "%");

            String iconName = UpdateUI.getIconID(
                    weather.conditionId,
                    weather.timestamp,
                    weather.sunrise,
                    weather.sunset
            );

            int iconId = context.getResources().getIdentifier(
                    iconName, "drawable", context.getPackageName()
            );

            if (iconId != 0) {
                icon.setImageResource(iconId);
            } else {
                icon.setImageResource(R.drawable.clear_day);
            }

            Log.d(TAG, "Day " + position + ": " + weather.dayName +
                    " | " + weather.tempMin + "-" + weather.tempMax + "°C");
        }
    }

    // Data Model
    static class DayWeatherData {
        String dayName;
        int tempMin;
        int tempMax;
        int conditionId;
        int pressure;
        double windSpeed;
        int humidity;
        long timestamp;
        long sunrise;
        long sunset;

        public DayWeatherData(String dayName, int tempMin, int tempMax,
                              int conditionId, int pressure, double windSpeed,
                              int humidity, long timestamp, long sunrise, long sunset) {
            this.dayName = dayName;
            this.tempMin = tempMin;
            this.tempMax = tempMax;
            this.conditionId = conditionId;
            this.pressure = pressure;
            this.windSpeed = windSpeed;
            this.humidity = humidity;
            this.timestamp = timestamp;
            this.sunrise = sunrise;
            this.sunset = sunset;
        }
    }
}