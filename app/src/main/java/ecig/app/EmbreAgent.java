package ecig.app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;

import ecig.app.ble.BluetoothLeService;

/**
 * Created by kssworld93 on 1/4/15.
 *
 *
 * Provides code to help the UI
 *  Init the BLEService,
 *  Scan for devices,
 *  Pair w a device,
 *  Disconnect a device
 *
 */
public class EmbreAgent {
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    Context context;
    BluetoothLeService mBluetoothLeService;


    // This is the accelerometer service UUID of the SensorTag.
    static final String SERVICE_UUID = "F000AA10-0451-4000-B000-000000000000";

    interface EmbreCB {
        public void call();
    }

    enum ConnState { DISCONNECTED, CONNECTED, CONNECTING }
    ConnState connState;

    public final static String TAG = "ecig.app.EmbreAgent";

    public void initialize(Context context) {
        this.context = context;
        initBluetoothAdapter();
        //initConnectionService();
    }

    private void initBluetoothAdapter() {

        // Initializes a Bluetooth adapter. For API level 18 and above, get a
        // reference to BluetoothAdapter through BluetoothManager.
        mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            throw new RuntimeException("No bluetooth");
        }

        // Ensures Bluetooth is enabled. If not, displays a dialog requesting user permission to enable Bluetooth.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
        }
    }

    // BluetoothProfile.STATE_CONNECTED or BluetoothProfile.STATE_DISCONNECTED;
    int state = BluetoothProfile.STATE_DISCONNECTED;
    private boolean mScanning = false;
    BluetoothGatt mGatt;

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        // https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback.html#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            // If connected, do callback
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Success.");
            } else {
                Log.i(TAG, "Failure.");
            }
            state = newState;
        }
    };


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            String name = device.getName();
            if (name != null) {
                String logoutput = name + " " + device.toString();
                Log.i(TAG, logoutput);
                if (logoutput.equals("SensorTag 34:B1:F7:D1:35:03")) {
                    // stop scanning
                    scanForDevice(null);
                    // connect to it http://blog.stylingandroid.com/bluetooth-le-part-4/
                    mGatt = device.connectGatt(context, true, mGattCallback);

                }
            }

        }
    };

    private EmbreCB stopScanCB;
    // pass null to stop scanning
    public void scanForDevice(EmbreCB onScanStop) {
        if (onScanStop != null) {
            if (mScanning) {
                throw new RuntimeException("Attempt to scan while already scanning");
            }
            mScanning = true;
            stopScanCB = onScanStop;
            // TODO handle error in case scan doesn't start successfully
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            if (!mScanning) {
                throw new RuntimeException("Attempt to stop scanning while not scanning");
            }
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            stopScanCB.call();
            stopScanCB = null;

        }

    }
}
