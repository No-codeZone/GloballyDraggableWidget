package com.example.globally_draggable_widget;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class GlobalCaptureOverlayService extends Service {
    private static final String TAG = "GlobalOverlayService";
    private static final String CHANNEL_ID = "OverlayServiceChannel";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private View overlayView;
    private ImageView mainButton;
    private LinearLayout menuContainer;
    private ImageView screenshotButton;
    private ImageView recordButton;
    private Handler mainHandler;
    private boolean isOverlayAdded = false;
    private boolean isMenuVisible = false;
    private static final int REQUEST_SCREENSHOT = 1001;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private Handler screenshotHandler;
    private Intent resultData;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");
        mainHandler = new Handler(Looper.getMainLooper());
        screenshotHandler = new Handler(Looper.getMainLooper());
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        initializeService();
    }

    private void initializeService() {
        try {
            Log.d(TAG, "initializeService called");
            createNotificationChannel();
            startForeground(NOTIFICATION_ID, createNotification());
            initializeOverlayWithRetry();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing service", e);
            stopSelf();
        }
    }

    private void isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return;
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Overlay Service",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Keeps the overlay service running");
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel", e);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Capture Service Active")
                .setContentText("Tap to manage capture options")
                .setSmallIcon(R.drawable.assistive_touch)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void initializeOverlayWithRetry() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!initializeOverlay()) {
                    mainHandler.postDelayed(this, 1000);
                }
            }
        });
    }

    /*private boolean initializeOverlay() {
        try {
            Log.d(TAG, "initializeOverlay called");
            if (windowManager == null) {
                windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                Log.d(TAG, "windowManager initialized: " + (windowManager != null));
            }

            if (overlayView == null) {
                overlayView = createOverlayView();
                Log.d(TAG, "overlayView created: " + (overlayView != null));
            }

            if (!isOverlayAdded && windowManager != null && overlayView != null) {
                WindowManager.LayoutParams params = createLayoutParams();
                windowManager.addView(overlayView, params);
                isOverlayAdded = true;
                Log.d(TAG, "Overlay added successfully");
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing overlay", e);
            e.printStackTrace();
            return false;
        }
        return isOverlayAdded;
    }*/

    /*private View createOverlayView() {
        try {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.activity_global_assistive_touch, null);

            // Find views
            mainButton = view.findViewById(R.id.main_button);
            if (mainButton == null) {
                Log.e(TAG, "Failed to find main_button");
                return null;
            }

            menuContainer = view.findViewById(R.id.menu_container);
            if (menuContainer == null) {
                Log.e(TAG, "Failed to find menu_container");
                return null;
            }

            screenshotButton = view.findViewById(R.id.menu_screenshot);
            if (screenshotButton == null) {
                Log.e(TAG, "Failed to find menu_screenshot");
                return null;
            }

            recordButton = view.findViewById(R.id.menu_record);
            if (recordButton == null) {
                Log.e(TAG, "Failed to find menu_record");
                return null;
            }

            setupTouchListener(view);
            setupMenuListeners();

            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error creating overlay view", e);
            e.printStackTrace();
            return null;
        }
    }*/

    private View createOverlayView() {
        try {
            LayoutInflater inflater = LayoutInflater.from(this);
            if (inflater == null) {
                Log.e(TAG, "Failed to get LayoutInflater");
                return null;
            }

            // Inflate with better error handling
            View view = null;
            try {
                view = inflater.inflate(R.layout.activity_global_assistive_touch, null, false);
            } catch (Exception e) {
                Log.e(TAG, "Error inflating layout", e);
                return null;
            }

            if (view == null) {
                Log.e(TAG, "Inflated view is null");
                return null;
            }

            // Find views with null checks
            mainButton = view.findViewById(R.id.main_button);
            if (mainButton == null) {
                Log.e(TAG, "Failed to find main_button");
                return null;
            }

            menuContainer = view.findViewById(R.id.menu_container);
            if (menuContainer == null) {
                Log.e(TAG, "Failed to find menu_container");
                return null;
            }

            screenshotButton = view.findViewById(R.id.menu_screenshot);
            if (screenshotButton == null) {
                Log.e(TAG, "Failed to find menu_screenshot");
                return null;
            }

            recordButton = view.findViewById(R.id.menu_record);
            if (recordButton == null) {
                Log.e(TAG, "Failed to find menu_record");
                return null;
            }

            setupTouchListener(view);
            setupMenuListeners();

            return view;
        } catch (Exception e) {
            Log.e(TAG, "Error creating overlay view", e);
            e.printStackTrace();
            return null;
        }
    }

    private boolean initializeOverlay() {
        try {
            if (windowManager == null) {
                windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                if (windowManager == null) {
                    Log.e(TAG, "Failed to get window manager service");
                    return false;
                }
            }

            if (overlayView == null) {
                overlayView = createOverlayView();
                if (overlayView == null) {
                    Log.e(TAG, "Failed to create overlay view");
                    return false;
                }
            }

            if (!isOverlayAdded && windowManager != null) {
                WindowManager.LayoutParams params = createLayoutParams();
                windowManager.addView(overlayView, params);
                isOverlayAdded = true;
                Log.d(TAG, "Overlay added successfully");
                return true;  // Successfully initialized
            }

            return isOverlayAdded;  // Return true if already added

        } catch (Exception e) {
            Log.e(TAG, "Error initializing overlay", e);
            e.printStackTrace();
            return false;
        }
    }

    /*private void setupMenuListeners() {
        mainButton.setOnClickListener(v -> toggleMenu());

        screenshotButton.setOnClickListener(v -> {
            // Handle screenshot
            toggleMenu();
            // Implement screenshot functionality
        });

        recordButton.setOnClickListener(v -> {
            // Handle recording
            toggleMenu();
            // Implement recording functionality
        });
    }*/

    private void setupMenuListeners() {
        mainButton.setOnClickListener(v -> toggleMenu());

        screenshotButton.setOnClickListener(v -> {
            toggleMenu();
            takeScreenshot();
        });

        recordButton.setOnClickListener(v -> {
            toggleMenu();
            // Implement recording functionality
        });
    }

    private void takeScreenshot() {
        try {
            // Create an intent to start the screenshot activity
            Intent intent = new Intent(this, ScreenshotActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error taking screenshot", e);
            Toast.makeText(this, "Failed to take screenshot", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleMenu() {
        isMenuVisible = !isMenuVisible;
        Log.d(TAG, "Toggling menu visibility to: " + isMenuVisible);
        menuContainer.setVisibility(isMenuVisible ? View.VISIBLE : View.GONE);
    }

    private WindowManager.LayoutParams createLayoutParams() {
        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;

        params.x = 0;
        params.y = 100;

        return params;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener(View view) {
        final float[] touchX = new float[1];
        final float[] touchY = new float[1];
        final int[] offsetX = new int[1];
        final int[] offsetY = new int[1];
        final boolean[] isDragging = new boolean[1];

        mainButton.setOnTouchListener((v, event) -> {
            if (isMenuVisible) {
                return false;
            }

            WindowManager.LayoutParams params =
                    (WindowManager.LayoutParams) overlayView.getLayoutParams();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchX[0] = event.getRawX();
                    touchY[0] = event.getRawY();
                    offsetX[0] = params.x;
                    offsetY[0] = params.y;
                    isDragging[0] = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - touchX[0];
                    float deltaY = event.getRawY() - touchY[0];

                    if (!isDragging[0] &&
                            (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
                        isDragging[0] = true;
                    }

                    if (isDragging[0]) {
                        params.x = offsetX[0] + (int) deltaX;
                        params.y = offsetY[0] + (int) deltaY;
                        try {
                            windowManager.updateViewLayout(overlayView, params);
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating overlay position", e);
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!isDragging[0]) {
                        mainButton.performClick();
                    }
                    return true;
            }
            return false;
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        removeOverlayView();
        super.onDestroy();
    }

    private void removeOverlayView() {
        if (windowManager != null && overlayView != null && isOverlayAdded) {
            try {
                windowManager.removeView(overlayView);
                isOverlayAdded = false;
            } catch (Exception e) {
                Log.e(TAG, "Error removing overlay view", e);
            }
        }
    }
}