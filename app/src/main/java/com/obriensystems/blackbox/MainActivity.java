package com.obriensystems.blackbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import com.obriensystems.blackbox.R;
//import android.R;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends FragmentActivity implements
	LocationListener,SensorEventListener, ActionBar.OnNavigationListener {
	public static final UUID HRP_SERVICE = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    public static final UUID DEVICE_INFORMATION = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    public static final UUID HEART_RATE_MEASUREMENT_CHARAC = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    public static final UUID CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID BODY_SENSOR_LOCATION = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");
    public static final UUID SERIAL_NUMBER_STRING = UUID.fromString("00002A25-0000-1000-8000-00805f9b34fb");
    public static final UUID MANUFATURE_NAME_STRING = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
    public static final UUID ICDL = UUID.fromString("00002A2A-0000-1000-8000-00805f9b34fb");
    public static final UUID HeartRate_ControlPoint = UUID.fromString("00002A39-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");

    static final String TAG = "HRPService";
    public static final int HRP_CONNECT_MSG = 1;
    public static final int HRP_DISCONNECT_MSG = 2;
    public static final int HRP_READY_MSG = 3;
    public static final int HRP_VALUE_MSG = 4;
    public static final int GATT_DEVICE_FOUND_MSG = 5;

    /** Source of device entries in the device list */
    public static final int DEVICE_SOURCE_SCAN = 0;
    public static final int DEVICE_SOURCE_BONDED = 1;
    public static final int DEVICE_SOURCE_CONNECTED = 2;
    public static final int RESET_ENERGY_EXPANDED = 1;
    /** Intent extras */
    public static final String EXTRA_DEVICE = "DEVICE";
    public static final String EXTRA_RSSI = "RSSI";
    public static final String EXTRA_SOURCE = "SOURCE";
    public static final String EXTRA_ADDR = "ADDRESS";
    public static final String EXTRA_CONNECTED = "CONNECTED";
    public static final String EXTRA_STATUS = "STATUS";
    public static final String EXTRA_UUID = "UUID";
    public static final String EXTRA_VALUE = "VALUE";
    public static final String BSL_VALUE = "com.siso.ble.hrpservice.bslval";
    public static final String HRM_VALUE = "com.siso.ble.hrpservice.hrmval";
    public static final String SERIAL_STRING = "com.siso.ble.hrpservice.serialstring";
    public static final String MANF_NAME = "com.siso.ble.hrpservice.manfname";
    public static final String ICDL_VALUE = "com.siso.ble.hrpservice.icdl";
    public static final String HRM_EEVALUE = "com.siso.ble.hrpservice.eeval";
    public static final String HRM_RRVALUE = "com.siso.ble.hrpservice.rrval";

    public static final int ADV_DATA_FLAG = 0x01;
    public static final int LIMITED_AND_GENERAL_DISC_MASK = 0x03;
    public static final int FIRST_BITMASK = 0x01;
    public static final int SECOND_BITMASK = FIRST_BITMASK << 1;
    public static final int THIRD_BITMASK = FIRST_BITMASK << 2;
    public static final int FOURTH_BITMASK = FIRST_BITMASK << 3;
    public static final int FIFTH_BITMASK = FIRST_BITMASK << 4;
    public static final int SIXTH_BITMASK = FIRST_BITMASK << 5;
    public static final int SEVENTH_BITMASK = FIRST_BITMASK << 6;
    public static final int EIGTH_BITMASK = FIRST_BITMASK << 7;

    private Handler mActivityHandler = null;
    private Handler mDeviceListHandler = null;
    public boolean isNoti = false;
    private static final int STATE_OFF = 10;
    private static final int HRP_PROFILE_CONNECTED = 20;
    private static final int HRP_PROFILE_DISCONNECTED = 21;
    public int mState = HRP_PROFILE_DISCONNECTED;
    private static final int STATE_READY = 10;
    
    private static final String SERVER_ADDRESS = "http://biometric.elasticbeanstalk.com/FrontController?action=setGps&";

	
	private BluetoothAdapter mBtAdapter;
    private BluetoothDevice mDevice = null;
    List<BluetoothDevice> deviceList;
    //private DeviceAdapter deviceAdapter;
    private ServiceConnection onService = null;
    Map<String, Integer> devRssiValues;

    
	private static final int green = Color.GREEN;
	private static final int magenta = Color.MAGENTA;
	private static final int red = Color.RED;
	private static final int blue = Color.CYAN;
	private static final int yellow = Color.YELLOW;
	private static final int grey = Color.LTGRAY;
	static int state = 0;
	static int gpsState = 0;

	static final String tag = "Main"; // for Log
	static int count = 0;	
	//TextView txtInfo;
	EditText statusInfo;
	EditText userIdView;
	
	ToggleButton toggleButton00;
	ToggleButton toggleButton10;
	ToggleButton toggleButton20;
	ToggleButton toggleButton30;
	ToggleButton toggleButton01;
	ToggleButton toggleButton11;
	ToggleButton toggleButton21;
	ToggleButton toggleButton31;
	ToggleButton toggleButton02;
	ToggleButton toggleButton12;
	ToggleButton toggleButton22;
	ToggleButton toggleButton32;
	ToggleButton toggleButton03;
	ToggleButton toggleButton13;
	ToggleButton toggleButton23;
	ToggleButton toggleButton33;
	ToggleButton toggleButton04;
	ToggleButton toggleButton14;
	ToggleButton toggleButton24;
	ToggleButton toggleButton34;
	ToggleButton toggleButton05;
	ToggleButton toggleButton15;
	ToggleButton toggleButton25;
	ToggleButton toggleButton35;
	ToggleButton toggleButton06;
	ToggleButton toggleButton16;
	ToggleButton toggleButton26;
	ToggleButton toggleButton36;
	ToggleButton toggleButtonCount;

	private int interval;
	private Timer timer = new Timer();

	private SensorManager mSensorManager;
	private Sensor mPressure;
	private Sensor mTemperature;
    private Sensor mGravity;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mLight;
    private Sensor mLinearAcceleration;
    private Sensor mMagneticField;
    //private Sensor mOrientation;
    private Sensor mProximity;
    private Sensor mRelativeHumidity;
    private Sensor mRotationVector;
    private int _heartRate = 1;

	LocationManager lm; 
	StringBuilder sb;
	StringBuilder urlBuffer;
	int noOfFixes = 0;
	static String userId = "22";
	static String transactionId = "22";
	/*
	 
00 bearing
10 Longitude
20 Latitude
30 Altitude

01 Temp c
11 Pressure hPa
21 Humidity %
31 speed m/s

02 X Magnetic uT
03 Y Magnetic uT
04 Z Magnetic uT
12 Z Azimuth
13 X Pitch
14 Y Roll
22 X Accel+g m/s2
23 Y Accel+g m/s2
24 Z Accel+g m/s2
32 X Gravity m/s2
33 Y Gravity m/s2
34 Z Gravity m/s2

04 X Gyro r/s
14 Y Gyro r/s
24 Z Gyro r/s

05 HeartRate
15 sound
25 Light lx
35 Proximity cm

X Lin Acc m/s2
Y Lin Acc m/s2
Z Lin Acc m/s2
X Rot
y Rot
Z Rot
S Rot
	 */
	float _pressure = 0;
	float _temp = 0;
	double _longitude = 0;
	double _latitude = 0; 
	double _altitude = 0;
	double _accuracy = 0;
	float _bearing = 0;
	float _speed = 0;
	float _proximity = 0;
	float _light = 0;
	String _provider = null;
	long _time = 0;
	float _magX = 0;
	float _magZ = 0;
	float _magY = 0;
	float _gyroX = 0;
	float _gyroZ = 0;
	float _gyroY = 0;
	float _gravityX = 0;
	float _gravityZ = 0;
	float _gravityY = 0;
	// deprecated
	//float _pitch = 0;
	//float _roll = 0;
	//float _azimuth = 0;
	float _linAccelX = 0;
	float _linAccelY = 0;
	float _linAccelZ = 0;
	float _accelX = 0;
	float _accelY = 0;
	float _accelZ = 0;
	float _rotX = 0;
	float _rotY = 0;
	float _rotZ = 0;
	float _rotS = 0;
	float _humidity = 0;
	int _gpsCount = 0;

	
	//http://developer.android.com/training/basics/network-ops/connecting.html
	public void myClickHandler(String urlBuffer) {
		// https://obrienscience-obrienlabs.java.us1.oraclecloudapps.com//gps/FrontController?action=setGps&u=1&lt=45.3439172&lg=-75.9404402&ac=1.1
	    //String stringUrl = "https://obrienscience-obrienlabs.java.us1.oraclecloudapps.com/gpsbio/FrontController?action=setGps" + urlBuffer;//urlText.getText().toString();
	    String stringUrl = SERVER_ADDRESS + urlBuffer;//urlText.getText().toString();
	    
	    ConnectivityManager connMgr = (ConnectivityManager) 
	        getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
	    if (networkInfo != null && networkInfo.isConnected()) {
	        // fetch data
	    	System.out.println(stringUrl);
	    	new DownloadWebpageTask().execute(stringUrl);
	    } else {
	        // display error
	    	//txtInfo.setText("No network connection available.");
	    }
	}	
	
	
	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
/*
    private Handler mHandler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            /*case HRPService.GATT_DEVICE_FOUND_MSG:
                Bundle data = msg.getData();
                final BluetoothDevice device = data.getParcelable(BluetoothDevice.EXTRA_DEVICE);
                final int rssi = data.getInt(HRPService.EXTRA_RSSI);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addDevice(device, rssi);
                    }
                });
                break;
            default:
                super.handleMessage(msg);
            }
        }
    };
    */
    
   /* 
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case HRPService.HRP_CONNECT_MSG:
                Log.d(TAG, "mHandler.HRP_CONNECT_MSG");
                runOnUiThread(new Runnable() {
                    public void run() {
                        mState = HRP_PROFILE_CONNECTED;
                        //setUiState();
                    }
                });
                break;

            case HRPService.HRP_DISCONNECT_MSG:
                Log.d(TAG, "mHandler.HRP_DISCONNECT_MSG");
                runOnUiThread(new Runnable() {
                    public void run() {
                        mState = HRP_PROFILE_DISCONNECTED;
                        //setUiState();
                    }
                });
                break;
*/
           /* case HRPService.HRP_READY_MSG:
                Log.d(TAG, "mHandler.HRP_READY_MSG");
                runOnUiThread(new Runnable() {
                    public void run() {
                        mState = STATE_READY;
                        //setUiState();
                    }
                });
*/
      /*      case HRPService.HRP_VALUE_MSG:
                Log.d(TAG, "mHandler.HRP_VALUE_MSG");
                Bundle data1 = msg.getData();
                final byte[] bslval = data1.getByteArray(HRPService.BSL_VALUE);
                final int hrmval = data1.getInt(HRPService.HRM_VALUE, 0);
                final int eeval = data1.getInt(HRPService.HRM_EEVALUE, 0);
                final String serialno = data1.getString(HRPService.SERIAL_STRING);
                final byte[] manfname = data1.getByteArray(HRPService.MANF_NAME);
                final byte[] icdl = data1.getByteArray(HRPService.ICDL_VALUE);
                final ArrayList<Integer> rrval = data1.getIntegerArrayList(HRPService.HRM_RRVALUE);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (bslval != null) {
                            try {
                                Log.i(TAG, "BYTE BSL VAL =" + bslval[0]);
                                TextView bsltv = (TextView) findViewById(R.id.BodySensorLocation);
                                bsltv.setText("\t" + mContext.getString(R.string.BodySensorLocation)
                                        + getBodySensorLocation(bslval[0]));
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());

                            }

                        }
                        if (serialno != null) {
                            try {
                                TextView serialtxt = (TextView) findViewById(R.id.SerialNumberString);
                                serialtxt.setText("\t" + mContext.getString(R.string.SerialNumberString) + "::"
                                        + serialno);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }

                        }
                        if (manfname != null) {
                            try {
                                String m = new String(manfname, "UTF-8");
                                TextView manfnametxt = (TextView) findViewById(R.id.ManfName);
                                manfnametxt.setText("\t" + mContext.getString(R.string.ManfName) + "::" + m);
                            } catch (UnsupportedEncodingException  e) {
                                Log.e(TAG, e.toString());
                            }

                        }
                        if (icdl != null) {
                            try {
                                display_reg_data(icdl);
                                TextView icdl_txt = (TextView) findViewById(R.id.CertDataList);
                                icdl_txt.setText("\t" + mContext.getString(R.string.CertDataList)
                                        + ":: Hex Value :: 0x" + toHex(new String(icdl)));

                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }

                        }
                        if (hrmval >= 0) {
                            TextView hrmtv = (TextView) findViewById(R.id.HeartRateValue);
                            hrmtv.setText("\t" + mContext.getString(R.string.HeartRateValue) + hrmval
                                    + " beats per minute");
                        } else {
                            TextView hrmtv = (TextView) findViewById(R.id.HeartRateValue);
                            hrmtv.setText("\t" + mContext.getString(R.string.HeartRateValue) + hrmval
                                    + " beats per minute");
                        }
                        if (eeval > 0) {
                            TextView eevaltv = (TextView) findViewById(R.id.EnergyExpended);
                            eevaltv.setText("\t" + mContext.getString(R.string.EnergyExpendedValue) + eeval + " KJ");
                        } else {
                            TextView eevaltv = (TextView) findViewById(R.id.EnergyExpended);
                            eevaltv.setText("\t" + mContext.getString(R.string.EnergyExpendedValue));
                        }
                        if (rrval != null && rrval.size() > 0) {
                            String rrvalstring = "\t" + mContext.getString(R.string.RR_IntervalValue);
                            for (int i = 0; i < rrval.size(); i++) {
                                int temp = rrval.get(i).intValue();
                                rrvalstring = rrvalstring.concat(temp + "(1/1024) sec. ");
                            }
                            TextView rrvaltv = (TextView) findViewById(R.id.RRInterval);
                            rrvaltv.setText(rrvalstring);
                        } else {
                            TextView rrvaltv = (TextView) findViewById(R.id.RRInterval);
                            rrvaltv.setText("\t" + mContext.getString(R.string.RR_IntervalValue));
                        }

                    }
                });

            default:
                super.handleMessage(msg);
            }
        }
    };*/

    /* Func to Display IEEE Reg cert data */
    public void display_reg_data(byte[] value) {
        int offset = 0;
        int count = 0;
        int length = 0;
        int auth_body = 0;
        int auth_body_stype = 0;
        int auth_body_slength = 0;
        int auth_body_data_MajIG = 0;
        int auth_body_data_MinIG = 0;
        int auth_body_data_cdcl_cnt = 0;
        int auth_body_data_cdcl_length = 0;
        int[] g_class_entry = null;
        int cont_reg_struct_data = 0;
        int cont_reg_struct_length = 0;
        int cont_reg_bitFType = 0;
        int jump_count = 1;
        boolean flag = false;
        while (offset < value.length && !flag) {
            switch (offset) {
            case 0x0:
                count = (value[offset++] << 8) | value[offset++];
                break;
            case 0x02:
                length = (value[offset++] << 8) | value[offset++];
                break;
            case 0x04:
                auth_body = value[offset++];
                break;
            case 0x05:
                auth_body_stype = value[offset++];
                break;
            case 0x06:
                auth_body_slength = (value[offset++] << 8) | value[offset++];
                break;
            case 0x08:
                auth_body_data_MajIG = value[offset++];
                break;
            case 0x09:
                auth_body_data_MinIG = value[offset++];
                break;
            case 0x0A:
                auth_body_data_cdcl_cnt = (value[offset++] << 8) | value[offset++];
                break;
            case 0x0C:
                auth_body_data_cdcl_length = (value[offset++] << 8) | value[offset++];
                int[] class_entry = new int[auth_body_data_cdcl_cnt];
                for (int i = 0; i < auth_body_data_cdcl_cnt; i++) {
                    class_entry[i] = (value[offset++] << 8) | value[offset++];
                }
                g_class_entry = class_entry;
                flag = true;
                break;
            default:
                Log.e(TAG, "wrong offset");
                break;
            }

        }

        while (offset < value.length) {
            switch (jump_count) {
            case 1:
                cont_reg_struct_data = (value[offset++] << 8) | value[offset++];
                jump_count++;
                break;
            case 2:
                cont_reg_struct_length = (value[offset++] << 8) | value[offset++];
                jump_count++;
                break;
            case 3:
                cont_reg_bitFType = (value[offset++] << 8) | value[offset++];
                break;
            default:
                Log.i(TAG, "wrong count");
                break;
            }
        }

        Log.i(TAG, "------------------IEEE REG CERT DATA---------------");
        Log.i(TAG, "count = " + count);
        Log.i(TAG, "length = " + length);
        Log.i(TAG, "auth_body = " + auth_body);
        Log.i(TAG, "auth_body_stype = " + auth_body_stype);
        Log.i(TAG, "auth_body_slength = " + auth_body_slength);
        Log.i(TAG, "auth_body_data_MajIG = " + auth_body_data_MajIG);
        Log.i(TAG, "auth_body_data_MinIG = " + auth_body_data_MinIG);
        Log.i(TAG, "auth_body_data_cdcl_cnt = " + auth_body_data_cdcl_cnt);
        Log.i(TAG, "auth_body_data_cdcl_length = " + auth_body_data_cdcl_length);
        if (g_class_entry != null) {
            Log.i(TAG, "Certified device class entry = ");
            for (int i : g_class_entry) {
                Log.i(TAG, "Certified device class entry[] =" + i);
            }
        }
        Log.i(TAG, "cont_reg_struct_data = " + cont_reg_struct_data);
        Log.i(TAG, "cont_reg_struct_length = " + cont_reg_struct_length);
        Log.i(TAG, "cont_reg_bitFType = " + cont_reg_bitFType);

    }

    public String toHex(String arg) {
        String temp = null;
        try {
            temp = String.format("%x", new BigInteger(arg.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return temp;
    }

    private String getBodySensorLocation(short bodySensorLocationValue) {
        Log.d(TAG, "getBodySensorLocation");
        if (bodySensorLocationValue == 0x00)
            return "Other";
        else if (bodySensorLocationValue == 0x01)
            return "Chest";
        else if (bodySensorLocationValue == 0x02)
            return "Wrist";
        else if (bodySensorLocationValue == 0x03)
            return "Finger";
        else if (bodySensorLocationValue == 0x04)
            return "Hand";
        else if (bodySensorLocationValue == 0x05)
            return "Ear Lobe";
        else if (bodySensorLocationValue == 0x06)
            return "Foot";
        return "reserved for future use";
    }    
    
         public boolean checkIfBroadcastMode(byte[] scanRecord) {
            int offset = 0;
            while (offset < (scanRecord.length - 2)) {
                int len = scanRecord[offset++];
                if (len == 0)
                    break; // Length == 0 , we ignore rest of the packet
                // TODO: Check the rest of the packet if get len = 0

                int type = scanRecord[offset++];
                switch (type) {
                case ADV_DATA_FLAG:

                    if (len >= 2) {
                        // The usual scenario(2) and More that 2 octets scenario.
                        // Since this data will be in Little endian format, we
                        // are interested in first 2 bits of first byte
                        byte flag = scanRecord[offset++];
                        /*
                         * 00000011(0x03) - LE Limited Discoverable Mode and LE
                         * General Discoverable Mode
                         */
                        if ((flag & LIMITED_AND_GENERAL_DISC_MASK) > 0)
                            return false;
                        else
                            return true;
                    } else if (len == 1) {
                        continue;// ignore that packet and continue with the rest
                    }
                default:
                    offset += (len - 1);
                    break;
                }
            }
            return false;
        }

        private boolean isRRintpresent(byte flags) {
            if ((flags & FIFTH_BITMASK) != 0)
                return true;
            return false;
        }

        private boolean isEEpresent(byte flags) {
            if ((flags & FOURTH_BITMASK) != 0)
                return true;
            return false;
        }

        private boolean isHeartRateInUINT16(byte flags) {
            Log.d(TAG, "isHeartRateInUINT16");
            if ((flags & FIRST_BITMASK) != 0)
                return true;
            return false;
        }        
      private void populateList() {
        /* Initialize device list container */
        //Log.d(TAG, "populateList");
        deviceList = new ArrayList<BluetoothDevice>();
        //deviceAdapter = new DeviceAdapter(this, deviceList);
        devRssiValues = new HashMap<String, Integer>();

        //ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        //newDevicesListView.setAdapter(deviceAdapter);
        //newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        /*Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        for (BluetoothDevice pairedDevice : pairedDevices) {
            boolean result = false;
            result = mService.isBLEDevice(pairedDevice);
            if (result == true) {
                addDevice(pairedDevice, 0);
            }
        }

        mService.scan(true);*/
    }

    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;

        for (BluetoothDevice listDev : deviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }
        devRssiValues.put(device.getAddress(), rssi);
        if (!deviceFound) {
            //mEmptyList.setVisibility(View.GONE);
            deviceList.add(device);
            //deviceAdapter.notifyDataSetChanged();
        }
    }	
    
    
    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<BluetoothDevice> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<BluetoothDevice> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            //if (convertView != null) {
                vg = (ViewGroup) convertView;
            //} else {
               // vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
            //}

            BluetoothDevice device = devices.get(position);
            //final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
            //final TextView tvname = ((TextView) vg.findViewById(R.id.name));
            //final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);
            //final TextView tvrssi = (TextView) vg.findViewById(R.id.rssi);

            /*tvrssi.setVisibility(View.VISIBLE);
            byte rssival = (byte) devRssiValues.get(device.getAddress()).intValue();
            if (rssival != 0) {
                tvrssi.setText("Rssi = " + String.valueOf(rssival));
            }

            tvname.setText(device.getName());
            tvadd.setText(device.getAddress());
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                tvname.setTextColor(Color.GRAY);
                tvadd.setTextColor(Color.GRAY);
                tvpaired.setTextColor(Color.GRAY);
                tvpaired.setVisibility(View.VISIBLE);
                tvpaired.setText(R.string.paired);
                tvrssi.setVisibility(View.GONE);
            } else {
                tvname.setTextColor(Color.WHITE);
                tvadd.setTextColor(Color.WHITE);
                tvpaired.setVisibility(View.GONE);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.WHITE);
            }
            if (mService.mBluetoothGatt.getConnectionState(device) == mService.mBluetoothGatt.STATE_CONNECTED){
                Log.i(TAG, "connected device::"+device.getName());
                tvname.setTextColor(Color.WHITE);
                tvadd.setTextColor(Color.WHITE);
                tvpaired.setVisibility(View.VISIBLE);
                tvpaired.setText(R.string.connected);
                tvrssi.setVisibility(View.GONE);
            }*/
            return vg;
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }    
	/*
	private BluetoothProfile.ServiceListener mProfileServiceListener = 
			 new BluetoothProfile.ServiceListener() { 
			 public void onServiceConnected(int profile, BluetoothProfile 
			proxy) { 
			 if (profile == BluetoothGattAdapter.GATT) { 
			 mBluetoothGatt = (BluetoothGatt) proxy; 
			 mBluetoothGatt.registerApp(mGattCallbacks); 
			 };}*/
    
   /* private BroadcastReceiver deviceStateListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Intent mIntent = intent;
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice device = mIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int devState = mIntent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                Log.d(TAG, "BluetoothDevice.ACTION_BOND_STATE_CHANGED");
                //setUiState();
                if (device.equals(mDevice) && devState == BluetoothDevice.BOND_NONE) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mDevice = null;
                            //setUiState();
                        }
                    });
                }
            }
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = mIntent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                Log.d(TAG, "BluetoothAdapter.ACTION_STATE_CHANGED" + "state is" + state);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if ( state == STATE_OFF) {
                                mDevice=null;
                                mState = HRP_PROFILE_DISCONNECTED;
                                //setUiStateForBTOff();
                            }
                        }
                    });
            }
        }
    };*/
    
   /* private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((HRPService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            mService.setActivityHandler(mHandler);
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };*/
    /*
    private void init() {
        Log.d(TAG, "init() mService= " + mService);
        Intent bindIntent = new Intent(this, HRPService.class);
        startService(bindIntent);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        this.registerReceiver(deviceStateListener, filter);
    }
    */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		/*
		//initializeBluetooth();
        onService = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder rawBinder) {
                mService = ((HRPService.LocalBinder) rawBinder).getService();
                if (mService != null) {
                    mService.setDeviceListHandler(mHandler1);
                }
                populateList();

            }

            public void onServiceDisconnected(ComponentName classname) {
                mService = null;
            }
        };

        // start service, if not already running (but it is)
        startService(new Intent(this, HRPService.class));
        Intent bindIntent = new Intent(this, HRPService.class);
        bindService(bindIntent, onService, Context.BIND_AUTO_CREATE);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        init();
        //mEmptyList = (TextView) findViewById(R.id.empty);
        //Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
	

        if(mService != null) {
        	mService.enableHRNotification(mDevice); 
        } else {
        	System.out.println("Heartrate not working");
        	this._heartRate=0;
        }
        */
        

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(actionBar.getThemedContext(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
								getString(R.string.title_section1),
								getString(R.string.title_section2),
								getString(R.string.title_section3), }), this);
		
		//txtInfo = (TextView) findViewById(R.id.textViewInfo);
		//statusInfo = (TextView) findViewById(R.id.textViewStatus);
		//userIdView = (TextView) findViewById(R.id.textViewUser);
		statusInfo = (EditText) findViewById(R.id.editTextStatus);
		userIdView = (EditText) findViewById(R.id.editTextUserId);
		
		/* the location manager is the most vital part it allows access 
		 * to location and GPS status services */
		lm = (LocationManager) getSystemService(LOCATION_SERVICE);
		//txtInfo.setText("initialized");
		statusInfo.setText("\n\n\nwait...");
		userIdView.setText(userId);
		toggleButton00 = (ToggleButton) findViewById(R.id.toggleButton00);
		toggleButton10 = (ToggleButton) findViewById(R.id.toggleButton10);
		toggleButton20 = (ToggleButton) findViewById(R.id.toggleButton20);
		toggleButton30 = (ToggleButton) findViewById(R.id.toggleButton30);
		toggleButton01 = (ToggleButton) findViewById(R.id.toggleButton01);
		toggleButton11 = (ToggleButton) findViewById(R.id.toggleButton11);
		toggleButton21 = (ToggleButton) findViewById(R.id.toggleButton21);
		toggleButton31 = (ToggleButton) findViewById(R.id.toggleButton31);
		toggleButton02 = (ToggleButton) findViewById(R.id.toggleButton02);
		toggleButton12 = (ToggleButton) findViewById(R.id.toggleButton12);
		toggleButton22 = (ToggleButton) findViewById(R.id.toggleButton22);
		toggleButton32 = (ToggleButton) findViewById(R.id.toggleButton32);
		toggleButton03 = (ToggleButton) findViewById(R.id.toggleButton03);
		toggleButton13 = (ToggleButton) findViewById(R.id.toggleButton13);
		toggleButton23 = (ToggleButton) findViewById(R.id.toggleButton23);
		toggleButton33 = (ToggleButton) findViewById(R.id.toggleButton33);
		toggleButton04 = (ToggleButton) findViewById(R.id.toggleButton04);
		toggleButton14 = (ToggleButton) findViewById(R.id.toggleButton14);
		toggleButton24 = (ToggleButton) findViewById(R.id.toggleButton24);
		toggleButton34 = (ToggleButton) findViewById(R.id.toggleButton34);
		toggleButton05 = (ToggleButton) findViewById(R.id.toggleButton05);
		toggleButton15 = (ToggleButton) findViewById(R.id.toggleButton15);
		toggleButton25 = (ToggleButton) findViewById(R.id.toggleButton25);
		toggleButton35 = (ToggleButton) findViewById(R.id.toggleButton35);
		toggleButton06 = (ToggleButton) findViewById(R.id.toggleButton06);
		toggleButton16 = (ToggleButton) findViewById(R.id.toggleButton16);
		toggleButton26 = (ToggleButton) findViewById(R.id.toggleButton26);
		toggleButton36 = (ToggleButton) findViewById(R.id.toggleButton36);
		//toggleButtonCount = (ToggleButton) findViewById(R.id.relativeLayout);
		boolean checked = true;
		//toggleButtonCount.setChecked(checked);
		toggleButton00.setChecked(checked);
		toggleButton10.setChecked(checked);
		toggleButton20.setChecked(checked);
		toggleButton30.setChecked(checked);
		toggleButton01.setChecked(checked);
		toggleButton11.setChecked(checked);
		toggleButton21.setChecked(checked);
		toggleButton31.setChecked(checked);
		toggleButton02.setChecked(checked);
		toggleButton12.setChecked(checked);
		toggleButton22.setChecked(checked);
		toggleButton32.setChecked(checked);
		toggleButton03.setChecked(checked);
		toggleButton13.setChecked(checked);
		toggleButton23.setChecked(checked);
		toggleButton33.setChecked(checked);
		toggleButton04.setChecked(checked);
		toggleButton14.setChecked(checked);
		toggleButton24.setChecked(checked);
		toggleButton34.setChecked(checked);
		toggleButton05.setChecked(checked);
		toggleButton15.setChecked(checked);
		toggleButton25.setChecked(checked);
		toggleButton35.setChecked(checked);
		toggleButton06.setChecked(checked);
		toggleButton16.setChecked(checked);
		toggleButton26.setChecked(checked);
		toggleButton36.setChecked(checked);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
	    mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE); // API 14
	    mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY); // API 9
	    mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	    mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	    mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
	    mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION); // API 9
	    mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    //mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION); // deprecated
	    mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
	    mRelativeHumidity = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY); // API 14
	    mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR); // API 9
	    // 20151206: api 20 to 22 move to onStartCommand()
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 3f, this);
	    
	    //this.
	    
	    setInterval(2000);
	    //startTimer();		
	}


	public int getInterval() {
	    return interval;
	}

	public void setInterval(int interval) {
	    this.interval = interval;
	}

	//http://developer.android.com/reference/android/app/AlarmManager.html
	private void startTimer() {

	    timer.scheduleAtFixedRate( new TimerTask() {

	        public void run() {
	            onTimer();       
	        }

	    }, 0, getInterval());; }	

	@Override
	public void onDestroy() {
	    timer.cancel();
	    super.onDestroy();
	}
	
	
	@Override
	protected void onResume() {
		/*
		 * onResume is is always called after onStart, even if the app hasn't been
		 * paused
		 * 
		 * add location listener and request updates every 1000ms or 10m
		 */
        // TODO: http://stackoverflow.com/questions/32083913/android-gps-requires-access-fine-location-error-even-though-my-manifest-file
		//lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 3f, this);		
		//lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1f, this);
		mSensorManager.registerListener(this, mPressure, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mRelativeHumidity, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(this, mMagneticField, SensorManager.SENSOR_DELAY_GAME);
	    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 3f, this); // required for app unfocus/focus
		//mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_ORIENTATION);

		super.onResume();
	}
	
	@Override
	protected void onPause() {
		lm.removeUpdates(this);
		mSensorManager.unregisterListener(this);
		super.onPause();
	}

	
	@Override
	// http://hejp.co.uk/android/android-gps-example/
	public void onLocationChanged(Location location) {
		System.out.println("onLocationChanged: " + location);
		Log.v(tag, "Location Changed");
		_longitude = location.getLongitude();
		_latitude = location.getLatitude(); 
		_altitude = location.getAltitude();
		_accuracy = location.getAccuracy();
		_bearing = location.getBearing();
		_speed = location.getSpeed();
		_provider = location.getProvider();
		_time = location.getTime();
		_gpsCount++;
		gpsState = 1 - gpsState;
		if(gpsState < 1) {
			toggleButton10.setBackgroundColor(blue);
			toggleButton20.setBackgroundColor(blue);
		} else {
			toggleButton10.setBackgroundColor(magenta);
			toggleButton20.setBackgroundColor(magenta);
		}
	}
	
	public void onTimer() {
		userId = userIdView.getText().toString();
		sb = new StringBuilder(512);
		urlBuffer = new StringBuilder(512);
		//https://obrienscience-obrienlabs.java.us1.oraclecloudapps.com//gps/FrontController?action=setGps&u=255
		//&lt=65.3439172&lg=-95.9404402&ac=1.1&te=1&p=1&ts=11&be=33&al=5
		urlBuffer.append("&u=").append(userId);
        urlBuffer.append("&de=andbb");
		urlBuffer.append("&pr=and51ß");
		if(state < 1) {
			toggleButton16.setBackgroundColor(blue);
		} else {
			toggleButton16.setBackgroundColor(magenta);
		}
		
		noOfFixes++;
		state = 1 - state;
		toggleButton16.setText(Integer.toString(noOfFixes) + " :Fi");

		sb.append("#: ");
		sb.append(noOfFixes);
		sb.append('\n');

		sb.append(_longitude);
		sb.append('\n');
		urlBuffer.append("&lg=").append(_longitude);
		toggleButton10.setText(truncateString(_gpsCount + ":lg:" + Double.toString(_longitude), 10));

		sb.append(_latitude);
		sb.append('\n');
		urlBuffer.append("&lt=").append(_latitude);
		toggleButton20.setText(truncateString(_gpsCount + ":la:" + Double.toString(_latitude), 9));
		
		sb.append("Al:");
		sb.append(_altitude);
		sb.append('\n');
		urlBuffer.append("&al=").append(_altitude);
		toggleButton30.setText(truncateString("al:" + Double.toString(_altitude), 7));

		sb.append("Ac:");
		sb.append(_accuracy);
		sb.append('\n');
		urlBuffer.append("&ac=").append(_accuracy);

		sb.append("T: ");
		sb.append(_time);
		sb.append('\n');
		urlBuffer.append("&ts=").append(_time);

		/*sb.append("C: ");
		sb.append(_bearing);
		sb.append('\n');*/
		urlBuffer.append("&be=").append(Integer.valueOf(Math.round(_bearing)));
		toggleButton00.setText(truncateString("be:" + Double.toString(_bearing), 7));

		/*sb.append("S: ");
		sb.append(_speed);
		sb.append('\n');*/
		urlBuffer.append("&s=").append(_speed);
		toggleButton31.setText(truncateString("sp:" + Double.toString(_speed), 7));
		if(_speed > 0) {
			toggleButton31.setBackgroundColor(green);
		} else {
			toggleButton31.setBackgroundColor(red);
		}

		
		/*sb.append("HR: ");
		sb.append(aHeartRate);
		sb.append('\n');*/
		urlBuffer.append("&hr=").append("1");
		toggleButton06.setText(truncateString("hr:" + Double.toString(_heartRate), 4));

		//http://developer.android.com/guide/topics/sensors/sensors_environment.html
		/*sb.append("PR: ");
		sb.append(_provider);
		sb.append('\n');*/
		urlBuffer.append("&pr=").append(_provider);
		
		/*sb.append("TE: ");
		sb.append(_temp);
		sb.append('\n');*/
		urlBuffer.append("&te=").append(_temp);
		toggleButton01.setText(truncateString("t:" + Double.toString(_temp), 6));
		
		/*sb.append("P: ");
		sb.append(_pressure);
		sb.append('\n');*/
		urlBuffer.append("&p=").append(_pressure);
		toggleButton11.setText(truncateString("p:" + Double.toString(_pressure), 8));
	
		/*sb.append("H: ");
		sb.append(_humidity);
		sb.append('\n');*/
		urlBuffer.append("&hu=").append(_humidity);		
		
		/*sb.append("L: ");
		sb.append(_light);
		sb.append('\n');*/
		urlBuffer.append("&li=").append(_light);	
		
		/*sb.append("G: ");
		sb.append(_gravityX);
		sb.append(",");
		sb.append(_gravityY);
		sb.append(",");
		sb.append(_gravityZ);
		sb.append('\n');*/
		urlBuffer.append("&grx=").append(_gravityX);
		urlBuffer.append("&gry=").append(_gravityY);
		urlBuffer.append("&grz=").append(_gravityZ);

		/*sb.append("Acel: ");
		sb.append(_accelX);
		sb.append(",");
		sb.append(_accelY);
		sb.append(",");
		sb.append(_accelZ);
		sb.append('\n');*/
		urlBuffer.append("&arx=").append(_accelX);
		urlBuffer.append("&ary=").append(_accelY);
		urlBuffer.append("&arz=").append(_accelZ);

		/*sb.append("lina: ");
		sb.append(_linAccelX);
		sb.append(",");
		sb.append(_linAccelY);
		sb.append(",");
		sb.append(_linAccelZ);
		sb.append('\n');*/
		urlBuffer.append("&lax=").append(_linAccelX);
		urlBuffer.append("&lay=").append(_linAccelY);
		urlBuffer.append("&laz=").append(_linAccelZ);
		
		/*sb.append("RV: ");
		sb.append(_rotX);
		sb.append(",");
		sb.append(_rotY);
		sb.append(",");
		sb.append(_rotZ);
		sb.append('\n');*/
		urlBuffer.append("&rvx=").append(_rotX);
		urlBuffer.append("&rvy=").append(_rotY);
		urlBuffer.append("&rvz=").append(_rotZ);

		/*sb.append("Gy: ");
		sb.append(_gyroX);
		sb.append(",");
		sb.append(_gyroY);
		sb.append(",");
		sb.append(_gyroZ);
		sb.append('\n');*/
		urlBuffer.append("&gsx=").append(_gyroX);
		urlBuffer.append("&gsy=").append(_gyroY);
		urlBuffer.append("&gsz=").append(_gyroZ);
		
		/*sb.append("mag: ");
		sb.append(_magX);
		sb.append(",");
		sb.append(_magY);
		sb.append(",");
		sb.append(_magZ);
		sb.append('\n');*/
		urlBuffer.append("&mfx=").append(_magX);
		urlBuffer.append("&mfy=").append(_magY);
		urlBuffer.append("&mfz=").append(_magZ);
		statusInfo.setText(sb.toString());
		if(toggleButton16.isChecked()) {
		this.myClickHandler(urlBuffer.toString());
		}
	}

	public String truncateString(String aString, int length) {
		if(aString.length() > length) {
			return aString.substring(0, length);
		} else {
			return aString;
		}
	}
	
	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	@Override
	public final void onSensorChanged(SensorEvent event) {
		if(count < 240) {
			count++;			
		} else {
			count = 0;
		}
		
		Sensor aSensor = event.sensor;
		if(aSensor.getType() == Sensor.TYPE_PRESSURE) {
			_pressure = event.values[0];
			System.out.println("Pressure: " + _pressure);
			//textViewRight7.setText(Double.toString(_pressure) + " :p");
			toggleButton11.setText(truncateString("p:" + Double.toString(_pressure), 8));
			if(_pressure > 1000) {
				toggleButton11.setBackgroundColor(blue);
			} else {
				toggleButton11.setBackgroundColor(red);
			}
		}
		if(aSensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
			_humidity = event.values[0];
			System.out.println("humidity: " + _humidity);
			//textViewRight7.setText(Double.toString(_pressure) + " :p");
			toggleButton21.setText(truncateString("h:" + Double.toString(_humidity), 8));
			if(_humidity > 50) {
				toggleButton21.setBackgroundColor(blue);
			} else {
				toggleButton21.setBackgroundColor(green);
			}
		}
		if(aSensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
			_temp = event.values[0];
			System.out.println("Temp: " + _temp);
			//textViewRight7.setText(Double.toString(_temp) + " :p");
			toggleButton01.setText(truncateString("t:" + Double.toString(_temp), 6));
			if(_temp > 25) {
				toggleButton01.setBackgroundColor(red);
			} else {
				if(_temp < 20) {
				toggleButton01.setBackgroundColor(blue);
				} else {
					toggleButton01.setBackgroundColor(magenta);
				}
			}
		}
		if(aSensor.getType() == Sensor.TYPE_LIGHT) {
			_light = event.values[0];
			System.out.println("light: " + _light);
			//textViewRight7.setText(Double.toString(_temp) + " :p");
			toggleButton36.setText(truncateString("li:" + Double.toString(_light), 8));
			if(_light > 100) {
				toggleButton36.setBackgroundColor(blue); 
			} else {
				toggleButton36.setBackgroundColor(magenta); 
					
			}
		}
		if(aSensor.getType() == Sensor.TYPE_PROXIMITY) {
			_proximity = event.values[0];
			System.out.println("prox: " + _proximity);
			//textViewRight7.setText(Double.toString(_temp) + " :p");
			toggleButton26.setText(truncateString("pr:" + Double.toString(_proximity), 6));
		}
		if(aSensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			_magX = event.values[0];
			_magY = event.values[1];
			_magZ = event.values[2];
			System.out.println("Mag: " + _magX +"," + _magY + "," + _magZ);
			toggleButton02.setText(truncateString("mX:" + Double.toString(_magX), 8));
			toggleButton03.setText(truncateString("mY:" + Double.toString(_magY), 8));
			toggleButton04.setText(truncateString("mZ:" + Double.toString(_magZ), 8));
			toggleButtonColor(toggleButton02, _magX);
			toggleButtonColor(toggleButton03, _magY);
			toggleButtonColor(toggleButton04, _magZ);
		}
		if(aSensor.getType() == Sensor.TYPE_GYROSCOPE) {
			_gyroX = event.values[0];
			_gyroY = event.values[1];
			_gyroZ = event.values[2];
			System.out.println("Gyro: " + _gyroX +"," + _gyroY + "," + _gyroZ);
			//textViewRight7.setText(Double.toString(_temp) + " :p");
			toggleButton35.setText(truncateString("gyX:" + Double.toString(_gyroX), 7));
			toggleButtonColor(toggleButton35, _gyroX);
			//toggleButton14.setText(truncateString("gyY:" + Double.toString(_gyroY), 7));
			//toggleButton24.setText(truncateString("gyZ:" + Double.toString(_gyroZ), 7));
		}
		if(aSensor.getType() == Sensor.TYPE_GRAVITY) {
			_gravityX = event.values[0];
			_gravityY = event.values[1];
			_gravityZ = event.values[2];
			System.out.println("Gravity: " + _gravityX +"," + _gravityY + "," + _gravityZ);
			//textViewRight7.setText(Double.toString(_temp) + " :p");
			toggleButton32.setText(truncateString("gX:" + Double.toString(_gravityX), 7));
			toggleButton33.setText(truncateString("gY:" + Double.toString(_gravityY), 7));
			toggleButton34.setText(truncateString("gZ:" + Double.toString(_gravityZ), 7));
			if(_gravityX > 0) {
				if(_gravityX > 9) {
					toggleButton32.setBackgroundColor(blue);
				} else {
					toggleButton32.setBackgroundColor(green);
				}
			} else {
				toggleButton32.setBackgroundColor(red);
			}
			if(_gravityY > 0) {
				if(_gravityY > 9) {
					toggleButton33.setBackgroundColor(blue);
				} else {
					toggleButton33.setBackgroundColor(green);
				}
			} else {
				toggleButton33.setBackgroundColor(red);
			}
			if(_gravityZ > 0) {
				if(_gravityZ > 9) {
					toggleButton34.setBackgroundColor(blue);
				} else {
					toggleButton34.setBackgroundColor(green);
				}
			} else {
				toggleButton34.setBackgroundColor(red);
			}
		}
		if(aSensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			_linAccelX = event.values[0];
			_linAccelY = event.values[1];
			_linAccelZ = event.values[2];
			System.out.println("Linear Acc: " + _linAccelX +"," + _linAccelY + "," + _linAccelZ);
			toggleButton12.setText(truncateString("laX:" + Double.toString(_linAccelX), 9));
			toggleButton13.setText(truncateString("laY:" + Double.toString(_linAccelY), 9));
			toggleButton14.setText(truncateString("laZ:" + Double.toString(_linAccelZ), 9));
			toggleButtonColor(toggleButton12, _linAccelX);
			toggleButtonColor(toggleButton13, _linAccelY);
			toggleButtonColor(toggleButton14, _linAccelZ);
		}
		if(aSensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			_accelX = event.values[0];
			_accelY = event.values[1];
			_accelZ = event.values[2];
			System.out.println("Acc+g: " + _accelX +"," + _accelY + "," + _accelZ);
			toggleButton22.setText(truncateString("aX:" + Double.toString(_accelX), 9));
			toggleButton23.setText(truncateString("aY:" + Double.toString(_accelY), 9));
			toggleButton24.setText(truncateString("aZ:" + Double.toString(_accelZ), 9));
			toggleButtonColor(toggleButton22, _accelX);
			toggleButtonColor(toggleButton23, _accelY);
			toggleButtonColor(toggleButton24, _accelZ);
		}
		if(aSensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			_rotX = event.values[0];
			_rotY = event.values[1];
			_rotZ = event.values[2];
			//_rotS = event.values[3]; // optional
			System.out.println("Rotation: " + _rotX +"," + _rotY + "," + _rotZ + "," + _rotS);
			toggleButton05.setText(truncateString("rX:" + Double.toString(_rotX), 9));
			toggleButton15.setText(truncateString("rY:" + Double.toString(_rotY), 9));
			toggleButton25.setText(truncateString("rZ:" + Double.toString(_rotZ), 9));
			//toggleButton35.setText(truncateString("sZ:" + Double.toString(_rotS), 9));
			toggleButtonColor(toggleButton05, _rotX);
			toggleButtonColor(toggleButton15, _rotY);
			toggleButtonColor(toggleButton25, _rotZ);
		}
	if(count == 0) {
		onTimer();
	}

	}

	private void toggleButtonColor(ToggleButton toggleButton, float value) {
		if(value > 0.05) {
			toggleButton.setBackgroundColor(green);
		} else {
			if(value < - 0.04) {
				toggleButton.setBackgroundColor(red);
			} else {
				toggleButton.setBackgroundColor(yellow);
			}
		}
	}
	
	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		System.out.println("onProviderDisabled: " + provider);
		Log.v(tag, "Disabled");

		/* bring up the GPS settings */
		Intent intent = new Intent(
				android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(intent);
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		System.out.println("onProviderEnabled: " + provider);
		Toast.makeText(this, "GPS Enabled", Toast.LENGTH_SHORT).show();
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		System.out.println("onStatusChanged: " + provider + " " + status);
		switch (status) {
		case LocationProvider.OUT_OF_SERVICE:
			Log.v(tag, "Status Changed: Out of Service");
			Toast.makeText(this, "Status Changed: Out of Service",
					Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			Log.v(tag, "Status Changed: Temporarily Unavailable");
			Toast.makeText(this, "Status Changed: Temporarily Unavailable",
					Toast.LENGTH_SHORT).show();
			break;
		case LocationProvider.AVAILABLE:
			Log.v(tag, "Status Changed: Available");
			Toast.makeText(this, "Status Changed: Available",
					Toast.LENGTH_SHORT).show();
			break;
		}		
		
	}	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {
		// When the given dropdown item is selected, show its contents in the
		// container view.
		Fragment fragment = new DummySectionFragment();
		Bundle args = new Bundle();
		args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
		fragment.setArguments(args);
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.container, fragment).commit();
		return true;
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main_dummy,
					container, false);
			TextView dummyTextView = (TextView) rootView
					.findViewById(R.id.section_label);
			dummyTextView.setText(Integer.toString(getArguments().getInt(
					ARG_SECTION_NUMBER)));
			//rootView.setBackgroundColor(Color.argb(255,0,0,0));
			return rootView;
		}
	}


	// Uses AsyncTask to create a task away from the main UI thread. This task takes a 
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
    private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
       @Override
       protected String doInBackground(String... urls) {
             
           // params comes from the execute() call: params[0] is the url.
           try {
               return downloadUrl(urls[0]);
           } catch (IOException e) {
               return "Unable to retrieve web page. URL may be invalid.";
           }
       }
       // onPostExecute displays the results of the AsyncTask.
       @Override
       protected void onPostExecute(String result) {
    	   statusInfo.setText(result);
    	   System.out.println(result);
      }
       
    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;
            
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000); // ms
            conn.setConnectTimeout(15000); // ms
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            //Log.d(DEBUG_TAG, "The response is: " + response);
            System.out.println(response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is, len);
            return contentAsString;
            
        // Makes sure that the InputStream is closed after the app is
        // finished using it.
        } finally {
            if (is != null) {
                is.close();
            } 
        }
    }
    
 // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");        
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }    
   }
	
}
