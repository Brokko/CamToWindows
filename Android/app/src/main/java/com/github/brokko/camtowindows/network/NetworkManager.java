package com.github.brokko.camtowindows.network;

import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkManager {
    private static NetworkManager instance;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public NetworkManager() {

    }

    public static NetworkManager getInstance() {
        if(instance == null)
            instance = new NetworkManager();

        return instance;
    }

    public void connect(String ip, int port) {
        Socket socket = new Socket();
    }
}
