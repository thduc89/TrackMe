package com.intelmob.trackme.ui.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import com.intelmob.trackme.db.AppDatabase;
import com.intelmob.trackme.db.model.WorkoutSession;

import java.util.List;

public class WorkoutSessionListViewModel extends AndroidViewModel {

    private final LiveData<List<WorkoutSession>> workoutSessionList;

    private AppDatabase appDatabase;

    public WorkoutSessionListViewModel(Application application) {
        super(application);
        appDatabase = AppDatabase.getDatabase(this.getApplication());
        workoutSessionList = appDatabase.workoutModel().getAllWorkoutSessions();
    }


    public LiveData<List<WorkoutSession>> getWorkoutSessionList() {
        return workoutSessionList;
    }

}