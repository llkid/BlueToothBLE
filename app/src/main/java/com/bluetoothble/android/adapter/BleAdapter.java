package com.bluetoothble.android.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bluetoothble.android.R;

import java.util.List;

public class BleAdapter extends BaseAdapter {

    private Context mContext;
    private List<BluetoothDevice> mBluetoothDevices;
    private List<Integer> mRssis;

    public BleAdapter(Context context, List<BluetoothDevice> bluetoothDevices, List<Integer> rssis) {
        this.mContext = context;
        this.mBluetoothDevices = bluetoothDevices;
        this.mRssis = rssis;
    }

    @Override
    public int getCount() {
        return mBluetoothDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return mBluetoothDevices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.bluetooth_device_list_item, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        BluetoothDevice bluetoothDevice = (BluetoothDevice) getItem(position);
        viewHolder.name.setText(bluetoothDevice.getName());
        viewHolder.introduce.setText(bluetoothDevice.getAddress());
        viewHolder.tvRssis.setText(mRssis.get(position) + "");
        return convertView;
    }

    private class ViewHolder {
        public TextView name;
        public TextView introduce;
        public TextView tvRssis;
        public ViewHolder(View view) {
            name = view.findViewById(R.id.name);
            introduce = view.findViewById(R.id.introduce);
            tvRssis = view.findViewById(R.id.rssi);
        }
    }
}
