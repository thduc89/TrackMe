package com.intelmob.trackme.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.os.AsyncTask;

import com.intelmob.trackme.db.AppDatabase;
import com.intelmob.trackme.db.WorkoutSession;

public class AddWorkoutSessionViewModel extends AndroidViewModel {

    private AppDatabase appDatabase;

    public AddWorkoutSessionViewModel(Application application) {
        super(application);
        appDatabase = AppDatabase.getDatabase(this.getApplication());
    }

    public void addWorkoutSession(final WorkoutSession workoutSession) {
        new AddAsyncTask(appDatabase).execute(workoutSession);
    }

    private static class AddAsyncTask extends AsyncTask<WorkoutSession, Void, Void> {

        private AppDatabase db;

        AddAsyncTask(AppDatabase appDatabase) {
            db = appDatabase;
        }

        @Override
        protected Void doInBackground(final WorkoutSession... params) {
            db.workoutModel().addWorkoutSession(params[0]);
            return null;
        }

    }
}