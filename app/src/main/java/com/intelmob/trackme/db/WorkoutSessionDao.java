package com.intelmob.trackme.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface WorkoutSessionDao {

    @Query("SELECT * FROM WorkoutSession")
    LiveData<List<WorkoutSession>> getAllWorkoutSessions();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addWorkoutSession(WorkoutSession workoutSession);
}
