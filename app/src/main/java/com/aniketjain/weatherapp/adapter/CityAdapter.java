package com.aniketjain.weatherapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aniketjain.weatherapp.R;
import com.aniketjain.weatherapp.model.City;

import java.util.ArrayList;
import java.util.List;

public class CityAdapter extends ArrayAdapter<City> {
    private final List<City> citiesOriginal;
    private List<City> citiesFiltered;
    private final LayoutInflater inflater;

    public CityAdapter(@NonNull Context context, @NonNull List<City> cities) {
        super(context, 0, cities);
        this.citiesOriginal = new ArrayList<>(cities);
        this.citiesFiltered = new ArrayList<>(cities);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return citiesFiltered.size();
    }

    @Nullable
    @Override
    public City getItem(int position) {
        return citiesFiltered.get(position);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
        }

        TextView textView = convertView.findViewById(android.R.id.text1);
        City city = getItem(position);

        if (city != null) {
            textView.setText(city.getName() + ", " + city.getCountry());
            textView.setPadding(32, 24, 32, 24);
        }

        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<City> suggestions = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    suggestions.addAll(citiesOriginal);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();

                    for (City city : citiesOriginal) {
                        if (city.getName().toLowerCase().startsWith(filterPattern)) {
                            suggestions.add(city);
                        }
                    }
                }

                results.values = suggestions;
                results.count = suggestions.size();
                return results;
            }

            @SuppressWarnings("unchecked")  // Tambahkan ini untuk suppress warning
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                citiesFiltered.clear();
                if (results.values != null) {
                    citiesFiltered.addAll((List<City>) results.values);
                }
                notifyDataSetChanged();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                return ((City) resultValue).getName();
            }
        };
    }
}