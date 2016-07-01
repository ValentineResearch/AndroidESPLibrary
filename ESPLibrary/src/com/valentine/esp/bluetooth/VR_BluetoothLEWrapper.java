/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.bluetooth;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.valentine.esp.ValentineESP;
import com.valentine.esp.constants.ESPLibraryLogController;
import com.valentine.esp.packets.ESPPacket;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VR_BluetoothLEWrapper extends VR_BluetoothWrapper {

	private static final String 		LOG_TAG = "VR_BluetoothLEWrapper LOG";
		
	// The UUID of the V1connection LE Service we need to discover so we can use it BluetoothGattCharacteristic.
	private static final String 		V1_CONNECTION_LE_SERVICE = "92a0aff4-9e05-11e2-aa59-f23c91aec05e";
	
	/**
	 * The UUID of the Generic Access Profile service's Device Name characteristic.
	 */
	public static final String 			GAP_DEVICE_NAME = "00002a00-0000-1000-8000-00805f9b34fb";
	
	/**
	 * The UUID of the Generic Access Profile service. 
	 */
	public static final String 			GENERIC_ACCESS_PROFILE = "00001800-0000-1000-8000-00805f9b34fb";
	
	// The UUID of the V1 connection service's BluetoothGattCharacteristic that we want to be notified about.
	/**
	 * The UUID for the BluetoothGattCharacteristic to read short data from the V1.
	 */
	private static final String 		V1_OUT_CLIENT_IN_SHORT = "92a0b2ce-9e05-11e2-aa59-f23c91aec05e";	

	// The UUID of the V1 connection service's BluetoothGattCharacteristic that we want to be write data to.
	/**
	 * The UUID for the BluetoothGattCharacteristic to write short data to the V1.
	 */
	private static final String 		CLIENT_OUT_V1_IN_SHORT = "92a0b6d4-9e05-11e2-aa59-f23c91aec05e";
	
	/**
	 *  This Descriptor allows the app to set notifications for the {@link V1_OUT_CLIENT_IN_SHORT} BluetoothGattCharacteristic.
	 */
	public static final String 			CLIENT_CHARACTERISTIC_CONFIGURATION ="00002902-0000-1000-8000-00805f9b34fb";
	
	private static final UUID					GAP_DEVICE_NAME_UUID = UUID.fromString(GAP_DEVICE_NAME);
	private static final UUID					GENERIC_ACCESS_PROFILE_UUID = UUID.fromString(GENERIC_ACCESS_PROFILE);
	private static final UUID					CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID.fromString(CLIENT_CHARACTERISTIC_CONFIGURATION);
	private static final UUID 					CLIENT_OUT_SHORT_UUID = UUID.fromString(CLIENT_OUT_V1_IN_SHORT);
	private static final UUID 					V1_OUT_SHORT_UUID = UUID.fromString(V1_OUT_CLIENT_IN_SHORT);
	public static final UUID 					V1_CONNECTION_LE_SERVICE_UUID = UUID.fromString(VR_BluetoothLEWrapper.V1_CONNECTION_LE_SERVICE);
	
	private BluetoothGatt 				mBluetoothGatt = null;	
	/**
	 * Arraylist that contains the {@link ESPPacket} received from the bluetooth device.
	 */
	private ArrayList<ESPPacket> 		receivedPackets = new ArrayList<ESPPacket>();
	private ReentrantLock 				lock = new ReentrantLock();
	
	private BluetoothManager  			mBluetoothManager = null;
	private BluetoothAdapter			mBluetoothAdapter = null;

	private long 						mLastTimeDataReceived;
	private ConnectionCheckRunnable 	mConnectionCheckRunnable = null;	
	private boolean 					mDisconnecting;
	private BluetoothGattCharacteristic mDeviceNameCharacteristic;
	private long 						mConnCheckDelay = 0L;
	boolean 							mDevicefound = false;

	public VR_BluetoothLEWrapper(ValentineESP valentineESP, BluetoothDevice deviceToConnect, int secondsToWait, Context context) {
		// Always get application context to make sure we don't unintentionally leak an activity.
		super(valentineESP, deviceToConnect, secondsToWait, ConnectionType.V1Connection_LE, context.getApplicationContext());
		// Recommended method for acquiring the BluetoothAdapter in Android 4.3.
		mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		mConnCheckDelay = (mSecondsToWait * 1000);
		mLastTimeDataReceived = System.currentTimeMillis();
	}
	
	private class ConnectionCheckRunnable implements Runnable {

		private volatile boolean mRun = true;
		
		public void cancel() {
			mRun = false;
		}

		@Override
		public void run() {
			
			if(mBluetoothGatt != null) {
				long time = System.currentTimeMillis();
				// If the last time data was received is equal to above the time to wait timeout, check to see if we've lost Bluetooth connection.
				if((time - mLastTimeDataReceived) >= mConnCheckDelay) {					
					
					if(mBluetoothGatt.readCharacteristic(mDeviceNameCharacteristic)) {
						// Immediately delay send Message by 500 milliseconds to check the Bluetooth Connection state.
						// This is necessary because when connection lost occurs the first call to readCharacterisitc will return true (Although no call to onCharacteristicRead).
						// Subsequent calls will fail and return false.
						postDelayed(this, 500);
						return;
					}					
					
					mRun = false;
					lostConnection();
				}
				// Prevents this runnable from executing anymore.
				if(mRun) {
					postDelayed(this, 500);
				}
			}
		}	
	};
	
	private void lostConnection() {
		mConnectionLost = true;
		// We were unable to read a characteristic from the V1Connection so we will treat this a connection loss.
		setIsConnected(false);
		// Notify the library of our new state.
		if(isESPRunning()) {
			if(ESPLibraryLogController.LOG_WRITE_WARNING) {
				Log.w(LOG_TAG, "VR_BluetoothLEWrapper, we have lost the bluetooth connection, shutting down the library.");
			}		
			handleThreadError();
		}
	}
	
	/**
	 * Callbacks that receive information about the Bluetooth LE connection, such as Connect, Disconnect, Data read & write, and service discovery.
	 */
	private BluetoothGattCallback 	mGattCallback = new BluetoothGattCallback() {
		
		/**
		 * Snooping in the Android Source code I found that 8 is equal to GATT_ERROR which gets returned
		 * when trying to connect to a V1Connection LE that is paired to a different device.
		 */
		private final static int GATT_ERROR = 0x85;

		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			mDevicefound = false;
			if(status == BluetoothGatt.GATT_SUCCESS) {
				if(newState == BluetoothGatt.STATE_CONNECTED) {
					if(ESPLibraryLogController.LOG_WRITE_DEBUG) {
						Log.d(LOG_TAG, "BluetoothGatt state change - STATE CONNECTED");
					}
					
					// To prevent stray connections from taking place always set this flag to false.
					mConnectOnResult = false;
					setIsConnected(true);

					// If we have connected we want to discover the services that are available on the device.
					// We need to discover the services before we can use them.
					gatt.discoverServices();
					
					// Create the reader and writer threads but do not start them, leave that to the ValentineESP object.
					mReaderThread = new DataReaderThread(mSecondsToWait);
					mWriterThread = new DataWriterThread();					
					// Start up the reader and writer threads. 
					mReaderThread.start();
					mWriterThread.start();
					
					// Notify the rest of the Library on the UI thread that we are disconnected to the LE device.
					sendEmptyMessageDelayed(CONNECTED, 0);
				}
				else if(newState == BluetoothGatt.STATE_DISCONNECTED) {
					if(ESPLibraryLogController.LOG_WRITE_DEBUG) { 
						Log.d(LOG_TAG, "BluetoothGatt state change - STATE DISCONNECTED");
					}
					// Once we've disconnected, remove the connection check runnable.
					mConnectionCheckRunnable.cancel();
					removeCallback(mConnectionCheckRunnable);
					
					mConnectOnResult = false;
					// If we are not experiencing a connection loss, call close on the BluetoothGatt object then set the reference to null.
					if(!mConnectionLost) {
						gatt.close();
						mBluetoothGatt = null;						
					}
					setIsConnected(false);
					// Notify the rest of the Library on the UI thread that we are disconnected to the LE device.
					sendEmptyMessageDelayed(DISCONNECTED, 0);
					
					// If ESP is still running we are experiencing an unexpected disconnect and should signal the ValentineESP object to stop.
					// This is necessary because some phones will indicate a disconnection instead of a CONNECTION_LOSS. 
					if(isESPRunning()) {
						handleThreadError();
					}					
				}
			}
			else {				
				// This condition is treated as a connection loss because the status returned back was not successful and the newState is disconnected.
				if(newState == BluetoothGatt.STATE_DISCONNECTED) {
					if(ESPLibraryLogController.LOG_WRITE_DEBUG) { 
						Log.d(LOG_TAG, "BluetoothGatt state change - Connection Loss : STATE DISCONNECTED NOW");
					}
					// Once we've disconnected, remove the connection check runnable.
					mConnectionCheckRunnable.cancel();
					removeCallback(mConnectionCheckRunnable);
					
					gatt.close();
					mBluetoothGatt = null;
					mConnectionLost = false;
					// If this flag is set to true, we are in the middle of 
					// connecting and was for the OS to notify that it has officially disconnected from the previous connection.
					if(mConnectOnResult) {
						sendEmptyMessageDelayed(ATTEMPT_CONNECTION, 0);
					}
				}
			}
		}

		@Override
		public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			// Set the can flag that controls writing to the V1Connection to true.
			setCanWrite(true);
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			// Try to convert the BluetoothCharacteristics's data to an ESPPacket.
			handleV1OutClientInData(characteristic.getValue());
		}
		
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {			
			// If this flag is set to true, we want to disconnect.
			if(mDisconnecting){
				// Create a message with the appropriate what field and handler to connect the LE device.
				sendEmptyMessageDelayed(ATTEMPT_DISCONNECTION, 0);
			}
		}
		
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {			
			if(status == BluetoothGatt.GATT_SUCCESS) {
				mBluetoothGatt = gatt;
				BluetoothGattService service = gatt.getService(UUID.fromString(V1_CONNECTION_LE_SERVICE));
				BluetoothGattCharacteristic characterisitic = service.getCharacteristic(V1_OUT_SHORT_UUID);
				// Tell the BluetoothGATT object the characteristic we want to be notified for.
				mBluetoothGatt.setCharacteristicNotification(characterisitic, true);
				// Get the Client Characteristic Configuration Descriptor from the V1 Short Client In Characteristic so we can enable notifications.
				BluetoothGattDescriptor descriptor = characterisitic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID);
				if(descriptor != null) {
					// Set the value to be NOTIFICATION instead of INDICATION. and write it to the Bluetooth LE device so it will start returning data to us.
					descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		            mBluetoothGatt.writeDescriptor(descriptor);
				}

				BluetoothGattService gapService = gatt.getService(GENERIC_ACCESS_PROFILE_UUID);
				mDeviceNameCharacteristic = gapService.getCharacteristic(GAP_DEVICE_NAME_UUID);

				// Initialize the Connection Check runnable.
				mConnectionCheckRunnable = new ConnectionCheckRunnable();
				// Post the check connection runnable.
				postDelayed(mConnectionCheckRunnable, 3000);
			}							
		}		
	};
	
	/**
	 * Callback that gets fired when a LE {@link BluetoothDevice} is discovered.
	 */
	private final LeScanCallback mLeScanCallback = new LeScanCallback() {
		
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			// Convert the byte data into a list of UUID's
			List<UUID> uuids = parseUuids(scanRecord);
			// Check to see if the discovered BLE device supports the V1Connection LE service.
			if(uuids.contains(V1_CONNECTION_LE_SERVICE_UUID)) {
				if(mScanCallback != null && mShouldNotify) {
					mScanCallback.onDeviceFound(new BluetoothDeviceBundle(device, rssi, System.currentTimeMillis(), ConnectionType.V1Connection_LE), ConnectionType.V1Connection_LE);
				}
			}
		}		
	};
	
	/**
	 * This method came from {@link http://stackoverflow.com/a/24539704/3591491}. 
	 * 
	 * Parses through advertisement data and returns the contained services.
	 * (This is necessary because BluetoothAdapter.startLeScan(UUID[], LeScanCallback) does not respect the UUID list when performing a bluetooth scan).
	 *  
	 * @param advertisedData	The advertisement data received from the bluetooth onLEScan().
	 *  
	 * @return	uuids A list containing all of the services hosted by the bluetooth device.
	 */
	private List<UUID> parseUuids(byte[] advertisedData) {
		// Bail out if we have no advertisement data.
		if(advertisedData == null) {
			return null;
		}
		
	     List<UUID> uuids = new ArrayList<UUID>();
	     // Create a byte buffer and arrange it Little Endian based off of the Bluetooth v 4.0 specification (Right-to-Left)
	     ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
	     //	Based off of Bluetooth SIG the advertisement packet format, can be constructed of multiple data structures.
	     while (buffer.remaining() > 2) {	    	 
	    	 // Get the length of the data.
	    	 // Based off of Bluetooth SIG the advertisement packet format, the first byte is the lengths of the resulting data that follows (includes data type byte).
	         byte length = buffer.get();
	         // if the length is zero we want to break out because their is bno data.
	         if (length == 0) break;
	         // Get the type of the data.
	         // Based off of Bluetooth SIG the advertisement packet format, the next byte is data type of the data. (Manufacturer Specific).
	         byte type = buffer.get();
	         switch (type) {
	         	 // case 0x01 Indicate Flags of the BLE devices. 
	             case 0x02: // Partial list of 16-bit UUIDs 
	             case 0x03: // Complete list of 16-bit UUIDs
	            	 /* AS OF NOW THE V1 CONNECTION DOES NOT USE 16-BIT UUID'S */
	                 while (length >= 2) {
	                     uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
	                     // This is necessary because ByteBuffer.getShort() will return the next two bytes and increase its internal position by two
	                     // so we must update the length variable to only extract the correct number of bits.
	                     length -= 2;
	                 } 
	                 break; 
	                 // If the data type of the byte is six or seven we want to extract the the 128 bit UUID of the service
	                 // by getting the the next sixteen bytes. In the order least significant bytes to most significant bytes. We do this because
	                 // the buffer is ordered by Little Endian.
	             case 0x06: // Partial list of 128-bit UUIDs 
	             case 0x07: // Complete list of 128-bit UUIDs 
	                 while (length >= 16) {
	                     long lsb = buffer.getLong();
	                     long msb = buffer.getLong();
	                     uuids.add(new UUID(msb, lsb));
	                    // This is necessary because ByteBuffer.getLong() will return the next eight bytes and increase its internal position by eight
	    	            // so we must update the length variable to only extract the correct number of bits.
	                     length -= 16;
	                 } 
	                 break; 
	 
	             default:
	            	 // Move the buffers current position to the next 'data length' index inside the backing array.
	            	 // This works because advertisement data based in contains several chunks of data byte data arranged {length, data type, data ...}
	            	 // repeating. 
	                 buffer.position(buffer.position() + length - 1);
	                 break; 
	         } 
	     } 
	 
	     return uuids;
	}	
	
	/**
	 * Takes a byte array and tries to convert it to an ESPPacket and stores it inside of a ESPPacket queue to be processed by the data reader thread.
	 * 
	 * @param packetData	Byte array to parse into a valid ESPPacket.
	 */
	private void handleV1OutClientInData(byte [] packetData) {
		 //If there is not packet data contained in this array, return and treat it as if there was no data.
		// This method is only called from onCharacteristicChanged
		// by the Android Bluetooth LE framework and there is not guarantee that a null data will ever be returned.
		if(packetData == null) {
			return;
		}
		// Save the last time we received data from the V1.
		mLastTimeDataReceived = System.currentTimeMillis();		
		
		// Add the packet data from the byte array to the byte buffer to be processed later.
		for(Byte data: packetData) {
			m_readByteBuffer.add(data);
		}
		ESPPacket packet = ESPPacket.makeFromBuffer(m_readByteBuffer, ConnectionType.V1Connection_LE, mlastKnownV1Type);
		if(packet != null) { 			
			// Lock the code to prevent access to the receivedPackets list.
			lock.lock();
			receivedPackets.add(packet);
			lock.unlock();
		}
	}
	
	/**
	 * Indicates if we are connected by checking if the V1Connection is inside of the connected devices list the framework returns.
	 * 
	 * @return True if there is a connection to a V1Connection LE otherwise, false.
	 */
	public boolean isLEConnected() {		
		List<BluetoothDevice> connDevices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);		
		boolean retVal = (mBluetoothDevice != null) ? connDevices.contains(mBluetoothDevice) : false;
		return retVal;
	}

	/**
	 * Starts scanning for V1Connection LE devices.	 
	 * 
	 */
	@Override
	protected void startScan() {
		// Start the LE bluetooth scan.
		// Note that the use of the deprecated startLEScan here is intentional because the newer BluetoothLEScanner.startScan() method is not compatible with
		// Android 4.3 and we want to support that version.
		mBluetoothAdapter.startLeScan(mLeScanCallback);
	}
	
	/**
	 * Stops scanning for LE {@link BluetoothDevice}s.
	 */
	@Override
	protected void stopScan() {
		// Stop the LE bluetooth scan.
		// Note that the use of the deprecated stopLeScan here is intentional because the newer BluetoothLEScanner.stopScan() method is not compatible with
		// Android 4.3 and we want to support that version.
		mBluetoothAdapter.stopLeScan(mLeScanCallback);
	}
	
	
	@Override
	protected boolean mStartConnectProcess() {
		return mBluetoothAdapter.startLeScan(mConnectScanCallback);		
	}
	
	private void mStopScanLe() {
		mBluetoothAdapter.stopLeScan(mConnectScanCallback);
	}
	
	private LeScanCallback mConnectScanCallback = new LeScanCallback() {
		
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			// Since the stopLeScan appears to be asynchronous, we could theoretically receive a scan result after calling stop, so we want to make sure we are not disconnecting.
 			if(!mDisconnecting && !mDevicefound && device.equals(mBluetoothDevice)) {
				mDevicefound = true;
				// Stop scanning and call connect.
				mStopScanLe();
				// If BluetoothGatt is not null, we did lost connection from the V1Connection and did not shutdown correctly. Set a flag to connect once
				// the system returns that we've lost connection.
				if(mBluetoothGatt != null) {
					mConnectOnResult = true;
				}
				else {
					// Call connect on the Bluetooth device and receive a BluetoothGatt object that will be used to interact with the V1Connection.
					mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback);
				}				
			}
		}
	};
	
	/**
	 * Tries to establish a connection with the {@link BluetoothDevice} passed into the constructor at runtime
	 * by opening a BluetoothGatt connection with it.
	 * 
	 * @return True if connected to the V1connection LE otherwise, false.
	 */
	@Override
	protected boolean connect() {
		// Return false if we have no bluetooth device to connect to.
		if(mBluetoothDevice == null) {
			return false;
		}
		if(mConnectionCheckRunnable != null) {
			mConnectionCheckRunnable.cancel();
			// Remove the runnable to check bluetooth connection.
			removeCallback(mConnectionCheckRunnable);
		}
		mDisconnecting = false;
		mConnectOnResult = true;
		if(!mConnectionLost) {
			// Create a message with the appropriate what field and handler to connect the LE device.
			sendEmptyMessageDelayed(ATTEMPT_CONNECTION, 0);
		}
		return true;
	}	
	
	@Override
	protected boolean disconnect(boolean waitToReturn) {		
		// Make sure we stop the BLE scan taking place.
		mStopScanLe();
		// This will prevent us from trying to connect in the case we have lost a bluetooth connection.
		mConnectOnResult = false;
		// There is not BluetoothGatt object or we are experiencing a connection loss just return.
		// This is necessary because if we are in the middle of a connection loss, calling BluetoothGatt.disconnect()
		// will stop the framework from issuing a callback indicating that the system has recognized the connection loss and is ready to reconnect.
		if(mBluetoothGatt == null || mConnectionLost) {
			return true;
		}		
				
		mDisconnecting = true;
		if(mConnectionCheckRunnable != null ) {
			mConnectionCheckRunnable.cancel();
			// Remove the runnable to check bluetooth connection.
			removeCallback(mConnectionCheckRunnable);
		}
		if(isConnected()) {
			// If the last time we received data was less than 1 second ago, we want to immediately disconnect.
			if((System.currentTimeMillis() - mLastTimeDataReceived) < 500) {
				// Create a message with the appropriate what field and handler to connect the LE device.
				sendEmptyMessageDelayed(ATTEMPT_DISCONNECTION, 0);
			}
			else {
				determineConnectionLoss();
			}
		}
		else {
			/**
			 *  If we are not yet connected but mConnectOnResult is true and mConnectionLost is false
			 *  we are experiencing a delayed connection, so make sure we call mDisconnect() to terminate the on going connection process. 
			 */
			if(mConnectOnResult && !mConnectionLost) {
				mDisconnect();
			}
		}
		
		return true;
	}	
	
	private void determineConnectionLoss() {
		// If we are unable to initiate a characteristic read then we know we've lost connection with the bluetooth device.
		// If the characteristic read operation is and the we are still connected, inside of the character read callback we will perform the disconnect.
		// If the characteristic read operation is successful but we are not connected, do nothing because we are in the middle of a connection loss
		// and should wait until the framework issue the callback.
		if(mBluetoothGatt!= null && mDeviceNameCharacteristic != null && !mBluetoothGatt.readCharacteristic(mDeviceNameCharacteristic)) {
			lostConnection();
		}
	}
	
	@Override
	protected void mDisconnect() {
		// Only call disconnect if we have a mBluetoothGatt object.
		if(mBluetoothGatt != null) {
			mBluetoothGatt.disconnect();
			// Once we've called disconnect, set IsESPRunning to false. The threads should have already stopped by now.
			setIsESPRunning(false);
		}
	}
	
	@Override
	protected boolean getAvailPackets(ArrayList<ESPPacket> packetList) {
		// If packetList is null, and error has occurred so return null.
		if ( packetList == null ) {
			return false;		
		}
		else {
			packetList.clear();
			// Lock the code to to prevent access to the receivedPackets.
			try {
				lock.lock();
				
				// Store the received ESPPackets into the packetList passed to us by our parent.
				for(ESPPacket curPacket : receivedPackets) {
					packetList.add(curPacket);
				}				
				// Clear the received ESPPackets.
				receivedPackets.clear();
			}
			finally {
				lock.unlock();
			}
			
			return true;			
		}
	}
	

	/**
	 * Initializes variables that are expected to be in a specific state before the library is started. 
	 */
	protected void prepForStart ()
	{
		// Update the base class
		prepForStartBase();
		
		if ( receivedPackets != null ){
		// Make sure we don't have any left over packets from a previous connection
			receivedPackets.clear();
		}
	}

	@Override
	protected boolean writePacket(ESPPacket packet) {
		// If the mBluetoothGatt device is null we are no longer connected to the V1.
		if(mBluetoothGatt != null) {
			BluetoothGattService service = mBluetoothGatt.getService(V1_CONNECTION_LE_SERVICE_UUID);
			if(service != null) {
				// We want to get the CLIENT_OUT_V1_IN_SHORT BluetoothGattGharacterisitic so we can write to ESPPacket to the V1.
				BluetoothGattCharacteristic characteristic = service.getCharacteristic(CLIENT_OUT_SHORT_UUID);			
				byte [] packetData = ESPPacket.makeByteStream(packet, ConnectionType.V1Connection_LE);
				if(packetData == null) {
					return false;
				}
				// Add the ESPPacket to the CLIENT_OUT_V1_IN_SHORT BluetoothGattGharacterisitic.
				characteristic.setValue(packetData);
				
				if(ESPLibraryLogController.LOG_WRITE_INFO){
					Log.i(LOG_TAG, "Writing to LE device " + packet.getPacketIdentifier().toString() + " to " + packet.getDestination().toString());
				}
				
				// Set the can write flag to false to prevent another packet from
				// attempting to write until the packet is successfully written.
				setCanWrite(false);
				// Only set canWrite to false if we were able to initiate the characteristic write.
				if(!mBluetoothGatt.writeCharacteristic(characteristic)) {
					// If the characteristic write initiation failed, set the can write flag to true to allow another write attempt
					setCanWrite(true);
					return false;
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Bluetooth LE does not use BluetoothSockets so always return null.
	 * 
	 * @deprecated DO NOT USE. THIS METHOD WILL BE REMOVED IN THE NEXT API RELEASE.
	 */
	@Override
	public BluetoothSocket getSocket() {
		return null;
	}
}
