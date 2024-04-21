package com.github.brokko.camtowindows.activity.search;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.brokko.camtowindows.R;

import java.util.ArrayList;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceHolder> {
    private final ArrayList<String> ips = new ArrayList<>();

    private final ListEvent event;

    private View selected;

    public DeviceListAdapter(ListEvent event) {
        this.event = event;
    }

    @NonNull
    @Override
    public DeviceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DeviceHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.holder_device, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceHolder holder, int position) {
        holder.setData(event.getName(ips.get(position)), ips.get(position));

        holder.itemView.setOnClickListener(v -> {
            if (selected != null)
                selected.setBackgroundColor(Color.WHITE);

            v.setBackgroundColor(Color.GRAY);
            selected = v;
        });
    }

    @Override
    public int getItemCount() {
        return ips.size();
    }

    public void addDevice(String ip) {
        ips.add(ip);
        notifyItemInserted(ips.size() - 1);
    }

    public void removeDevice(String ip) {
        if (!ips.contains(ip))
            return;

        int index = ips.indexOf(ip);
        ips.remove(index);
        notifyItemRemoved(index);
    }

    public static class DeviceHolder extends RecyclerView.ViewHolder {
        public String ip;

        public DeviceHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void setData(String name, String ip) {
            this.ip = ip;

            TextView txtName = itemView.findViewById(R.id.txt_device_name);
            txtName.setText(name);

            TextView txtIP = itemView.findViewById(R.id.txt_device_ip);
            txtIP.setText(ip);
        }
    }

    public interface ListEvent {
        void onDeviceSelected(String ip);

        String getName(String ip);
    }
}
