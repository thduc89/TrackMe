
package com.intelmob.trackme.db;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

@Entity
@TypeConverters(LocationConverter.class)
public class WorkoutSession {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public long dateCreated;
    public float distance;
    public float avgSpeed;
    public int duration;

    public List<LatLng> travelRoutes;

    /** Current workout session or not */
    public boolean isRecording = false;
}
