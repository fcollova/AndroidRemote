/*
 /*
 * Android application for remote control of Arduino Robot
 * By Matthieu Varagnat, 2013
 * 
 * This application connects over bluetooth to an Arduino, and sends commands
 * It also receives confirmation messages and displays them in a log
 * 
 * Shared under Creative Common Attribution licence * 
 */

package com.example.androidremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.DialogInterface;
import android.content.Intent;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


import android.content.Context;
import android.graphics.Color;

import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.androidplot.xy.*;

import java.util.Arrays;
import android.widget.Toast;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;



public class AndroidRemoteActivity extends Activity implements OnClickListener {




    //F.C. Used by Androidplot library
    private static final int HISTORY_SIZE = 50;            // number of points to plot in history
    private XYPlot aprHistoryPlot = null;
    private SimpleXYSeries AccXHistorySeries = null;
    private SimpleXYSeries AccYHistorySeries = null;
    private SimpleXYSeries AccZHistorySeries = null;

    //F.C. User Interface
    private TextView logview;
    private TextView GyroX;
    private TextView GyroY;
    private TextView GyroZ;
    private TextView AccX;
    private TextView AccY;
    private TextView AccZ;
    private ToggleButton tButton;
    private Button Buttonstatus;

    //private Button connect, deconnect, next;
    private Button next;

    private ImageView forwardArrow, backArrow, rightArrow, leftArrow, stop;

    // Bluetooth Management
    private BluetoothAdapter mBluetoothAdapter = null;
    private BtInterface bt = null;
    private String[] logArray = null;
    static final String TAG = "Chihuahua";
    static final int REQUEST_ENABLE_BT = 3;




     //This handler listens to messages from the bluetooth interface and adds them to the log
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {

            String data = msg.getData().getString("receivedData");
            String[] GyroValue;
            String[] AccValue;

            // FC
            String[] parts = data.split(":");
            if (parts.length >= 3) {
                String Gyro = parts[0];
                String Acc = parts[1];
                String AccR = parts[2];

                GyroValue = Gyro.split(";", -1);
                AccValue = AccR.split(";", -1);

                if (GyroValue.length == 3) {
                    GyroX.setText(GyroValue[0]);
                    GyroY.setText(GyroValue[1]);
                    GyroZ.setText(GyroValue[2]);
                }
                if (AccValue.length == 3) {
                    AccX.setText(AccValue[0]);
                    AccY.setText(AccValue[1]);
                    AccZ.setText(AccValue[2]);
                    //Manage update to History Plot
                    float NewFloatX = Float.valueOf(AccValue[0]);
                    float NewFloatY = Float.valueOf(AccValue[1]);
                    float NewFloatZ = Float.valueOf(AccValue[2]);

                    // get rid the oldest sample in history:
                    if (AccXHistorySeries.size() > HISTORY_SIZE) {
                        AccXHistorySeries.removeFirst();
                        AccYHistorySeries.removeFirst();
                        AccZHistorySeries.removeFirst();
                    }
                    // add the latest history sample:
                    AccXHistorySeries.addLast(null, NewFloatX);
                    AccYHistorySeries.addLast(null, NewFloatY);
                    AccZHistorySeries.addLast(null, NewFloatZ);

                    aprHistoryPlot.redraw();
                }
                }

            // ->> addToLog(data);
        }
    };

    //this handler is dedicated to the status of the bluetooth connection
    final Handler handlerStatus = new Handler() {
        public void handleMessage(Message msg) {
            int status = msg.arg1;
            if (status == BtInterface.CONNECTED)
            {
                addToLog("Connected");
                //tButton.setBackgroundColor(getResources().getColor(R.color.green));
                tButton.setChecked(true);
            }
            else
            if (status == BtInterface.DISCONNECTED)
            {
                addToLog("Disconnected");
                //tButton.setBackgroundColor(getResources().getColor(R.color.red));
                tButton.setChecked(false);

            }
        }
    };

    //handles the log view modification
    //only the most recent messages are shown
    private void addToLog(String message) {
        System.arraycopy(logArray, 1, logArray, 0, logArray.length - 1);
        logArray[logArray.length - 1] = message;

        logview.setText("");
        for (int i = 0; i < logArray.length; i++) {
            if (logArray[i] != null) {
                logview.append(logArray[i] + "\n");
            }
        }
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_android_remote);

        //first, inflate all layout objects, and set click listeners

        logview = (TextView) findViewById(R.id.logview);
        //I chose to display only the last 3 messages
        logArray = new String[3];

        GyroX = (TextView) findViewById(R.id.GyroX);
        GyroY = (TextView) findViewById(R.id.GyroY);
        GyroZ = (TextView) findViewById(R.id.GyroZ);
        AccX = (TextView) findViewById(R.id.AccX);
        AccY = (TextView) findViewById(R.id.AccY);
        AccZ = (TextView) findViewById(R.id.AccZ);

        tButton = (ToggleButton) findViewById(R.id.tButton);
        tButton.setOnClickListener(this);

        Buttonstatus = (Button) findViewById(R.id.status);

        /*
        OLD Connect Deconnect Button
        connect = (Button) findViewById(R.id.connect);
        connect.setOnClickListener(this);

        deconnect = (Button) findViewById(R.id.deconnect);
        deconnect.setOnClickListener(this);
        */

        next = (Button) findViewById(R.id.next);
        next.setOnClickListener(this);

        //FC---------------
        // setup the History plot:

        aprHistoryPlot = (XYPlot) findViewById(R.id.HistoryPlot);


        AccXHistorySeries = new SimpleXYSeries("Acc-X");
        AccXHistorySeries.useImplicitXVals();

        AccYHistorySeries = new SimpleXYSeries("Acc-Y");
        AccYHistorySeries.useImplicitXVals();

        AccZHistorySeries = new SimpleXYSeries("Acc-Z");
        AccZHistorySeries.useImplicitXVals();


        aprHistoryPlot.setRangeBoundaries(-1000, 1000, BoundaryMode.FIXED);
        aprHistoryPlot.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);

        LineAndPointFormatter formatterX = new LineAndPointFormatter(Color.rgb(0, 200, 0), null, null, null);
        aprHistoryPlot.addSeries(AccXHistorySeries, formatterX);

        LineAndPointFormatter formatterY = new LineAndPointFormatter(Color.rgb(200, 0, 0), null, null, null);
        aprHistoryPlot.addSeries(AccYHistorySeries, formatterY);

        LineAndPointFormatter formatterZ = new LineAndPointFormatter(Color.rgb(0, 0, 200), null, null, null);
        aprHistoryPlot.addSeries(AccZHistorySeries, formatterZ);

        aprHistoryPlot.setDomainStepValue(5);
        aprHistoryPlot.setTicksPerRangeLabel(3);
        aprHistoryPlot.setDomainLabel("Sample Index");
        aprHistoryPlot.getDomainLabelWidget().pack();
        aprHistoryPlot.setRangeLabel("Angle (Degs)");
        aprHistoryPlot.getRangeLabelWidget().pack();

        //Imposta la ricezione degli eventi Bluetooth
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter1);
        this.registerReceiver(mReceiver, filter2);
        this.registerReceiver(mReceiver, filter3);

    }


    public void my_finish() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("No Bluetooth Interface Present! Exit");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                quit();
            }
        });
        /* Per Gestire anche il NO!!
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        */
        AlertDialog alert = builder.create();
        alert.show();
    }
    public void quit() {
        super.finish();
    }

    //it is better to handle bluetooth connection in onResume (ie able to reset when changing screens)
    @Override
    public void onResume() {
        super.onResume();
        //first of all, we check if there is bluetooth on the phone
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Log.v(TAG, "Device does not support Bluetooth");
            my_finish(); //Exit From App Device Bluetooth Does not exit!!

        } else {
            //Device supports BT
            if (!mBluetoothAdapter.isEnabled()) {
                //if Bluetooth not activated, then request it
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                //BT activated, then initiate the BtInterface object to handle all BT communication
                bt = new BtInterface(handlerStatus, handler);
            }
        }

    }

    //called only if the BT is not already activated, in order to activate it
    protected void onActivityResult(int requestCode, int resultCode, Intent moreData) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                //BT activated, then initiate the BtInterface object to handle all BT communication
                bt = new BtInterface(handlerStatus, handler);
            } else if (resultCode == Activity.RESULT_CANCELED)
                Log.v(TAG, "BT not activated");
            else
                Log.v(TAG, "result code not known");
        } else {
            Log.v(TAG, "request code not known");
        }
    }

    //handles the clicks on various parts of the screen
    //all buttons launch a function from the BtInterface object
    @Override
    public void onClick(View v) {

        // Verify if Bluetooth is enabled in the device
        String toastText;
        if (mBluetoothAdapter.isEnabled()) {
            String address = mBluetoothAdapter.getAddress();
            String name = mBluetoothAdapter.getName();
            toastText = "Bluetooth is enabled: " + name + " : " + address;
        }else toastText = "Bluetooth is not enabled";
        Toast.makeText(getApplicationContext(), toastText, Toast.LENGTH_SHORT).show();

        /*
        if (v == connect) {
            //addToLog("Trying to connect");
            //bt.connect(this);
        } else if (v == deconnect) {
            //addToLog("closing connection");
            //bt.close();
        } else
        */
        if (v == next) {
            Intent Myactivity = new Intent(this, GraphButtonActivity.class);
            startActivity(Myactivity);
        } else if (v == forwardArrow) {
            //addToLog("Move Forward");
            bt.sendData("F");
        } else if (v == backArrow) {
            //addToLog("Move back");
            bt.sendData("B");
        } else if (v == rightArrow) {
            //addToLog("Turn Right");
            bt.sendData("R");
        } else if (v == leftArrow) {
            //addToLog("Turn left");
            bt.sendData("L");
        } else if (v == stop) {
            //addToLog("Stopping");
            bt.sendData("S");
        } else if (v == tButton){
            if(tButton.isChecked()){

                addToLog("Trying to connect");
                bt.connect(this);
            }
            else
            {
                addToLog("closing connection");
                bt.close();
            }
        }
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {}

                else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                        //Device is now connected
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        //Done searching
                }
                else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                        //Device is about to disconnect
                }
                else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
                        {
                            if(bt.BtInterfaceStatus == BtInterface.CONNECTED){bt.close();}
                            addToLog("Broadcast Disconnected");
                            //Device has disconnected
                        }
            }
    };


}


