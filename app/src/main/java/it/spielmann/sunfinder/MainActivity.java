package it.spielmann.sunfinder;

import android.Manifest;
import java.util.Locale;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SunCompassView compassView;
    private TextView tvRecommendation;
    private TextView tvDetails;
    private TextView tvStatus;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] gravity;
    private float[] geomagnetic;
    private double compassHeading = 0;

    private double lat = 47.376;
    private double lon = 8.541;
    private boolean hasLocation = false;

    private double currentSunAzimuth = 0;
    private double currentSunElevation = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable sunUpdateRunnable;
    private long lastUiUpdate = 0;

    private final ActivityResultLauncher<String> locationPermLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startLocationUpdates();
                } else {
                    tvStatus.setText(R.string.status_denied);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        compassView = findViewById(R.id.compassView);
        tvRecommendation = findViewById(R.id.tvRecommendation);
        tvDetails = findViewById(R.id.tvDetails);
        tvStatus = findViewById(R.id.tvStatus);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        requestLocationPermission();

        sunUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                recalculateSun();
                handler.postDelayed(this, 60_000);
            }
        };
        handler.post(sunUpdateRunnable);
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void startLocationUpdates() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 30_000, 50, locationListener);

            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null) {
                last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (last != null) {
                lat = last.getLatitude();
                lon = last.getLongitude();
                hasLocation = true;
                recalculateSun();
                tvStatus.setVisibility(View.GONE);
            }
        } catch (SecurityException e) {
            tvStatus.setText(getString(R.string.status_error, e.getMessage()));
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lat = location.getLatitude();
            lon = location.getLongitude();
            hasLocation = true;
            recalculateSun();
            tvStatus.setVisibility(View.GONE);
        }
    };

    private void recalculateSun() {
        double[] pos = SunCalculator.calculate(lat, lon, System.currentTimeMillis());
        currentSunAzimuth = pos[0];
        currentSunElevation = pos[1];
        compassView.setSunPosition(currentSunAzimuth, currentSunElevation);
        updateUi();
    }

    private void updateUi() {
        SunCompassView.ShadeSide side = compassView.getShadeSide();
        String cardinal = azimuthToCardinal(currentSunAzimuth);

        if (currentSunElevation <= 0) {
            tvRecommendation.setText(R.string.rec_sunset);
        } else {
            switch (side) {
                case LEFT:
                    tvRecommendation.setText(R.string.rec_left);
                    break;
                case RIGHT:
                    tvRecommendation.setText(R.string.rec_right);
                    break;
                default:
                    tvRecommendation.setText(R.string.rec_front_back);
                    break;
            }
        }

        if (!hasLocation) {
            tvDetails.setText(String.format(Locale.getDefault(),
                    getString(R.string.details_default_location),
                    cardinal, currentSunAzimuth, currentSunElevation));
        } else {
            tvDetails.setText(String.format(Locale.getDefault(),
                    getString(R.string.details_with_location),
                    cardinal, currentSunAzimuth, currentSunElevation));
        }
    }

    private String azimuthToCardinal(double az) {
        String[] dirs = {"N", "NO", "O", "SO", "S", "SW", "W", "NW"};
        int idx = (int) Math.round(az / 45.0) % 8;
        return dirs[idx];
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(sunUpdateRunnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values.clone();
        }

        if (gravity == null || geomagnetic == null) return;

        float[] R = new float[9];
        float[] I = new float[9];
        if (!SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) return;

        float[] orientation = new float[3];
        SensorManager.getOrientation(R, orientation);
        double azimuthDeg = Math.toDegrees(orientation[0]);
        if (azimuthDeg < 0) azimuthDeg += 360;

        // Low-pass filter — use shortest angular path to avoid 0°/360° wrap jump
        double delta = azimuthDeg - compassHeading;
        if (delta > 180) delta -= 360;
        else if (delta < -180) delta += 360;
        compassHeading = (compassHeading + 0.15 * delta + 360) % 360;
        compassView.setVehicleHeading(compassHeading);

        // Update recommendation text at most 5 times per second
        long now = SystemClock.elapsedRealtime();
        if (now - lastUiUpdate > 200) {
            lastUiUpdate = now;
            updateUi();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
