
package com.intelmob.trackme;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.arch.lifecycle.LifecycleService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.intelmob.trackme.db.AppDatabase;
import com.intelmob.trackme.db.WorkoutSession;
import com.intelmob.trackme.ui.view.WorkoutActivity;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class WorkoutService extends LifecycleService {

    private static final String ACTION_START_NEW_RECORDING = "action_start_new_recording";
    private static final String ACTION_PAUSE_RECORDING = "action_pause_recording";
    private static final String ACTION_RESUME_RECORDING = "action_resume_recording";
    private static final String ACTION_STOP_RECORDING = "action_stop_recording";
    private static final String ACTION_START_LOCATION_UPDATE = "action_start_location_update";

    public static void startNewRecording(Context context) {
        Intent intent = new Intent(context, WorkoutService.class);
        intent.setAction(ACTION_START_NEW_RECORDING);
        context.startService(intent);
    }

    public static void pauseRecording(Context context) {
        Intent intent = new Intent(context, WorkoutService.class);
        intent.setAction(ACTION_PAUSE_RECORDING);
        context.startService(intent);
    }

    public static void resumeRecording(Context context) {
        Intent intent = new Intent(context, WorkoutService.class);
        intent.setAction(ACTION_RESUME_RECORDING);
        context.startService(intent);
    }

    public static void stopRecording(Context context) {
        Intent intent = new Intent(context, WorkoutService.class);
        intent.setAction(ACTION_STOP_RECORDING);
        context.startService(intent);
    }

    public static void startLocationUpdates(Context context) {
        Intent intent = new Intent(context, WorkoutService.class);
        intent.setAction(ACTION_START_LOCATION_UPDATE);
        context.startService(intent);
    }

    private AppDatabase appDatabase;

    private FusedLocationProviderClient mLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);

            if (locationResult == null) {
                return;
            }

            Location location = locationResult.getLocations().get(0);
            double currentLat = location.getLatitude();
            double currentLon = location.getLongitude();
            float speed = location.getSpeed();
            float speedKPH = speed / 1000 * 3600;

            getRecordingWorkoutSession()
                    .map(workoutSession -> {
                        if (workoutSession.travelRoutes == null) {
                            workoutSession.travelRoutes = new ArrayList<>();
                        }

                        workoutSession.travelRoutes.add(new LatLng(currentLat, currentLon));
                        int points = workoutSession.travelRoutes.size();

                        workoutSession.speedKPH = speedKPH;
                        workoutSession.avgSpeed = (workoutSession.avgSpeed + speedKPH) / points;
                        if (points > 1) {
                            LatLng lastPoint = workoutSession.travelRoutes.get(points - 2);
                            float[] results = new float[3];
                            Location.distanceBetween(currentLat, currentLon, lastPoint.latitude,
                                    lastPoint.longitude, results);
                            float distance = results[0];
                            float distanceKM = distance / 1000;
                            workoutSession.distance += distanceKM;
                        }

                        appDatabase.workoutModel().updateWorkoutSession(workoutSession);

                        return true;
                    }).subscribe(new SubscriberImpl<>());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        appDatabase = AppDatabase.getDatabase(this.getApplication());

        mLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setSmallestDisplacement(10);
            mLocationRequest.setInterval(10000);
            mLocationRequest.setFastestInterval(5000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopDurationCounter();
        stopLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_START_NEW_RECORDING:
                    internalStartNewRecording();
                    showNotification();
                    break;
                case ACTION_PAUSE_RECORDING:
                    internalPauseRecording();
                    break;
                case ACTION_RESUME_RECORDING:
                    internalResumeRecording();
                    break;
                case ACTION_STOP_RECORDING:
                    internalStopRecording();
                    break;
                case ACTION_START_LOCATION_UPDATE:
                    startLocationUpdates();
                    break;
            }
        }

        return START_STICKY;
    }

    private void internalStartNewRecording() {
        getRecordingWorkoutSession()
                .map(recordingSession -> {
                    recordingSession.isRecording = true;
                    recordingSession.distance = 0;
                    recordingSession.avgSpeed = 0;
                    recordingSession.duration = 0;
                    recordingSession.travelRoutes = null;
                    appDatabase.workoutModel().updateWorkoutSession(recordingSession);

                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SubscriberImpl<Boolean>() {
                    @Override
                    public void onNext(Boolean success) {
                        startDurationCounter();
                    }
                });
    }

    private void internalPauseRecording() {
        stopLocationUpdates();
        stopDurationCounter();
        stopForeground(true);
    }

    private void internalResumeRecording() {
        startLocationUpdates();
        startDurationCounter();
        showNotification();
    }

    private void internalStopRecording() {
        stopDurationCounter();

        getRecordingWorkoutSession()
                .map(recordingSession -> {
                    recordingSession.isRecording = false;
                    appDatabase.workoutModel().updateWorkoutSession(recordingSession);

                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SubscriberImpl<Boolean>() {
                    @Override
                    public void onNext(Boolean success) {
                        stopLocationUpdates();
                        stopForeground(true);
                        stopSelf();
                    }
                });
    }

    private Flowable<WorkoutSession> getRecordingWorkoutSession() {
        return Flowable.just(appDatabase)
                .subscribeOn(Schedulers.io())
                .map(db -> {
                    WorkoutSession recordingSession = db.workoutModel()
                            .getRecordingWorkoutSessionSync();
                    if (recordingSession == null) {
                        recordingSession = new WorkoutSession();
                        recordingSession.dateCreated = System.currentTimeMillis();
                        recordingSession.isRecording = true;
                        appDatabase.workoutModel().addWorkoutSession(recordingSession);

                        recordingSession = appDatabase.workoutModel()
                                .getRecordingWorkoutSessionSync();
                    }
                    return recordingSession;
                });
    }

    private Disposable disposable;

    public void startDurationCounter() {
        stopDurationCounter();

        disposable = Flowable.interval(1, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .map(timeInterval -> appDatabase.workoutModel().getRecordingWorkoutSessionSync())
                .map(recordingWorkoutSession -> {
                    recordingWorkoutSession.duration++;
                    appDatabase.workoutModel().updateWorkoutSession(recordingWorkoutSession);
                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new SubscriberImpl<>());
    }

    public void stopDurationCounter() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private void showNotification() {
        String title = getString(R.string.app_name);
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);
        Intent notificationIntent = new Intent(this, WorkoutActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, "1")
                .setContentTitle(title)
                .setTicker(title)
                .setContentText("Running")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        startForeground(12345, notification);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        mLocationProviderClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    private void stopLocationUpdates() {
        if (mLocationProviderClient != null)
            mLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}
