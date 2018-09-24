
package com.intelmob.trackme.ui.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.intelmob.trackme.BuildConfig;
import com.intelmob.trackme.R;
import com.intelmob.trackme.SubscriberImpl;
import com.intelmob.trackme.WorkoutService;
import com.intelmob.trackme.db.RecordingState;
import com.intelmob.trackme.db.model.TravelPoint;
import com.intelmob.trackme.db.model.WorkoutSession;
import com.intelmob.trackme.ui.viewmodel.WorkoutSessionViewModel;
import com.intelmob.trackme.util.AlertUtils;
import com.intelmob.trackme.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class WorkoutActivity extends AppCompatActivity
        implements View.OnClickListener, OnMapReadyCallback {

    private static final int LOCATION_RC = 999;
    private static final int DEFAULT_ZOOM_LEVEL = 14;
    private static final int DEFAULT_LINE_WIDTH = 12;
    private static final int DEFAULT_BOUNDS_PADDING = 100;
    private static final int DEFAULT_ANIMATION_DURATION = 300;

    private ImageButton btnBack;
    private ImageButton btnPause;
    private ImageButton btnResume;
    private ImageButton btnStop;
    private Group groupResumeStop;

    private TextView tvDistance;
    private TextView tvSpeed;
    private TextView tvDuration;

    private View loadingView;

    private GoogleMap mMap;

    private int mWorkoutSessionId = -1;
    private boolean startMarkerAdded;
    private List<TravelPoint> mTravelPoints;

    private WorkoutSessionViewModel mViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.workout_mapView);
        mapFragment.getMapAsync(this);

        btnBack = findViewById(R.id.workout_btnBack);
        btnPause = findViewById(R.id.workout_btnPause);
        btnResume = findViewById(R.id.workout_btnResume);
        btnStop = findViewById(R.id.workout_btnStop);
        groupResumeStop = findViewById(R.id.groupResumeStop);

        tvDistance = findViewById(R.id.workout_info_tvDistance);
        tvSpeed = findViewById(R.id.workout_info_tvSpeed);
        tvDuration = findViewById(R.id.workout_info_tvDuration);

        loadingView = findViewById(R.id.loadingView);

        btnBack.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnResume.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        mViewModel = ViewModelProviders.of(this)
                .get(WorkoutSessionViewModel.class);
        mViewModel.getRecordingWorkoutSession().observe(this, session -> {
            if (session == null) {
                return;
            }

            mWorkoutSessionId = session.id;
            toggleGroupResumeStop(session.recordingState == RecordingState.PAUSED);

            if (session.travelPoints == null || session.travelPoints.size() == 0) {
                return;
            }

            if (!startMarkerAdded) {
                startMarkerAdded = true;
                TravelPoint firstPoint = session.travelPoints.get(0);
                addMarker(firstPoint.latLng.latitude, firstPoint.latLng.longitude);
            }

            if (mTravelPoints == null) {
                mTravelPoints = new ArrayList<>();
                mTravelPoints.addAll(session.travelPoints);

                drawRoutes(mTravelPoints);
                moveCameraToBounds(mTravelPoints, true, null);

            } else if (mTravelPoints.size() < session.travelPoints.size()) {
                int fromIndex = mTravelPoints.size();
                int toIndex = session.travelPoints.size();

                List<TravelPoint> newPoints = session.travelPoints.subList(fromIndex, toIndex);
                mTravelPoints.addAll(newPoints);

                drawRoutes(mTravelPoints.subList(fromIndex - 1, toIndex));

                if (toIndex - fromIndex == 1) {
                    moveCameraToBounds(newPoints, true, null);
                }
            }

            TravelPoint lastPoint = mTravelPoints.get(mTravelPoints.size() - 1);

            tvDistance.setText(String.format(
                    getString(R.string.format_distance), session.distance));
            tvSpeed.setText(String.format(getString(R.string.format_speed), lastPoint.speedKPH));
            tvDuration.setText(Utils.formatDuration(session.duration));
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndRequestLocationPermissionIfNeed();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing()) {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.workout_btnBack:
                onBackPressed();
                break;
            case R.id.workout_btnPause:
                WorkoutService.pauseRecording(this);
                toggleGroupResumeStop(true);
                break;
            case R.id.workout_btnResume:
                WorkoutService.resumeRecording(this);
                toggleGroupResumeStop(false);
                break;
            case R.id.workout_btnStop:
                btnPause.performClick();

                Flowable.just(1)
                        .subscribeOn(Schedulers.io())
                        .map(integer -> mViewModel.getRecordingWorkoutSessionSync())
                        .filter(workoutSession -> workoutSession != null
                                && workoutSession.travelPoints != null
                                && workoutSession.travelPoints.size() > 0)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SubscriberImpl<WorkoutSession>() {
                            @Override
                            public void onNext(WorkoutSession workoutSession) {
                                if (workoutSession.travelPoints.size() > 1) {
                                    AlertUtils.showConfirmDialog(WorkoutActivity.this,
                                            getString(R.string.notice),
                                            getString(R.string.stop_workout_prompt),
                                            getString(R.string.continue_workout), null,
                                            getString(R.string.stop),
                                            (dialog, which) -> moveCameraToBounds(
                                                    workoutSession.travelPoints,
                                                    false,
                                                    new GoogleMap.CancelableCallback() {
                                                        @Override
                                                        public void onFinish() {
                                                            takeMapSnapshotAndStopRecording();
                                                        }

                                                        @Override
                                                        public void onCancel() {

                                                        }
                                                    }));
                                } else {
                                    AlertUtils.showConfirmDialog(WorkoutActivity.this,
                                            getString(R.string.notice),
                                            getString(R.string.cancel_workout_prompt),
                                            getString(R.string.continue_workout), null,
                                            getString(R.string.stop),
                                            (dialog, which) -> {
                                                WorkoutService.stopRecording(WorkoutActivity.this,
                                                        false);
                                                finish();
                                            });
                                }
                            }
                        });
                break;
        }
    }

    private void takeMapSnapshotAndStopRecording() {
        loadingView.setVisibility(View.VISIBLE);

        mMap.snapshot(bitmap -> {
            File sd = getFilesDir();
            File dest = new File(sd, String.valueOf(mWorkoutSessionId));

            try {
                FileOutputStream out = new FileOutputStream(dest);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
            } catch (Exception e) {
                Log.e("Error", e.getLocalizedMessage(), e);
                Toast.makeText(this, R.string.take_snapshot_fail, Toast.LENGTH_SHORT).show();
            }

            loadingView.setVisibility(View.GONE);

            WorkoutService.stopRecording(this, true);
            finish();
        });
    }

    private void toggleGroupResumeStop(boolean show) {
        groupResumeStop.setVisibility(show ? View.VISIBLE : View.GONE);
        btnPause.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (BuildConfig.DEBUG) {
            mMap.setOnMapLongClickListener(latLng -> {
                Location location = new Location("user");
                location.setLatitude(latLng.latitude);
                location.setLongitude(latLng.longitude);
                location.setTime(System.currentTimeMillis());

                WorkoutService.addLocation(this, location);
            });
        }

        if (isLocationPermissionGranted()) {
            onMapReadyAndLocationPermissionGranted();
        }
    }

    private void onLocationPermissionGranted() {
        if (mMap != null) {
            onMapReadyAndLocationPermissionGranted();
        }
    }

    @SuppressLint("MissingPermission")
    private void onMapReadyAndLocationPermissionGranted() {
        mMap.setMyLocationEnabled(true);

        Flowable.just(1)
                .subscribeOn(Schedulers.io())
                .map(integer -> {
                    WorkoutSession workoutSession = mViewModel.getRecordingWorkoutSessionSync();
                    return workoutSession.recordingState;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SubscriberImpl<RecordingState>() {
                    @Override
                    public void onNext(RecordingState recordingState) {
                        switch (recordingState) {
                            case NONE:
                                WorkoutService.startNewRecording(WorkoutActivity.this);
                                break;
                            case RECORDING:
                                WorkoutService.resumeRecording(WorkoutActivity.this);
                                break;
                            case PAUSED:
                                break;
                        }
                    }
                });
    }

    private void addMarker(double lat, double lon) {
        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)));
    }

    private void moveCameraToBounds(List<TravelPoint> travelPoints, boolean animate,
            GoogleMap.CancelableCallback cancelableCallback) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (TravelPoint point : travelPoints) {
            builder.include(point.latLng);
        }

        int width = Utils.getScreenWidth();
        int height = width / 16 * 9;

        LatLngBounds bounds = builder.build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, width, height,
                DEFAULT_BOUNDS_PADDING);

        if (animate) {
            mMap.animateCamera(cameraUpdate, DEFAULT_ANIMATION_DURATION, cancelableCallback);
        } else {
            mMap.moveCamera(cameraUpdate);
            if (cancelableCallback != null) {
                cancelableCallback.onFinish();
            }
        }
    }

    private void drawRoutes(List<TravelPoint> travelPoints) {
        for (int i = 0; i < travelPoints.size() - 1; i++) {
            LatLng startPoint = travelPoints.get(i).latLng;
            LatLng endPoint = travelPoints.get(i + 1).latLng;
            drawRoute(startPoint.latitude, startPoint.longitude, endPoint.latitude,
                    endPoint.longitude);
        }
    }

    private void drawRoute(double startLat, double startLon, double endLat, double endLon) {
        PolylineOptions options = new PolylineOptions();
        options.add(new LatLng(startLat, startLon), new LatLng(endLat, endLon));
        options.startCap(new RoundCap());
        options.endCap(new RoundCap());
        options.width(DEFAULT_LINE_WIDTH);
        options.color(getResources().getColor(R.color.colorAccent));
        options.geodesic(true);

        mMap.addPolyline(options);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_RC) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onLocationPermissionGranted();
            } else {
                if (shouldShowRequestLocationPermissionRationale()) {
                    showLocationPermissionDescriptionDialog();
                } else {
                    showLocationPermissionSettingDialog();
                }
            }
        }
    }

    private boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void askForLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION
                },
                LOCATION_RC);
    }

    private boolean shouldShowRequestLocationPermissionRationale() {
        return ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void checkAndRequestLocationPermissionIfNeed() {
        if (!isLocationPermissionGranted()) {
            if (shouldShowRequestLocationPermissionRationale()) {
                showLocationPermissionDescriptionDialog();
            } else {
                askForLocationPermission();
            }
        } else {
            onLocationPermissionGranted();
        }
    }

    private void showLocationPermissionDescriptionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notice)
                .setMessage(R.string.location_permission_description)
                .setPositiveButton(R.string.ok,
                        (dialog, which) -> askForLocationPermission())
                .show();
    }

    private void showLocationPermissionSettingDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notice)
                .setMessage(R.string.location_permission_description)
                .setPositiveButton(R.string.ok,
                        (dialog, which) -> openSettingScreen(WorkoutActivity.this))
                .show();
    }

    private void openSettingScreen(Context context) {
        Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }

    @Override
    public void onBackPressed() {
        btnStop.performClick();
    }
}
