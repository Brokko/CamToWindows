package com.github.brokko.camtowindows.activity.connected;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.github.brokko.camtowindows.R;
import com.github.brokko.camtowindows.service.InputService;

public class ConnectedActivity extends FragmentActivity implements View.OnTouchListener {
    private static final int REQUEST_CODE_CAMERA = 1;

    // TODO Should be non-static
    private static PreviewView previewView;
    private ImageButton btnBrightness;
    private ImageButton btnRotate;
    private ImageButton btnTorch;

    private int brightnessLevel = 0;
    private boolean torchActive = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            btnTorch.setEnabled(intent.getBooleanExtra("torch", false));
            btnTorch.setBackgroundResource(R.drawable.flashlight_off_24px);

            // When InputService sends InputService.ACTION_READY, we tell him the view is ready too
            sendBroadcast(new Intent().setAction(InputService.ACTION_PREVIEW_START));
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_connected);

        // Permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(ConnectedActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, new IntentFilter(InputService.MESSAGE_READY), RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, new IntentFilter(InputService.MESSAGE_READY));
        }

        startService(new Intent(this, InputService.class));

        previewView = findViewById(R.id.previewView);
        btnBrightness = findViewById(R.id.btn_brightness);
        btnRotate = findViewById(R.id.btn_rotate);
        btnTorch = findViewById(R.id.btn_torch);

        btnBrightness.setOnTouchListener(this);
        btnRotate.setOnTouchListener(this);
        btnTorch.setOnTouchListener(this);

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        sendBroadcast(new Intent().setAction(InputService.ACTION_ORIENTATION_CHANGED));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onResume() {
        super.onResume();

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        sendBroadcast(new Intent().setAction(InputService.ACTION_PREVIEW_START));
    }

    @Override
    public void onPause() {
        super.onPause();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        sendBroadcast(new Intent().setAction(InputService.ACTION_PREVIEW_STOP));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }

    public static Preview.SurfaceProvider getSurfaceProvider() {
        return previewView.getSurfaceProvider();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() != MotionEvent.ACTION_UP)
            return false;

        if (v == btnBrightness) {
            if(brightnessLevel >= 3) {
                brightnessLevel = 0;
            }else {
                brightnessLevel++;
            }

            WindowManager.LayoutParams layout = getWindow().getAttributes();
            switch (brightnessLevel) {
                case 0:
                    layout.screenBrightness = -1F;
                    btnBrightness.setBackgroundResource(R.drawable.brightness_auto_24px);
                    break;
                case 1:
                    layout.screenBrightness = 0.1F;
                    btnBrightness.setBackgroundResource(R.drawable.brightness_low_24px);
                    break;
                case 2:
                    layout.screenBrightness = 0.5F;
                    btnBrightness.setBackgroundResource(R.drawable.brightness_medium_24px);
                    break;
                case 3:
                    layout.screenBrightness = 1F;
                    btnBrightness.setBackgroundResource(R.drawable.brightness_high_24px);
                    break;
            }

            getWindow().setAttributes(layout);
            return true;
        }

        if(v == btnRotate) {
            sendBroadcast(new Intent().setAction(InputService.ACTION_CHANGE_PERSPECTIVE));
            return true;
        }

        if(v == btnTorch) {
            torchActive = !torchActive;
            btnTorch.setBackgroundResource(torchActive ? R.drawable.flashlight_on_24px : R.drawable.flashlight_off_24px);

            sendBroadcast(new Intent().setAction(InputService.ACTION_TOGGLE_TORCH));
            return true;
        }

        return false;
    }
}
