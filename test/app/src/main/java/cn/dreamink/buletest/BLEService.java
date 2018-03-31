package cn.dreamink.buletest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.UUID;

import static android.content.ContentValues.TAG;


public class BLEService extends Service {

        static {
        System.loadLibrary("AlgorithmLib");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }



    //初始化算法
    //algorithm method
    public native static void initForAlgorithm();

    //初始化后面的东西
    public native static void readModelFile(AssetManager assetManager);

    //将设备返回的byte数据转化为心电图数据的算法
    public native static void writeValue(int idx, double data);

    //获取心电图数据
    public native static double[] readValue();

//    //获取情绪
//    public native static int getEmotionFromNative(double[] ecgdata);
//
//    //获取心电
//    public native static int[] getHRFromNative();

//    //关闭算法
    public native static void destroyAlgorithmThread();
}
