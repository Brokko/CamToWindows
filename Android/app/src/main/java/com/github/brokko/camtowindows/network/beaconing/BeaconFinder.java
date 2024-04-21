package com.github.brokko.camtowindows.network.beaconing;

import androidx.annotation.WorkerThread;

import com.github.brokko.camtowindows.network.object.Device;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BeaconFinder {
    private static BeaconFinder instance;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private FinderCallback callback;

    private BeaconFinder() {

    }

    @NotNull
    public static BeaconFinder getInstance() {
        if (instance == null)
            instance = new BeaconFinder();

        return instance;
    }

    public void setCallback(FinderCallback callback) {
        this.callback = callback;
    }

    public void search() {
        executor.execute(this::run);
    }

    @WorkerThread
    private void run() {
        callback.onDeviceFound(new Device("Bobo", "1234"));
        callback.onDeviceFound(new Device("Boboi", "4324"));
        callback.onDeviceFound(new Device("Botgo", "654645"));
        callback.onDeviceFound(new Device("rew", "432"));
    }
}
