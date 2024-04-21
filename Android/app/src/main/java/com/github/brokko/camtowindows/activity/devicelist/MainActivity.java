package com.github.brokko.camtowindows.activity.devicelist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.brokko.camtowindows.R;
import com.github.brokko.camtowindows.activity.connected.ConnectedActivity;
import com.github.brokko.camtowindows.activity.search.DeviceListAdapter;
import com.github.brokko.camtowindows.network.beaconing.BeaconFinder;
import com.github.brokko.camtowindows.network.beaconing.FinderCallback;
import com.github.brokko.camtowindows.databinding.ActivityMainBinding;
import com.github.brokko.camtowindows.network.object.Device;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements FinderCallback, DeviceListAdapter.ListEvent {
    private final ArrayList<Device> devices = new ArrayList<>();

    private final BeaconFinder beaconFinder = BeaconFinder.getInstance();

    private ArrayList<String> knownIPs;
    private ActivityMainBinding binding;
    private DeviceListAdapter listAdapter;
    private Device selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(new Intent(MainActivity.this, ConnectedActivity.class));

        beaconFinder.setCallback(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        RecyclerView deviceList = findViewById(R.id.list_device);
        listAdapter = new DeviceListAdapter(this);
        deviceList.setAdapter(listAdapter);
        deviceList.setLayoutManager(new LinearLayoutManager(this));

        Button btnSearch = findViewById(R.id.btn_search);

        Button btnConnect = findViewById(R.id.btn_connect);
        btnConnect.setOnClickListener(v -> {
            if (selected == null) {
                Toast.makeText(this, "Kein Gerät ausgewählt", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!devices.contains(selected)) {
                Toast.makeText(this, "Das Gerät ist nicht verfügbar!", Toast.LENGTH_LONG).show();
                beaconFinder.search();
                return;
            }

            // Neue activity
        });

        beaconFinder.search();
    }

    @Override
    public void onSearchStarted() {
        knownIPs = new ArrayList<>();
    }

    @Override
    public void onSearchEnded() {
        for (Device device : devices) {
            boolean known = false;
            for (String ip : knownIPs) {
                if (device.getIp().equals(ip)) {
                    known = true;
                    break;
                }
            }

            if (!known) {
                listAdapter.removeDevice(device.getIp());
                if (selected == device)
                    selected = null;
            }
        }

        knownIPs = null;
    }

    @Override
    public void onDeviceFound(Device device) {
        runOnUiThread(() -> listAdapter.addDevice(device.getIp()));
    }

    @Override
    public void onDeviceSelected(String ip) {

    }

    @Override
    public String getName(String ip) {
        return "null";
    }
}