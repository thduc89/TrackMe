package com.intelmob.trackme.db;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.intelmob.trackme.db.model.WorkoutSession;

@Database(entities = {WorkoutSession.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "trackme_db";
    private static AppDatabase INSTANCE;

    public static AppDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DB_NAME).build();
        }
        return INSTANCE;
    }

    public abstract WorkoutSessionDao workoutModel();

}