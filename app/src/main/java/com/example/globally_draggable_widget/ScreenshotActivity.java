package com.example.globally_draggable_widget;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenshotActivity extends Activity {
    private static final int REQUEST_SCREENSHOT = 1001;
    private MediaProjectionManager mediaProjectionManager;
    private ImageReader imageReader;
    private Handler handler;
    private MediaProjection mediaProjection;
    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get display metrics
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;

        // Initialize MediaProjectionManager
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        handler = new Handler(Looper.getMainLooper());

        // Request screen capture permission
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_SCREENSHOT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SCREENSHOT && resultCode == Activity.RESULT_OK) {
            // Initialize screen capture
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 1);
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);

            // Create virtual display
            MediaProjection.Callback callback = new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    if (mediaProjection != null) {
                        mediaProjection.stop();
                        mediaProjection = null;
                    }
                }
            };

            mediaProjection.registerCallback(callback, handler);

            final VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    screenWidth,
                    screenHeight,
                    screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null,
                    handler
            );

            // Capture the screen after a short delay
            handler.postDelayed(() -> {
                Image image = imageReader.acquireLatestImage();
                if (image != null) {
                    try {
                        // Convert Image to Bitmap
                        Bitmap bitmap = imageToBitmap(image);
                        // Save and share the screenshot
                        shareScreenshot(bitmap);
                    } finally {
                        image.close();
                        virtualDisplay.release();
                        mediaProjection.stop();
                        finish();
                    }
                }
            }, 100);
        } else {
            finish();
        }
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * screenWidth;

        Bitmap bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    private void shareScreenshot(Bitmap bitmap) {
        try {
            // Save bitmap to file
            File cachePath = new File(getCacheDir(), "screenshots");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "screenshot_" + System.currentTimeMillis() + ".png");

            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            // Create content URI using FileProvider
            Uri contentUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    imageFile
            );

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Start share activity
            startActivity(Intent.createChooser(shareIntent, "Share Screenshot"));
        } catch (IOException e) {
            Log.e("ScreenshotActivity", "Error saving screenshot", e);
            Toast.makeText(this, "Failed to save screenshot", Toast.LENGTH_SHORT).show();
        }
    }
}
