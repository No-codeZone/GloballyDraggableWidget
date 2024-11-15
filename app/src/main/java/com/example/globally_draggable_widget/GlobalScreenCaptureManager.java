// GlobalScreenCaptureManager.java
package com.example.globally_draggable_widget;

import android.app.Activity;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GlobalScreenCaptureManager {
    public static final int REQUEST_SCREENSHOT = 100;
    public static final int REQUEST_RECORD = 101;
    public static final int OVERLAY_PERMISSION_REQUEST_CODE = 102;

    private final Context context;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private boolean isCapturing = false;

    public GlobalScreenCaptureManager(Context context) {
        this.context = context;
        this.projectionManager = (MediaProjectionManager)
                context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public void initiateCapture(boolean isScreenshot, boolean requiresOverlay) {
        if (requiresOverlay && !Settings.canDrawOverlays(context)) {
            requestOverlayPermission();
            return;
        }

        if (projectionManager != null) {
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(captureIntent,
                        isScreenshot ? REQUEST_SCREENSHOT : REQUEST_RECORD);
            }
        }
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        if (context instanceof Activity) {
            ((Activity) context).startActivityForResult(intent,
                    OVERLAY_PERMISSION_REQUEST_CODE);
        }
    }

    public void handleScreenshotResult(Intent data) {
        if (!isCapturing) {
            isCapturing = true;
            mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, data);

            // Start the overlay service
            Intent serviceIntent = new Intent(context, GlobalCaptureOverlayService.class);
            serviceIntent.putExtra("capture_type", "screenshot");
            context.startService(serviceIntent);

            // Delay to ensure service is started
            new Handler().postDelayed(() -> {
                captureScreenshot();
                isCapturing = false;
            }, 1000);
        }
    }

    public void handleRecordingResult(Intent data) {
        if (!isCapturing) {
            isCapturing = true;
            mediaProjection = projectionManager.getMediaProjection(Activity.RESULT_OK, data);

            // Start recording service
            Intent serviceIntent = new Intent(context, GlobalCaptureOverlayService.class);
            serviceIntent.putExtra("capture_type", "recording");
            context.startService(serviceIntent);
        }
    }

    private void captureScreenshot() {
        // Implementation similar to your existing screenshot code
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        String fileName = "Screenshot_" + timeStamp + ".png";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Screenshots");
        }

        Uri imageUri = context.getContentResolver()
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            if (imageUri != null) {
                try (FileOutputStream fos = (FileOutputStream)
                        context.getContentResolver().openOutputStream(imageUri)) {
                    // Implement actual screenshot capture using mediaProjection
                    // This is a placeholder for the actual implementation
                    Bitmap screenshot = captureScreenBitmap();
                    if (screenshot != null) {
                        screenshot.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        Toast.makeText(context, "Screenshot saved!",
                                Toast.LENGTH_SHORT).show();
                        shareScreenshot(imageUri);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save screenshot",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap captureScreenBitmap() {
        // Implement actual screen capture using mediaProjection
        // This would require setting up a VirtualDisplay and capturing its contents
        return null; // Placeholder
    }

    private void shareScreenshot(Uri imageUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, "Share Screenshot"));
    }

    public void stopCapture() {
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        isCapturing = false;
    }
}