package com.zjut.sky.androidbleformotor;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDevicesActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener{

    private static final String TAG = BluetoothDevicesActivity.class.getSimpleName();
    public static final String CONNECTED_DEVICE = "CONNECTED_DEVICE";

    private BluetoothLeService mBluetoothLeService;
    private IntentFilter intentFilter = new IntentFilter();

    private Switch scanSwitch;
    private ListViewForScrollView deviceLv;
    private Toolbar toolbar;

    private BluetoothAdapter mBluetoothAdapter;
    private List<BluetoothDevice> mAvailableDevices = new ArrayList<>();
    private DeviceAdapter mDeviceAdapter;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_GPS = 2;
    private static final int REQUEST_ENABLE_COARSE_GPS = 111;
    private static final long SCAN_PERIOD = 20000;

    private boolean mScanning = true;
    private Handler mHandler = new Handler();

    public boolean GATT_CONNECTED = false;
    private boolean mServiceConnected = false;

    private String mDeviceName;
    private String mDeviceAddress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_bluetooth_devices);

        scanSwitch = (Switch) findViewById(R.id.scan_device_switch);
        deviceLv = (ListViewForScrollView) findViewById(R.id.listView);
        toolbar = (Toolbar) findViewById(R.id.toolbar_bluetooth_scan);

        scanSwitch.setOnCheckedChangeListener(this);

        /**
         * 检查设备是否支持BLE
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "本设备不支持BLE", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

    }

    @Override
    protected void onResume() {
        super.onResume();

        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);

        registerReceiver(mGattReceiver,intentFilter);

        /**
         * 打开蓝牙
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        mDeviceAdapter = new DeviceAdapter(BluetoothDevicesActivity.this,mAvailableDevices);
        deviceLv.setAdapter(mDeviceAdapter);
        deviceLv.setOnItemClickListener(new DeviceListItemClickListener());

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        switch (compoundButton.getId()){
            case R.id.scan_device_switch:
                if (isChecked){
                    mAvailableDevices.clear();
                    mDeviceAdapter.notifyDataSetChanged();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        int hasLocationPermission = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);
                        if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(BluetoothDevicesActivity.this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_COARSE_GPS);
                            return;
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (isGpsEnable(BluetoothDevicesActivity.this)) {
                            scanLeDevice(true);
                        } else {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, REQUEST_ENABLE_GPS);
                        }
                        return;
                    }
                    scanLeDevice(true);
                } else {
                    scanLeDevice(false);
                }
                break;
            default:
                break;
        }
    }

    public static final boolean isGpsEnable(final Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_ENABLE_COARSE_GPS:
                if (permissions[0].equals(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    scanSwitch.setChecked(true);
                    scanLeDevice(true);
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("功能限制")
                                .setMessage("该APP需要赋予访问GPS的权限，不开启将无法正常工作")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                });
                        builder.show();
                        return;
                    }
                }
                break;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!mAvailableDevices.contains(device)) {
                        mAvailableDevices.add(device);
                    }
                    Log.d(TAG, "device--" + device.getName());
                    mDeviceAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            Log.d(TAG, "Start LeScan");
            Toast.makeText(BluetoothDevicesActivity.this, "Start Scan", Toast.LENGTH_SHORT).show();
            mHandler.postDelayed(runnable, SCAN_PERIOD);

        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mHandler.removeCallbacks(runnable);
        }
        invalidateSwitchButton();
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            invalidateSwitchButton();
        }
    };

    private void invalidateSwitchButton(){
        if (!mScanning){
            scanSwitch.setChecked(false);
        } else {
            scanSwitch.setChecked(true);
        }
    }



    private class DeviceAdapter extends BaseAdapter{
        private Context context;
        private List<BluetoothDevice> devices;

        public DeviceAdapter(Context context,List<BluetoothDevice> devices) {
            this.context = context;
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int i) {
            return devices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            if (view == null){
                viewHolder = new ViewHolder();
                view = LayoutInflater.from(context).inflate(R.layout.list_item,null);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = devices.get(i);
            final String deviceName = device.getName();

            if (deviceName != null && deviceName.length() > 0){
                viewHolder.deviceName.setText(device.getName());
            } else {
                viewHolder.deviceName.setText("未知设备");
            }

            return view;
        }

        class ViewHolder{
            TextView deviceName;
        }
    }

    private BluetoothDevice device;
    private class DeviceListItemClickListener implements AdapterView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            Log.d(TAG, "--------position------" + position);
            scanLeDevice(false);
            //final BluetoothDevice device = mBluetoothDevices.get(position);
            device = mAvailableDevices.get(position);
            mDeviceName = device.getName();
            mDeviceAddress = device.getAddress();
            Intent gattServiceIntent = new Intent(BluetoothDevicesActivity.this, BluetoothLeService.class);
            boolean bll = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

            if (bll) {
                Log.i(TAG, "----------bind success----------");
            } else {
                Log.w(TAG, "----------bind failed-----------");
            }
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mServiceConnected = true;
            //Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            //overTimeHandler.postDelayed(overTimeRunnable,OVER_TIME);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    private Intent intentActivity;
    private BroadcastReceiver mGattReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)){

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){

                Toast.makeText(BluetoothDevicesActivity.this,"连接失败",Toast.LENGTH_SHORT).show();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                GATT_CONNECTED = true;
                //overTimeHandler.removeCallbacks(overTimeRunnable);
                mAvailableDevices.clear();
                mDeviceAdapter.notifyDataSetChanged();
                intentActivity = new Intent(BluetoothDevicesActivity.this,BluetoothControlActivity.class);
                //intentActivity = new Intent(BluetoothScanActivity.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(CONNECTED_DEVICE, device);
                //bundle.putString(LoginActivity.BLUETOOTH_DEVICE_ADDRESS,mDeviceAddress);
                intentActivity.putExtras(bundle);
                startActivity(intentActivity);
            }
        }
    };
}
