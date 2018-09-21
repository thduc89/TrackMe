package com.intelmob.trackme.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;

import com.intelmob.trackme.db.WorkoutSession;

import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class WorkoutSessionViewModel extends AndroidViewModel {

    private final MutableLiveData<WorkoutSession> workoutSession;
    private WorkoutSession currentWorkoutSession;
    private Disposable disposable;

    public WorkoutSessionViewModel(@NonNull Application application) {
        super(application);
        workoutSession = new MutableLiveData<>();
        currentWorkoutSession = new WorkoutSession();
        workoutSession.setValue(currentWorkoutSession);
    }


    public void startDurationCounter() {
        stopDurationCounter();

        disposable = Flowable.interval(1, TimeUnit.SECONDS)
                .doOnNext(seconds -> {
                    currentWorkoutSession.duration++;
                    workoutSession.postValue(currentWorkoutSession);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
    }

    public void stopDurationCounter() {
        if (disposable != null) {
            disposable.dispose();
        }
    }

    public LiveData<WorkoutSession> getWorkoutSession() {
        return workoutSession;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopDurationCounter();
    }
}
