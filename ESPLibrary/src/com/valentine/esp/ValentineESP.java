/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.valentine.esp.bluetooth.BluetoothDeviceBundle;
import com.valentine.esp.bluetooth.ConnectionType;
import com.valentine.esp.bluetooth.VRScanCallback;
import com.valentine.esp.bluetooth.VR_BluetoothLEWrapper;
import com.valentine.esp.bluetooth.VR_BluetoothSPPWrapper;
import com.valentine.esp.bluetooth.VR_BluetoothWrapper;
import com.valentine.esp.constants.Devices;
import com.valentine.esp.constants.ESPLibraryLogController;
import com.valentine.esp.constants.PacketId;
import com.valentine.esp.demo.DemoData;
import com.valentine.esp.packets.ESPPacket;
import com.valentine.esp.utilities.Utilities;

/** This is the underlying class that connects to and processes packets from the Valentine One.
 * This is wrapped by the ValentineClient class to extract out the data from the packets so the programmer
 * does not not need to handle packets in any fashion.  Nothing here should be called directly, but instead through the 
 * ValentineClient class.
 * 
 */
public class ValentineESP {
	
	private static final String 	LOG_TAG = "ValentineESP LOG";
	
	private Map<PacketId, ArrayList<CallbackData>> m_callbackData = new HashMap<PacketId, ArrayList<CallbackData>>();
	
	private Object 					m_stopObject;
	private String 					m_stopFunction;
	
	private boolean 				m_notified;	
	private boolean 				m_inDemoMode;
	
	private DemoData 				m_demoData;
	
	// Callback object and function references.
	private Object 					m_noDataObject;
	private String 					m_noDataFunction;
	private Object 					m_unsupportedObject;
	private String 					m_unsupportedFunction;
	

	private Context 				mContext;
	private ProcessingThread 		m_processingThread;
	private VR_BluetoothWrapper 	mVrBluetoothWrapper;
	private ProcessDemoFileThread 	m_demoFileThread;
	private BluetoothDevice 		mBluetoothDevice;
	
	private Object							  m_ConnectionCallbackObject;
	private String							  m_ConnectionCallbackName;
	
	private Object							  m_DisconnectionCallbackObject;
	private String							  m_DisconnectionCallbackName;
	
	
	/**
	 * Time out to be passed into the DataReaderThread that will be used to notify the library of the no data.
	 * Passed in using seconds.
	 *
	 */
	private final int 				m_secondsToWait;
	
	/**
	 * Object to hold the items needed to register a callback
	 */
	private class TempCallbackInfo
	{
		public PacketId type;
		public Object callBackObject;
		public String method;	
	}
	
	// The m_packetCallbackLock protects all of the following class members. 
	private ReentrantLock						m_packetCallbackLock = new ReentrantLock();
	private PacketId							m_lockedPacket = PacketId.unknownPacketType;;
	private ArrayList<DeregCallbackInfo>		m_packetsToDeregister = new ArrayList<DeregCallbackInfo>();
	private ArrayList<TempCallbackInfo> 		m_packetsToRegister = new ArrayList<TempCallbackInfo>();
	private boolean								m_clearAllCallbacksOnUnlock = false;
	
	private VR_BluetoothWrapper 				scanner;
	private VRScanCallback 						activeCallback;
	
	/**
	 * Local reference to the current {@link ConnectionType}.
	 */
	private ConnectionType 						mConnectionType = ConnectionType.UNKNOWN;

	/**
	 * Indicates if the device supports Bluetooth LE (Low Energy).
	 */
	private boolean 							mBLESupport = false;	
	
	/**
	 * A wrapper {@link VRScanCallback} that dispatches discovery events callbacks outside of the library.
	 */
	private VRScanCallback vrScanCallback = new VRScanCallback() {

		@Override
		public void onDeviceFound(BluetoothDeviceBundle bluetoothDeviceBundle, ConnectionType connectionType) {
			if(activeCallback != null) {
				activeCallback.onDeviceFound(bluetoothDeviceBundle, connectionType);
			}			
		}

		@Override
		public void onScanComplete(ConnectionType connectionType) {
			scanner = null;
			if(activeCallback != null) {
				activeCallback.onScanComplete(connectionType);
			}
		}		
	};	
	
	public ValentineESP(int secondsToWait, Context context)
	{
		mContext = context;
		m_notified = true;
		m_inDemoMode = false;
		m_demoData = new DemoData();
		m_secondsToWait = secondsToWait;
		// Determine Bluetooth LE supports.
		mBLESupport = ValentineClient.checkBluetoothLESupport(context);	
	}
	
	/**
	 * Set the delay to wait before the data reader thread will notify that the
	 * no data has been received.
	 * 
	 * @param delay	Number of seconds to wait before notifying that no data has been received.
	 */
	protected void setNoDataReceivedDelay(int delay) {
		if(mVrBluetoothWrapper != null){
			mVrBluetoothWrapper.setNoDataReceivedDelay(delay);
		}
	}
	
	/**
	 * Return whether the current device supports Bluetooth LE (Low Energy)
	 * 
	 * @return	True if the device support Bluetooth LE (Low Energy), false otherwise.
	 */
	protected boolean isBluetoothLESupported() {
		return mBLESupport;
	}	
	
	/**
	 * Stops the Bluetooth discovery process, if any.
	 * There is no way to resume a scan; a new scan process must be initiated by calling
	 * {@link #scanForDevices(ConnectionType, VRScanCallback, int)}.
	 * 
	 * This method is safe to call multiple times.
	 * 
	 */
	protected void stopScanningForDevice() {
		if(scanner != null) {
			scanner.stopScanningForDevices();			
			// Once we stop the scanning process, we must set the scanner to
			// null in order to start another scanning process.
			scanner = null;
		}		
	}
	
	/**
	 * Starts bluetooth scan based off the ConnectionType.
	 * 
	 * Should not be called multiple times.
	 * 
	 * @param connectionType	The connectionType for the type of bluetooth scan performed. Valid connection types is LE, and SPP.
	 * @param scanCallback		The callback to triggered when a device is found and when scanning completes.
	 * @param secondsToScan		The time used to perform a bluetooth scan.
	 * 
	 * @return	True if the bluetooth discovery process was started.
	 */
	protected boolean scanForDevices(ConnectionType connectionType, VRScanCallback scanCallback, int secondsToScan) {
		// If Bluetooth LE is not supported and connectionType is V1Connection_LE, return false.
		if(ConnectionType.V1Connection_LE.equals(connectionType) && !mBLESupport) {
			Log.e(LOG_TAG, "Unable to scan for devices because ConnectionType = " + connectionType.toString() + " is not supported on this device.");
			return false;
		}
		
		if(scanner != null) {
			scanner.stopScanningForDevices();
			return false;
		}		
		activeCallback = scanCallback;
		
		// Create a new VR_BluetoothWrapper object to perform the Bluetooth scan.
		switch(connectionType) {
			case V1Connection_LE:
				scanner = new VR_BluetoothLEWrapper(this, null, 0, mContext);				
				break;
			case V1Connection:
				scanner = new VR_BluetoothSPPWrapper(this, null, 0, mContext);
				break;
			case UNKNOWN:
				break;		
		}
		
		// If the VR_BluetoothWrapper object is not null start scanning.
		if(scanner != null) {
			scanner.scanForDevices(vrScanCallback, secondsToScan);
			return true;
		}
		
		return false;
	}		
	
	/**
	 * This method will show all callbacks in LogCat.
	 * 
	 * @precondition The caller has m_packetCallbackLock locked.
	 * 
	 */
	/*
	private void m_showAllCallbacks ()
	{
		if ( ESPLibraryLogController.LOG_WRITE_VERBOSE ){
			Log.v("CallbackList","Current ValentineESP callbacks (" + this + ")");
		
			for (Map.Entry<PacketId, ArrayList<CallbackData>> entry : m_callbackData.entrySet())
			{
			    for ( int i = 0; i < entry.getValue().size(); i++ ){
			    	Log.v("CallbackList", "   " + entry.getKey().toString() + ":" + entry.getValue().get(i).callBackOwner.toString());
			    }
			}
		}
	}
	*/
	
	/**
	 * This method will set the locked packet type for this object. A locked Packet can not have any new
	 * callbacks registered or any current callbacks deregistered. This is to prevent modifying
	 * the arraylist while it is being traversed.
	 *  
	 * @param lockedType The packet type to lock
	 */
	protected void setLockedPacket (PacketId lockedType)
	{
		m_packetCallbackLock.lock();
		m_lockedPacket = lockedType;
		
		if ( lockedType.toByteValue() == PacketId.unknownPacketType.toByteValue() ){
			if ( m_clearAllCallbacksOnUnlock ){
				// A request to clear all callbacks was received while we had a locked packet
				if ( ESPLibraryLogController.LOG_WRITE_VERBOSE ){
		    		Log.v(LOG_TAG, "Clearing " + m_callbackData.size() + " callbacks after waiting for unlock");
				}
				
				m_callbackData.clear();
				m_clearAllCallbacksOnUnlock = false;
			}
			
			// Note that we want to process the pending registrations and deregistrations even if m_clearAllCallbacksOnUnlock is set. We are doing that
			// just in case someone performed one of those actions after calling clearAllCallbacks.
			
			// If the packet passed in is for an unknown packet type, we are free to handle the packets that
			// are queued up for registration and deregistration.
			for ( int i = 0; i < m_packetsToDeregister.size(); i++ ){
				DeregCallbackInfo curInfo = m_packetsToDeregister.get(i);
				// Call the private method because we have the queue locked
				m_deregisterForPacket(curInfo.type, curInfo.callBackOwner, curInfo.method);
			}			
			m_packetsToDeregister.clear();
			
			for ( int i = 0; i < m_packetsToRegister.size(); i++ ){
				TempCallbackInfo temp = m_packetsToRegister.get(i);
				// Call the private method because we have the queue locked
				m_registerForPacket(temp.type, temp.callBackObject, temp.method);
			}			
			m_packetsToRegister.clear();
		}
		m_packetCallbackLock.unlock();
	}

	/**
	 * This method will allow the caller to determine if the ESP library is operating in demo mode or not.
	 * 
	 * @return	Returns true if the library is running in demo mode otherwise, false.
	 */
	public boolean isInDemoMode()
	{
		return m_inDemoMode;
	}
	
	/**
	 * Returns if the library has an active Bluetooth connection.
	 * 
	 * @return True if there is an active connection with a V1Connection otherwise, false.
	 */
	public boolean getIsConnected() {
		// If VR_BluetoothWrapper is null return false because if it has not be instantiated
		// we know for a fact we are not connected to a bluetooth device.
		return mVrBluetoothWrapper != null ? mVrBluetoothWrapper.isConnected() : false;
	}
	
	/**
	 * This method will set up a callback to use when we determine that the running device may not support SPP. 
	 * 
	 * @param _obj The object that will receive the unsupported callback.
	 * @param _function The function to be called,
	 */
	public void setUnsupportedCallbacks(Object _obj, String _function)
	{
		m_unsupportedObject = _obj;
		m_unsupportedFunction = _function;
	}
	
	/**
	 * Sets the callback data for when the demo data processing finds a User Settings packet in it. 
	 * Requires a function with a UserSettings parameter:  public void function( UserSettings _parameter)
	 * 
	 * @param _owner Object to be notified when there is a demo data user settings object in the demo data
	 * @param _function Function to be called
	 */
	public void setDemoConfigurationCallbackData(Object _owner, String _function)
	{
		m_demoData.setConfigCallbackData(_owner, _function);
	}
	
	/**
	 * Synchronously connects with deviceToConnect. 
	 * This method is synchronous and WILL NOT return until a connection has failed or succeeded.
	 * 
	 * (DO NOT CALL FROM UI THREAD)
	 * 
	 * @param deviceToConnect	A V1Connection you want to make a connection with.
	  
	 * 
	 * @throws 			RuntimeException if called from the UI (Main) Thread.
	 * @return 			True if a connection was made otherwise returns false.
	 * 
	 * @deprecated DO NOT USE. THIS METHOD WILL BE REMOVED IN THE NEXT API RELEASE.
	 */
	public boolean startUpSync(BluetoothDevice deviceToConnect) {
		if(Thread.currentThread() == Looper.getMainLooper().getThread()){
			throw new RuntimeException("startUpSync() will/may block and should not be called on the " + Thread.currentThread().getName() + " thread.");
		}
		// If the ConnectionType or Device to connect are null, return false.
		if(deviceToConnect == null) {
			return false;
		}
		
		if (m_inDemoMode) {
			stopDemo( false);
		}
		
		// Check to see if our VRBluetoothWrapper object is null. If so, create a new object.		
		if(mVrBluetoothWrapper == null) {
			mConnectionType = ConnectionType.V1Connection;
			mVrBluetoothWrapper = new VR_BluetoothSPPWrapper(this, deviceToConnect, m_secondsToWait, mContext);
		}
		
		// Only create a new VR_BluetoothWrapper if the ConnectionType is different than the last.
		if(!deviceToConnect.equals(mBluetoothDevice)) {
			mBluetoothDevice = deviceToConnect;
			mVrBluetoothWrapper.setDevice(deviceToConnect);
		}				
		
		if(mVrBluetoothWrapper != null) {
			// Set the connectionType for the remainder of all packets.
			ESPPacket.setConnectionType(ConnectionType.V1Connection);
			if (mVrBluetoothWrapper.startSync()) {
				if(m_processingThread == null) {
					m_processingThread = new ProcessingThread();
					m_processingThread.start();
				}
				m_notified = false;
				return true;
			}
		}
		return false;
	}
	
	/**
	 * A callback method that gets triggered when a connection event completes.
	 * 
	 * @param isConnected true if the connection event results in a connection, false otherwise.
	 */
	public void onConnectEvent(boolean isConnected) {
		if(m_ConnectionCallbackObject != null && m_ConnectionCallbackName != null && !m_ConnectionCallbackName.isEmpty()) {
			try {
				// Pass in true to indicate there is there is a connection.
				Utilities.doCallback(m_ConnectionCallbackObject, m_ConnectionCallbackName, Boolean.class, isConnected);
			} 
			catch(Exception e) {
				if(ESPLibraryLogController.LOG_WRITE_WARNING ){
					Log.w(LOG_TAG, m_ConnectionCallbackObject.toString() + " " + m_ConnectionCallbackName);
				}
			}
		}
	}
	
	/**
	 * A callback method that gets triggered when a disconnection event completes.
	 */
	public void onDisconnected() {
		if(m_DisconnectionCallbackObject != null && m_DisconnectionCallbackName != null && !m_DisconnectionCallbackName.isEmpty()) {
			try {
				// Pass in false to indicate there is there is no connection.
				Utilities.doCallback(m_DisconnectionCallbackObject, m_DisconnectionCallbackName, null, null);
			} 
			catch(Exception e) {
				if(ESPLibraryLogController.LOG_WRITE_WARNING ){
					Log.w(LOG_TAG, m_ConnectionCallbackObject.toString() + " " + m_ConnectionCallbackName);
				}
			}
		}		
	}
	
	/**
	 * A callback method that gets triggered when a connection loss occurs.
	 */
	public void onConnectionLoss() {
		// INTENTIONALLY LEFT BLANK
	}

	/**
	 * Asynchronously connects to deviceToConnect using the specified connectionType. 
	 * This method is asynchronous and will return the result of the connection attempt to the calling objects callback method.
	 * 
	 * (Safe to call from the UI thread)
	 * 
	 * @param deviceToConnect	A V1Connection you want to make a connection to.
	 * @param connectionType	The ConnectionType to use when connecting with the V1Connection. Valid values are: {@link ConnectionType.V1Connection}, {@link ConnectionType.V1Connection_LE}
	 * 
	 * @return	True if a connection process has been initiated, false otherwise.
	 */
	public int startUpAsync(BluetoothDevice deviceToConnect, ConnectionType connectionType) {
		// If Bluetooth LE is not supported and connectionType is V1Connection_LE, return false.
		if(!mBLESupport && ConnectionType.V1Connection_LE.equals(connectionType)) {
			Log.e(LOG_TAG, "Cannot start a bluetooth connection because ConnectionType = " + connectionType.toString() + " is not supported on this device.");
			return ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_LE_NOT_SUPPORTED;
		}
		
		// If the ConnectionType or Device to connect are null, return false.
		if(deviceToConnect == null || connectionType == null) {
			return ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_NO_DEVICE;
		}
		
		if (m_inDemoMode)
		{
			stopDemo( false);
		}
		
		if(!connectionType.equals(mConnectionType)) {
			switch(connectionType) {
				case V1Connection_LE:
					mVrBluetoothWrapper = new VR_BluetoothLEWrapper(this, deviceToConnect, m_secondsToWait, mContext);				
					break;
				case V1Connection:
					mVrBluetoothWrapper = new VR_BluetoothSPPWrapper(this, deviceToConnect, m_secondsToWait, mContext);
					break;
				default:
					// The ConnectionType specified was not supported so return false.
					return ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_UNSUPPORTED_CONNTYPE;
			}
			mConnectionType = connectionType;
		}		
		
		// Only change the BluetoothDevice if the one passed in is different.
		if(!deviceToConnect.equals(mBluetoothDevice)) {
			mBluetoothDevice = deviceToConnect;
			mVrBluetoothWrapper.setDevice(mBluetoothDevice);
		}
		// Set the connectionType for the remainder of all packets.
		ESPPacket.setConnectionType(connectionType);
		int retVal = mVrBluetoothWrapper.startAsync();
		if (retVal >= ValentineClient.RESULT_OF_CONNECTION_EVENT_CONNECTING) {
			if(m_processingThread == null) {
				m_processingThread = new ProcessingThread();
				m_processingThread.start();
			}
			m_notified = false;
		}
		return retVal;		
	}
	
	/**
	 * Set method to determine if the ESP library should protect legacy mode. If this value is true when the
	 * V1 is running in Legacy mode, the write thread will not send any commands that are not compatible with
	 * Legacy Mode. The only commands that are compatible with Legacy Mode is a V1 mute request and a version
	 * request to the V1connection. If this value is false, the write thread will send all commands to all
	 * devices.  
	 * 
	 * This value is defaulted to false to make it compatible with older versions of this library.
	 *   
	 * @param val The new value of this setting.
	 */
	public void setProtectLegacyMode (boolean val)
	{
		VR_BluetoothWrapper.setProtectLegacyMode(val);
	}
		
	/**
	 * Get the current state of Legacy Mode protection provided by the ESP library. Refer to 
	 * setProtectLegacyMode for more information on this feature. 
	 * 
	 * @return The current state of Legacy Mode protection provided by the ESP library.
	 */
	public boolean getProtectLegacyMode ()
	{
		return VR_BluetoothWrapper.getProtectLegacyMode();
	}
	
	/**
	 * Registers an object with this class to receive a disconnection event.
	 * 
	 * @param callbackMethod	The method name inside of the object that would like to receive a disconnection event. The method name is case sensitive.
	 * @param callbackOwner		The object to which the method belongs.
	 * 
	 * @return	Returns true if the callback was successfully registered. Returns false if the callbackOwner/callbackMethod is null or if the callbackMethod is empty.
	 */
	public boolean registerForDisconnectEvent(String callbackMethod, Object callbackOwner) {
		if(callbackOwner == null || callbackMethod == null || callbackMethod.isEmpty()) {
			return false;
		}
		m_DisconnectionCallbackObject = callbackOwner;
		m_DisconnectionCallbackName = callbackMethod;
		return true;
	}
	
	/**
	 * Registers an object with this class to receive a connection event.
	 * 
	 * @param callbackMethod	The method name inside of the object that would like to receive a disconnection event.
	 *  The method must accept a boolean flag that indicates if the connection event resulted in a connection. The method name is case sensitive.
	 *  
	 * @param callbackOwner		The object to which the method belongs.
	 * 
	 * @return	Returns true if the callback was successfully registered. Returns false if the callbackOwner/callbackMethod is null or if the callbackMethod is empty.
	 */
	public boolean registerForConnectEvent(String callbackMethod, Object callbackOwner) {
		// Functions in all or nothing pattern.
		if(callbackOwner == null || callbackMethod == null || callbackMethod.isEmpty()) {
			return false;
		}
		m_ConnectionCallbackObject = callbackOwner;
		m_ConnectionCallbackName = callbackMethod;
		return true;
	}
	
	/** 
	 * This registers an object/function combination to be notified when the ESP client stops.  
	 * Only one allowed at a time
	 * Requires a function with a Void parameter:  public void function( Void _parameter)
	 *  
	 * @param _stopObject  Object with the function to be called
	 * @param _stopFunction  Function to be called
	 */
	public void registerForStopNotification(Object _stopObject, String _stopFunction)
	{
		m_stopObject = _stopObject;
		m_stopFunction = _stopFunction;
	}
	
	/** This removes all registrations for callbacks to be notified when the ESP client stops.  
	 *  
	 * @param _stopObject  Object with the function to be called
	 * @param _stopFunction  Function to be called
	 */
	public void deregisterForStopNotification()
	{
		// Note that this type of callback does not use the callback data list, so it is safe to modify it here.
		m_stopObject = null;
		m_stopFunction = null;
	}
	
	/** This registers an object/function combination to be notified when a specific ESP packet is received.  
	 *  Requires a function with a Void parameter:  public void function( ESPPacket _parameter)
	 * 
	 * @param _type - The packet id the registration is for.
	 * @param _callBackObject - The object to use for the callback.
	 * @param _method - The method in _callbackObject to call.
	 */
	public void registerForPacket(PacketId _type, Object _callBackObject, String _method)
	{
		m_packetCallbackLock.lock();
		if ( m_lockedPacket.toByteValue() != _type.toByteValue() ){
			// We are allowed to deregister the packet right now
			m_registerForPacket(_type, _callBackObject, _method);			
		}
		else{
			// We are not allowed to register the packet right now. Put it in the queue for deregistration later.
			TempCallbackInfo temp = new TempCallbackInfo();
			temp.type = _type;
			temp.callBackObject = _callBackObject;
			temp.method = _method;
			m_packetsToRegister.add( temp );
		}
		m_packetCallbackLock.unlock();
	}
	
	/**
	 * This method will perform the actual work for registering a callback packet for a specific object.
	 * 
	 * @precondition The caller has m_packetCallbackLock locked.
	 * 
	 * @param _type - The packet type to register.
	 * @param _callBackObject - The object to register.
	 * @param _method - The name of the callback method.
	 */
	private void m_registerForPacket(PacketId _type, Object _callBackObject, String _method)
	{
		CallbackData newCallbackData = new CallbackData();
		newCallbackData.callBackOwner = _callBackObject;
		newCallbackData.method = _method;
		
		if (m_callbackData.containsKey(_type))
		{
			ArrayList<CallbackData> list = m_callbackData.get(_type);
			list.add(newCallbackData);
		}
		else
		{
			ArrayList<CallbackData> newList = new ArrayList<CallbackData>();
			newList.add(newCallbackData);
			m_callbackData.put(_type, newList);
		}
	}
	
	/**
	 * This method will deregister callback set up using registerForPacket. All callbacks for the PacketId and object 
	 * passed in will be deregistered.
	 * 
	 * @param _type		The packet id to deregister.
	 * @param _object	The object to deregister.
	 * @param _method	The callback method.
	 */
	public void deregisterForPacket(PacketId _type, Object _object, String _method)
	{
	
		m_packetCallbackLock.lock();
		if ( m_lockedPacket.toByteValue() != _type.toByteValue() ){
			// We are allowed to deregister the packet right now
			m_deregisterForPacket(_type, _object, _method);			
		}
		else{
			// We are not allowed to register the packet right now. Put it in the queue for deregistration later.
			DeregCallbackInfo info = new DeregCallbackInfo();
			info.callBackOwner = _object;
			info.type = _type;
			info.method = _method;
			m_packetsToDeregister.add(info);
		}
		m_packetCallbackLock.unlock();
	}
	
	/**
	 * This method will deregister callback set up using registerForPacket. Only the callback that matches the object and method 
	 * passed in will be deregistered.
	 * 
	 * @param _type - The packet id to deregister.
	 * @param _object - The object to deregister.
	 * @param _method - The name of the callback method.
	 */
	public void deregisterForPacket(PacketId _type, Object _object)
	{
		m_packetCallbackLock.lock();
		if ( m_lockedPacket.toByteValue() != _type.toByteValue() ){
			// We are allowed to deregister the packet right now
			m_deregisterForPacket(_type, _object, "");			
		}
		else{
			// We are not allowed to register the packet right now. Put it in the queue for deregistration later.
			DeregCallbackInfo info = new DeregCallbackInfo();
			info.callBackOwner = _object;
			info.type = _type;
			info.method = "";
			m_packetsToDeregister.add(info);
		}
		m_packetCallbackLock.unlock();
	}
	
	/**
	 * This method will perform the actual work for deregistering a packet for a specific object.
	 * 
	 * @precondition The caller has m_packetCallbackLock locked.
	 * 
	 * @param _type - The packet type to deregister
	 * @param _object - The object to deregister.
	 * @params _method - If "", then remove all callbacks else remove only the callbacks to _method.
	 */
	private void m_deregisterForPacket(PacketId _type, Object _object, String _method)
	{
		if (m_callbackData.containsKey(_type))
		{
			for (Iterator<Entry<PacketId, ArrayList<CallbackData>>> it = m_callbackData.entrySet().iterator(); it.hasNext();) 
			{ 
			    Map.Entry<PacketId, ArrayList<CallbackData>> pairs = (Map.Entry<PacketId, ArrayList<CallbackData>>)it.next();
			    
			    if (pairs.getKey() == _type)
			    {
				    for (Iterator<CallbackData> it2 = pairs.getValue().iterator(); it2.hasNext();)
				    {
				    	CallbackData data = it2.next();
				    	if (data.callBackOwner == _object && (_method == "" || _method == data.method) )				    		
				    	{
				    		if ( ESPLibraryLogController.LOG_WRITE_VERBOSE ){
				    			Log.v(LOG_TAG, "Deregistering " + data.callBackOwner.toString() + "." + data.method + " for packet id " + _type.toString());
				    		}
				    		
				    		it2.remove();
				    		break;
				    	}
				    }
			    }
			    
			}
		}
		
	}
	
	/**
	 * This method will allow the caller to determine if they have already registered for a specific packet using registerForPacket.
	 * 
	 * @param _type - The PacketId to search for.
	 * @param _object - The object to search for.
	 * 
	 * @return - true if the obect is already registered for the packet id passed in, else false.
	 */
	public boolean isRegisteredForPacket (PacketId _type, Object _object)
	{
		// Lock here so the queue doesn't get modified while we are traversing it.
		m_packetCallbackLock.lock();
		
		if (m_callbackData.containsKey(_type))
		{
			for (Iterator<Entry<PacketId, ArrayList<CallbackData>>> it = m_callbackData.entrySet().iterator(); it.hasNext();) 
			{ 
			    Map.Entry<PacketId, ArrayList<CallbackData>> pairs = (Map.Entry<PacketId, ArrayList<CallbackData>>)it.next();
			    
			    if (pairs.getKey() == _type)
			    {
				    for (Iterator<CallbackData> it2 = pairs.getValue().iterator(); it2.hasNext();)
				    {
				    	CallbackData data = it2.next();
				    	if (data.callBackOwner == _object)
				    	{
				    		// Found a registration for the requested packet
				    		
				    		m_packetCallbackLock.unlock();
				    		
				    		return true;
				    	}
				    }
			    }
			    
			} 
			
		}
		
		m_packetCallbackLock.unlock();
		
		// We didn't find any registration for the packet/object pair passed in. 
		return false;		
	}
	
	/**
	 * Synchronously disconnects from the V1Connection if connected and shuts down the ESP Library.
	 * 
	 * (DO NOT CALL FROM UI THREAD)
	 * 
	 * @param deviceToConnect	A V1Connection you want to make a connection with.
	 * 
	 * @throws 					RuntimeException if called from the UI (Main) Thread.
	 * 
	 * @deprecated DO NOT USE. THIS METHOD WILL BE REMOVED IN THE NEXT API RELEASE.
	 */	
	public void stopSync() {
		if(Thread.currentThread() == Looper.getMainLooper().getThread()){
			throw new RuntimeException("stopSync() will/may block and should not be called on the " + Thread.currentThread().getName() + " thread.");
		}
		if (!m_inDemoMode)
		{
			try 
			{				
				if (m_processingThread != null)
				{
					m_processingThread.setRun(false);
					m_processingThread.interrupt();
					// Once the processing thread is stopped always set the object to null so it will be GB'd.
					m_processingThread = null;
				}
				// Do not directly stop the reader and writer threads. Tell the VR_BluetoothWrapper to stop the the threads.
				if(mVrBluetoothWrapper != null) {
					mVrBluetoothWrapper.stopSync();
				}
				
				if ((m_stopObject != null) && (m_stopFunction != null) && !m_notified)
				{
					m_notified = true;
					Utilities.doCallback(m_stopObject, m_stopFunction, Boolean.class, true);
				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Asynchronously disconnect from the V1Connection and shuts down the ESP Library.
	 * 
	 * (Safe to call from the UI thread)
	 */
	public void stopAsync()
	{
		if (!m_inDemoMode)
		{
			try 
			{				
				if (m_processingThread != null)
				{
					m_processingThread.setRun(false);
					m_processingThread.interrupt();
					// Once the processing thread is stopped always set the object to null so it will be GB'd.
					m_processingThread = null;
				}
				// Do not directly stop the reader and writer threads. Tell the VR_BluetoothWrapper to stop the the threads.
				if(mVrBluetoothWrapper != null) {
					mVrBluetoothWrapper.stopAsync();
				}
				
				if ((m_stopObject != null) && (m_stopFunction != null) && !m_notified)
				{
					m_notified = true;
					Utilities.doCallback(m_stopObject, m_stopFunction, Boolean.class, true);
				}
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This method performs the actual callback for packets registered using registerForPacket
	 * 
	 * @param _callbackData - The callback information.
	 * @param _packet - The ESP packet to pass through the callback.
	 */
	private void m_doCallback(final CallbackData _callbackData, final ESPPacket _packet) 
	{
		Runnable callback = new Runnable()
		{
			public void run()
			{
				try 
				{
					if ((_callbackData != null) && (_callbackData.callBackOwner != null ) && (_callbackData.callBackOwner != null))
					{
						Class<? extends ESPPacket> packetClass = _packet.getClass();
						_callbackData.callBackOwner.getClass().getMethod(_callbackData.method, packetClass).invoke(_callbackData.callBackOwner, _packet);
					}
				} 
				catch ( InvocationTargetException e ) 
				{
		//			ValentineClient.getInstance().reportError(e.toString());
					if(ESPLibraryLogController.LOG_WRITE_INFO){
						Log.i(LOG_TAG, _callbackData.callBackOwner.toString() + " " + _callbackData.method + " There was an invoke error calling back to owner: " + e.getTargetException().toString());
					}
					e.printStackTrace();
				} 
				catch (Exception e) 
				{
					ValentineClient.getInstance().reportError(e.toString());
					if(ESPLibraryLogController.LOG_WRITE_INFO){
						Log.i(LOG_TAG, _callbackData.callBackOwner.toString() + " " + _callbackData.method + " There was an error calling back to owner: " + e.toString());
					}
					e.printStackTrace();
				}
			}
		};
		
		callback.run();
	}
	
	/**
	 * Send a packet to the hardware.
	 * 
	 * @param _packet - The packet to send.
	 */
	public void sendPacket(ESPPacket _packet)
	{
		if (m_inDemoMode)
		{
			m_demoData.handleDemoPacket(_packet);
		}
		else
		{
			PacketQueue.pushOutputPacketOntoQueue(_packet);
		}
	}
	
	/**
	 * Starts the ESP Library demo mode. This will disconnect the client from a connected V1Connection.
	 *  
	 * @param _demoData		String containing demo mode data.
	 * @param _repeat		Indicates if the the demo mode data should loop indefinitely.
	 */
	public void startDemo(String _demoData, boolean _repeat)
	{
		stopDemo( false);
		
		try
		{
			m_inDemoMode = true;
			// Close the bluetooth connection and stop the reader and writer threads
			if(mVrBluetoothWrapper != null) {
				Log.d("Debug", "calling stop from stop startDemo inside of ValentineESP");
				mVrBluetoothWrapper.stopAsync();
			}
		}
		catch (Exception e)
		{
			if(ESPLibraryLogController.LOG_WRITE_ERROR) {
				Log.e(LOG_TAG,"An error occured while interrupting the reader/writer thread. " + e.getMessage());
			}
		}
		
		m_demoFileThread = new ProcessDemoFileThread(_demoData, _repeat);
		m_demoFileThread.start();
		
		if ((m_processingThread == null) || (!m_processingThread.isAlive()))
		{
			m_processingThread = new ProcessingThread();
			
			m_processingThread.start();
		}
		
		// Allow a stop notification when exiting demo mode
		m_notified = false;
	}
	
	/**
	 * Stop demo mode and optionally establish a new bluetooth connection.
	 * 
	 * @param _restartLiveMode	If true, an attempt will be made to connect to the last used V1Connection.
	 * 
	 * @return	Always returns true, unless unable to connect with a V1Connection.
	 */
	public boolean stopDemo( boolean _restartLiveMode)
	{
		m_inDemoMode = false;
		
		if (m_demoFileThread != null)
		{
			try 
			{
				m_demoFileThread.setRun(false);
				m_demoFileThread.interrupt();
			
				while (m_demoFileThread.isAlive())
				{
					try 
					{
						Thread.sleep(10);						
					} 
					catch (InterruptedException e) 
					{
						e.printStackTrace();
					}
				}
			} 
			catch (Exception e1) 
			{
				/** DELIBERATELY EMPTY **/
			}
		}
			
		m_demoFileThread = null;
		
		if (_restartLiveMode)
		{
			if (mBluetoothDevice != null && mVrBluetoothWrapper != null) {
				// Initiate a bluetooth connection using the last bluetooth device and connectiontype.
				return mVrBluetoothWrapper.startSync();
			}
		}
		return true;
	}
	
	/**
	 * Class to hold the callback data.
	 *
	 */
	public class CallbackData
	{
		public Object callBackOwner;
		public String method;
	}
	
	/** 
	 * Class to hold information about callbacks that are waiting to be deregistered
	 */
	public class DeregCallbackInfo
	{
		public Object callBackOwner;	
		public PacketId type;
		public String method;
	}
	
	/**
	 * The ProcessingThread class is responsible for processing all packets received from the ESP bus.
	 */
	private class ProcessingThread extends Thread
	{
		private boolean m_run;
		
		/**
		 * Sets or clears the flag to keep the thread running.
		 * @param _run - Set to true to keep the thread running, set to false to stop the thread.
		 */
		public void setRun(boolean _run)
		{
			m_run = _run;
		}
		
		/**
		 * ProcessingThread constructor.
		 */
		public ProcessingThread()
		{
			m_run = true;
		}
		
		/**
		 * This is the actual thread execution method.
		 */
		public void run()
		{
			while (m_run)
			{
				try 
				{
					final ESPPacket packet = PacketQueue.getNextInputPacket();
					
					if (packet == null)
					{
						// No packets, so hang out for a while.
						Thread.sleep(100);
					}
					else
					{
						if (m_callbackData.containsKey(packet.getPacketIdentifier()))
						{
							// Do not allow deregistering packets while iterating through this list because that will
							// cause a structural change to the list, which should not be done while iterating through
							// the list.							
							ValentineESP.this.setLockedPacket( packet.getPacketIdentifier() );
							
							final ArrayList<CallbackData> list = m_callbackData.get(packet.getPacketIdentifier());
							
							for (int i = 0; i < list.size(); i++)
							{
								final CallbackData data = list.get(i);
								
								if (data == null)
								{

								}
								else
								{
									new Runnable()
									{
										public void run()
										{											
											m_doCallback(data, packet);
										}
									}.run();
								}
							}
						}
						
						// Allow registering and deregistering after we are done processing this packet
						ValentineESP.this.setLockedPacket( PacketId.unknownPacketType );						
					}
				} 
				catch (Exception e) 
				{
					m_run = false;
				} 
			}
		}
	};
	
	/**
	 * This Thread will process the demo mode file contents as if they were ESP data.
	 *
	 */
	private class ProcessDemoFileThread extends Thread
	{
		private boolean m_run;
		private String m_demoFileStream;
		private boolean m_repeat;
		
		/**
		 * Sets or clears the flag to keep the thread running.
		 * @param _run - Set to true to keep the thread running, set to false to stop the thread.
		 */
		public void setRun(boolean _run)
		{
			m_run = _run;
		}
		
		/**
		 * ProcessDemoFileThread constuctor.
		 * 
		 * @param _demoFileContents - The contents of the demo mode file that will be processed as ESP data.
		 * @param _repeat - If true, the demo mode loop will return to the beginning of _demoData when it reached the end.
		 */
		public ProcessDemoFileThread(String _demoFileContents, boolean _repeat)
		{
			if(_demoFileContents == null || _demoFileContents.isEmpty()) {
				throw new IllegalArgumentException("The demo file was null or empty. A valid demo file is required to start Demo Mode.");
			}
			m_run = true;
			m_demoFileStream = _demoFileContents;
			m_repeat = _repeat;
		}
		
		/**
		 * This is the actual thread execution method.
		 */
		public void run()
		{
			int current = 0;
			boolean process = true;
			while (m_run)
			{
				try 
				{
					do
					{
						int eolnLocation = m_demoFileStream.indexOf("\n", current);
						String currentByteStream = m_demoFileStream.substring(current, eolnLocation);
						current = eolnLocation + 1;
						
						String specialTest = currentByteStream.substring(0, 2);
						if (specialTest.equals("//"))
						{
							//do nothing
						}
						else if (specialTest.charAt(0) == '<')
						{
							String notification = "";
							int notificationStartLocation = currentByteStream.indexOf(":");
							int notificationStopLocation = currentByteStream.indexOf(">");
							
							notification = currentByteStream.substring(notificationStartLocation+1, notificationStopLocation);
							
							ValentineClient.getInstance().doNotification(notification);
						}
						else
						{
							ArrayList<Byte> byteBuffer = m_makeByteBufferFromString(currentByteStream);
							ESPPacket newPacket = ESPPacket.makeFromBuffer(byteBuffer, ConnectionType.V1Connection, Devices.VALENTINE1_WITH_CHECKSUM);
							m_demoData.handleDemoPacket(newPacket);
							
							sleep(68);
						}
						
						if (current == m_demoFileStream.length())
						{
							if (m_repeat)
							{
								current = 0;
							}
							else
							{
								process = false;
							}
						}
					}
					while (process);
				} 
				catch (Exception e) 
				{
					m_run = false;
				} 
			}
		}
		
		/**
		 * This method will convert a string of space delimited, 2 character strings into the hex bytes the strings represent. 
		 * @param _data - The string data to convert.
		 * @return - An array of bytes.
		 */
		private ArrayList<Byte> m_makeByteBufferFromString(String _data)
		{
			ArrayList<Byte> rc = new ArrayList<Byte>();
			String[] split = _data.split(" ");
			
			for (int i = 0; i < split.length; i++)
			{
				String item = split[i];
				byte temp = (byte) ((Character.digit(item.charAt(0), 16) << 4) + Character.digit(item.charAt(1), 16));
				rc.add(temp);
			}
			return rc;
		}
	};
	
	/**
	 * This method will remove all callbacks set up using registerForCallbacks.
	 */
	public void clearAllCallbacks()
	{
		m_packetCallbackLock.lock();
		if ( m_lockedPacket.toByteValue() == PacketId.unknownPacketType.toByteValue() ){
		// We don't have a locked packet, so just clear everything
			if ( ESPLibraryLogController.LOG_WRITE_VERBOSE ){
	    		Log.v(LOG_TAG, "Clearing " + m_callbackData.size() + " callbacks");
			}			
			
			m_callbackData.clear();
			m_clearAllCallbacksOnUnlock = false;
		}
		else{		
		// We have a locked packet type, so set the flag to clear all packets when we unlock the current packet type
			m_clearAllCallbacksOnUnlock = true; 
		}
		
		// Clear the pending queues while we are locked
		m_packetsToDeregister.clear();
		m_packetsToRegister.clear();
		
		m_packetCallbackLock.unlock();
	}
	
	/**
	 * This method will clear all callbacks set up using registerForCallbacks for the packet id passed in
	 *  
	 * @param _type - The packet id to deregister.
	 */
	public void clearCallbacks(PacketId _type)
	{
		// Keep this locked for the duration of this method 
		m_packetCallbackLock.lock();
		
		for (Iterator<Entry<PacketId, ArrayList<CallbackData>>> it = m_callbackData.entrySet().iterator(); it.hasNext();) 
		{ 
		    Map.Entry<PacketId, ArrayList<CallbackData>> pairs = (Map.Entry<PacketId, ArrayList<CallbackData>>)it.next();
		    
		    if (pairs.getKey() == _type)
		    {
		    	if ( _type.toByteValue() != m_lockedPacket.toByteValue() ){
		    		if ( ESPLibraryLogController.LOG_WRITE_VERBOSE ){
		        		Log.v(LOG_TAG, "Clearing all callbacks for packet id " + _type.toString());
		    		}
		    		
		    		it.remove();
		    		break;
		    	}
		    	else{
		    		// Add all callbacks of this packet type to the pending degistration queue
		    		for ( int i = 0; i < pairs.getValue().size(); i++ ){
		    			DeregCallbackInfo info = new DeregCallbackInfo();
		    			info.callBackOwner = pairs.getValue().get(i);
		    			info.type = pairs.getKey();
		    			info.method = "";
		    			m_packetsToDeregister.add(info);
		    		}
		    	}
		    }
		} 
		
		m_packetCallbackLock.unlock();
		
	}
		
	/** 
	 * Register a function to be notified if the ESP client has not received any data from the Valentine One after X seconds.
	 * Example case of this being called, the valentine one has been turned off for more than X seconds.
	 * Requires a function with a String parameter:  public void function( String _parameter)
	 *  
	 *  Only one Notification callback is allowed at a time, registering a second will overwrite the first
	 *  
	 * @param _noDataObject			The object with the function to be notified when there is no data from the Valentine One.
	 * @param _noDataFunction	The callback function to receive the no data event.
	 */
	public void registerForNoDataNotification(Object _noDataObject, String _noDataFunction)
	{
		// Note that this type of callback does not use the callback data list, so it is safe to modify it here.		
		
		m_noDataObject = _noDataObject;
		m_noDataFunction = _noDataFunction;
	}
	
	/**
	 * Deregister callbacks set up using registerForNoDataNotification.
	 */
	public void deregisterForNoDataNotification()
	{
		// Note that this type of callback does not use the callback data list, so it is safe to modify it here.		
		m_noDataObject = null;
		m_noDataFunction = null;
	}
	
	/**
	 * Performs the registerForNoDataNotification callback.
	 */
	public void notifyNoData()
	{
		if ((m_noDataObject != null) && m_noDataFunction != null)
		{
			Utilities.doCallback(m_noDataObject, m_noDataFunction, String.class, "No Data Received");
		}
	}
	
	/**
	 * Return the last demo mode data file contents loaded in.
	 * @return The last demo mode data file contents loaded in.
	 */	
	public DemoData getDemoData()
	{
		return m_demoData;
	}
	
	/**
	 * Notifies who ever is registered to receiver the Unsupported Device callback.
	 */
	public void doUnsupportedDeviceCallback() {
		if ((m_unsupportedObject != null) && (m_unsupportedFunction != null))
		{
			Utilities.doCallback(m_unsupportedObject, m_unsupportedFunction, String.class, "Unsupported Device");
		}
	}

	/**
	 * This method will allow the caller to determine if this object is currently set up to process ESP data.
	 * 
	 * @return 	Returns true if Processing Thread is alive otherwise, false.
	 */
	public boolean isRunning() 
	{
		if (m_processingThread == null)
		{
			return false;
		}
		return m_processingThread.isAlive();
	}
	
	/**
	 * This method returns the {@link BluetoothSocket} of the current Bluetooth connection (IF SUPPORTED).
	 * 
	 * @return  Returns {@link BluetoothSocket} of the current Bluetooth connection, if available, otherwise, null.
	 * (Bluetooth LE connections do not using BluetoothSockets so null will always be returned)
	 * 
	 * @deprecated DO NOT USE. THIS METHOD WILL BE REMOVED IN THE NEXT API RELEASE.
	 */
	@Deprecated
	public BluetoothSocket getSocket() {
		// If the VR_BluetoothWrapper is null, return null because we know for a fact that no BluetoothSocket exist.
		return mVrBluetoothWrapper != null ? mVrBluetoothWrapper.getSocket() : null;
	}
	
	/**
	 * Returns a human friendly name for the connected V1Connection.
	 * 
	 * @return	If there is a connection with a V1Connection returns a human friendly name for the device otherwise, an empty string.
	 */
	public String getConnectedBTDeviceName() {		
		return mVrBluetoothWrapper != null ? mVrBluetoothWrapper.getConnectedBTDeviceName() : new String();
	}
}
