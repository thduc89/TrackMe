package com.intelmob.trackme.db;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

@Entity
public class WorkoutSession {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String imagePath;
    public float distance;
    public float avgSpeed;
    public int duration;
}
