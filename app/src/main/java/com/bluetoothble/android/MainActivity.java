package com.bluetoothble.android;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blankj.utilcode.util.ToastUtils;
import com.bluetoothble.android.adapter.BleAdapter;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private static final String TAG ="ble_tag" ;
    ProgressBar pbSearchBle;
    ImageView ivSerBleStatus;
    TextView tvSerBleStatus;
    TextView tvSerBindStatus;
    ListView bleListView;
    private LinearLayout operaView;
    private Button btnWrite;
    private Button btnRead;
    private EditText etWriteContent;
    private TextView tvResponse;
    private List<BluetoothDevice> mDatas;
    private List<Integer> mRssis;
    private BleAdapter mAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private boolean isScaning=false;
    private boolean isConnecting=false;
    private BluetoothGatt mBluetoothGatt;

    //服务和特征值
    private UUID write_UUID_service;
    private UUID write_UUID_chara;
    private UUID read_UUID_service;
    private UUID read_UUID_chara;
    private UUID notify_UUID_service;
    private UUID notify_UUID_chara;
    private UUID indicate_UUID_service;
    private UUID indicate_UUID_chara;
    private String hex="7B46363941373237323532443741397D";
    BluetoothLeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_device);

        initView();
        initData();

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 0);
        }
    }

    private void initData() {
        mDatas = new ArrayList<>();
        mRssis = new ArrayList<>();
        mAdapter = new BleAdapter(MainActivity.this, mDatas, mRssis);
        bleListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    private void initView() {
        pbSearchBle = (ProgressBar) findViewById(R.id.progress_ser_bluetooth);
        ivSerBleStatus = (ImageView) findViewById(R.id.iv_ser_ble_status);
        tvSerBleStatus = (TextView) findViewById(R.id.tv_ser_ble_status);
        tvSerBindStatus = (TextView) findViewById(R.id.tv_ser_bind_status);
        bleListView = (ListView) findViewById(R.id.ble_list_view);
        operaView = (LinearLayout) findViewById(R.id.opera_view);
        btnWrite = (Button) findViewById(R.id.btnWrite);
        btnRead = (Button) findViewById(R.id.btnRead);
        etWriteContent = (EditText) findViewById(R.id.et_write);
        tvResponse = (TextView) findViewById(R.id.tv_response);

        btnRead.setOnClickListener(this);
        btnWrite.setOnClickListener(this);
        ivSerBleStatus.setOnClickListener(this);
        bleListView.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnRead:
                readData();
                break;
            case R.id.btnWrite:
                writeData();
                break;
            case R.id.iv_ser_ble_status:
                if (isScaning) {
                    tvSerBindStatus.setText("停止搜索");
                    stopScanDevice();
                } else {
                    checkPermissions();
                }
                break;
            default:
                break;
        }
    }

    private void checkPermissions() {
        RxPermissions rxPermissions = new RxPermissions(MainActivity.this);
        rxPermissions.request(Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe(new io.reactivex.functions.Consumer<Boolean>() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            //同意该权限
                            scanDevice();
                        } else {
                            ToastUtils.showLong("用户开启权限后才能使用");
                        }
                    }
                });
    }

    private void writeData() {
        BluetoothGattService service = mBluetoothGatt.getService(write_UUID_service);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(write_UUID_chara);
        byte[] data;
        String content = etWriteContent.getText().toString();
        if (!TextUtils.isEmpty(content)) {
            data = HexUtil.hexStringToBytes(content);
        } else {
            data = HexUtil.hexStringToBytes(hex);
        }
        //数据大于个字节 分批次写入
        if (data.length > 20) {
            Log.i(TAG, "writeData: length = " + data.length);
            int num = 0;
            if (data.length % 20 != 0) {
                num = data.length / 20 + 1;
            } else {
                num = data.length / 20;
            }

            for (int i = 0; i < num; ++i) {
                byte[] tempArry;
                if (i == num - 1) {
                    tempArry = new byte[data.length - i * 20];
                    System.arraycopy(data, i * 20, tempArry, 0, data.length - i * 20);
                } else {
                    tempArry = new byte[20];
                    System.arraycopy(data, i * 20, tempArry, 0, 20);
                }
                characteristic.setValue(tempArry);
                mBluetoothGatt.writeCharacteristic(characteristic);
            }
        } else {
            characteristic.setValue(data);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    private void readData() {
        BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(read_UUID_service).getCharacteristic(read_UUID_chara);
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (view.getId()) {
            case R.id.ble_list_view:
                if (isScaning) {
                    stopScanDevice();
                }
                if (!isConnecting) {
                    isConnecting = true;
                    BluetoothDevice bluetoothDevice = mDatas.get(position);
                    //连接设备
                    tvSerBindStatus.setText("连接中");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
                    } else {
                        mBluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, true, gattCallback);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * 停止扫描
     * */
    private void stopScanDevice() {
        isScaning = false;
        pbSearchBle.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (scanner == null) {
                scanner = mBluetoothAdapter.getBluetoothLeScanner();
            }
            scanner.stopScan(scanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(scanLeCallback);
        }
    }

    /**
     * 开始扫描 10秒后自动停止
     * */
    private void scanDevice() {
        tvSerBindStatus.setText("正在搜索...");
        isScaning = true;
        pbSearchBle.setVisibility(View.VISIBLE);
        //创建ScanSettings的build对象用于设置参数
        ScanSettings.Builder builder = new ScanSettings.Builder()
                //设置高功耗模式
                .setScanMode(SCAN_MODE_LOW_LATENCY);
        //android 6.0添加设置回调类型、匹配模式等
        if(Build.VERSION.SDK_INT >= 23) {
            //定义回调类型
            builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
            //设置蓝牙LE扫描滤波器硬件匹配的匹配模式
            builder.setMatchMode(ScanSettings.MATCH_MODE_STICKY);
        }
        //芯片组支持批处理芯片上的扫描
        if (mBluetoothAdapter.isOffloadedScanBatchingSupported()) {
            //设置蓝牙LE扫描的报告延迟的时间（以毫秒为单位）
            //设置为0以立即通知结果
            builder.setReportDelay(0L);
        }
        builder.build();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (scanner == null) {
                scanner = mBluetoothAdapter.getBluetoothLeScanner();
            }
            scanner.startScan(null, builder.build(),scanCallback);
        } else {
            mBluetoothAdapter.startLeScan(scanLeCallback);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //扫描结束
                isScaning = false;
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
                    scanner.stopScan(scanCallback);
                } else {
                    mBluetoothAdapter.stopLeScan(scanLeCallback);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pbSearchBle.setVisibility(View.GONE);
                        tvSerBindStatus.setText("搜索结束");
                    }
                });
            }
        }, 10000); // 10 mec
    }

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            final BluetoothDevice bluetoothDevice = result.getDevice();
            Log.d(TAG, "execute here");
            if (!mDatas.contains(bluetoothDevice)) {
                mDatas.add(bluetoothDevice);
                mRssis.add(result.getRssi());
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            super.onBatchScanResults(results);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "扫描onBatchScanResults==" + results.size());
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e(TAG, "扫描失败onScanFailed， errorCode==" + errorCode);
        }
    };

    BluetoothAdapter.LeScanCallback scanLeCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Log.d(TAG, "run: scanning...");
            if (!mDatas.contains(device)) {
                mDatas.add(device);
                mRssis.add(rssi);
                mAdapter.notifyDataSetChanged();
            }
        }
    };

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        /**
         * 断开或连接 状态发生变化时调用
         * */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e(TAG, "on Connection State Change");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.i(TAG, "连接成功");
                    gatt.discoverServices();
                }
            } else {
                Log.i(TAG, "连接失败" + status);
                mBluetoothGatt.close();
                isConnecting = false;
            }
        }

        /**
         * 发现设备（真正建立连接）
         * */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            isConnecting = false;
            Log.i(TAG, "onServicesDiscovered 建立连接");
            //获取初始化服务和特征值
            initServiceAndChara();
            mBluetoothGatt.setCharacteristicNotification(mBluetoothGatt.getService(notify_UUID_service).getCharacteristic(notify_UUID_chara), true);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bleListView.setVisibility(View.GONE);
                    operaView.setVisibility(View.VISIBLE);
                    tvSerBindStatus.setText("已连接");
                }
            });
        }

        /**
         * 读操作的回调
         * */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.i(TAG, "onCharacteristicRead() executed");
        }

        /**
         * 写操作的回调
         * */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.i(TAG,"onCharacteristicWrite()  status = " + status + ",value = " + HexUtil.encodeHexStr(characteristic.getValue()));
        }

        /**
         * 接收到硬件返回的数据
         * */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, "onCharacteristicChanged()" + characteristic.getValue());
            final byte[] data = characteristic.getValue();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addText(tvResponse, bytes2Hex(data));
                }
            });
        }
    };

    private void addText(TextView textView, String content) {
        textView.append(content + "\n");
        int offset = textView.getLineCount() * textView.getLineHeight();
        if (offset > textView.getHeight()) {
            textView.scrollTo(0, offset - textView.getHeight());
        }
    }

    private static final String HEX = "0123456789abcdef";
    private static String bytes2Hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            // 取出这个字节的高4位，然后与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt((b >> 4) & 0x0f));
            // 取出这个字节的低位，与0x0f与运算，得到一个0-15之间的数据，通过HEX.charAt(0-15)即为16进制数
            sb.append(HEX.charAt(b & 0x0f));
        }
        return sb.toString();
    }

    /**
     * 初始化服务uuid
     * */
    private void initServiceAndChara() {
        List<BluetoothGattService> bluetoothGattServices = mBluetoothGatt.getServices();
        for (BluetoothGattService bluetoothGattService : bluetoothGattServices) {
            List<BluetoothGattCharacteristic> characteristics = bluetoothGattService.getCharacteristics();
            for (BluetoothGattCharacteristic characteristic : characteristics) {
                int charaProp = characteristic.getProperties();
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    read_UUID_chara = characteristic.getUuid();
                    read_UUID_service = bluetoothGattService.getUuid();
                    Log.i(TAG, "read_chara = " + read_UUID_chara + " read_service = " + read_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    write_UUID_chara = characteristic.getUuid();
                    write_UUID_service = bluetoothGattService.getUuid();
                    Log.i(TAG, "write_chara = " + write_UUID_chara + " write_service = " + write_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                    write_UUID_chara = characteristic.getUuid();
                    write_UUID_service = bluetoothGattService.getUuid();
                    Log.i(TAG, "write_chara = " + write_UUID_chara + " write_service = " + write_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    notify_UUID_chara = characteristic.getUuid();
                    notify_UUID_service = bluetoothGattService.getUuid();
                    Log.i(TAG, "notify_UUID_chara = " + notify_UUID_chara + " notify_UUID_service = " + notify_UUID_service);
                }
                if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    indicate_UUID_chara = characteristic.getUuid();
                    indicate_UUID_service = bluetoothGattService.getUuid();
                    Log.i(TAG, "indicate_UUID_chara = " + indicate_UUID_chara + " indicate_UUID_service = " + indicate_UUID_service);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothGatt.disconnect();
    }
}
