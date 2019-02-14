package com.example.kayjean.arduinoadk;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;


//import com.android.future.usb.UsbAccessory;
//import com.android.future.usb.UsbManager;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

public class MainActivity extends Activity {


    // TAG is used to debug in Android logcat console
    private static final String TAG = "ArduinoAccessory";


    private static final String ACTION_USB_PERMISSION = "com.google.android.DemoKit.action.USB_PERMISSION";


    private UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private boolean mPermissionRequestPending;
    private ToggleButton buttonLED;


    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;


    private SeekBar sb_normal_red;
    private TextView txt_cur_red;
    private SeekBar sb_normal_green;
    private TextView txt_cur_green;
    private SeekBar sb_normal_blue;
    private TextView txt_cur_blue;

    private int led_red = 0;
    private int led_green = 0;
    private int led_blue = 0;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                    if (intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        openAccessory(accessory);
                    } else {
                        Log.d(TAG, "permission denied for accessory "
                                + accessory);
                    }
                    mPermissionRequestPending = false;
                }
            } else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
            }
        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbReceiver, filter);


        if (getLastNonConfigurationInstance() != null) {
            mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
            openAccessory(mAccessory);
        }


        setContentView(R.layout.activity_main);
        buttonLED = (ToggleButton) findViewById(R.id.toggleButtonLED);
        bindViews();

    }


    private void bindViews() {
        sb_normal_red = (SeekBar) findViewById(R.id.sb_normal_red);
        txt_cur_red = (TextView) findViewById(R.id.txt_cur_red);
        sb_normal_red.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                led_red = progress;
                txt_cur_red.setText("progress:" + progress + "  / 100 ");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(mContext, "触碰SeekBar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(mContext, "放开SeekBar", Toast.LENGTH_SHORT).show();
            }
        });


        sb_normal_green = (SeekBar) findViewById(R.id.sb_normal_green);
        txt_cur_green = (TextView) findViewById(R.id.txt_cur_green);
        sb_normal_green.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                led_green = progress;
                txt_cur_green.setText("progress:" + progress + "  / 100 ");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        sb_normal_blue = (SeekBar) findViewById(R.id.sb_normal_blue);
        txt_cur_blue = (TextView) findViewById(R.id.txt_cur_blue);
        sb_normal_blue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                led_blue = progress;
                txt_cur_blue.setText("progress:" + progress + "  / 100 ");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (mAccessory != null) {
            return mAccessory;
        } else {
            return super.onRetainNonConfigurationInstance();
        }
    }


    @Override
    public void onResume() {
        super.onResume();


        if (mInputStream != null && mOutputStream != null) {
            return;
        }


        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
        UsbAccessory accessory = (accessories == null ? null : accessories[0]);
        if (accessory != null) {
            if (mUsbManager.hasPermission(accessory)) {
                openAccessory(accessory);
            } else {
                synchronized (mUsbReceiver) {
                    if (!mPermissionRequestPending) {
                        mUsbManager.requestPermission(accessory,mPermissionIntent);
                        mPermissionRequestPending = true;
                    }
                }
            }
        } else {
            Log.d(TAG, "mAccessory is null");
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        closeAccessory();
    }


    @Override
    public void onDestroy() {
        unregisterReceiver(mUsbReceiver);
        super.onDestroy();
    }


    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            Log.d(TAG, "accessory opened");
        } else {
            Log.d(TAG, "accessory open fail");
        }
    }


    private void closeAccessory() {
        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }


    public void blinkLED(View v){


        byte[] buffer = new byte[4];

/*
        if(buttonLED.isChecked()) {
            buffer[0] = (byte) 0; // button says on, light is off
            buffer[1] = (byte) 0; // button says on, light is off
            buffer[2] = (byte) 0; // button says on, light is off
            buffer[3] = (byte) 0; // button says on, light is off
        }
        else {
            buffer[0] = (byte) 1; // button says off, light is on
            buffer[1] = (byte) led_red; // button says off, light is on
            buffer[2] = (byte) led_green; // button says off, light is on
            buffer[3] = (byte) led_blue; // button says off, light is on
        }
*/
        buffer[0] = (byte) 1; // button says off, light is on
        buffer[1] = (byte) led_red; // button says off, light is on
        buffer[2] = (byte) led_green; // button says off, light is on
        buffer[3] = (byte) led_blue; // button says off, light is on

        if (mOutputStream != null) {
            try {
                mOutputStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "write failed", e);
            }
        }
    }


}
