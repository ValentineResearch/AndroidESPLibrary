/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.bluetooth;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import com.valentine.esp.PacketQueue;
import com.valentine.esp.ValentineClient;
import com.valentine.esp.ValentineESP;
import com.valentine.esp.constants.Devices;
import com.valentine.esp.constants.ESPLibraryLogController;
import com.valentine.esp.constants.PacketId;
import com.valentine.esp.constants.PacketIdLookup;
import com.valentine.esp.packets.ESPPacket;

public abstract class VR_BluetoothWrapper implements IVR_BluetoothWrapper {
	
	protected static final int 						CONNECTED = 200;
	protected static final int 						DISCONNECTED = CONNECTED + 1;
	protected static final int 						CONNECTIONLOSS = DISCONNECTED + 1;
	protected static final int 						CONNECT_FAILURE = CONNECTIONLOSS + 1;	
	protected static final int 						CHECK_CONNECTION_STATE = CONNECT_FAILURE + 1;
	
	protected static final int 						ATTEMPT_CONNECTION = 100;
	protected static final int 						ATTEMPT_DISCONNECTION = ATTEMPT_CONNECTION + 1;
	protected static final int 						STOP_SCAN = ATTEMPT_DISCONNECTION + 1;
	
	
	private static final String						LOG_TAG = "VR_BluetoothWrapper LOG";
	private static final int 						EMPTY_READ_SLEEP_TIME 	= 100;
	private int 									MAX_EMPTY_READS;	
	private static boolean 							m_protectLegacyMode = false;
	
	protected 	ConnectionType 						mConnectedType;
	protected 	ValentineESP 						mValentineESP;
	private 	int             					m_emptyReadCount;
	private 	boolean 							m_notifiedNoData;
	protected boolean 								mShouldNotify = false;
	
	/**
	 * Lock to control read & write access to the isConnected flag.
	 */
	protected ReentrantLock 						mConnectedLock = new ReentrantLock();
	/** 
	 * Lock to control read & write access to the canWrite flag.
	 */
	private ReentrantLock 							mWritelock = new ReentrantLock();
	/** 
	 * Lock to control read & write access to the mIsESPRunning flag.
	 */
	private ReentrantLock							mESPRunningLock = new ReentrantLock();
	
	private boolean									mIsESPRunning = false;
	private boolean 								mIsConnected = false;
	
	protected DataReaderThread 						mReaderThread;
	protected DataWriterThread  					mWriterThread;
	
	protected BluetoothDevice 						mBluetoothDevice;	
	protected boolean								m_retryOnConnectFailure;
	protected int 									mSecondsToWait;
	protected Context 								mContext;
	protected ArrayList<Byte> 						m_readByteBuffer;
	
	private boolean									mCanWrite = true;
	
	protected VRScanCallback 						mScanCallback = null;

	protected int 									mSecondsToScan = -1;
	
	protected ReentrantLock							echoLock  = new ReentrantLock();
	protected ArrayList<Pair<Long,ESPPacket>>		expectedEchoPackets = new ArrayList<Pair<Long,ESPPacket>>();
	
	protected Devices 								mlastKnownV1Type =  Devices.UNKNOWN;
	
	private Thread		 							mConnThread = null;
	
	protected boolean								mConnectionLost = false;
	protected volatile boolean 						mConnectOnResult = false;
	private final Object 							mLock = new Object();
	
	/**
	 * Private handler used to post {@link Runnable}'s or {@link Message}'s on the UI (main) thread.
	 */
	private UIHandler								mUIHandler = null;
	
	class UIHandler extends Handler {
		private WeakReference <VR_BluetoothWrapper> mVRBlueWrapper;
		
		public UIHandler(VR_BluetoothWrapper ref, Looper looper) {
			super(looper);
			// Create a WeakReference to the VR_BluetoothWrapper.
			this.mVRBlueWrapper = new WeakReference<VR_BluetoothWrapper>(ref);
		}

		@Override
		public void handleMessage(Message msg) {
			// Get the reference to the BluetoothWrapper.
			VR_BluetoothWrapper wrapper = mVRBlueWrapper.get();
			if(wrapper == null) {
				// TODO
				return;
			}
			
			if (msg.what != ATTEMPT_CONNECTION) {
				// Remove the message to check connection state. 
				removeMessages(CHECK_CONNECTION_STATE);
			}
			// Remove any messages to attempt connections. This will prevent several instances from piling up.
			removeMessages(ATTEMPT_CONNECTION);
			
			switch(msg.what) {
				case CONNECTED:
					// Set the Is ESP Running Flag to true.
					wrapper.mESPRunningLock.lock();
					wrapper.mIsESPRunning = true;
					wrapper.mESPRunningLock.unlock();
					// Notify the ValentineESP object that the connection has been established.
					wrapper.mValentineESP.onConnectEvent(true);					
					break;
				case DISCONNECTED:
					// Set the Is ESP Running Flag to false.
					wrapper.mESPRunningLock.lock();
					wrapper.mIsESPRunning = false;
					wrapper.mESPRunningLock.unlock();
					// Notify the ValentineESP object that the connection has been disconnected.
					wrapper.mValentineESP.onDisconnected();
					break;
				case CONNECTIONLOSS:
					// Set the Is ESP Running Flag to false.
					wrapper.mESPRunningLock.lock();
					wrapper.mIsESPRunning = false;
					wrapper.mESPRunningLock.unlock();
					// Notify the ValentineESP object that the connection has been lost.
					wrapper.mValentineESP.onConnectionLoss();
					break;
				case CONNECT_FAILURE:
					// Set the Is ESP Running Flag to false.
					wrapper.mESPRunningLock.lock();
					wrapper.mIsESPRunning = false;
					wrapper.mESPRunningLock.unlock();
					wrapper.mValentineESP.onConnectEvent(false);
					break;						
				case CHECK_CONNECTION_STATE:
					if(!isConnected()) {
						// If we have not connected by now we want to stop any pending connections.
						wrapper.disconnect(false);
					}
					// Notify the ValentineESP object that the connection has been established.
					wrapper.mValentineESP.onConnectEvent(false);	
					break;
				case ATTEMPT_CONNECTION:
					// Attempt a scan to make sure we can find the V1Connection LE and once it has been discovered connect to it.
					mStartConnectProcess();
					break;
				case ATTEMPT_DISCONNECTION:
					wrapper.mDisconnect();
					break;
				case STOP_SCAN:
					stopScan();
					if(mScanCallback != null) {
						// Stop bluetooth scanning, and notify the caller that the scan has completed.
						mScanCallback.onScanComplete(mConnectedType);
					}
					mScanCallback = null;
					break;
			}			
		}		
	}	
	
	/**
	 * Passed to a thread to call connect() for SPP connection's because the {@link BluetoothSocket.connect()} is blocking.
	 */
	private Runnable connectRunnable = new Runnable() {
		
		@Override
		public void run() {
			// If we become interrupted, do not call connect.
			if(!Thread.currentThread().isInterrupted()) {
				// Safe to call connect because we are on a background worker thread.
				connect();				
			}
		}
	};
	
	public VR_BluetoothWrapper(ValentineESP valentineESP, BluetoothDevice deviceToConnect, int secondsToWait, ConnectionType connectionType, Context context) {
		this.mBluetoothDevice = deviceToConnect;
		if(valentineESP == null) {
			throw new IllegalArgumentException("The ValentineESP instance was null. A valid instance of ValentienESP must be passed in.");
		}
		this.mValentineESP = valentineESP;
		this.m_retryOnConnectFailure = true;
		this.mSecondsToWait =  secondsToWait;
		this.MAX_EMPTY_READS = mSecondsToWait * 10;
		this.mConnectedType = connectionType;
		this.mContext = context;
		// Initialize the byte Arraylist that needs to be used by both the SPP and LE implementations.
		this.m_readByteBuffer = new ArrayList<Byte>();
		
		// Instantiate a Handler on the UI Looper (Main Thread's Looper for sending messages).
		mUIHandler = new UIHandler(this, Looper.getMainLooper());		
	}
	
	/**
	 * Set the delay to wait before the data reader thread will notify that the
	 * no data has been received.
	 * 
	 * @param delay	Number of seconds to wait before notifying that no data has been received.
	 */
	public void setNoDataReceivedDelay(int delay) {
		MAX_EMPTY_READS = delay * 10;		
	}
	
	/**
	 * Helper method for posting runnable to the UIHandler.
	 * 
	 * @param runnable  	The Runnable that will be executed.delayMillis The delay (in milliseconds) until the Runnable will be executed.
	 *   
	 * @param delayMillis 	The delay (in milliseconds) until the Runnable will be executed.
	 * 
	 * @return				True if the Runnable was successfully posted, false otherwise.
	 */
	protected boolean postDelayed(Runnable runnable, long delayMillis) {
		if(delayMillis < 0) {
			delayMillis = 0L;
		}
		return mUIHandler.postDelayed(runnable, delayMillis);
	}
	
	/**
	 * Helper method for sending an empty message to the UIHandler.
	 * 
	 * @param what			A message code to identify the 'what' the message is about.
	 * @param delayMillis	The delay (in milliseconds) until the Runnable will be delivered.
	 * 
	 * @return 				True if the Message was successfully sent, false otherwise.
	 */
	protected boolean sendEmptyMessageDelayed(int what, long delayMillis) {
		if(delayMillis < 0) {
			delayMillis = 0L;
		}
		return mUIHandler.sendEmptyMessageDelayed(what, delayMillis);
	}
	
	/**
	 * Helper method to remove any posted runnables waiting for execution.
	 * 
	 * @param runnable	The runnable the that you would like to remove from the UIHandler.
	 */
	protected void removeCallback(Runnable runnable) {
		mUIHandler.removeCallbacks(runnable);
	}
	
	/**
	 * Helper method to remove any sent messages whose Message.what value equals the value passed in, waiting for execution.
	 * 
	 * @param what	The message code to identify the message.
	 */
	protected void removeMessage(int what) {
		mUIHandler.removeMessages(what);
	}

	@Override
	public boolean startSync() {
		// Set variables to a known value before connecting
		prepForStart();
		// Connect to the V1connection
		boolean retVal = connect();		
		mESPRunningLock.lock();
		mIsESPRunning = retVal;
		mESPRunningLock.unlock();		
		return retVal;
	}	
	
	@Override
	public boolean stopSync() {
		mESPRunningLock.lock();
		// In case we are in the middle of starting up.
		if(mConnThread != null) {
			mConnThread.interrupt();
		}
		
		/*
		 * Disconnect has to be called first or these errors arise.
		 * 05-19 10:42:38.424: A/libc(6751): @@@ ABORTING: INVALID HEAP ADDRESS IN dlfree
		 * 05-19 10:42:38.424: A/libc(6751): Fatal signal 11 (SIGSEGV) at 0xdeadbaad (code=1)
		 * 
		 * This is because BluetoothSocket.close() could be called from multiple threads. 
		*/
		boolean retVal = disconnect(true);
		if(!retVal) {
			if(ESPLibraryLogController.LOG_WRITE_ERROR) {
				Log.e(LOG_TAG, "stop(). Unable to disconnect the from the BluetoothDevice.");
			}
		}
		// If the threads are running we want stop stop them.
		if (mReaderThread != null)
		{
			mReaderThread.setRun(false);
			mReaderThread.interrupt();
		}
		
		if (mWriterThread != null)
		{
			mWriterThread.setRun(false);
			mWriterThread.interrupt();
		}
		
		mIsESPRunning = false;
		
		mESPRunningLock.unlock();
		// Disconnect the bluetooth connection.
		return retVal;
	}
	
	@Override
	public int startAsync() {		
		// Return RESULT_OF_CONNECTION_EVENT_FAILED_NO_DEVICE if we have no bluetooth device to connect to.
		if(mBluetoothDevice == null) {
			return ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_NO_DEVICE;
		}		
		// Set variables to a known value before connecting
		prepForStart();
		int retVal = ValentineClient.RESULT_OF_CONNECTION_EVENT_CONNECTING;
		if(mConnectedType == ConnectionType.V1Connection) {
			mConnThread = new Thread(connectRunnable);
			mConnThread.start();
			retVal = ValentineClient.RESULT_OF_CONNECTION_EVENT_CONNECTING;
		}
		else {			
			connect();
			int delay = 15000;
			// If we are reconnecting after a connection loss, set the connection timeout and a return value indicating that their will be a delay.
			if(mConnectionLost){
				delay = 25000;
				retVal = ValentineClient.RESULT_OF_CONNECTION_EVENT_CONNECTING_DELAY;
			}
			// Post a message to check the connection state in case we are unable to connect for 10 seconds
			sendEmptyMessageDelayed(CHECK_CONNECTION_STATE, delay);
		}		
		return retVal;
	}

	@Override
	public void stopAsync() {		
		// In case we are in the middle of starting up.
		if(mConnThread != null) {
			mConnThread.interrupt();
		}
		
		// Return false if we have no bluetooth device to connect to.
		if(mBluetoothDevice == null) {
			return;
		}
		removeMessage(CHECK_CONNECTION_STATE);
				
		/*
		 * mIsESPRunning is set to false inside of the uiHandler.handleMessage() implementation inside of the constructor for this class.
		 * This will make sure that the ESP data flows for as long as their is a bluetooth connection.
		 */
		
		/*
		 * Disconnect has to be called first or these errors arise.
		 * 05-19 10:42:38.424: A/libc(6751): @@@ ABORTING: INVALID HEAP ADDRESS IN dlfree
		 * 05-19 10:42:38.424: A/libc(6751): Fatal signal 11 (SIGSEGV) at 0xdeadbaad (code=1)
		 * 
		 * This is because BluetoothSocket.close() could be called from multiple threads. 
		*/
		
		disconnect(false);		
		
		// If the threads are running we want stop stop them.
		if (mReaderThread != null)
		{
			mReaderThread.setRun(false);
			mReaderThread.interrupt();
		}
		
		if (mWriterThread != null)
		{
			mWriterThread.setRun(false);
			mWriterThread.interrupt();
		}
	}

	/**
	 * Returns if the library is currently connected with a {@link BluetoothDevice}.	
	 * 
	 * @return	True if there is a current connection with a V1Connection otherwise, false.
	 */
	@Override
	public boolean isConnected() {
		mConnectedLock.lock();
		boolean isConnected = mIsConnected;
		mConnectedLock.unlock();
		return isConnected;
	}
	
	/**
	 * Method initialize the base class variables that are expected to be in a specific state before the library is started. 
	 */
	protected void prepForStartBase () {
		// Always allow writing when starting the ESP library
		mCanWrite = true;
		// Insure the running flag is false when starting a new connection attempt
		mESPRunningLock.lock();
		mIsESPRunning = false;		
		mESPRunningLock.unlock();
	}
	
	/**
	 * Sets the isESPRunning flag.
	 * 
	 * @param isRunning	If true, set isESPRunning flag to true otherwise, isESPRunning to false. 
	 */
	protected final void setIsESPRunning(boolean isRunning) {
		mESPRunningLock.lock();
		mIsESPRunning = isRunning;		
		mESPRunningLock.unlock();
	}
	
	
	/**
	 * Sets the isConnected flag based on given parameter.
	 * 
	 * @param isConnected	The value to set isConnected flag.
	 * 
	 * INTERNAL USE ONLY!!!
	 */
	protected final void setIsConnected(boolean isConnected) {
		mConnectedLock.lock();
		mIsConnected = isConnected;
		mConnectedLock.unlock();
	}
	
	/**
	 *	Sets a flag that controls whether the library can write the {@link BluetoothDevice}. 
	 */
	protected void setCanWrite(boolean canWrite) {
		// Lock the canWrite access to prevent others from reading it.	
		mWritelock.lock();
		mCanWrite = canWrite;
		mWritelock.unlock();
	}
	
	/**
	 * Returns whether writing to the {@link BluetoothDevice} is allowed.
	 * 
	 * @return True if the library is allowed to write the bluetooth device.
	 */
	protected boolean canWriteToV1() {
		// lock the canwirte access to prevent other from writing to it.
		boolean retVal = true;
		mWritelock.lock();
		// Save the value to a temporary variable that will actually be returned.
		retVal = mCanWrite;
		mWritelock.unlock();
		return retVal;
	}
	
	/**
	 * Get the current state of Legacy Mode protection provided by the ESP library. Refer to 
	 * setProtectLegacyMode for more information on this feature. 
	 * 
	 * @return The current state of Legacy Mode protection provided by the ESP library.
	 */
	public static boolean getProtectLegacyMode() {
		return m_protectLegacyMode;
	}
	
	/**
	 * Setter method for controlling whether the ESP library should protect legacy mode. 
	 * When set to true and the V1 is running in Legacy mode, the DataWriter thread will 
	 * not send any commands that are not compatible with Legacy Mode. The commands compatible 
	 * with Legacy Mode are V1 mute request, and V1connection Version request. When false, the 
	 * DataWriter thread will send all commands to all devices.  
	 * 
	 * This value is defaulted to false to make it compatible with older versions of this library.
	 *   
	 * @param protectMode Flag that controls if protect legacy is enabled.
	 */
	public static void setProtectLegacyMode(boolean protectMode) {
		m_protectLegacyMode = protectMode;
	}

	/**
	 * This method will queue a packet to be sent to the ESP bus via the Bluetooth connection.
	 * 
	 * @param packet - The packet to send.
	 */		
	@Override
	public void sendPacket(ESPPacket packet) {
		// The write thread is responsible for the actual packet write. We just need to add the packet 
		// to the queue here so the write thread can find it.		
		PacketQueue.pushOutputPacketOntoQueue(packet);		
	}
	
	/**
	 * When a error is received calling this method will stop the library e.g. the reader, writer, and processing thread 
	 * will be stopped.
	 */
	protected void handleThreadError () {
		if(mValentineESP != null) {
			mValentineESP.stopAsync();
		}
	}
	
	/**
	 * Initiates a Bluetooth scanning process for 'x' seconds.
	 * 
	 * @param vrScanCallback	A callback that will be triggered when {@link BluetoothDevice}s are discovered or 
	 * the scanning process stops.
	 * @param secondsToScan
	 */
	public void scanForDevices(VRScanCallback vrScanCallback, int secondsToScan) {
		synchronized (mLock) {
			// Stop any scanning events taking places
			stopScan();
			// Keep a reference to the scan callback, so we can pass updates on the scan process to the caller.
			mSecondsToScan = secondsToScan;
			mShouldNotify = true;
			// Tell the implementing class to actually scan for BluetoothDevices.
			startScan();		
			// Scan infinitely
			mScanCallback = vrScanCallback;
			if(secondsToScan != 0) {
				// Create handler that will be used to stop the scanning process.
				sendEmptyMessageDelayed(STOP_SCAN, (secondsToScan * 1000));
			}
		}
	}
	
	/**
	 * Cancels any currently running Bluetooth scans.
	 */
	public void stopScanningForDevices() {
		synchronized (mLock) {
			removeMessage(STOP_SCAN);		
			stopScan();
			mScanCallback = null;
			mShouldNotify = false;
		}
	}
	
	/**
	 * Returns if ESP is running.
	 * 
	 * @return	If ESP is running, returns true otherwise, false.
	 */
	protected boolean isESPRunning() {
		// Lock access to the is ESP running flag.
		boolean retVal;
		mESPRunningLock.lock();
		retVal = mIsESPRunning;
		mESPRunningLock.unlock();
		return retVal; 
	}	
	
	/**
	 * Returns a human friendly name for the connected V1Connection.
	 * 
	 * @return	If there is a connection with a V1Connection returns a human friendly name for the device otherwise, an empty string.
	 */
	public String getConnectedBTDeviceName() {
		return  mBluetoothDevice != null ? mBluetoothDevice.getName() : "No V1Connection";
	}
		
	/**
	 * Set the BluetoothDevice to connect to.
	 * 
	 * @param device	The {@link BluetoothDevice} to connect with.
	 */
	public void setDevice(BluetoothDevice device) {
		// If the passed in device is the same, return immediately.
		if(device.equals(mBluetoothDevice)) {
			return;
		}
		if(isConnected()) {
			// Disconnect the connection with the socket.
			disconnect(false);
		}
		// Perform the swap of the bluetooth device			
		mBluetoothDevice = device;		
	}
	
	/**
	 * Scans for {@link BluetoothDevices}.
	 * 
	 * Implementing class actually handles starting scanning for BluetoothDevices
	 */
	protected abstract void 			startScan();
	
	/**
	 * Stops scanning for {@link BluetoothDevices}.
	 * 
	 * Implementing class actually handles stopping {@link BluetoothDevices} scanning.
	 */
	protected abstract void 			stopScan();
	
	/**
	 * Connects the library with the given {@link BluetoothDevices} at runtime.
	 * 
	 * Implementation handles actual connection of the {@link BluetoothDevices}. 
	 * @throws InterruptedException 
	 *  
	 */
	protected abstract boolean				connect();
	
	/**
	 * Asynchronously disconnects the library from the currently connected {@link BluetoothDevices}.
	 * 
	 * Implementation handles actual disconnection of the {@link BluetoothDevices}.
	 * 
	 * @param waitToReturn Flag indicating whether the disconnect should wait until a disconnect notification 
	 * from the system before returning.
	 * 
	 * @return	True if the disconnect was successful.
	 */
	
	protected abstract boolean	 			disconnect(boolean waitToReturn);	
	/**
	 * Writes an {@link ESPPacket} to destination. Implementation responsible for transforming the packet to a meaningful format
	 * to be sent over a given connection type.	 
	 * 
	 * 
	 * @param packet	a valid {@link ESPPacket} that will be transformed and written to the implementations destination. 
	 * 
	 * @return	True if the packet written to the implementations desired destination otherwise, false.
	 */
	protected abstract boolean  		writePacket(ESPPacket packet);
	
	/**
	 * Stores {@link ESPPacket}s into an ArrayList.
	 * 
	 * Implementation handles writing the {@link ESPPacket}s into the arraylist.
	 * 
	 * @param packetList		Arraylist to store the {@link ESPPacket}s.
	 * @param lastKnownV1Type  	The last known V1 type.
	 * 
	 * @return	True if there were no problems processing the ESP data otherwise, false.
	 */
	protected abstract boolean 			getAvailPackets(ArrayList<ESPPacket> packetList);
	
	
	/**
	 * This method returns the {@link BluetoothSocket} of the current Bluetooth connection (IF SUPPORTED).
	 * 
	 * @return  {@link BluetoothSocket} of the current Bluetooth connection, if available otherwise, null.
	 * (Bluetooth LE connections do not using BluetoothSockets so null will always be returned)
	 * 
	 * @deprecated DO NOT USE. THIS METHOD WILL BE REMOVED IN THE NEXT API RELEASE.
	 */
	public abstract	BluetoothSocket 	getSocket();
	
	/**
	 * Method initialize the sub class variables that are expected to be in a specific state before the library is started. 
	 */
	protected abstract void prepForStart ();
	
	
	/**
	 * Performs the actual disconnect call. 
	 * 
	 * @hide
	 */
	protected abstract void mDisconnect();

	/**
	 * Starts the connection process for LE by first performing an LE scan.
	 * 
	 * @return	true, if the LE Scan was initiated.
	 * @hide
	 */
	protected abstract boolean mStartConnectProcess();

	/**
	 * Thread that handles writing data out to the connected {@link BluetoothDevice}.
	 * 
	 * Ignores {@link ESPPacket}s that the V1 is unable to process.
	 * 
	 * @author jdavis
	 */
	class DataWriterThread extends Thread 
	{
		private static final String LOG_TAG = "ValentineESP/DataWriterThread";
		
		/**
		 * Control looping of the run method.
		 */
		private volatile boolean m_run;	
		
		/**
		 * Sets the flag that controls the looping functionality of the run method.
		 * @param _run	True if the run method should run indefinitely, false to execute only once.
		 */
		public void setRun(boolean _run) {
			m_run = _run;
		}
		/**
		 * Sets up the data writer thread with a ValentineESP object, and an outputstream.
		 * 
		 * @param _esp			ValentineESP object to shut down various threads in the library if an exception occurs.
		 * @param _outStream	OutputStream to write to the V1Connection with.
		 */
		public DataWriterThread() {
			m_run = true;
		}
		
		/**
		 * Executes indefinitely attempting to write  {@link ESPPacket}s in the packetqueue to the open outputstream with the V1Connection.
		 */
		public void run() {
			
			int maxPendingEchoes = 4;
			
			// Clear the echo queue when starting the write thread
			echoLock.lock();
			expectedEchoPackets.clear();
			echoLock.unlock();
			
			while (m_run) {
				// Check to see if the writer thread is able to write to the bluetooth connection.
				if(canWriteToV1()) {
					try {
						ESPPacket packet = PacketQueue.getNextOutputPacket();
						
						if ( packet != null && !m_allowSendingPacket (packet) ){
							// Don't send this packet
							packet = null;
						}
						
						if (packet == null)
						{
							sleep(100);
						}
						else
						{	
							if (PacketQueue.isPacketIdInBusyList(packet.getPacketIdentifier()))
							{
								PacketQueue.pushOutputPacketOntoQueue(packet);
								sleep(100);
							}
							else
							{
								// Wait while the echo wait queue is full to avoid the request not processed from the V1connection due to 
								// a full request buffer in the hardware.
								echoLock.lock();									
								int pendingEchoCnt = expectedEchoPackets.size();
								echoLock.unlock();									
								while ( pendingEchoCnt >= maxPendingEchoes ){
									Thread.sleep (5);
									echoLock.lock();
									pendingEchoCnt = expectedEchoPackets.size();
									echoLock.unlock();
								}
								
								// Store the last packet of this type in the packet queue for handling busy and not processed responses
								PacketQueue.putLastWrittenPacketOfType(packet);

								// Write ESPPacket to the V1 connection; bluetooth connection type agnostic. 
								boolean retVal = writePacket(packet);
								
								// If writeBuffer returned false that means we encountered an error and we need to notify the ValentineESP object.
								if(!retVal) {
									if(ESPLibraryLogController.LOG_WRITE_ERROR){
										Log.e(LOG_TAG, "Failed to write ESPPacket to " + packet.getDestination() + ".");
									}
									VR_BluetoothWrapper.this.handleThreadError();
								}
								else{
									// Add the packet to the echo queue
									mAddPacketToEchoQueue(packet);
								}
							}
		
						}
					} 
					catch (InterruptedException e) 
					{						
						// No need to set run to false. The thread has been interrupted and is already stopping.
						if(m_run) {							
							if(ESPLibraryLogController.LOG_WRITE_ERROR){
								Log.e(LOG_TAG, "DataWriterThread Interrupted. Shutting down esp...", e);
							}							
							VR_BluetoothWrapper.this.handleThreadError();
						}
					}
				}				
			}
			if(ESPLibraryLogController.LOG_WRITE_INFO) {
				Log.i(LOG_TAG, "DataWriterThread is stopping...");
			}
		}
				
		/**
		 * This method will add a packet to the queue of packets for which we are expecting an echo.
		 * 
		 * @param sentPacket - The packet to be added to the queue
		 */
		private void mAddPacketToEchoQueue (ESPPacket sentPacket)
		{			
			if ( sentPacket.getPacketIdentifier() == PacketId.reqMuteOn ){
				// This is a special case packet that does not get echoed back from the V1connection LE. This packet does not take
				// up space in the V1connection/LE buffer, so we don't need to put it into the echo wait queue.
				return;		
			}
			
			// Add the packet that was sent to the echo wait queue.
			echoLock.lock();	
			expectedEchoPackets.add(Pair.create(SystemClock.elapsedRealtime(), sentPacket));
			echoLock.unlock();
		}
		
		/**
		 * Determines if the {@link ESPPacket} passed in is allowed to be sent
		 *
		 * @param packet - The  {@link ESPPacket} to check.
		 * 
		 * @return True if the we are allowed to send the {@link ESPPacket} otherwise, false.
		 */
		private boolean m_allowSendingPacket (ESPPacket packet)
		{
			if ( PacketQueue.getV1Type() == Devices.VALENTINE1_LEGACY && m_protectLegacyMode ){
				// Check to see if the packet should be prohibited by Legacy Mode rules
				PacketId id = packet.getPacketIdentifier();
				if ( id == PacketId.reqVersion ){
					byte dest = (byte)(packet.getDestination().toByteValue() & 0x0F);
					byte origin = (byte)(packet.getOrigin().toByteValue() & 0x0F);
					
					// The V1connection is the only device that can accept a version query in Legacy Mode
					// Refer to the Bluetooth addendum for a discussion of why the origin should match the 
					// destination for the V1connection.
					if ( dest != origin ){
						// Do not allow version requests that are not to the V1connection
						if(ESPLibraryLogController.LOG_WRITE_INFO){
							Log.i(LOG_TAG, "Ignoring version request to " + packet.getDestination().toString() + " because the app is in Legacy mode.");
						}
						return false;
					}
				}			
				else if ( id == PacketId.reqMuteOn ){
					// The V1 can accept a mute request
					byte dest = (byte)(packet.getDestination().toByteValue() & 0x0F);
					byte v1Type = (byte)(packet.getV1Type().toByteValue() & 0x0F);
					if ( dest == v1Type ){
						// The request is not for the V1
						if(ESPLibraryLogController.LOG_WRITE_INFO){
							Log.i(LOG_TAG, "Ignoring mute on request to " + packet.getDestination().toString() + " because the app is in Legacy mode.");
						}
						return false;
					}
				}
				else{
					// There are no other requests allowed in Legacy Mode
					if(ESPLibraryLogController.LOG_WRITE_INFO){
						Log.i(LOG_TAG, "Ignoring " + id.toString() + " request to " + packet.getDestination().toString() + " because the app is in Legacy mode.");
					}
					return false;
				}
			}
			
			// No rules prohibited sending it, so allow it to be sent
			return true;
		}
	}
	
	/**
	 * Thread that handles reading the {@link ESPPacket}s received from the connected {@link BluetoothDevice}.
	 * 
	 * @author jdavis
	 *
	 */
	class DataReaderThread extends Thread 
	{
		private static final String 	LOG_TAG         = "DataReaderThread LOG";
		private int 					m_dispCount;
		private volatile boolean 		m_run;

		/* This is the expected sequence of m_dispCount
		 * Event															Result
		 * infV1Busy packet received										m_dispCount reset to 0
		 * infDisplayData received WITH a preceding invV1Busy packet		m_dispCount incremented to 1
		 * infDisplayData received WITHOUT a preceding invV1Busy packet		m_dispCount incremented
		 */
		private static final int V1_BUSY_RESET_VAL = 0;
		private static final int V1_NOT_BUSY_THRESH = 2;
		private static final int BUSY_INCREMENT_THRESH = V1_NOT_BUSY_THRESH + 1; // Increment to 1 past the trigger point but don't
		
		/**
		 * Sets the flag that controls the looping functionality of the run method.
		 * @param _run	True if the run method should run indefinitely, false to execute only once.
		 */
		public void setRun(boolean _run) {
			m_run = _run;
		}
		
		/**
		 * Sets up the reader thread with the ValentineESP object, an inputstream and a timeout.
		 * 
		 * @param _esp				A ValentineESP object to to notify the library of the data read from the inputstream (V1/V1connection)
		 *  or shut down the library if an exception occurs.. 
		 * @param _inStream			InputStream with the bluetooth connection. Stream data from the V1connection.
		 * @param secondsToWait		A time to wait before notify the library the no data has been received.
		 */
		public DataReaderThread(int secondsToWait) {
			m_run = true;
			m_dispCount = 0;
		}
		
		/**
		 * Execute indefinitely reading data from the open inputstream with V1Connection.
		 */
		public void run() {
			// Reset the display count before starting and clear the busy packets in the packet queue
			m_dispCount = V1_BUSY_RESET_VAL;
			PacketQueue.setBusyPacketIds(null);
			PacketQueue.clearSendAfterBusyQueue();
			// An array list that holds the packets received from the Bluetooth connection.
			ArrayList<ESPPacket> packets = new ArrayList<ESPPacket>();
			
			while (m_run) {
				try {					
					// Get a list of new ESP packets, Bluetooth connection agnostic.
					boolean result = getAvailPackets(packets);
					// If getAvailPackets returned false that means we encountered an error and we need to notify the ValentineESP object.
					if(!result) {
						if(ESPLibraryLogController.LOG_WRITE_ERROR){
							Log.d(LOG_TAG, "Failed to read ESPPackets from the V1connection");
						}
						VR_BluetoothWrapper.this.handleThreadError();
						continue;
					}
					if (packets.size() == 0 ) {
						// Sleep if no data available
						m_emptyReadCount++;
						sleep(EMPTY_READ_SLEEP_TIME);
						
						if (m_emptyReadCount == MAX_EMPTY_READS) {
							if (!m_notifiedNoData){
								m_notifiedNoData = true;
								mValentineESP.notifyNoData();
							}
						}
						// Purge expired echoes
						mPurgeOldEchoWaits ();
					}
					else {
						m_notifiedNoData = false;
						// Clear the empty count whenever we get data
						m_emptyReadCount = 0;
						
						for (ESPPacket newPacket : packets){
							if ( newPacket == null ){
								// Go get the next packet
								continue;
							}
							// Update the lastKnownV1Type.
							mlastKnownV1Type = newPacket.getV1Type();
							// Check for an echo
							mCheckForEcho(newPacket);							
							
							if (isPacketForMe(newPacket)){
								if ( ESPLibraryLogController.LOG_WRITE_INFO && ESPLibraryLogController.LOG_WRITE_VERBOSE ) {
									// Log the data packet if it is not a display or alert data packet.
									if ((newPacket.getPacketIdentifier() != PacketId.infDisplayData) && (newPacket.getPacketIdentifier() != PacketId.respAlertData)){
										mLogNonDispNonAlertPacket(newPacket);
									}
								}

								if (newPacket.getPacketIdentifier() == PacketId.infV1Busy)
								{
									// Clear the counter when we get a busy packet and tell the packet queue what the V1 is busy working on.
									m_dispCount = V1_BUSY_RESET_VAL;
									PacketQueue.setBusyPacketIds(newPacket);
								}
								else if (newPacket.getPacketIdentifier() == PacketId.respRequestNotProcessed)
								{
									if ( ESPLibraryLogController.LOG_WRITE_INFO ){
										Log.e (LOG_TAG, "Received request not processed response from " + newPacket.getOrigin().toString());
									}
									// The hardware could not process this requee. We are going to assume it is because the hardware is busy
									// so we will requeue the packet to be sent out again
									Integer idInt = (Integer)newPacket.getResponseData();
									byte packetId = idInt.byteValue(); // (Byte)newPacket.getResponseData();
									ESPPacket packet = PacketQueue.getLastWrittenPacketOfType(PacketIdLookup.getConstant(packetId));
									if (packet != null && !packet.getResentFlag())
									{
										if(ESPLibraryLogController.LOG_WRITE_INFO){
											Log.i(LOG_TAG, "Requeuing packet of type " + PacketIdLookup.getConstant(packetId).toString() ); 
										}
										packet.setResentFlag(true);
										
										if ( m_dispCount < V1_NOT_BUSY_THRESH ){
											// Send the try resending the packet after the V1 is not busy
											PacketQueue.pushOnToSendAfterBusyQueue(packet);
										}
										else{
											// The V1 is not busy now, so send the packet to the V1 now
											PacketQueue.pushOutputPacketOntoQueue(packet);
										}
									}
									else
									{
										if(ESPLibraryLogController.LOG_WRITE_INFO){
											Log.i(LOG_TAG, "Aborting resend of packet of type " + PacketIdLookup.getConstant(packetId).toString() );												
										}
										PacketQueue.pushInputPacketOntoQueue(newPacket);
									}
								}
								else if (newPacket.getPacketIdentifier() == PacketId.infDisplayData)
								{
									// See the table above to determine why the values 2 and 3 were chosen for this task
									if ( m_dispCount < BUSY_INCREMENT_THRESH){
										// Increment to 1 past the trigger point but don't keep incrementing because we don't want to wrap
										// the counter.
										m_dispCount++;
									}
									
									if ( m_dispCount == V1_NOT_BUSY_THRESH){
										// We hit the trigger point to decide that the V1 is not busy. Transfer packets that were not processsed
										// to the output queue so we can try again.
										PacketQueue.setBusyPacketIds(null);
										PacketQueue.sendAfterBusyQueue();
									}

									// Always send display packets to the app.
									PacketQueue.pushInputPacketOntoQueue(newPacket);
								}

								else
								{
									// This is not a special case, so send this packet to the app
									PacketQueue.pushInputPacketOntoQueue(newPacket);
								}
							}												
						} // for (ESPPacket newPacket : packets)
					}
				}				
				catch (InterruptedException e) 
				{
					// No need to set run to false. The thread has been interrupted and is already stopping.
					if(m_run) {						
						if(ESPLibraryLogController.LOG_WRITE_ERROR){
							Log.e(LOG_TAG, "DataReader Thread Interrupted, shutting down esp...", e);
							e.printStackTrace();
						};						
						VR_BluetoothWrapper.this.handleThreadError();
					}
				} 
			}
			if(ESPLibraryLogController.LOG_WRITE_INFO) {
				Log.i(LOG_TAG, "DataReaderThread is stopping...");
			}
		}
		
		/**
		 * This method will remove all expired packets from the queue of expected echo packets. 
		 */
		private void mPurgeOldEchoWaits ()
		{
			// Keep echoes for 1 second, then assume 
			long millisToKeep = 1000;
			long minKeepTime = SystemClock.elapsedRealtime() - millisToKeep;
			int purgeCnt = 0;
			ArrayList<Pair<Long,ESPPacket>> modifiedPackets = null;
			
			echoLock.lock();
			
			Iterator<Pair<Long,ESPPacket>>it = expectedEchoPackets.iterator();
			
			while ( it.hasNext() ){
				Pair<Long,ESPPacket> curPacketPair = it.next();
				if ( curPacketPair.first < minKeepTime ){
					if ( PacketQueue.getHoldoffOutput() ){
						// Don't throw away the echoes if the V1 is not allowing transmission right now. Instead, update the transmission time to now
						if ( modifiedPackets == null ){
							modifiedPackets = new ArrayList<Pair<Long,ESPPacket>>();
						}
						modifiedPackets.add(Pair.create(SystemClock.elapsedRealtime(), curPacketPair.second));
					}
					
					if ( ESPLibraryLogController.LOG_WRITE_ERROR ){				
						Log.e (LOG_TAG, "Purging expired " + curPacketPair.second.getPacketIdentifier().toString() + " packet that was destined for " + curPacketPair.second.getDestination().toString()); 
					}
					
					// Purge this echo packet because it is more than 1 second old. This most likely happened because there is either a problem 
					// with the hardware or we have lost the Bluetooth connection.
					it.remove();
					purgeCnt ++;
				}
			}
			
			if ( ESPLibraryLogController.LOG_WRITE_ERROR && purgeCnt > 0 ){				
				Log.e (LOG_TAG, "Purged " + purgeCnt + " expired echo wait packet(s). Queue size = " + expectedEchoPackets.size());
			}
			
			if ( modifiedPackets != null ){
				// Add the packets that have been modified due to the V1 time slice holdoff
				expectedEchoPackets.addAll(modifiedPackets);

				if ( ESPLibraryLogController.LOG_WRITE_ECHO_INFO || ESPLibraryLogController.LOG_WRITE_ERROR ){
					Log.d (LOG_TAG, "Added " + modifiedPackets.size() + " expired packet(s) back to queue because time slices are being held off. Queue size = " + expectedEchoPackets.size());
				}
			}
			echoLock.unlock();
			
		}
		
		/**
		 * This method will check a received packet to see if it is an echo of a sent packet. If the received packet is an echo, the packet
		 * will be removed from the queue of expected echo packets.
		 * 
		 * @param newPacket - The received packet to check for.
		 */
		private void mCheckForEcho (ESPPacket newPacket)
		{
			echoLock.lock();			
			boolean removePacket = false;
			
			// NOTE: This loop will remove at most 1 packet from the queue.
			for ( int i = 0; i < expectedEchoPackets.size(); i++ ){
				
				Pair<Long,ESPPacket> packetPair = expectedEchoPackets.get(i);
				ESPPacket testPacket = packetPair.second;				
				if (testPacket == null ){
					if ( ESPLibraryLogController.LOG_WRITE_ECHO_INFO ){
						Log.e (LOG_TAG, "Huh? There should not be a null here");
					}
					removePacket = true;
				}
				else{
					if ( newPacket.getPacketIdentifier() == PacketId.respRequestNotProcessed ){
						// Remove the not processed packet from the echo wait queue
						byte packetId = ((Integer)newPacket.getResponseData()).byteValue();
						if ( testPacket.getPacketIdentifier() == PacketIdLookup.getConstant(packetId) ){
							// If we receive a request not processed for the packet we are waiting for, treat is like an echo
							if ( ESPLibraryLogController.LOG_WRITE_ECHO_INFO ){
								Log.d (LOG_TAG, "Handling request not processed as an echo");
							}
							removePacket = true;
						}
						
					}
					else{
						// Handle a normal response
						if ( newPacket.isSamePacket(testPacket) ){
							if ( ESPLibraryLogController.LOG_WRITE_ECHO_INFO ){
								Log.i (LOG_TAG, "Removing echo packet #" + i);
							}
							removePacket = true;					
						}
						else{
							// The V1connection/LE hardware only echoes packets that are placed on the ESP hardware bus. It will not echo packets
							// that are destined for the V1connection/LE hardware itself. Therefore, we should look for the response to those 'special'
							// packets and remove the echo when the response is found
							if ( testPacket.getDestination() == testPacket.getOrigin() ){
								// The expected echo we are working with is destined for the V1connection/LE and not the ESP hardware bus
								if ( newPacket.getDestination() == newPacket.getOrigin() ){
								// The received packet is from the V1connection/LE and it was never placed on the ESP hardware bus
									if ( newPacket.getPacketIdentifier() == PacketId.respVersion && 
										 testPacket.getPacketIdentifier() == PacketId.reqVersion ){
									// The newPacket passed in is the response to the V1connection/LE version request
										removePacket = true;
									}	
								}
							}
							
						}
					}
				}
				
				if ( removePacket ){
					// We want to remove this packet from the queue.
					expectedEchoPackets.remove(i);					
					if ( ESPLibraryLogController.LOG_WRITE_ECHO_INFO ){
						Log.i (LOG_TAG, "After removal, queue size = " + expectedEchoPackets.size() );
					}
					// Stop looping when the echo has been found
					break;
				}
			}
			echoLock.unlock();
		}

		/**
		 * Prints out a log statement containing information about the supplied {@link ESPPacket}.
		 * 
		 * @param newPacket	An ESPPacket containing data that will be printed out in a log statement.
		 */
		private void mLogNonDispNonAlertPacket(ESPPacket newPacket) {
			if (newPacket != null && newPacket.getPacketIdentifier().toByteValue() == PacketId.infV1Busy.toByteValue() ){
				try {
					ArrayList<Integer> packetList = (ArrayList<Integer>)(newPacket.getResponseData());
					int busyCnt = packetList.size();
					if ( newPacket.getV1Type() == Devices.VALENTINE1_WITH_CHECKSUM ){
						// The response data includes the check sum, which we don't care about
						busyCnt --;
					}
					String logStr = "Received infV1Busy packet: ";
					for ( int i = 0; i < busyCnt; i++ ){
						if ( i != 0 ){
							logStr += ",";
						}
						PacketId id = PacketIdLookup.getConstant( packetList.get(i).byteValue());
						logStr += id.toString();
						
					}
					Log.i(LOG_TAG, logStr);
				}
				catch(ClassCastException ex) {
					if(ESPLibraryLogController.LOG_WRITE_ERROR){
						Log.e(LOG_TAG, String.format("Invalid Packet Data encountered: %s", newPacket), ex);
					}
				}
			}
		}
		
		/**
		 * Determines if the received {@link ESPPacket} is for us or not.
		 *  
		 * @param newPacket		The packet to check the destination of.
		 * 
		 * @return	True if the packet is a for the V1Connection or a General Broadcast otherwise, false.
		 */
		private boolean isPacketForMe(ESPPacket newPacket)
		{
			if ((newPacket.getDestination() == Devices.V1CONNECT) || (newPacket.getDestination() == Devices.GENERAL_BROADCAST))
			{
				return true;
			}
			
			return false;
		}
	}	
}
