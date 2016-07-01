/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.valentine.esp.ValentineESP;
import com.valentine.esp.constants.ESPLibraryLogController;
import com.valentine.esp.packets.ESPPacket;

public class VR_BluetoothSPPWrapper extends VR_BluetoothWrapper {
	
	private static final String 					LOG_TAG = "VR_BluetoothsppWrapper LOG";
	
	private static final int						STREAM_BUFFER_SIZE = 1024;
	
	private BluetoothSocket 						mBluetoothSocket;
	private InputStream 							mInputStream;
	private OutputStream 							mOutputStream;	
	protected ArrayList<Byte> 						m_readByteBuffer;
	
	private static byte[] 							m_streamBuffer = new byte[STREAM_BUFFER_SIZE];		
	
	private static final String 					SPP_NAME_SEARCH_PREFIX = "V1connection-";
	
	public VR_BluetoothSPPWrapper(ValentineESP valentineESP, BluetoothDevice deviceToConnect, int secondsToWait, Context context) {
		super(valentineESP, deviceToConnect, secondsToWait, ConnectionType.V1Connection, context.getApplicationContext());
		// Always get application context to make sure we don't unintentionally leak an activity.
		mContext = context.getApplicationContext();
		m_readByteBuffer = new ArrayList<Byte>();
		// SPP is always allowed to write because writing to the output stream is blocking call.
		setCanWrite(true);
		registerConnectStateReceiver();
	}
	
	/**
	 * BroadcastReceiver that gets called when a SPP {@link BluetoothDevice} is found.
	 * Also responsible for restarting the scanning process when it terminates, 
	 * The Android framework will only perform a Bluetooth (SPP) scan (Discovering) for approximately 12 seconds. 
	 */
	private BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				// If seconds to scan is zero, scan infinitely.
				if(mSecondsToScan == 0) {
					BluetoothAdapter.getDefaultAdapter().startDiscovery();
				}
				
			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				if(mScanCallback != null) {
					BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 1);
					// Check to make sure the bluetooth device is a V1Connection device or at least a bluetooth device whose name starts with V1connection. 
					if(isV1ConnectionSPP(device) && mShouldNotify) {
						mScanCallback.onDeviceFound(new BluetoothDeviceBundle(device, rssi, System.currentTimeMillis(), ConnectionType.V1Connection), ConnectionType.V1Connection);
					}
				}
			}
		}		
	};
	
	/**
	 * BroadcastReceiver that gets called when a SPP {@link BluetoothDevice} has been connected/disconnected. 
	 */
	private BroadcastReceiver connectionStateBroadcastReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			// Guaranteed to not to be null for which the intent actions this Broadcast Receiver is registered.
			BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			
			if(device != null && device.equals(mBluetoothDevice)) {
				if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
					if(ESPLibraryLogController.LOG_WRITE_DEBUG) {
		        		Log.d(LOG_TAG, "ACTION_ACL_CONNECTED");
		        	}					
				}
				else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
					// We have  disconnected from the bluetooth device.
					if(ESPLibraryLogController.LOG_WRITE_DEBUG) {
		        		Log.d(LOG_TAG, "ACTION_ACL_DISCONNECTED");
		        	}
					// We only want to do something if we are not about to connect.
					// If we experience a connection loss and a new connection is attempted before the systems has indicated a loss.
					// We will get a disconnected action and then a connection_action.
					if(!mConnectOnResult) {
						mConnectOnResult = false;
						// Set the BluetoothSocket reference to null now.
						mBluetoothSocket = null;
						// Set the isConnected flag to false before notifying the sleeping thread.
						setIsConnected(false);					
						// Once we know from the Android OS that the bluetooth device has been connected we want to notify the waiting disconnect method.
						synchronized(VR_BluetoothSPPWrapper.this) {
							VR_BluetoothSPPWrapper.this.notify();
						}
						// Send a message to the UI handler in the base class that will handle notify the UI.
						sendEmptyMessageDelayed(DISCONNECTED, 0);
						// If ESP is running when we get a disconnect, stop on the ValentineESP.
						if(isESPRunning()) {
							handleThreadError();
						}
					}					
				}
			}
		}
	};	
	
	@Override
	protected void finalize() throws Throwable {
		// Before this object gets garbage collected, unregister the BroadcastReceivers.
		// This is in a try-catch because if the broadcast receiver is not registered, a exception will be thrown.
		unregisterBroadcastReceiver();
		super.finalize();
	}
	
	public void unregisterBroadcastReceiver() {
		try {
			mContext.unregisterReceiver(deviceFoundReceiver);
			mContext.unregisterReceiver(connectionStateBroadcastReceiver);
		}
		catch(IllegalArgumentException e) {
			// If the BroadcastReceiver is not registered it will throw an exception so catch it here.			
		}
	}

	/**
	 * Helper method that registers the connectionStateBroadcastReceiver {@link BroadcastReceiver} for Bluetooth connection events.
	 */
	private void registerConnectStateReceiver() {
		// Create an Intentfilter and register it for the connect/disconnection/disconnect filters.
		IntentFilter connFilter = new IntentFilter();
		// Registers to Device found and connection state changes.
		connFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
		connFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
		connFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
		// Register the broadcast receiver.
		mContext.registerReceiver(connectionStateBroadcastReceiver, connFilter);
	}

	/**
	 * Helper method that checks if the {@link BluetoothDevice} is a V1Connection (SPP).
	 * 
	 * The method simple extracts the name {@link device} and checks if it starts with {@link SPP_NAME_SEARCH_PREFIX}
	 * 
	 * @param device	The {@link BluetoothDevice} to check.
	 * 
	 * @return	True if the {@link device}'s name starts with {@link SPP_NAME_SEARCH_PREFIX} otherwise, false.
	 */
	public static boolean isV1ConnectionSPP(BluetoothDevice device) {
		String temp = device.getName();
		// Trim the whitespace and make all the characters in the string lowercase before checking if it starts with the name prefix.
		return temp != null && temp.trim().startsWith(SPP_NAME_SEARCH_PREFIX);
	}
	
	/**
	 * Starts scanning for SPP {@link BluetoothDevice}s.
	 */
	@Override
	protected void startScan() {
		// Register for the scanning receiver only when we are scanning for device.
		IntentFilter scanFilter = new IntentFilter();
		// Adds actions to be notified when a BluetoothDevice (SPP) is found and when the scanning process stops. 
		scanFilter.addAction(BluetoothDevice.ACTION_FOUND);
		scanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		// Register the broadcast receiver.
		mContext.registerReceiver(deviceFoundReceiver, scanFilter);
		
		BluetoothAdapter.getDefaultAdapter().startDiscovery();
	}

	/**
	 * Stops scanning for SPP {@link BluetoothDevice}s.
	 */
	@Override
	protected void stopScan() {
		// Set this to true so we don't notify the anyone that we stop. For cases when we are exiting the app.
		BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
		try {
			mContext.unregisterReceiver(deviceFoundReceiver);
		}
		catch(Exception e) {
			
		}
	}
	
	/**
	 * Tries to establish a connection with the {@link BluetoothDevice} provided to the constructor at runtime, by connecting
	 * to the BluetoothSocket.
	 * @throws InterruptedException 
	 */
	@Override
	protected boolean connect() {
		// Register for the Bluetooth connection state broadcast receiver.
		registerConnectStateReceiver();
		// We want to cancel discovery at this point.
		stopScan();
		mConnectOnResult = true;
		try {
			
			disconnect(true);
			
			// Assume the connect attempt will not be aborted
			m_retryOnConnectFailure = true;
			
			UUID uu = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
			mBluetoothSocket = mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(uu);
			// This call is synchronous and will  until a connection is made with the BluetoothDevice.
			mBluetoothSocket.connect();
			
			mConnectOnResult = false;
			// Set connect on result to false so we do not try to connect if we loss a connection.
			mConnectOnResult = false;
			
			// Set the isConnected flag to true.
			setIsConnected(true);
			// Send a message to the UI handler in the base class that will handle notify the UI.
			sendEmptyMessageDelayed(CONNECTED, 0);
			
			mInputStream = mBluetoothSocket.getInputStream();
			mOutputStream = mBluetoothSocket.getOutputStream();
			// Create the reader and writer threads but do not start them, leave that to the ValentineESP object.
			mReaderThread = new DataReaderThread(mSecondsToWait);
			mWriterThread = new DataWriterThread();
			
			// Start up the reader and writer threads. 
			mReaderThread.start();
			mWriterThread.start();
			
			return true;
		}
		catch (IOException e) {
			if ( m_retryOnConnectFailure ) {
				// Make a second attempt to connect the the V1Connection.				
				return connectAttemptTwo();
			}			
			return false;
		}
	}
	
	/**
	 * Attempts a second connection using reflection.
	 * 
	 * @return	True if the connection was successful.
	 */
	private boolean connectAttemptTwo() {
		try 
		{
			Method m = mBluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
			mBluetoothSocket = (BluetoothSocket) m.invoke(mBluetoothDevice, 1);
			mBluetoothSocket.connect();
			
			// Set connect on result to false so we do not try to connect if we loss a connection.
			mConnectOnResult = false;
			
			// Set the isConnected flag to true.
			setIsConnected(true);
			// Send a message to the UI handler in the base class that will handle notify the UI.
			sendEmptyMessageDelayed(CONNECTED, 0);
			
			mInputStream = mBluetoothSocket.getInputStream();
			mOutputStream = mBluetoothSocket.getOutputStream();
			
			mReaderThread = new DataReaderThread(mSecondsToWait);
			mWriterThread = new DataWriterThread();
	
			// Start up the reader and writer threads.
			mReaderThread.start();
			mWriterThread.start();
			
			return true;
		} 
		catch (IOException e) {
			// Deliberately empty
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// Deliberately empty
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// Deliberately empty
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// Deliberately empty
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// Deliberately empty
			e.printStackTrace();
		}
		// If we've made it to this point, we are unable to connect so post a message to the UI thread handler.
		sendEmptyMessageDelayed(CONNECT_FAILURE, 0);
		// Callback to the valentineESP object to handle notify the rest of the library that SPP is not supported.
		mValentineESP.doUnsupportedDeviceCallback();		
		return false;
	}

	/**
	 * Tries to disconnects from the currently connected {@link BluetoothDevice} by closing
	 * the BluetoothSocket.
	 * 
	 */
	@Override
	protected boolean disconnect(boolean waitToReturn) {
		if ( mBluetoothSocket == null ) {
			return true;
		}
		
		try {			
			// Calling close on the BluetoothSocket will also close input
			// and output streams so there is no need to explicitly calling close on those objects.
			mBluetoothSocket.close();
			// Once we call close on the BluetoothSocket, set mIsESPRunning to false.
			setIsESPRunning(false);
			
			long startTime = System.currentTimeMillis(); // Convert to milliseconds.
			// The wait call is known to experience spurious wakeup, though they rarely occur, so we must call inside of a loop.
			while(waitToReturn && isConnected() && ((System.currentTimeMillis() - startTime)) < 15000) {
				synchronized(this) {
					if(ESPLibraryLogController.LOG_WRITE_INFO){
						Log.i(LOG_TAG, "Thread starting wait until the framework notifies us of disconnection.");
					}
					/* 
					 * Wait for 15 seconds to allow the system to disconnect from the BluetoothAdapter.
					 * When the Broadcast intent is sent from the Android OS that we've disconnected from the V1Connection it will set isConnected to false
					 * and notify this object to wake up the currently waiting thread.
					 * 
					 * Only wait 15 seconds from the initial call...
					 */					
					wait(15000 - (System.currentTimeMillis() - startTime));					
				}
			}
			
		} catch (IOException e) {
			if(ESPLibraryLogController.LOG_WRITE_ERROR){
				Log.e(LOG_TAG, "IOException received. Message: " + e.getMessage());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if(isConnected()){
			return false;
		}
		return true;
	}
	

	/**
	 * Returns the {@link BluetoothSocket} for in-out communication with the V1 connection.
	 * 
	 * @return	The {@link BluetoothSocket} of the current Bluetooth connection, if available, otherwise, null.
	 * 
	 * @deprecated DO NOT USE. THIS METHOD WILL BE REMOVED IN THE NEXT API RELEASE.
	 */
	@Override
	public BluetoothSocket getSocket() {
		return mBluetoothSocket;
	}
	
	/**
	 * Stores {@link ESPPacket}s into an ArrayList, read from the bluetooth connections inputstream.
	 */
	@Override
	protected boolean getAvailPackets(ArrayList<ESPPacket> packetList) {
		boolean result = true;
		// PacketList was null an error has occurred at this point
		if ( packetList == null ) {
			result = false;		
		}
		else {

			packetList.clear();
			try {
				// First, read all bytes that are available from the Bluetooth socket
				if (mInputStream.available() != 0) {			
					int readSize = mInputStream.read(m_streamBuffer);
					
					// Copy the data just read into the buffer for later processing. We can't use the read buffer for
					// processing because there may be data left in the buffer that will need to be used on the next 
					// call to this method. We can eliminate the need to copy each byte if we can find a way to read
					// from the input stream and append directly to the m_readByteBuffer ArrayList instead of using the
					// interim m_streamBuffer buffer.
					for (int i = 0; i < readSize; i++)
					{
						// Using the value of method is more efficient than casting according to the warning compiler warning message.
						m_readByteBuffer.add(Byte.valueOf(m_streamBuffer[i]));
					}
				}
				
				// Generate all of the packets available in the data just read. Note that there may be data left in
				// the buffer after this processing. If there is data left in the buffer, then the data represents a
				// partial ESP packet and it will be used to build packets the next time this method is called.
				ESPPacket curPacket;			
				do {
					curPacket = ESPPacket.makeFromBuffer(m_readByteBuffer, mConnectedType, mlastKnownV1Type);
					if ( curPacket != null ){
						packetList.add(curPacket);
					}				
				} while ( curPacket != null);
				
			} catch (IOException e) {
				if(ESPLibraryLogController.LOG_WRITE_WARNING){
					Log.w(LOG_TAG, "IOException encountered, shutting down esp...", e);
				}
				// We have an I/O exception so the inputstream must be no longer valid. Return false so we can let
				// valentineESP object know and stop the thread.
				result = false;
				e.printStackTrace();
				// Tell the ValentineESP to stop because the input stream has unexpectedly closed.
				handleThreadError();
			} 
		}		
		return result;
	}	
	

	/**
	 * Method initialize the sub class variables that are expected to be in a specific state before the library is started. 
	 */
	protected void prepForStart ()
	{
		// Update the base class
		prepForStartBase();
		
		// Make sure there is no left over data in the buffer for a previous connection
		Arrays.fill(m_streamBuffer, 0, STREAM_BUFFER_SIZE, (byte)0);		
		if ( m_readByteBuffer != null ){
			m_readByteBuffer.clear();
		}
	}
	
	/**
	 * Writes an {@link ESPPacket}s to the Bluetooth connection's output stream. 
	 */
	@Override
	protected boolean writePacket(ESPPacket packet) {
		// Pass in the connection type to makeByteStream to get the byte array for ESP packet.
		byte[] buffer = ESPPacket.makeByteStream(packet, mConnectedType);
		if(buffer == null) {
			return false;
		}
		boolean retVal = false;		
		// Delimit the byte array.
		buffer = escape(buffer);		
		try {
			
			if(ESPLibraryLogController.LOG_WRITE_INFO){
				Log.i("Valentine", "Writing to SPP device " + packet.getPacketIdentifier().toString() + " to " + packet.getDestination().toString());
			}
			
			// Do the actual writing
			mOutputStream.write(buffer);
			mOutputStream.flush();
			retVal = true;
		} catch (IOException e) {
			// If we have caught an I/O exception something has happened to the outputstream, most likely it was closed.
			retVal = false;
			// Tell the ValentineESP to stop because the output stream has unexpectedly closed.
			handleThreadError();
		}
		return retVal;
	}
	
	/**
	 * Converts reserved bytes to escape bytes, so they can be sent to the V1Connection.
	 * 
	 * @param _bytes	The byte to escape.
	 * 
	 * @return			The modified byte.
	 */
	private byte[] escape(byte[] _bytes)
	{
		int count = 0;
		for (int i = 1; i < _bytes.length-1; i++)
		{
			if (_bytes[i] == 0x7d )
			{
				count++;
			}
			else if (_bytes[i] == 0x7f)
			{
				count++;
			}
		}
		
		if (count == 0)
		{
			return _bytes;
		}
		else
		{
			byte[] escaped = new byte[_bytes.length + count];
			int escapedCounter = 1;
			escaped[0] = _bytes[0];
			
			for (int i = 1; i < _bytes.length-1; i++)
			{
				if (_bytes[i] == 0x7d )
				{
					escaped[escapedCounter] = 0x7d;
					escapedCounter++;
					escaped[escapedCounter] = 0x5d;
				}
				else if (_bytes[i] == 0x7f)
				{
					escaped[escapedCounter] = 0x7d;
					escapedCounter++;
					escaped[escapedCounter] = 0x5f;
				}
				else
				{
					escaped[escapedCounter] = _bytes[i];
				}
				escapedCounter++;
			}
			escaped[escaped.length-1] = _bytes[_bytes.length-1];
			return escaped;
		}
	}

	protected void mDisconnect() {
		// DO NOTHING HERE. THIS METHOD WAS DESIGNED FOR A DISCONNECTION PROCESS THAT NEED TO OCCUR ON THE UI THREAD.
		// IMPLEMENTATION SHOULD BE ASYNCHRONOUS
	}

	protected boolean mStartConnectProcess() {
		// ALWAYS RETURN FALSE. THIS METHOD WAS DESIGNED FOR A CONNECTION PROCESS THAT NEED TO OCCUR ON THE UI THREAD. 
		// IMPLEMENTATION SHOULD BE ASYNCHRONOUS
		return false;
	}
}
