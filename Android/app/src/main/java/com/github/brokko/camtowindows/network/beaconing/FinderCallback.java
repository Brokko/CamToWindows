package com.github.brokko.camtowindows.network.beaconing;

import com.github.brokko.camtowindows.network.object.Device;

public interface FinderCallback {
    void onSearchStarted();

    void onSearchEnded();

    void onDeviceFound(Device device);
}
