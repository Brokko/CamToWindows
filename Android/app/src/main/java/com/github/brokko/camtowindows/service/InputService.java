package com.github.brokko.camtowindows.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.github.brokko.camtowindows.R;
import com.github.brokko.camtowindows.activity.connected.ConnectedActivity;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InputService extends Service implements Runnable {
    private static final String TAG = InputService.class.getCanonicalName();

    /* Notification values */
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MyForegroundServiceChannel";
    private static final String CHANNEL_NAME = "MyForegroundService";

    /* Values for communicating via BroadcastReceiver */
    public static final String MESSAGE_READY = "ACTION_READY";
    public static final String ACTION_PREVIEW_START = "PREVIEW_START";
    public static final String ACTION_PREVIEW_STOP = "PREVIEW_STOP";
    public static final String ACTION_ORIENTATION_CHANGED = "ROTATION_CHANGED";
    public static final String ACTION_CHANGE_PERSPECTIVE = "CHANGE_PERSPECTIVE";
    public static final String ACTION_TOGGLE_TORCH = "TOGGLE_TORCH";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "Received action: " + intent.getAction());

            // Checks if camera is setup
            if (!cameraProviderFuture.isDone())
                return;

            switch (intent.getAction()) {
                case ACTION_PREVIEW_START:
                    preview.setSurfaceProvider(ConnectedActivity.getSurfaceProvider());
                    break;

                case ACTION_PREVIEW_STOP:
                    preview.setSurfaceProvider(null);
                    break;

                case ACTION_ORIENTATION_CHANGED:
                    // Changes the rotation of the preview according to display rotation
                    preview.setTargetRotation(windowManager.getDefaultDisplay().getRotation());
                    break;

                case ACTION_CHANGE_PERSPECTIVE:
                    lensAlignment = lensAlignment == CameraSelector.LENS_FACING_FRONT ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;

                    // Disable the torch if its on before switching
                    camera.getCameraControl().enableTorch(false);
                    torchActive = false;

                    // Unbind all use-cases and rebuild them
                    provider.unbindAll();
                    openCamera();
                    break;

                case ACTION_TOGGLE_TORCH:
                    // Torch only lights up if user is currently using the back camera
                    if (lensAlignment == CameraSelector.LENS_FACING_BACK) {
                        torchActive = !torchActive;
                        camera.getCameraControl().enableTorch(torchActive);
                    }

                    break;
            }
        }
    };

    private int lensAlignment = CameraSelector.LENS_FACING_FRONT;
    private boolean torchActive = false;

    private WindowManager windowManager;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider provider;

    private Preview preview;
    private Camera camera;

    public InputService() {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Starting service...");

        IntentFilter listeningActions = new IntentFilter();
        listeningActions.addAction(ACTION_PREVIEW_START);
        listeningActions.addAction(ACTION_PREVIEW_STOP);
        listeningActions.addAction(ACTION_ORIENTATION_CHANGED);
        listeningActions.addAction(ACTION_CHANGE_PERSPECTIVE);
        listeningActions.addAction(ACTION_TOGGLE_TORCH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, listeningActions, RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, listeningActions);
        }

        // Start service as foreground service
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Foreground service notification");
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        Intent notificationIntent = new Intent(this, ConnectedActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Recording")
                .setContentText("You are visible")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
        } else {
            startForeground(NOTIFICATION_ID, builder.build());
        }

        windowManager = getSystemService(WindowManager.class);

        // Create a new CameraProvider
        cameraProviderFuture = ProcessCameraProvider.getInstance(getApplicationContext());
        cameraProviderFuture.addListener(this, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void run() {
        try {
            provider = cameraProviderFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        openCamera();
    }

    private void openCamera() {
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensAlignment)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        preview = new Preview.Builder().build();

        camera = provider.bindToLifecycle(new LC(), cameraSelector, imageAnalysis, preview);

        Log.i(TAG, "Camera setup successfully finished!");

        // Notify activity, we are ready to receive a PreviewView instance
        // We don't know in which state ConnectedActivity currently is, so we request and hope
        // for a answer, instead of calling directly ConnectedActivity.getSurfaceProvider()
        Intent intent = new Intent()
                .setAction(MESSAGE_READY)
                .putExtra("torch", lensAlignment == CameraSelector.LENS_FACING_BACK);

        sendBroadcast(intent);



        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return;
        }

        imageAnalysis.setAnalyzer(executorService, image -> {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            image.close();
        });
    }
}
