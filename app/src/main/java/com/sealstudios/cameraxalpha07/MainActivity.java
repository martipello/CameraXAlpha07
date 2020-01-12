package com.sealstudios.cameraxalpha07;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.Size;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnClickListener  {

    private Executor cameraProviderExecutor;

    private final ThreadPoolExecutor imageCaptureExecutor = new ThreadPoolExecutor(2, 6, 0,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

    private final String[] camPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraSelector cameraSelector;
    private ProcessCameraProvider cameraProvider;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    public static final int PERMISSION_ACCESS = 1001;
    private Preview preview;
    private String TAG = "CameraX";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderExecutor = ContextCompat.getMainExecutor(this);

        cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.preview_view);
//        Log.d(TAG,"preview view " + previewView.getDisplay().getRotation());
        Button shutterButton = findViewById(R.id.shutter_button);
        shutterButton.setOnClickListener(this);

        previewView.post(() -> {

            preview = getPreview();
            imageCapture = getImageCapture();
            preview.setPreviewSurfaceProvider(previewView.getPreviewSurfaceProvider());


            if (Build.VERSION.SDK_INT >= 23) {
                if (hasPermission(camPermissions)) {
                    startCamera();
                }
            } else {
                startCamera();
            }

        });

    }


    private void startCamera() {
        Log.d(TAG,"startCamera");

        cameraProviderFuture.addListener(() -> {
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
                Log.d(TAG, "cameraProvider.unbindAll");
            }
            try {
                Log.d(TAG, "try");
                Log.d(TAG, "cameraProviderFuture.get");
                cameraProvider = cameraProviderFuture.get();

                Log.d(TAG, "build image provider");
                // Set up the capture use case to allow users to take photos
                imageCapture = getImageCapture();

                // Apply declared configs to CameraX using the same lifecycle owner
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
//                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, cameraProviderExecutor);

    }

    private ImageCapture getImageCapture() {
        return new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(previewView.getDisplay().getRotation())
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();
    }

    private Preview getPreview() {
        return new Preview.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .setTargetRotation(previewView.getDisplay().getRotation())
                            .build();
    }

    private void takePicture(){
        String format = "yyyy-MM-dd-HH-mm-ss-SSS";
        SimpleDateFormat fmt = new SimpleDateFormat(format, Locale.US);
        String date = fmt.format(System.currentTimeMillis());

        File file = new File(getCacheDir(), date+".jpg");
        imageCapture.takePicture(file, imageCaptureExecutor, imageSavedListener);
    }

    private ImageCapture.OnImageSavedCallback imageSavedListener = new ImageCapture.OnImageSavedCallback() {

        @Override
        public void onImageSaved(@NonNull File photoFile) {
            //TODO do something with the file
        }

        @Override
        public void onError(int imageCaptureError, @NonNull String message, @Nullable Throwable cause) {
            if (cause != null) {
                cause.printStackTrace();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean hasPermission(String[] permissions) {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissions(listPermissionsNeeded.toArray(new String[0]),
                    PERMISSION_ACCESS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        int denied = 0;
        if (requestCode == PERMISSION_ACCESS) {
            if (grantResults.length > 0) {
                StringBuilder permissionsDenied = new StringBuilder();
                for (String per : permissions) {
                    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        permissionsDenied.append("\n").append(per);
                        denied++;
                    }
                }
                if (denied == 0) {
                    startCamera();
                }
            }
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.shutter_button){
            takePicture();
        }
    }
}
