package com.example.camera;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.OutputResults;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.util.Consumer;

import com.example.camera.databinding.ActivityVideoModeBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import kotlin.jvm.internal.Intrinsics;


public final class VideoModeActivity extends AppCompatActivity implements CameraSwitch {
    private ActivityVideoModeBinding viewBinding;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private ExecutorService cameraExecutor;
    private CameraSelector cameraSelector;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.RECORD_AUDIO"};

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityVideoModeBinding.inflate(this.getLayoutInflater());

        setContentView(viewBinding.getRoot());
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 10);
        }
        viewBinding.videoCaptureButton.setOnClickListener(view -> captureVideo());
        viewBinding.switchButton.setOnClickListener(view -> switchCamera());
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @SuppressLint("SetTextI18n")
    private void captureVideo() {
        Button videoButton = viewBinding.videoCaptureButton;
        if (recording != null) {
           recording.stop();
        }
        else {
            String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis());
            ContentValues contentValues = new ContentValues();
            contentValues.put("_display_name", name);
            contentValues.put("mime_type", "video/mp4");
            if (Build.VERSION.SDK_INT > 28) {
                contentValues.put("relative_path", "Movies/Camera-Video");
            }
            MediaStoreOutputOptions mediaStoreOutputOptions = new MediaStoreOutputOptions.Builder(getContentResolver(),
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(contentValues).build();
            PendingRecording pendingRecording = videoCapture.getOutput().prepareRecording(this, mediaStoreOutputOptions);
            if (PermissionChecker.checkSelfPermission(this, "android.permission.RECORD_AUDIO") == PermissionChecker.PERMISSION_GRANTED)
                pendingRecording.withAudioEnabled();
            recording = pendingRecording.start(ContextCompat.getMainExecutor(this), new Consumer<VideoRecordEvent>() {
                        @Override
                        public void accept(VideoRecordEvent videoRecordEvent) {

                            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                videoButton.setText("Stop");
                            }
                            else
                                if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                VideoRecordEvent.Finalize finalize = (VideoRecordEvent.Finalize) videoRecordEvent;
                                if (!(finalize.hasError())) {
                                    StringBuilder stringBuilder = new StringBuilder().append("Video capture succeeded:");
                                    OutputResults outputResults = finalize.getOutputResults();
                                    String msg = stringBuilder.append(outputResults.getOutputUri()).toString();
                                    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                                    Log.d("Camera", msg);
                                }
                                else {
                                    recording.close();
                                    recording = null;
                                    Log.e("Camera", "Video capture ends with error: " + finalize.getError());
                                }
                                videoButton.setText("Start ");
                            }
                        }});
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
            getWindow().setNavigationBarColor(Color.BLACK);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());
                Recorder recorder = new Recorder.Builder().
                        setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build();
                videoCapture = VideoCapture.withOutput(recorder);
                cameraProvider.bindToLifecycle(VideoModeActivity.this, cameraSelector, preview, videoCapture);
            } catch (Exception e) {
                Log.e("Camera", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (this.allPermissionsGranted()) {
                this.startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    @Override
    public void switchCamera() {
        if (cameraSelector.equals(CameraSelector.DEFAULT_FRONT_CAMERA)) {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        } else if (cameraSelector.equals(CameraSelector.DEFAULT_BACK_CAMERA)) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
        }
        startCamera();
    }
}