
package com.intelmob.trackme.db;

import android.arch.persistence.room.TypeConverter;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class LocationConverter {

    @TypeConverter
    public static List<LatLng> fromString(String value) {
        return new Gson().fromJson(value, new TypeToken<List<LatLng>>() {
        }.getType());
    }

    @TypeConverter
    public static String fromList(List<LatLng> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

}
