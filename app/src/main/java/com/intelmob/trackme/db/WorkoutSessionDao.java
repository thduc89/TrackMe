
package com.intelmob.trackme.db;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.intelmob.trackme.db.model.WorkoutSession;

import java.util.List;

@Dao
public interface WorkoutSessionDao {

    @Query("SELECT * FROM WorkoutSession WHERE recordingState = 0 ORDER BY dateCreated DESC")
    LiveData<List<WorkoutSession>> getAllWorkoutSessions();

    @Query("SELECT * FROM WorkoutSession WHERE recordingState > 0 LIMIT 1")
    LiveData<WorkoutSession> getRecordingWorkoutSession();

    @Query("SELECT * FROM WorkoutSession WHERE recordingState > 0 LIMIT 1")
    WorkoutSession getRecordingWorkoutSessionSync();

    @Update
    void updateWorkoutSession(WorkoutSession workoutSession);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long addWorkoutSession(WorkoutSession workoutSession);

    @Delete
    void deleteWorkoutSession(WorkoutSession workoutSession);
}
