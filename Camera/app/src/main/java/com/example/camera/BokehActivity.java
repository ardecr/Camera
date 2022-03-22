package com.example.camera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.extensions.ExtensionMode;
import androidx.camera.extensions.ExtensionsManager;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.camera.databinding.ActivityBokehBinding;
import com.google.common.util.concurrent.ListenableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class BokehActivity extends AppCompatActivity implements SensorEventListener, CameraSwitch {

    private ActivityBokehBinding viewBinding;
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
    private ImageButton options;


    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager=(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        viewBinding = ActivityBokehBinding.inflate(getLayoutInflater());  // si crea un istanza della classe ActivityBinding
        setContentView(viewBinding.getRoot());
        permission();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        viewBinding.imageCaptureButton.setOnClickListener((new View.OnClickListener() {
            public final void onClick(View it) {
                takePhoto();
            }
        }));


        viewBinding.switchButton.setOnClickListener((new View.OnClickListener() {
            public final void onClick(View it) {
                switchCamera();
            }
        }));
        cameraExecutor= Executors.newSingleThreadExecutor();
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA; // si mette la fotocamera di default all apertura
        proximity= new Proximity(this);
        gyroscope= new Gyroscope(this,this);
        light= new Light(this);

    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //regola la luminosità dello schermo in base alla luce
        if (sensorEvent.sensor.equals(light.getLightSensor())) {
            light.menageLight(sensorEvent.values[0]);
        }
        //avvia l'autoscatto se si avvicina la mano davanti al telefono
        if (sensorEvent.sensor.equals(proximity.getProximitySensor()) && proximity.isNear(sensorEvent.values[0])) {
            startTimer();
        }
        //cambia  fotocamera se si piega in avanti la parte alta del telefono
        if (sensorEvent.sensor.equals(gyroscope.getGyroscopeSensor())) {
            gyroscope.menageRotation(sensorEvent.values[0]);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
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

    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    public void startCamera() {
        try {
            ProcessCameraProvider  cameraProvider = ProcessCameraProvider.getInstance(this).get();
            ListenableFuture cameraProviderFuture = ExtensionsManager.getInstanceAsync(this, cameraProvider);
            cameraProviderFuture.addListener((new Runnable() {
                public final void run() {
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                        getWindow().setNavigationBarColor(Color.BLACK);
                    try {
                        ExtensionsManager extensionsManager = (ExtensionsManager) cameraProviderFuture.get();
                        if (extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.BOKEH)) {
                            cameraProvider.unbindAll();
                            CameraSelector bokehCameraSelector = extensionsManager
                                    .getExtensionEnabledCameraSelector(cameraSelector, ExtensionMode.BOKEH);
                            imageCapture = new ImageCapture.Builder().build();
                            Preview preview = new Preview.Builder().build();
                            preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());
                            cameraProvider.bindToLifecycle(BokehActivity.this, bokehCameraSelector,
                                    imageCapture, preview);
                        }
                    } catch (Exception e) { Log.e(TAG, "Use case binding failed", e); }
                }
            }), ContextCompat.getMainExecutor(this));
        } catch (Exception e) {  e.printStackTrace(); }
    }

    private void takePhoto() {
        if (imageCapture != null) {
            String name = (new SimpleDateFormat(FILENAME_FORMAT, Locale.US)).format(System.currentTimeMillis());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Camera-Image");
            }
            ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.
                    Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build();

            imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                    (new ImageCapture.OnImageSavedCallback() {
                        public void onError(@NotNull ImageCaptureException exc) {
                            Log.e(TAG, "Photo capture failed: " + exc.getMessage(),exc);
                        }
                        public void onImageSaved(@NotNull ImageCapture.OutputFileResults output) {
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
