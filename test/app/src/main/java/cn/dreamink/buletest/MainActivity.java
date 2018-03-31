package cn.dreamink.buletest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.dreamink.buletest.utils.LogUtils;
import cn.dreamink.buletest.utils.ToastUtil;

import static cn.dreamink.buletest.BLEService.destroyAlgorithmThread;
import static cn.dreamink.buletest.BLEService.initForAlgorithm;
import static cn.dreamink.buletest.BLEService.readModelFile;
import static cn.dreamink.buletest.BLEService.readValue;
import static cn.dreamink.buletest.BLEService.writeValue;

public class MainActivity extends AppCompatActivity {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private static ECGView ecgView;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic measurement_ch;
    private AssetManager assetManager;
    private int index = 0;
    private byte[] value;

    //     * 心电Service的UUID
    final static public UUID HEART_RATE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    //     * 用于配置Notify的Descriptor  客户端配置特点
    final static public UUID CHAR_CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //     * 电量和心电Characteristic的UUID
    final static public UUID HEART_RATE_MEASUREMENT = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");


    //     扫描具有此UUIDs的设备
    private UUID[] mUUIDs = {HEART_RATE};
    //     * 扫描状态，搜索设备时使用
    private boolean flagScan = true;
    //     * 扫描到的设备列表
    private List<DeviceData> list = new ArrayList<>();


    //     * HandlerThread，向Native层写入数据，同时各个调用Native层算法的Handler（ecghandler等）均从这里运行
    private HandlerThread pushThread = new HandlerThread("tempTask_pushData");
    //     * 绑定pushThread
    private Handler pushHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        assetManager = getResources().getAssets();
        pushThread.start();
        pushHandler = new Handler(pushThread.getLooper());

        ecgView = (ECGView) findViewById(R.id.ecg);

        //开启系统蓝牙
        setUpBLE();
        //搜索设备
//        findBLEDevices();
        //通过MAC地址来连接蓝牙设备
//        connecBLE();


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        ecgView.stopThread();
        //关闭算法
        destroyAlgorithmThread();
        mBluetoothAdapter.disable();
    }

    /**
     * 连接蓝牙
     */
    private void connecBLE() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (list.size() != 0) {
                    mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(list.get(0).getMac());
                    initForAlgorithm();
                    readModelFile(assetManager);
                    ToastUtil.showToast("连接：" + list.get(0).getMac());

                    //三个参数：一个Context对象，自动连接（boolean值,表示只要BLE设备可用是否自动连接到它），和BluetoothGattCallback调用。
                    bluetoothGatt = mBluetoothDevice.connectGatt(MainActivity.this, false, mBleCallback);
                }
                ecgView.startThread();
            }
        }).start();
    }

    //获取服务
    private void getSupportedServices() {
        BluetoothGattService service = bluetoothGatt.getService(HEART_RATE);
        if (service != null) {
            measurement_ch = service.getCharacteristic(HEART_RATE_MEASUREMENT);
        }
    }


    /**
     * 蓝牙连接状态及数据传输的回调
     */
    private final BluetoothGattCallback mBleCallback =
            new BluetoothGattCallback() {
                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    Log.e("==========", "onCharacteristicRead");
                }

                //传数据到蓝牙数据的时候调用
                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    Log.e("==========", "onCharacteristicWrite");
                    super.onCharacteristicWrite(gatt, characteristic, status);
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.e("==========", "333333333333333333333333333333");
                        //获取心电图服务后，请求心电图数据到measurement_ch
                        getSupportedServices();
                        //设置接受measurement_ch传回来的数据
                        boolean success = bluetoothGatt.setCharacteristicNotification(measurement_ch, true);
                        if (!success) {
                            Log.e("------", "Seting proper notification status for characteristic failed!");
                        }

//                        客户端特点配置
                        BluetoothGattDescriptor descriptor = measurement_ch.getDescriptor(CHAR_CLIENT_CONFIG);
                        if (descriptor != null) {
                            Log.e("descriptor", measurement_ch.getUuid() + " " + descriptor.getUuid());
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            bluetoothGatt.writeDescriptor(descriptor);
                        }
                    } else {
                        gatt.disconnect();
                        LogUtils.i("=====================没发现服务");
                    }
                }

                /**
                 * 蓝牙连接状态变化，连接成功且GATT_SUCCESS时
                 */
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.e("==========", "11111111111111111111111111111111111");
                            gatt.discoverServices();//连接成功，开始搜索服务，一定要调用此方法，否则获取不到服务
                        } else {
                            gatt.disconnect();
//                            finish();
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        ToastUtil.showToast("设备已断开，请退出重连");
                        Log.e("==========", "1111111111111111111111关闭");
//                        finish();
                    }
                }

                @Override
                /**
                 * 只有心电Characteristic会Notify，得到count_max个包后应该读取一次电量。
                 */
                public void onCharacteristicChanged(final BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    gatt.readCharacteristic(characteristic);
                    value = characteristic.getValue();
                    int format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    for (int i = 0; i < 10; i++) {
                        double data = (double) (characteristic.getIntValue(format, i * 2) * 256
                                + characteristic.getIntValue(format, i * 2 + 1)) / 8192;
                        pushData(index++, data);
                    }
                    Log.e("==============", "6666" + value.length);
                    pushHandler.post(new Runnable() {
                        @Override
                        public void run() {

                            double[] data = readValue();
                            Log.e("==========", "2222222222222222222222222222222222222" + data.length + ":::");
                            int size = data.length;
                            float[] temp = new float[size];
                            for (int i = 0; i < size; i++) {
                                temp[i] = (float) data[i];
                            }
                            ecgView.addData(temp);
                        }
                    });
                }
            };

    private void pushData(final int index, final double data) {
        pushHandler.post(new Runnable() {
            @Override
            public void run() {
                writeValue(index, data);
            }
        });
    }

    /**
     * 搜索设备
     */
    public void findBLEDevices() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    //最长扫描时间7500ms,15000ms
                    stopScanning();
                    flagScan = true;
                    LogUtils.i("开始搜索");
                    mBluetoothAdapter.startLeScan(mUUIDs, mLeScanCallback);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (flagScan) {
                                //15秒后没有检测到设备就关闭
                                stopScanning();
                                ToastUtil.showToast("没有检测到设备");
                                flagScan = false;
                            }
                        }
                    }, 15000);

                    int i = 15;
                    while (flagScan) {
                        try {
                            Thread.sleep(1000);
                            i--;
                            ToastUtil.showToast("正在搜索..." + i + "s");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 扫描到有效设备时立即终止扫描，将MAC地址放入overSearch事件中，并发送，
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            DeviceData data = new DeviceData();
            data.setMac(device.getAddress());
            data.setName(device.getName());
            Log.e("onLeScan", device.getAddress() + " " + device.getName());
            if (flagScan && device.getName() != null && device.getName().contains("HengAi ECG")) {
                stopScanning();
                list.add(data);
                flagScan = false;
                //搜索到可用设备就连接
                connecBLE();
            }
        }
    };

    /**
     * 停止扫描
     */
    public void stopScanning() {
        Log.e("=============", "stopScanning");
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }


    /**
     * 设备数据
     */
    private class DeviceData {
        String mac;
        String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }
    }

    /**
     * 开启蓝牙
     */
    private void setUpBLE() {
        Log.e("===================", "setUpBLE");
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        //如果蓝牙没开启
        if (!mBluetoothAdapter.isEnabled()) {

            Log.e("======================", "开启蓝牙");

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(enableBtIntent);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    setUpBLE();
                }
            }).start();
        } else {
            if (mBluetoothManager == null || mBluetoothAdapter == null) {
                ToastUtil.showToast("设备不支持蓝牙");
                finish();
            } else {
                Log.e("===============", "找设备");
                findBLEDevices();
            }
        }
    }


}
