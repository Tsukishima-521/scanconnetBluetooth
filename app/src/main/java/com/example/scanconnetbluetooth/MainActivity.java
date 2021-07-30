package com.example.scanconnetbluetooth;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class MainActivity extends Activity {
    private static boolean isChecked;
    String tag = "bluetoothConnect";
    //控件
    Switch bluetoothSwitch;
    Button stopScan;
    ListView connect_Paired, scanned;
    //蓝牙连接
    BluetoothAdapter bluetoothAdapter;

    //扫描到的设备列表 (index都一样）
    ArrayList<BluetoothDevice> scannedDevice;
    ArrayList<String> scannedDeviceName;
    ArrayList<String> scannedDeviceMAC;
    ArrayAdapter<String> scannedDeviceDisplay;
    //已配对的设备列表
    ArrayList<BluetoothDevice> pairedDevice;

    ArrayList<String> pairedDeviceName;
    ArrayList<String> pairedDeviceMAC;
    ArrayAdapter<String> pairedDeviceDisplay;
    //控制stopScan和reScan
    //boolean isChecked=false;
    int count = 0;

    //指定连接
    BluetoothDevice targetDevice;
    BluetoothDevice disconnectDevice;
    BluetoothGatt bluetoothGatt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        // 2. 设置接收广播的类型
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//要收到的action/intent
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        // 3. 动态注册：调用Context的registerReceiver（）方法
        registerReceiver(receiver,intentFilter);
        enableBluetooth();
    }

    public void init() {
        bluetoothSwitch = findViewById(R.id.btSwitch);
        stopScan = findViewById(R.id.stopScan);
        connect_Paired = findViewById(R.id.connectedandPaired);
        scanned = findViewById(R.id.Scanned);

        scannedDevice = new ArrayList<>();
        scannedDeviceName = new ArrayList<>();
        scannedDeviceMAC = new ArrayList<>();

        pairedDevice = new ArrayList<>();
        pairedDeviceName = new ArrayList<>();
        pairedDeviceMAC = new ArrayList<>();




        if (scannedDeviceName != null) {
            scannedDeviceDisplay = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, scannedDeviceName);
        }



    }


    public void enableBluetooth() {
        //获取蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            //先设置onCheckedChangeListener
            bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    //Log.d(tag, "触发onCheckedChanged Listener");
                    if (isChecked) {

                        //询问打开蓝牙
                        Intent enableBtItent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtItent, 1);
                        //开始扫描
                        scanDisplayAndStop();
                        stopScan.setEnabled(true);



                    } else {
                        //关闭蓝牙

                        bluetoothAdapter.disable();
                        stopScan.setEnabled(false);


                    }
                }
            });

            if (!bluetoothAdapter.isEnabled()) {
                Log.d(tag, "蓝牙未开启");

                bluetoothSwitch.setChecked(false);


            } else {
                Log.d(tag, "蓝牙已开启");

                    bluetoothSwitch.setChecked(true);
                    getAndDisplayPairedDevices();

            }
            //stop scanning Button
            if (bluetoothAdapter.isEnabled()) {
                stopScan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        count += 1;
                        if (count == 1) {
                            bluetoothAdapter.stopLeScan(scanCallback);
                            Log.d(tag, "停止扫描");
                            bondDevice();
                            //getAndDisplayPairedDevices();
                            stopScan.setText("reScan");
                            stopScan.setEnabled(false);
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                public void run() {
                                    stopScan.setEnabled(true);// yourMethod();
                                }
                            }, 100);   //3 seconds
                        } else if (count == 2) {
                            scanDisplayAndStop();
                            stopScan.setText("stop");
                            count = 0;
                        }
                    }
                });
            }
            //reScan button


        }


    }

    //蓝牙开启监听
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(MainActivity.this, "蓝牙成功开启", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(MainActivity.this, "没有蓝牙权限", Toast.LENGTH_SHORT).show();
                bluetoothSwitch.setChecked(false);
            }
        }
    }


    //蓝牙扫描设备监听
    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device.getName() != null) {
                //防止设备重复
                if (!scannedDeviceMAC.contains(device.getAddress())) {
                    scannedDeviceMAC.add(device.getAddress());
                    //Log.d(tag, "扫描到设备, 设备名：" + device.getName() + "| MAC地址： " + device.getAddress());
                    scannedDevice.add(device);
                    //scannedDeviceName.add(device.getName());
                    scannedDeviceDisplay.add(device.getName());
                }
            }

        }
    };

    //扫描设备并显示
    public void scanDisplayAndStop() {
        //每次要清空上次扫描到的设备
        scannedDeviceDisplay.clear();
        scannedDeviceMAC.clear();
        scannedDevice.clear();
        //扫描设备
        Log.d(tag, "开始扫描");
        bluetoothAdapter.startLeScan(scanCallback);
        //显示listView(实时更新？）
        //Log.d(tag, "设置adapter,显示ListView");
        scanned.setAdapter(scannedDeviceDisplay);



    }
    //获取已连接和已配对的设备
    public void getAndDisplayPairedDevices(){

        pairedDevice.clear();
        pairedDeviceName.clear();
        Log.d(tag, "获取配对设备");
        //每次重新建一个对象，不用clear
        Set<BluetoothDevice> pairedDeviceSet=  bluetoothAdapter.getBondedDevices();
        if (pairedDevice!=null)
        {
            if (pairedDeviceSet.size() > 0) {
                Log.d(tag, "有配对设备");
                for (BluetoothDevice device : pairedDeviceSet) {
                    pairedDevice.add(device);
                    pairedDeviceName.add(device.getName());
                    Log.d(tag, "已配对的设备, 设备名：" + device.getName() + "| MAC地址： " + device.getAddress());
                }
            }
            else {
                Log.d(tag, "无配对设备");
            }

        }
        else {
            Log.d(tag, "set 为null");
        }

        if (pairedDeviceName != null) {
            pairedDeviceDisplay = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, pairedDeviceName);
            connect_Paired.setAdapter(pairedDeviceDisplay);
            BondDeviceRemovable();
        }

    }
    //reflection从blueTooth device获取removeBond method
    public void unpairDevice(BluetoothDevice device){
        Method removeBond= null;
        try {
            removeBond = device.getClass().getMethod("removeBond",(Class[])null);//方法名和方法形参列表的类
            removeBond.setAccessible(true);
            Boolean remove=(Boolean) removeBond.invoke(device,(Object[]) null);
            Log.d(tag,"removeBond执行："+remove.toString());

        } catch (NoSuchMethodException e) {
            Log.d(tag, "解除配对异常："+e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.d(tag, "解除配对异常："+e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Log.d(tag, "解除配对异常："+e.getMessage());
            e.printStackTrace();
        }
        catch (Exception ex)
        {
            Log.d(tag, "解除配对异常："+ex.getMessage());
            ex.printStackTrace();
        }

    }
    public void BondDeviceRemovable(){
        connect_Paired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                disconnectDevice = pairedDevice.get(position);
                Log.d(tag, "扫描到设备, 设备名：" + disconnectDevice.getName() + "| MAC地址： " + disconnectDevice.getAddress() + "| 配对状态：" + disconnectDevice.getBondState());
                unpairDevice(disconnectDevice);
                Log.d(tag, "removeBond执行完毕");
                //connect_Paired.setOnItemClickListener(null);
                //Log.d(tag, "直到收到bond_none,不能点击选择remove");

                //用reflection获取到一个方法，不知道为什么只能用映射，直接调没有（被封装起来了）

            }

        });

    }



    //选择设备连接
    public void bondDevice() {


        scanned.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                targetDevice = scannedDevice.get(position);
                Log.d(tag, "所选配对设备为： "+targetDevice.getName());
                try {
                    targetDevice.createBond();
                    Toast.makeText(MainActivity.this, "开始与"+targetDevice.getName()+"配对...", Toast.LENGTH_LONG).show();
                    //scanned.setOnItemClickListener(null);

                } catch (Exception e) {
                    Log.d(tag, "配对异常："+e.getMessage());

                }

                //targetDevice.connectGatt(MainActivity.this, false, bluetoothGATTCallback);
            }
        });

    }
    //接收配对intent, broadcast reciever要注册广播
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                getAndDisplayPairedDevices();
                Log.d(tag, "ACTION_BOND_STATE_CHANGED");
                if (targetDevice!=null&&disconnectDevice!=null) {
                    if (targetDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                        //means device paired
                        Log.d(tag, "已配对bonded");
                        Toast.makeText(MainActivity.this, targetDevice.getName()+"配对成功", Toast.LENGTH_SHORT).show();
                        getAndDisplayPairedDevices();

                        //targetDevice = null;
                    } else if (targetDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                        Log.d(tag, "与" + targetDevice.getName() + "配对中bonding...");
                        Toast.makeText(MainActivity.this, "获取" + targetDevice.getName() + "配对申请...", Toast.LENGTH_LONG).show();
                    /*}else if (targetDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                            Log.d(tag, "与"+targetDevice.getName()+"配对失败");
                            Toast.makeText(MainActivity.this, "与"+targetDevice.getName()+"配对失败", Toast.LENGTH_SHORT).show();
*/

                    }  else if (disconnectDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                        Log.d(tag, "disconnectDevice状态为未配对none");
                        Toast.makeText(MainActivity.this, "解除配对成功", Toast.LENGTH_SHORT).show();
                        //disconnectDevice=null;
                        getAndDisplayPairedDevices();

                    }
                }
            }
        }
    };

    //GATT协议连接回调
    public BluetoothGattCallback bluetoothGATTCallback = new BluetoothGattCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        //连接GATT状态改变
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String result = "";
            switch (newState) {
                case 0: {
                    result = "未连接";
                    gatt.disconnect();


                }
                break;
                case 1: {
                    result = "连接中...";
                    if (targetDevice == null) {
                        Log.d(tag, "未找到设备，连接失败");
                        return;
                    }
                }
                break;
                case 2: {
                    bluetoothGatt = gatt;
                    result = "连接成功";
                    if (bluetoothGatt != null) {
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        //设置MTU模式
                        bluetoothGatt.requestMtu(256);
                        bluetoothGatt.discoverServices();//discoverService完成后，有可能成功，有可能失败，成功了怎样，失败了怎样。就在回调onServiceDiscovered
                    }
                }
                break;
                case 3: {
                    result = "连接失败";
                    gatt.disconnect();
                }
                break;
                case 4: {
                    result = "连接关闭";
                    gatt.disconnect();
                }
                break;
            }
            Log.d(tag, "GATT: on connection state change " + result);
        }
    };
}




