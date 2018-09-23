package com.intelmob.trackme.ui.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import com.intelmob.trackme.db.AppDatabase;
import com.intelmob.trackme.db.WorkoutSession;

public class WorkoutSessionViewModel extends AndroidViewModel {


    private final LiveData<WorkoutSession> recordingWorkoutSession;

    private AppDatabase appDatabase;

    public WorkoutSessionViewModel(Application application) {
        super(application);
        appDatabase = AppDatabase.getDatabase(this.getApplication());
        recordingWorkoutSession = appDatabase.workoutModel().getRecordingWorkoutSession();
    }
    
    public LiveData<WorkoutSession> getRecordingWorkoutSession() {
        return recordingWorkoutSession;
    }

    public WorkoutSession getRecordingWorkoutSessionSync(){
        return appDatabase.workoutModel().getRecordingWorkoutSessionSync();
    }

}
