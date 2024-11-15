package com.example.globally_draggable_widget.screen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.globally_draggable_widget.GlobalCaptureOverlayService;
import com.example.globally_draggable_widget.GlobalScreenCaptureManager;
import com.example.globally_draggable_widget.R;

import java.util.ArrayList;
import java.util.List;

public class GlobalAssistiveTouchActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private GlobalScreenCaptureManager captureManager;
    private boolean isServiceRunning = false;

    // Updated permissions list based on Android version
    private String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();

        // Basic permissions
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        permissions.add(Manifest.permission.RECORD_AUDIO);

        // Add foreground service permission for Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }

        return permissions.toArray(new String[0]);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_global_assistive_touch);

        try {
            // Initialize the capture manager
            captureManager = new GlobalScreenCaptureManager(this);

            // Check and request permissions
            if (checkAndRequestPermissions()) {
                initializeGlobalAssistiveTouch();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to initialize: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private boolean checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check each permission
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        // Request any missing permissions
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            return false;
        }

        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return false;
        }

        return true;
    }

    private void requestOverlayPermission() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent,
                    GlobalScreenCaptureManager.OVERLAY_PERMISSION_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to request overlay permission",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void initializeGlobalAssistiveTouch() {
        try {
            // Check if service is already running
            if (!isServiceRunning) {
                // Start the overlay service
                Intent serviceIntent = new Intent(this, GlobalCaptureOverlayService.class);
                serviceIntent.setAction("START_SERVICE");  // Add explicit action

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                isServiceRunning = true;
            }

            // Minimize the activity
            moveTaskToBack(true);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start service: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // Check overlay permission before proceeding
                if (!Settings.canDrawOverlays(this)) {
                    requestOverlayPermission();
                } else {
                    initializeGlobalAssistiveTouch();
                }
            } else {
                Toast.makeText(this,
                        "Required permissions were denied. Please grant them in Settings.",
                        Toast.LENGTH_LONG).show();
                // Open app settings instead of just finishing
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (requestCode == GlobalScreenCaptureManager.OVERLAY_PERMISSION_REQUEST_CODE) {
                if (Settings.canDrawOverlays(this)) {
                    initializeGlobalAssistiveTouch();
                } else {
                    Toast.makeText(this,
                            "Overlay permission is required for the floating button",
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            } else if (resultCode == RESULT_OK && captureManager != null) {
                if (requestCode == GlobalScreenCaptureManager.REQUEST_SCREENSHOT) {
                    captureManager.handleScreenshotResult(data);
                } else if (requestCode == GlobalScreenCaptureManager.REQUEST_RECORD) {
                    captureManager.handleRecordingResult(data);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error processing result: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (isServiceRunning) {
                // Stop the service properly
                Intent serviceIntent = new Intent(this, GlobalCaptureOverlayService.class);
                serviceIntent.setAction("STOP_SERVICE");
                stopService(serviceIntent);
                isServiceRunning = false;
            }

            // Clean up capture manager
            if (captureManager != null) {
                captureManager.stopCapture();
                captureManager = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}