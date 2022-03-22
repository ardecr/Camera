package com.example.camera;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.provider.Settings;
import android.util.Log;

import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageCapture.OnImageSavedCallback;
import androidx.camera.core.ImageCapture.OutputFileOptions;
import androidx.camera.core.ImageCapture.OutputFileResults;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.camera.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class MainActivity extends AppCompatActivity  implements SensorEventListener, CameraSwitch {

    private ActivityMainBinding viewBinding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static  final String TAG="Camera";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS =new String[]{"android.permission.CAMERA"};
    private SensorManager sensorManager;
    private TextView timer;
    private CameraSelector cameraSelector;
    private Gyroscope gyroscope;
    private Light light;
    private Proximity proximity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager=(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        permission();
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        viewBinding.imageCaptureButton.setOnClickListener(view -> takePhoto());
        viewBinding.optionsIcon.setOnClickListener(view -> openOptionMenu());
        viewBinding.switchButton.setOnClickListener(it -> switchCamera());
        cameraExecutor= Executors.newSingleThreadExecutor();
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        proximity= new Proximity(this);
        gyroscope= new Gyroscope(this,this);
        light= new Light(this);
    }

    private void openOptionMenu(){
        Intent intent= new Intent(this, OptionActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.equals(light.getLightSensor())) {
            light.menageLight(sensorEvent.values[0]);
        }
        if (sensorEvent.sensor.equals(proximity.getProximitySensor())
                && proximity.isNear(sensorEvent.values[0])) {
            startTimer();
        }
        if (sensorEvent.sensor.equals(gyroscope.getGyroscopeSensor())) {
            gyroscope.menageRotation(sensorEvent.values[0]);
        }
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private boolean allPermissionsGranted() {
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this,permission)!= PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return  true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener((new Runnable() {
            public final void run() {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                    getWindow().setNavigationBarColor(Color.BLACK);
                try {
                    ProcessCameraProvider  cameraProvider =
                            (ProcessCameraProvider) cameraProviderFuture.get();
                    Preview preview = new Preview.Builder().build();
                    preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());
                    imageCapture = new ImageCapture.Builder().build();
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector,
                            imageCapture, preview);
                } catch (Exception e) {
                    Log.e(TAG, "Use case binding failed", e);
                }
            }
        }), ContextCompat.getMainExecutor(this));
    }



    private void takePhoto() {
        if (imageCapture != null) {
            String name = (new SimpleDateFormat(FILENAME_FORMAT, Locale.US)).format(System.currentTimeMillis());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                contentValues.put(Media.RELATIVE_PATH, "Pictures/Camera-Image");
            }
            OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.
                    Builder(getContentResolver(), Media.EXTERNAL_CONTENT_URI, contentValues).build();

            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                    (new OnImageSavedCallback() {
                        public void onError(@NotNull ImageCaptureException exc) {
                            Log.e(TAG, "Photo capture failed: " + exc.getMessage(),exc);
                        }
                        public void onImageSaved(@NotNull OutputFileResults output) {
                            String msg = "Photo capture succeeded: " + output.getSavedUri();
                            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                            Log.d(TAG, msg);
                }
            }));
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this,light.getLightSensor(),SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,proximity.getProximitySensor(),SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,gyroscope.getGyroscopeSensor(),SensorManager.SENSOR_DELAY_NORMAL);
        startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }


    private void permission(){
        boolean value;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            value= Settings.System.canWrite(getApplicationContext());
            if(!value) {
                Intent intent= new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:"+ getApplicationContext().getPackageName())); // scorre fino all' app
                startActivityForResult(intent,100);
            }
        }
    }

    private void startTimer(){
        timer=findViewById(R.id.timer);
        long duration = TimeUnit.SECONDS.toMillis(5);
        new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long l) {
               String sDuration= String.format(Locale.ENGLISH,"%02d",TimeUnit.MILLISECONDS.toSeconds(l));
                timer.setText(sDuration);
            }
            @Override
            public void onFinish() {
                timer.setText("");
                takePhoto();
            }
        }.start();
    }

    @Override
    public void switchCamera(){
        if (cameraSelector.equals(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        }
        else
            if (cameraSelector.equals(CameraSelector.DEFAULT_BACK_CAMERA)) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        }
        startCamera();
    }

}
