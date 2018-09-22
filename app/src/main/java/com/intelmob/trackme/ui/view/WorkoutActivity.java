
package com.intelmob.trackme.ui.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.Group;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.intelmob.trackme.R;
import com.intelmob.trackme.WorkoutService;
import com.intelmob.trackme.ui.viewmodel.WorkoutSessionViewModel;
import com.intelmob.trackme.util.Utils;

import java.io.File;
import java.io.FileOutputStream;

public class WorkoutActivity extends AppCompatActivity
        implements View.OnClickListener, OnMapReadyCallback {

    private static final int LOCATION_RC = 999;
    private static final int DEFAULT_ZOOM_LEVEL = 14;
    private static final int DEFAULT_LINE_WIDTH = 12;
    private static final int DEFAULT_BOUNDS_PADDING = 50;

    private Button btnPause;
    private Button btnResume;
    private Button btnStop;
    private Group groupResumeStop;

    private TextView tvDistance;
    private TextView tvAvgSpeed;
    private TextView tvDuration;

    private View loadingView;

    private GoogleMap mMap;

    private int mWorkoutSessionId = -1;
    private boolean isDropStartMarker;
    private LatLng mLastPoint;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.workout_mapView);
        mapFragment.getMapAsync(this);

        btnPause = findViewById(R.id.workout_btnPause);
        btnResume = findViewById(R.id.workout_btnResume);
        btnStop = findViewById(R.id.workout_btnStop);
        groupResumeStop = findViewById(R.id.groupResumeStop);

        tvDistance = findViewById(R.id.workout_info_tvDistance);
        tvAvgSpeed = findViewById(R.id.workout_info_tvAvgSpeed);
        tvDuration = findViewById(R.id.workout_info_tvDuration);

        loadingView = findViewById(R.id.loadingView);

        btnPause.setOnClickListener(this);
        btnResume.setOnClickListener(this);
        btnStop.setOnClickListener(this);

        WorkoutSessionViewModel viewModel = ViewModelProviders.of(this)
                .get(WorkoutSessionViewModel.class);
        viewModel.getRecordingWorkoutSession().observe(this, session -> {
            if (session == null) {
                return;
            }

            mWorkoutSessionId = session.id;

            tvDistance.setText(String.format(
                    getString(R.string.format_distance), session.distance));
            tvAvgSpeed.setText(String.format(
                    getString(R.string.format_avgSpeed), session.avgSpeed));
            tvDuration.setText(Utils.formatDuration(session.duration));

            if (session.travelRoutes != null && session.travelRoutes.size() > 0) {
                if (session.travelRoutes.size() > 1) {
                    LatLng endPoint = session.travelRoutes.get(session.travelRoutes.size() - 1);

                    if (mLastPoint != null && mLastPoint.latitude == endPoint.latitude
                            && mLastPoint.longitude == endPoint.longitude) {
                        return;
                    }

                    mLastPoint = endPoint;
                    LatLng startPoint = session.travelRoutes.get(session.travelRoutes.size() - 2);

                    moveCameraToBounds(startPoint.latitude, startPoint.longitude,
                            endPoint.latitude, endPoint.longitude);
                    drawRoute(startPoint.latitude, startPoint.longitude, endPoint.latitude,
                            endPoint.longitude);
                } else if (!isDropStartMarker) {
                    isDropStartMarker = true;

                    LatLng firstPoint = session.travelRoutes.get(0);
                    addMarker(firstPoint.latitude, firstPoint.longitude);
                    moveCameraTo(firstPoint.latitude, firstPoint.longitude, DEFAULT_ZOOM_LEVEL);
                }
            }
        });

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
            case R.id.workout_btnPause:
                WorkoutService.pauseRecording(this);
                toggleGroupResumeStop(true);
                break;
            case R.id.workout_btnResume:
                WorkoutService.resumeRecording(this);
                toggleGroupResumeStop(false);
                break;
            case R.id.workout_btnStop:
                takeMapSnapshot();
                break;
        }
    }

    private void takeMapSnapshot() {
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

            WorkoutService.stopRecording(this);
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

        WorkoutService.startLocationUpdates(this);
        WorkoutService.startNewRecording(this);
    }

    private void addMarker(double lat, double lon) {
        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)));
    }

    private void moveCameraTo(double lat, double lon, int zoomLevel) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                        .target(new LatLng(lat, lon))
                        .zoom(zoomLevel)
                        .build()));
    }

    private void moveCameraToBounds(double startLat, double startLon, double endLat,
            double endLon) {
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(new LatLng(startLat, startLon))
                .include(new LatLng(endLat, endLon))
                .build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, DEFAULT_BOUNDS_PADDING));
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
                showLocationPermissionDescriptionDialog();
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

    private void checkAndRequestLocationPermissionIfNeed() {
        if (!isLocationPermissionGranted()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
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
}
