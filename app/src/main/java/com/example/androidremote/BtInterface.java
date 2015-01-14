/*
 * Android application for remote control of Arduino Robot
 * By Matthieu Varagnat, 2013
 * 
 * This application connects over bluetooth to an Arduino, and sends commands
 * It also receives confirmation messages and displays them in a log
 * 
 * Shared under Creative Common Attribution licence * 
 * 
 * This BtInterface class comes from the tutorial (in French) here
 * http://nononux.free.fr/index.php?page=elec-brico-bluetooth-android-microcontroleur
 */

package com.example.androidremote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BtInterface {
	
	//Required bluetooth objects
	private BluetoothDevice device = null;
	private BluetoothSocket socket = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private InputStream receiveStream = null;
	private BufferedReader receiveReader = null;
	private OutputStream sendStream = null;	//no need to buffer it as we are going to send 1 char at a time
	
	//this thread will listen to incoming messages. It will be killed when connection is closed
	private ReceiverThread receiverThread;

	//these handlers corresponds to those in the main activity
	Handler handlerStatus, handlerMessage;
	
	public static int CONNECTED = 1;
	public static int DISCONNECTED = 2;
	static final String TAG = "Chihuahua";	

	public BtInterface(Handler hstatus, Handler h) {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		handlerStatus = hstatus;
		handlerMessage = h;		
	}
	
	//when called from the main activity, it sets the connection with the remote device
	public void connect() {
		
		Set<BluetoothDevice> setpairedDevices = mBluetoothAdapter.getBondedDevices();
    	BluetoothDevice[] pairedDevices = (BluetoothDevice[]) setpairedDevices.toArray(new BluetoothDevice[setpairedDevices.size()]);
	
		boolean foundChihuahua = false;
		for(int i=0;i<pairedDevices.length;i++) {
			if(pairedDevices[i].getName().contains("FC_MWII")) {
				device = pairedDevices[i];
				try {
					//the String "00001101-0000-1000-8000-00805F9B34FB" is standard for Serial connections
					socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
					receiveStream = socket.getInputStream();
					receiveReader = new BufferedReader(new InputStreamReader(receiveStream));
					sendStream = socket.getOutputStream();
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
				foundChihuahua = true;
				break;
			}
		}
		if(foundChihuahua == false){
			Log.v(TAG, "You have not turned on your Chihuahua");
		}
		
		receiverThread = new ReceiverThread(handlerMessage);
		//This thread will try to connect (it is always recommended to do so in a separate Thread) and return a confirmation message through the Handler handlerstatus
		new Thread() {
			@Override public void run() {
				try {
					socket.connect();
					
					Message msg = handlerStatus.obtainMessage();
					msg.arg1 = CONNECTED;
	                handlerStatus.sendMessage(msg);
	                
					receiverThread.start();
					
				} 
				catch (IOException e) {
					Log.v("N", "Connection Failed : "+e.getMessage());
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	//properly closing the socket and updating the status
	public void close() {
		try {
			socket.close();
			receiverThread.interrupt();
			
			Message msg = handlerStatus.obtainMessage();
			msg.arg1 = DISCONNECTED;
			handlerStatus.sendMessage(msg);
            
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//the main function of the app : sending character over the Serial connection when the user presses a key on the screen
		public void sendData(String data) {
			try {
				sendStream.write(data.getBytes());
		        sendStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
	//this thread listens to replies from Arduino as it performs actions, then update the log through the Handler
	private class ReceiverThread extends Thread {
		Handler handler;
		
		ReceiverThread(Handler h) {
			handler = h;
		}
		
		@Override public void run() {
			while(socket != null) {
				if (isInterrupted()){
						try {
							join();
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
				}
				try {
					if(receiveStream.available() > 0) {
						String dataToSend = ""; //when we hit a line break, we send the data									
						
						dataToSend = receiveReader.readLine();
						if (dataToSend != null){
							Log.v(TAG, dataToSend);
							Message msg = handler.obtainMessage();
							Bundle b = new Bundle();
							b.putString("receivedData", dataToSend);
			                msg.setData(b);
			                handler.sendMessage(msg);
			                dataToSend = "";
						}
						
					}
				} 
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
