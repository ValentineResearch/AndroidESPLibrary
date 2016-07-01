/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import com.valentine.esp.bluetooth.BluetoothDeviceBundle;
import com.valentine.esp.bluetooth.ConnectionType;
import com.valentine.esp.bluetooth.VRScanCallback;
import com.valentine.esp.constants.Devices;
import com.valentine.esp.constants.ESPLibraryLogController;
import com.valentine.esp.constants.PacketId;
import com.valentine.esp.data.InfDisplayInfoData;
import com.valentine.esp.data.SavvyStatus;
import com.valentine.esp.data.SweepDefinition;
import com.valentine.esp.data.SweepSection;
import com.valentine.esp.data.UserSettings;
import com.valentine.esp.packets.InfDisplayData;
import com.valentine.esp.packets.request.RequestBatteryVoltage;
import com.valentine.esp.packets.request.RequestChangeMode;
import com.valentine.esp.packets.request.RequestFactoryDefault;
import com.valentine.esp.packets.request.RequestMaxSweepIndex;
import com.valentine.esp.packets.request.RequestMuteOff;
import com.valentine.esp.packets.request.RequestMuteOn;
import com.valentine.esp.packets.request.RequestOverrideThumbwheel;
import com.valentine.esp.packets.request.RequestSavvyStatus;
import com.valentine.esp.packets.request.RequestSerialNumber;
import com.valentine.esp.packets.request.RequestSetSavvyUnmute;
import com.valentine.esp.packets.request.RequestSetSweepsToDefault;
import com.valentine.esp.packets.request.RequestStartAlertData;
import com.valentine.esp.packets.request.RequestStopAlertData;
import com.valentine.esp.packets.request.RequestSweepSections;
import com.valentine.esp.packets.request.RequestTurnOffMainDisplay;
import com.valentine.esp.packets.request.RequestTurnOnMainDisplay;
import com.valentine.esp.packets.request.RequestUserBytes;
import com.valentine.esp.packets.request.RequestVehicleSpeed;
import com.valentine.esp.packets.request.RequestVersion;
import com.valentine.esp.packets.request.RequestWriteUserBytes;
import com.valentine.esp.packets.response.ResponseBatteryVoltage;
import com.valentine.esp.packets.response.ResponseDataError;
import com.valentine.esp.packets.response.ResponseMaxSweepIndex;
import com.valentine.esp.packets.response.ResponseRequestNotProcessed;
import com.valentine.esp.packets.response.ResponseSavvyStatus;
import com.valentine.esp.packets.response.ResponseSerialNumber;
import com.valentine.esp.packets.response.ResponseSweepSections;
import com.valentine.esp.packets.response.ResponseUnsupported;
import com.valentine.esp.packets.response.ResponseUserBytes;
import com.valentine.esp.packets.response.ResponseVehicleSpeed;
import com.valentine.esp.packets.response.ResponseVersion;
import com.valentine.esp.statemachines.GetAlertData;
import com.valentine.esp.statemachines.GetAllSweeps;
import com.valentine.esp.statemachines.WriteCustomSweeps;
import com.valentine.esp.utilities.Range;
import com.valentine.esp.utilities.Utilities;
import com.valentine.esp.utilities.V1VersionSettingLookup;

/** This is the class that handles all the interfacing with the Valentine One.  All the calls to the device are done through this class.
 * It will wrap all the handling and unpacking of packets and handling the registering and deregistering for packets leaving the user to use the data 
 * structures in the data package to handle the data.   Only create one instance of this class at a time, so do not create multiple instances of
 * this class.  Put this in your application class and save the instance there.
 */
public class ValentineClient 
{	
	
	/**
	 * Int result code that indicates the connection attempt was not initiated.
	 */
	public static final int RESULT_OF_CONNECTION_EVENT_NO_CONNECTION_ATTEMPT = 0;
	/**
	 * Int result code that indicates the connection attempt was not initiated because there a valid {@link BluetoothDevice} wasn't passed in.
	 */
	public static final int RESULT_OF_CONNECTION_EVENT_FAILED_NO_DEVICE = RESULT_OF_CONNECTION_EVENT_NO_CONNECTION_ATTEMPT + 1;
	/**
	 * Int result code that indicates the connection attempt was not initiated because the device does not support Bluetooth LE (Low Energy).
	 */
	public static final int RESULT_OF_CONNECTION_EVENT_FAILED_LE_NOT_SUPPORTED = RESULT_OF_CONNECTION_EVENT_FAILED_NO_DEVICE + 1;
	/**
	 * Int result code that indicates the connection attempt was not initiated because no callback was provided.
	 */
	public static final int RESULT_OF_CONNECTION_EVENT_FAILED_NO_CALLBACK = RESULT_OF_CONNECTION_EVENT_FAILED_LE_NOT_SUPPORTED + 1;
	/**
	 * Int result code that indicates the connection attempt was not initiated because the {@link ConnectionType} passed-in is not supported.
	 */
	public static final int RESULT_OF_CONNECTION_EVENT_FAILED_UNSUPPORTED_CONNTYPE = RESULT_OF_CONNECTION_EVENT_FAILED_NO_CALLBACK + 1;	
	/**
	 * Int result code that indicates the connection attempt was initiated, no delay.
	 */
	public static final int RESULT_OF_CONNECTION_EVENT_CONNECTING = RESULT_OF_CONNECTION_EVENT_FAILED_UNSUPPORTED_CONNTYPE + 1;
	/**
	 * Int result code that indicates the connection attempt was initiated, with delay.
	 */
	public static final int RESULT_OF_CONNECTION_EVENT_CONNECTING_DELAY = RESULT_OF_CONNECTION_EVENT_CONNECTING + 1;
		
	private static final String LOG_TAG = "ValentineClient LOG";
	private static final int MAX_INDEX_NOT_READ = -1;
	BluetoothDevice   m_bluetoothDevice;
	ValentineESP      m_valentineESP;
	SharedPreferences m_preferences;
	String            m_bluetoothAddress;
	V1VersionSettingLookup m_settingLookup;
	
	Object m_errorCallbackObject;
	String m_errorCallbackFunction;	

	Context m_context;

	public Devices m_valentineType;
	
	private Devices m_lastV1Type = Devices.UNKNOWN;
	private int m_v1TypeChangeCnt = 0;
	private final int V1_TYPE_SWITCH_THRESHOLD = 8;

	private Map<Devices, Object> m_versionCallbackObject;
	private Map<Devices, String> m_versionCallbackFunction;
	private Map<Devices, Object> m_serialNumberCallbackObject;
	private Map<Devices, String> m_serialNumberCallbackFunction;
	private Object               m_userBytesCallbackObject;
	private String               m_userBytesCallbackFunction;
	private Object               m_voltageCallbackObject;
	private String               m_voltageCallbackFunction;
	private Object               m_savvyStatusObject;
	private String               m_savvyStatusFunction;
	private Object               m_vehicleSpeedObject;
	private String               m_vehicleSpeedFunction;
	private Object               m_getSweepsObject;
	private String               m_getSweepsFunction;
	private Object               m_getMaxSweepIndexObject;
	private String               m_getMaxSweepIndexFunction;

	private GetAllSweeps      m_getAllSweepsMachine;
	private WriteCustomSweeps m_writeCustomSweepsMachine;
	private ConcurrentHashMap<Object, GetAlertData> m_getAlertDataMachineMap = new ConcurrentHashMap<Object, GetAlertData>();

	private ConcurrentHashMap<Object, String> m_infCallbackCallbackData;
	private Object							  m_ConnectionCallbackObject;
	private String							  m_ConnectionCallbackName;
	private Object							  m_DisconnectionCallbackObject;
	private String							  m_DisconnectionCallbackName;

	private Object m_notificationObject;
	private String m_notificationFunction;

	private static ValentineClient m_instance;

	private Object m_stopObject;
	private String m_stopFunction;

	private Object m_unsupportedDeviceObject;
	private String m_unsupportedDeviceFunction;

	private Object m_unsupportedPacketObject;
	private String m_unsupportedPacketFunction;
	private Object m_requestNotProcessedObject;
	private String m_requestNotProcessedFunction;
	private Object m_dataErrorObject;
	private String m_dataErrorFunction;
	private Object m_dataErrorObjectRaw;
	private String m_dataErrorFunctionRaw;
	
	private ArrayList<SweepSection> m_sweepSections;
	private ArrayList<SweepDefinition> m_defaultSweepDefinitions;
	private int m_maxSweepIndex = -1;
	
	private String m_lastV1ConnVer;
	private ConnectionType mConnectionType;
	private boolean isShuttingDown = false;
	private final boolean m_supportBLE;
	
	/**
	 * Constructor for the ValentineClient class.
	 * 
	 * @param _context The context to use for this object.
	 */
	public ValentineClient(Context _context) {		
		// Initializes the library with the default timeout supplied.
		this(_context, 10);
	}
	
	/**
	 * Constructor for the ValentineClient object that takes a custom timeout for no data received notification.
	 * 
	 * @param context 			The context to use for this object.
	 * @param secondsToWait		The seconds to wait before a no data notification is sent.
	 */
	public ValentineClient(Context context, int secondsToWait) {
		if(context == null) {
			throw new IllegalArgumentException("Context received was null. A valid instance of android.content.Context");
		}
		m_supportBLE = checkBluetoothLESupport(context);
		// Initializes the library with the supplied timeout.
		init(context, secondsToWait);
	}
	
	/**
	 * Set the delay to wait before the data reader thread will notify that the
	 * no data has been received.
	 * 
	 * @param delay	Number of seconds to wait before notifying that no data has been received.
	 */
	public void setNoDataReceivedDelay(int delay) {
		this.m_valentineESP.setNoDataReceivedDelay(delay);
	}
	
	/**
	 * Initializes the library.
	 * 
	 * @param context 	 		Context needed to register broadcast listeners and retrieve the SharedPreferences.
	 * @param secondsToWait		The seconds to wait before the data reader thread notifies the library of no data. To be passed in in seconds.
	 */
	public void init(Context context, int secondsToWait) {
		m_instance = this;
		m_context = context;		
		m_valentineESP = new ValentineESP(secondsToWait, m_context);
		m_settingLookup = new V1VersionSettingLookup();
		m_lastV1ConnVer = "";
		
		// Initialize the callback maps
		m_versionCallbackObject = new HashMap<Devices, Object>();
		m_versionCallbackFunction = new HashMap<Devices, String>();
		m_serialNumberCallbackObject = new HashMap<Devices, Object>();
		m_serialNumberCallbackFunction = new HashMap<Devices, String>();
		m_infCallbackCallbackData = new ConcurrentHashMap<Object, String>();    	
		
		// Set up the callbacks used within this object.
		registerLocalCallbacks();

		m_preferences = m_context.getSharedPreferences("com.valentine.esp.savedData", Context.MODE_PRIVATE);
		// Retrieve the last previously connected BluetoothDevices MAC address from the SharedPreferences.
		// Stored in startUP() down below, we a new BluetoothDevice is passed in.
		m_bluetoothAddress = m_preferences.getString("com.valentine.esp.LastBlueToothConnectedDevice", "");
		// Get the int value of the ConnectionType from the sharedPreferences.
		int temp = m_preferences.getInt("com.valentine.esp.ConnectionType", ConnectionType.UNKNOWN.getValue());
		// If the ConnectionType is UNKNOWN and there is previous Bluetooth address then the app has been recently updated so
		// default the ConnectionType to SPP to maintain back compatibility.
		if (ConnectionType.UNKNOWN.getValue() == temp && !m_bluetoothAddress.isEmpty()) {
			temp = ConnectionType.V1Connection.getValue();
			// Immediately save SPP inside of the SharedPreferences.
			m_preferences.edit().putInt("com.valentine.esp.ConnectionType", temp);
		}		
		// Set the library's connectiontype.
		setConnectionType(ConnectionType.values()[temp]);

		// Make sure the stored bluetooth device address is valid and retrieve the BluetoothDevice.		
		if (BluetoothAdapter.checkBluetoothAddress(m_bluetoothAddress)) {
			// getRemoteDevice guarantees that a BluetoothDevice will be returned if a valid MAC address is provided.
			m_bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(m_bluetoothAddress);			
		}
		m_valentineType = Devices.UNKNOWN;
	}
	
	/**
	 * Returns the current Bluetooth {@link ConnectionType} of the Library.
	 * 
	 * @return The current Bluetooth {@link ConnectionType}.
	 */
	public ConnectionType getConnectionType() {
		return mConnectionType;
	}
	
	/**
	 * Sets the Bluetooth {@link ConnectionType} that will be used by the library.
	 * 
	 * @param connectionType	The desired {@link ConnectionType} for the library to use.
	 * 
	 * @return	True if successfully changed the connection type otherwise, false.
	 */
	public boolean setConnectionType(ConnectionType connectionType) {
		// If Bluetooth LE is not supported and connectionType is V1Connection_LE, do not assign the ConnectionType and return false.
		if(!isBluetoothLESupported() && ConnectionType.V1Connection_LE.equals(connectionType)) {
			Log.e(LOG_TAG, "Unable to set the ConnectionType because ConnectionType = " + connectionType.toString() + " is not supported on this device.");
			return false;
		}
		mConnectionType = connectionType;
		return true;
	}
	
	/**
	 * Returns a human friendly name for the connected V1Connection.
	 * 
	 * @return	A string containing the device name concatenated with the the last four 
	 * characters of the Bluetooth address.
	 */
	public String getBTDeviceName() {
		return m_valentineESP.getConnectedBTDeviceName();
	}
	
	/**
	 * Returns true if Bluetooth LE is supported by the running hardware.
	 * 
	 * @return True if the running device supports Bluetooth LE otherwise, false.
	 */
	public final boolean isBluetoothLESupported() {
		return m_supportBLE;
	}
	
	/**
	 * Checks the current devices Bluetooth LE support.
	 * (If the Android version on the device is below 4.3 (JellyBean) always return false)
	 * 
	 * @param context	Content used to query the package manager for the Bluetooth LE feature.
	 *  
	 * @return	Returns true if the device supports Bluetooth LE otherwise, false. 
	 */
	public static boolean checkBluetoothLESupport(Context context){
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 &&
				context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
	}
	
	/**
	 * Initiates a Bluetooth discovery for the specified {@link ConnectionType}
	 * for 'x' seconds. Discovered {@link BluetoothDevice}s are delivered
	 * to {@link VRScanCallback#onDeviceFound(BluetoothDeviceBundle, ConnectionType)}.
	 * 
	 * @param connectionType	The connection type of the bluetooth discovery. Valid values are {@link ConnectionType#V1Connection}, {@link ConnectionType#V1Connection_LE}
	 * @param scanCallback		The callback that will receive the bluetooth devices, and the scan complete notification.
	 * @param secondsToScan		The time length to perform the bluetooth scan.
	 * 
	 * @return True if the bluetooth discovery process was started.
	 */
	public boolean scanForDevices(ConnectionType connectionType, VRScanCallback scanCallback, int secondsToScan) {
		// If Bluetooth LE is not supported and connectionType is V1Connection_LE, return false.
		if(ConnectionType.V1Connection_LE.equals(connectionType) && !m_valentineESP.isBluetoothLESupported()) {
			Log.e(LOG_TAG, "Unable to scan for devices because ConnectionType = " + connectionType.toString() + " is not supported on this device.");
			return false;
		}
		return m_valentineESP.scanForDevices(connectionType, scanCallback, secondsToScan);
	}
	
	/**
	 * Stops the Bluetooth discovery process, if any.
	 * SAFE TO CALL MORE THAN ONCE. 
	 */
	public void stopScanningForDevices() {
		m_valentineESP.stopScanningForDevice();
	}
	
	/** 
	 * Returns if the connected Valentine One is a legacy device.
	 * 
	 * @return True if the Valentine One is in legacy mode returns true otherwise, false. 
	 */
	public boolean isLegacyMode()
	{
		if (m_valentineType == Devices.VALENTINE1_LEGACY)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Registers the callbacks that will be used by the {@link ValentineClient}. 
	 */
	private void registerLocalCallbacks()
	{
		// There can only be one unsupported packet callback so we don't need to see if we are already registered.
		m_valentineESP.setUnsupportedCallbacks(this, "unsupportedDeviceCallback");

		if ( !m_valentineESP.isRegisteredForPacket(PacketId.respUnsupportedPacket, this) ){
			m_valentineESP.registerForPacket(PacketId.respUnsupportedPacket, this, "unsupportedPacketCallback");
		}
		
		if ( !m_valentineESP.isRegisteredForPacket(PacketId.respRequestNotProcessed, this) ){
			m_valentineESP.registerForPacket(PacketId.respRequestNotProcessed, this, "RequestNotProcessedCallback");
		}
		if ( !m_valentineESP.isRegisteredForPacket(PacketId.respDataError, this) ){
			m_valentineESP.registerForPacket(PacketId.respDataError, this, "dataErrorCallback");
		}

		if ( !m_valentineESP.isRegisteredForPacket(PacketId.infDisplayData, this) ){
			m_valentineESP.registerForPacket(PacketId.infDisplayData, this, "infDisplayCallback");
		}		
	}
	
	/**
	 * Returns true if the Library is connected to a V1Connection device.
	 *  
	 * @return True if there is a current connection with a V1Connection device otherwise, false.
	 */
	public boolean isConnected()
	{
		return m_valentineESP.getIsConnected();
	}
	
	/**
	 * Returns the last V1connection version received.
	 * 
	 * @return The last V1connection version received.
	 */
	public String getCachedV1connectionVersion()
	{
		return m_lastV1ConnVer;
	}
	
	/** 
	 * Returns the last set of sweep sections returned from the Valentine One.
	 * 
	 * @return The last set of SweepSections returned from the device.
	 */
	public ArrayList<SweepSection> getCachedSweepSections()
	{
		ArrayList<SweepSection> retArray;
		if ( m_valentineESP.isInDemoMode() ){
			// We are in demo mode, so use the demo default sweep sections
			retArray = m_settingLookup.getV1DefaultSweepSections();
		}
		else if ( m_sweepSections == null || m_sweepSections.size() == 0 ){
			// We are not in demo mode but we have not read the sweep section data from the V1 yet, so use
			// the 3.8920 default values.
			retArray = m_settingLookup.getV1DefaultSweepSections();
		}
		else{
			// We are not in demo mode and we have read the sweep sections from the V1, so use the values from the V1.
			// If we've reached this point we don't need to do anything but exit to the return statement below.
			retArray = m_sweepSections;
		}
		
		return retArray;
	}
	
	/** 
	 * Sets the cached sweep sections for the custom sweeps.
	 * 
	 * @param _sections the sweep sections to set.
	 */
	public void setCachedSweepSections(ArrayList<SweepSection> _sections)
	{
		m_sweepSections = _sections;
	}
	
	/** 
	 * Returns the the last known max sweep index for the custom sweeps.
	 * 
	 * @return The max index of the custom sweeps.
	 */
	public int getCachedMaxSweepIndex()
	{
		int retVal;
		
		if ( m_valentineESP.isInDemoMode() ){
			// We are in demo mode, so use the V3.8920 default max sweep index
			retVal = m_settingLookup.getV1DefaultMaxSweepIndex();
		}
		else if ( m_maxSweepIndex == MAX_INDEX_NOT_READ ){
			// We are not in demo mode but we have not read the max sweep index from the V1 yet, so use
			// the 3.8920 max sweep index.
			retVal = m_settingLookup.getV1DefaultMaxSweepIndex();
		}
		else{
			// We are not in demo mode and we have read the max sweep index from the V1, so use the values from the V1.
			// If we've reached this point we don't need to do anything but exit to the return statement below.
			retVal = m_maxSweepIndex;
		}
		
		return retVal;
	}
	
	/** 
	 * Sets the cached max sweep index for custom sweeps.
	 * 
	 * @param _maxIndex the index of the max index for custom sweeps.
	 */
	public void setCachedMaxSweepIndex(Integer _maxIndex)
	{
		m_maxSweepIndex = _maxIndex;
	}
	
	/** 
	 * Returns the the last known set of default sweep definitions.
	 * 
	 * @return The last known set of default sweep definitions.
	 */
	public Range[] getCachedDefaultSweepDefinitions()
	{
		// Use '+ 1' because the sweep index is zero based
		int expectedSweepCnt = getCachedMaxSweepIndex() + 1;
		
		Range[] retArray = null;
		
		if ( m_valentineESP.isInDemoMode() ){
			// We are in demo mode, so use the V3.8920 default sweep definitions
			retArray = m_settingLookup.getV1DefaultCustomSweeps();
		}
		else if ( m_defaultSweepDefinitions == null || m_defaultSweepDefinitions.size() < expectedSweepCnt ){
			// We are not in demo mode but we have not read the default sweep definitions from the V1 yet, so use
			// the 3.8920 sweep defaults.
			retArray = m_settingLookup.getV1DefaultCustomSweeps();
		}
		else{
			// We are not in demo mode and we have read the sweep defaults from the V1, so use the values from the V1.
			// If we've reached this point we don't need to do anything but exit to the return statement below.
			
			// Convert the sweep definition into an array of ranges for convenience.
			retArray = new Range[expectedSweepCnt];			
			for ( int i = 0; i < expectedSweepCnt; i++ ){
				SweepDefinition sweepDef = m_defaultSweepDefinitions.get(i);
				retArray[i] = new Range(sweepDef.getLowerFrequencyEdge(), sweepDef.getUpperFrequencyEdge());
			}
		}
		
		return retArray;
	}	
	
	/** 
	 * Add a new sweep definition to the list of default sweeps.
	 * 
	 * @param newDef The definition to add.
	 */
	public void addDefaultSweepDefintionToCache(SweepDefinition newDef)
	{
		if(m_defaultSweepDefinitions == null) {
			// Check to see if the arraylist already contains the sweep definition and mark it's index.
			m_defaultSweepDefinitions = new ArrayList<SweepDefinition>();
		}
		
		int sweepIndex = newDef.getIndex();
		
		if ( sweepIndex > m_defaultSweepDefinitions.size() - 1){
			// Just add the definition to the end of the list. This is the most common code path.
			m_defaultSweepDefinitions.add(newDef);
		}
		else{
			// Look for this sweep in the existing list and replace is
			int  index = -1;
			for ( int i = 0; i < m_defaultSweepDefinitions.size(); i++ ){
				if ( m_defaultSweepDefinitions.get(i).getIndex() == sweepIndex ){
					index = i;
					break;
				}
			}
			
			if ( index == -1 ){
				// We didn't find the sweep in the list
				m_defaultSweepDefinitions.add(newDef);
			}
			else{
				// Replace the existing definition with the new defintion
				m_defaultSweepDefinitions.set(index, newDef);
			}
		}
	}
	
	/**
	 * This method will clear the cache of sweep definition information read from the V1.
	 */
	public void clearSweepCache() 
	{
		if ( m_defaultSweepDefinitions != null ){
			m_defaultSweepDefinitions.clear();
		}
		if ( m_sweepSections != null ){
			m_sweepSections.clear();
		}
		m_maxSweepIndex = MAX_INDEX_NOT_READ;
	}
	
	/**
	 * This method will determine if the current version of the V1 supports reading the custom sweep defaults.
	 * 
	 * @return True if the last version read from the V1 indicates the V1 supports reading the custom sweep defaults, else false.
	 */
	public boolean allowDefaulSweepDefRead()
	{
		if ( m_settingLookup != null && m_settingLookup.allowDefaulSweepDefRead() ){
			return true;
		}
		
		return false;
	}	
	
	/**
	 *  Returns the current instance of the ValentineClient, only one ValentineClient should exist.
	 * 
	 * @return The instance of the ValentineClient.
	 */
	public static ValentineClient getInstance()
	{
		return m_instance;
	}
	
	/** 
	 * Sets the function and object to handle errors.
	 * 
	 * @param _errorHandlerObject The object that has the function on it to handle errors.
	 * @param _errorHandlerFunction The function to call when there is an error.
	 */
	public void setErrorHandler(Object _errorHandlerObject, String _errorHandlerFunction)
	{
		m_errorCallbackObject = _errorHandlerObject;
		m_errorCallbackFunction = _errorHandlerFunction;
	}
	
	/** 
	 * Calls the registered Error Handler callback to handle the given error
	 * 
	 * @param _error The error that happened that needs to be passed to the callback function to handle errors.
	 */
	public void reportError(String _error)
	{
		if(ESPLibraryLogController.LOG_WRITE_WARNING){
			Log.w("Valentine",_error);
		}
		if ((m_errorCallbackObject != null ) && (m_errorCallbackFunction != null))
		{
			Utilities.doCallback(m_errorCallbackObject, m_errorCallbackFunction, String.class, _error);
		}
	}
	
	/** 
	 * Used to check if we have connected to a bluetooth device previously.
	 * 
	 * @return True of we have connected to a previous bluetooth device, false if we have not.
	 */
	public boolean havePreviousDevice() {
		return (m_bluetoothDevice != null);
	}
	
	/**
	 * Convenience method to clear the {@link ValentineClient}s stored in the SharedPreferences. 
	 * THIS IS FINAL AND WILL PERMANENTLY REMOVE ANY PREVIOUSLY SAVED V1Connection.
	 */
	public void clearPreviousDevice() {
		m_bluetoothDevice = null;
		// We want to removed the previously saved device from the SharedPreferences.
		Editor editor = m_context.getSharedPreferences("com.valentine.esp.savedData", Context.MODE_PRIVATE).edit();
		editor.putString("com.valentine.esp.LastBlueToothConnectedDevice", "");
		editor.commit();
	}

	/**
	 * Gets the {@link BluetoothDevice} saved by the library.
	 * 
	 * @return The saved {@link BluetoothDevice}.
	 */
	public BluetoothDevice getDevice() {
		return m_bluetoothDevice;
	}
	
	/**
	 * Checks to see if the connection to the Bluetooth is up and the ValentineESP class is reading and
	 * writing to the bluetooth connection.
	 * 
	 * @return True if the is data being received from the Bluetooth connection otherwise, false.
	 */
	public boolean isRunning()
	{
		return m_valentineESP.isRunning();
	}
	
	/**
	 * This will synchronously disconnect the connected V1Connection and shutdown the Processing thread.
	 * This effectively shuts down the ESP Library.
	 * 
	 * (DO NOT CALL FROM UI THREAD)
	 * 
	 * @deprecated DO NOT USE. THIS METHOD WILL BE REMOVED IN THE NEXT API RELEASE.
	 */
	public void abortConnectionSync() {
		m_valentineESP.stopSync();
	}
	
	/**
	 * Synchronously connect to deviceToConnect using the SPP protocol. This method will block until a connection has been made or an exception occurs.
	 * Calling this thread on the UI thread will throw a Runtime exception.
	 * 
	 * (DO NOT CALL FROM UI THREAD)
	 * 
	 * @param deviceToConnect The V1Connection to connect.
	 * 
	 * @throws RuntimeException if called from the UI (Main) Thread
	 * 
	 * @return True if a bluetooth connection was made with deviceToConnect. If deviceToConnect is null, returns false immediately.
	 * 
	 * @deprecated DO NOT USE. THIS METHOD WILL BE REMOVED IN THE NEXT API RELEASE.
	 */
	public boolean startUpSync(BluetoothDevice deviceToConnect) {
		if(Thread.currentThread() == Looper.getMainLooper().getThread()){
			throw new RuntimeException("startUpSync() will/may block and should not be called on the " + Thread.currentThread().getName() + " thread.");
		}
		
		isShuttingDown = true;
		
		// Return false, if the passed in devices is null.
		if(deviceToConnect == null) {
			if(ESPLibraryLogController.LOG_WRITE_ERROR) {
				Log.e(LOG_TAG, "The passed in bluetooth device was null");
			}
			return false;
		}
		stopDemoMode(false);
		
		// Start the callbacks into the local methods
		registerLocalCallbacks();
		
		// Force an unknown V1 type until we get the infDisplayData packet from the V1.
		m_valentineType = Devices.UNKNOWN;
		m_lastV1Type = Devices.UNKNOWN;
		m_v1TypeChangeCnt = 0;
		
		// Do not allow writing to the V1 until the type is known.
		PacketQueue.initOutputQueue(Devices.UNKNOWN, false, true);
		PacketQueue.initInputQueue (true);
		
		if (m_bluetoothDevice == null || !m_bluetoothDevice.equals(deviceToConnect)) {
			// Save the new bluetooth devices. 			
			Editor edit = m_preferences.edit();			
			m_bluetoothDevice = deviceToConnect;
			// Store the last ConnectionType and BluetoothDevices to the SharedPreferences.
			edit.putString("com.valentine.esp.LastBlueToothConnectedDevice", m_bluetoothDevice.getAddress());
			edit.putInt("com.valentine.esp.ConnectionType", ConnectionType.V1Connection.ordinal());
			edit.commit();
		}
		
		if (m_valentineESP.startUpSync(deviceToConnect))
    	{
    		m_versionCallbackObject.clear();
    		m_versionCallbackFunction.clear();
    		m_serialNumberCallbackObject.clear();
    		m_serialNumberCallbackFunction.clear();
    		//m_infCallbackCallbackData.clear();
    		doSearch();
    		return true;
    	}
    	
    	return false;
	}
		
	/**
	 * Asynchronously connects to deviceToConnect using the specified connectionType. 
	 * This method is asynchronous and will return immediately with the result of the connection attempt.
	 * Once the connection event completes, the callbackOwners' callbackMethod will be invoked with a boolean result of the current connection state. True for connected, false for anything else.
	 * 
	 * (Safe to call from the UI thread)
	 * 
	 * @param deviceToConnect	The V1Connection to connect.
	 * @param connectionType	The protocol to use when initiating the connection with deviceToConnect. 
	 * @param callbackMethod	The name of the method inside of the callbackOwner that will receive handle the connection result. Must accept a boolean parameter. Not allowed to be null.
	 * @param callbackOwner		The object that will receive handle the connection result. Not allowed to be null.
	 * 
	 * @return Int constants that indicates the result of the connection attempt. Valid return values are = {
	 *			{@link ValentineClient#RESULT_OF_CONNECTION_EVENT_NO_CONNECTION_ATTEMPT },
	 *			{@link ValentineClient#RESULT_OF_CONNECTION_EVENT_FAILED_NO_DEVICE },
	 *			{@link ValentineClient#RESULT_OF_CONNECTION_EVENT_FAILED_LE_NOT_SUPPORTED },
	 *			{@link ValentineClient#RESULT_OF_CONNECTION_EVENT_FAILED_NO_CALLBACK },
	 *			{@link ValentineClient#RESULT_OF_CONNECTION_EVENT_FAILED_UNSUPPORTED_CONNTYPE },
	 *			{@link ValentineClient#RESULT_OF_CONNECTION_EVENT_CONNECTING },
	 *			{@link ValentineClient#RESULT_OF_CONNECTION_EVENT_CONNECTING_DELAY} }
	 */
	public int startUpAsync(BluetoothDevice deviceToConnect, ConnectionType connectionType, String callbackMethod, Object callbackOwner) {
		// If Bluetooth LE is not supported and connectionType is V1Connection_LE, do not assign the ConnectionType and return false.
		if(ConnectionType.V1Connection_LE.equals(connectionType) && !m_supportBLE) {			
			if(ESPLibraryLogController.LOG_WRITE_ERROR) {
				Log.e(LOG_TAG, "Cannot start a bluetooth connection becuase ConnectionType = " + connectionType.toString() + " is not supported on this device.");
			}			
			return RESULT_OF_CONNECTION_EVENT_FAILED_LE_NOT_SUPPORTED;
		}
		
		if(callbackOwner == null || callbackMethod == null || callbackMethod.isEmpty()) {
			return RESULT_OF_CONNECTION_EVENT_FAILED_NO_CALLBACK;
		}
		isShuttingDown = false;
		m_ConnectionCallbackObject = callbackOwner;
		m_ConnectionCallbackName = callbackMethod;		
		
		// Return false, if the passed in devices is null.
		if(deviceToConnect == null) {
			if(ESPLibraryLogController.LOG_WRITE_ERROR) {
				Log.e(LOG_TAG, "The passed in bluetooth device was null.");
			}
			return RESULT_OF_CONNECTION_EVENT_FAILED_NO_DEVICE;
		}
		stopDemoMode(false);
		
		// Start the callbacks into the local methods
		registerLocalCallbacks();
		
		// Force an unknown V1 type until we get the infDisplayData packet from the V1.
		m_valentineType = Devices.UNKNOWN;
		m_lastV1Type = Devices.UNKNOWN;
		m_v1TypeChangeCnt = 0;
		
		// Do not allow writing to the V1 until the type is known.
		PacketQueue.initOutputQueue(Devices.UNKNOWN, false, true);
		PacketQueue.initInputQueue (true);
		
		if (m_bluetoothDevice == null || !m_bluetoothDevice.equals(deviceToConnect)) {
			// Save the new bluetooth devices. 			
			Editor edit = m_preferences.edit();			
			m_bluetoothDevice = deviceToConnect;
			// Store the last ConnectionType and BluetoothDevices to the SharedPreferences.
			edit.putString("com.valentine.esp.LastBlueToothConnectedDevice", m_bluetoothDevice.getAddress());
			edit.putInt("com.valentine.esp.ConnectionType", connectionType.ordinal());
			edit.commit();
		}
		
		m_valentineESP.registerForConnectEvent("onConnectionEventCallback", this);
		int retval = m_valentineESP.startUpAsync(deviceToConnect, connectionType);
		if (retval == RESULT_OF_CONNECTION_EVENT_CONNECTING)
    	{
    		m_versionCallbackObject.clear();
    		m_versionCallbackFunction.clear();
    		m_serialNumberCallbackObject.clear();
    		m_serialNumberCallbackFunction.clear();
    		//m_infCallbackCallbackData.clear();
    		doSearch();
    		return retval;
    	}
    	
    	return retval;
	}
	
	/**
	 * Callback that will receive the connection event from the ValentineESP object.
	 * **(DO NOT CALL DIRECTLY, SHOULD ONLY BE CALLED VALENTINEESP)**
	 * 
	 * @param isConnected	A flag that indicates if the connection event resulted in a bluetooth connection.
	 * 
	 * @hide
	 */
	public void onConnectionEventCallback(Boolean isConnected) {
		if(m_ConnectionCallbackObject != null && m_ConnectionCallbackName != null && !m_ConnectionCallbackName.isEmpty()) {
			try {
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
	 * Synchronously disconnects from the V1Connection if connected and shuts down the ESP Library.
	 * 
	 * (DO NOT CALL FROM UI THREAD)
	 * @throws RuntimeException if called from the UI (Main) Thread
	 * @deprecated DO NOT USE. THIS METHOD WILL BE REMOVED IN THE NEXT API RELEASE.
	 */
	public void ShutdownSync() {
		if(Thread.currentThread() == Looper.getMainLooper().getThread()){
			throw new RuntimeException("ShutdownSync() will/may block and should not be called on the " + Thread.currentThread().getName() + " thread.");
		}
		
		if (isLibraryInDemoMode()) {
			stopDemoMode(false);
		}
		// Disconnect from the V1Connection.
		m_valentineESP.stopSync();
		
		if(ESPLibraryLogController.LOG_WRITE_VERBOSE){
			Log.v(LOG_TAG, "Unregistering from all Callbacks");
		}
		// Deregisters the Library.
		clearAllCallbacks();
	}
	
	/**
	 * Asynchronously disconnect from the V1Connection if connected and shuts down the ESP Library.
	 * This will effectively shut down the ESP Library.
	 * (Safe to call from the UI thread)
	 * 
	 * @param callbackMethod	The name of the method inside of the callbackOwner that will receive handle the disconnection result. Null is allowed.
	 * @param callbackOwner		The object that will receive handle the disconnection result. Null is allowed.
	 */
	public void ShutdownAsync(String callbackMethod, Object callbackOwner) {
		// Stop the DemoMode processing thread from running.
		if (isLibraryInDemoMode()) {
			stopDemoMode(false);
		}
		
		m_DisconnectionCallbackObject = callbackOwner;
		m_DisconnectionCallbackName = callbackMethod;	
		
		m_valentineESP.registerForDisconnectEvent("onDisconnectionEventCallback", this);
		
		// Disconnect from the V1Connection.
		m_valentineESP.stopAsync();
		
		if(ESPLibraryLogController.LOG_WRITE_VERBOSE){
			Log.v(LOG_TAG, "Unregistering from all Callbacks");
		}
		isShuttingDown = true;
		// Deregister the library's callbacks.
		clearAllCallbacks();
	}
	
	/**
	 * Callback that will receive the connection event from the ValentineESP object.
	 * 
	 * **(DO NOT CALL DIRECTLY, SHOULD ONLY BE CALLED VALENTINEESP)**
	 * @hide
	 */
	public void onDisconnectionEventCallback() {
		if(m_DisconnectionCallbackObject != null && m_DisconnectionCallbackName != null && !m_DisconnectionCallbackName.isEmpty()) {
			try {
				Utilities.doCallback(m_DisconnectionCallbackObject, m_DisconnectionCallbackName, null, null);
			} 
			catch(Exception e) {
				if(ESPLibraryLogController.LOG_WRITE_WARNING ){
					Log.w(LOG_TAG, m_DisconnectionCallbackObject.toString() + " " + m_DisconnectionCallbackName);
				}
			}
		}
		// If the library is shutting down, set the callback objects to null.... PREVENT LEAKS
		if(isShuttingDown){
			m_DisconnectionCallbackObject = null;
			m_DisconnectionCallbackName = null;
		}
	}
	
	/**
	 * Checks to see if the library is in 'Demo' mode.
	 * 
	 * @return True if the library is running in Demo mode otherwise, false.
	 */
	public boolean isLibraryInDemoMode() {
		return m_valentineESP.isInDemoMode();
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
		m_valentineESP.setProtectLegacyMode(val);
	}
		
	/**
	 * Get the current state of Legacy Mode protection provided by the ESP library. Refer to 
	 * setProtectLegacyMode for more information on this feature. 
	 * 
	 * @return The current state of Legacy Mode protection provided by the ESP library.
	 */
	public boolean getProtectLegacyMode ()
	{
		return m_valentineESP.getProtectLegacyMode();
	}	
	
	/** 
	 * Sets up the Valentine One discovery mechanism. This is now done through the infDisplayDataData received from the Valentine One. 
	 */
	private void doSearch()
	{
		m_valentineESP.registerForPacket(PacketId.infDisplayData, this, "doSearchCallback");
	}
	
	/**
	 * This is the Callback registered with the system to determine which of the Valentine One types we have.  This is updated each time we 
	 * get a InfDisplayData packet response.
	 * 
	 * @param _resp The display data to tell us what type of Valentine One we have.
	 */
	public void doSearchCallback(InfDisplayData _resp)
	{
		InfDisplayInfoData data = (InfDisplayInfoData)_resp.getResponseData();
		
		Devices devType = Devices.UNKNOWN;
		
		if (data.getAuxData().getLegacy())
		{
			devType = Devices.VALENTINE1_LEGACY;
		}
		else if (_resp.getOrigin() == Devices.VALENTINE1_WITH_CHECKSUM)
		{
			devType = Devices.VALENTINE1_WITH_CHECKSUM;
		}
		else if (_resp.getOrigin() == Devices.VALENTINE1_WITHOUT_CHECKSUM)
		{
			devType = Devices.VALENTINE1_WITHOUT_CHECKSUM;
		}
		
		if ( devType != m_valentineType ){
			// Possibly change the device type
			if ( devType == m_lastV1Type ){
				// This is the same as the last device type we found last time. This could be a possible V1 type change.
				m_v1TypeChangeCnt ++;
			}
			else{
				// This is the first time we received this V1 type. Initialize the switch variables.
				m_v1TypeChangeCnt = 1;
				m_lastV1Type = devType;
			}
			
			if ( m_v1TypeChangeCnt >= V1_TYPE_SWITCH_THRESHOLD ){
				// Change the V1 type and tell the packet queue what type of V1 to use.
				if( ESPLibraryLogController.LOG_WRITE_INFO ){
					Log.i( LOG_TAG, "Changing V1 type from " + m_valentineType.toString() + " to " + devType.toString() );
				}
				
				m_valentineType = devType;
				PacketQueue.setNewV1Type(m_valentineType);
				
				// Always request the V1connection version whenever we change the V1 type
				getV1connectionVerison();
				
				// Reset the switch variables to prevent a quick switch next time
				m_lastV1Type = Devices.UNKNOWN;
				m_v1TypeChangeCnt = 0;
			}
		}
		else{
			// The V1 type is not changing
			m_lastV1Type = Devices.UNKNOWN;
			m_v1TypeChangeCnt = 0;
		}
		
		if( PacketQueue.getV1Type() != Devices.UNKNOWN ){
			// Use the TS Holdoff bit to hold off or allow requests
			PacketQueue.setHoldoffOutput(data.getAuxData().getTSHoldOff());
		}	
	}
	
	/** 
	 * This registers an object/function combination to be notified when the ESP client stops.  Only one allowed at a time
	 * Requires a function with a Void parameter:  public void function( Void _parameter).
	 *  
	 * @param _stopObject  Object with the function to be called
	 * @param _stopFunction  Function to be called
	 */
	public void registerStopNotification(Object _stopObject, String _stopFunction)
	{
		if ( ESPLibraryLogController.LOG_WRITE_INFO ){
			Log.i ("ValentineClient", "Register stop notifcation for " + _stopObject);
		}
		
		m_stopObject = _stopObject;
		m_stopFunction = _stopFunction;
		
		m_valentineESP.registerForStopNotification(this, "handleStopNotification");
	}
	
	/** 
	 * Removes the registered stop notification callback information.
	 * 
	 * @param _stopObject	The object to deregister for stop notifications.
	 */
	public void deregisterStopNotification(Object _stopObject)
	{
		if ( _stopObject == m_stopObject ){
			// Only deregister if called from the registered object.
			if(ESPLibraryLogController.LOG_WRITE_INFO){
				Log.i("ValentineClient", "Deregistering stop notification");	
			}
			m_stopObject = null;
			m_stopFunction = null;
			m_valentineESP.deregisterForStopNotification();
		}
	}
	
	/** 
	 * This is the call back from the ESP client to call. Don't call this directly.
	 * 
	 * @param _rc	A Boolean value that is always true.
	 */
	public void handleStopNotification(Boolean _rc)
	{
		if ((m_stopObject != null) && (m_stopFunction != null)) {
			Utilities.doCallback(m_stopObject, m_stopFunction, Void.class, null);
		}
	}
		
	/**
	 * Registers a callback to handle InfDisplayInfoData data structures from the Valentine One.  Many can be registered.
	 * Requires a function with a InfDisplayInfoData parameter:  public void function( InfDisplayInfoData _parameter).
	 * 
	 * @param _callbackObject Object with the function to be called.
	 * @param _function Function on the object to be called.
	 */
	public void registerForDisplayData(Object _callbackObject, String _function)
	{
		if(m_infCallbackCallbackData.containsKey(_callbackObject)) {
			m_infCallbackCallbackData.remove(_callbackObject);
		}
		
		m_infCallbackCallbackData.put(_callbackObject, _function);
	}
	
	/** 
	 * Registers a callback to handle an ArrayList of AlertData data structures from the Valentine One.  One can be registered. 
	 * Does not start the flow of the alert data, that requires a call to sendAlertData.
	 * Requires a function with a ArrayList<AlertData> parameter:  public void function( ArrayList<AlertData> _parameter).
	 * 
	 * @param _callbackObject Object with the function to be called.
	 * @param _function Function on the object to be called.
	 */
	public void registerForAlertData(Object _callbackObject, String _function) 
	{
		if (m_getAlertDataMachineMap.containsKey(_callbackObject)) {
			//this callback object was previously registered, de-register it and remove it from the map
			m_getAlertDataMachineMap.get(_callbackObject).stop();
			m_getAlertDataMachineMap.remove(_callbackObject);
		}

		GetAlertData newSubscriber = new GetAlertData(m_valentineESP, _callbackObject, _function);
		m_getAlertDataMachineMap.put(_callbackObject, newSubscriber);
		newSubscriber.start();
	}

	/**
	 * This is the callback from the ESP client to handle InfDisplayData coming from the Valentine One, it converts the response packet
	 * into InfDisplayInfoData data and sends that on to the list of registered functions. 
	 * Do not call this directly.
	 * 
	 * @param _resp The InfDisplayData packet from the Valentine One.
	 */
	public void infDisplayCallback(InfDisplayData _resp)
	{
		Set<Object> keys = m_infCallbackCallbackData.keySet();
	
		for (Object o : keys)
		{
			String function = m_infCallbackCallbackData.get(o);
			try {
				Utilities.doCallback(o, function, String.class, (InfDisplayInfoData)_resp.getResponseData());
			} 
			catch(Exception e) {
				if(ESPLibraryLogController.LOG_WRITE_WARNING ){
					Log.w(LOG_TAG, o.toString() + " " + function);
				}
			}
		}
	}
	
	/**
	 * Removes the callback for the given object so it will no longer receive InfDisplayInfoData data structures to process.
	 * 
	 * @param _source The object which to stop getting InfDisplayInfoData from.
	 */
	public void deregisterForDisplayData(Object _source)
	{
		m_infCallbackCallbackData.remove(_source);

		
		if(ESPLibraryLogController.LOG_WRITE_DEBUG){
			Log.d(LOG_TAG, String.format("Got deregisterForDisplayData requested for %s", _source.toString()));// System.identityHashCode(_source)));
		}
	}
	
	/**
	 * Stops the processing of the alert data from the Valentine One.
	 * 
	 * @param _originalCallbackObject	The object that you would like to unregister for AlertData events.
	 */
	public void deregisterForAlertData(Object _originalCallbackObject)
	{
		if (m_getAlertDataMachineMap.containsKey(_originalCallbackObject) )
		{
			//stop alerts and release the state machine from the map
			m_getAlertDataMachineMap.get(_originalCallbackObject).stop();
			m_getAlertDataMachineMap.remove(_originalCallbackObject);
		}		
	}

	/**
	 * Gets the version of the requested device.  Don't use this one for getting the Valentine One version, use the getV1Version call instead.
	 * Requires a function with a String parameter:  public void function( String _parameter).
	 * 
	 * @param _callbackObject The object that has the function on it to be notified when it has the version of the device.
	 * @param _function The function to call when the version is available.
	 * @param _destination The device to query for its version.
	 */
	public void getVersion(Object _callbackObject, String _function, Devices _destination)
	{
		m_versionCallbackObject.put(_destination, _callbackObject);
		m_versionCallbackFunction.put(_destination, _function);

		RequestVersion packet = new RequestVersion(m_valentineType, _destination);
		if ( !m_valentineESP.isRegisteredForPacket(PacketId.respVersion, this) ){
			// Only register if not already registered
			m_valentineESP.registerForPacket(PacketId.respVersion, this, "getVersionCallback");
		}
		
		m_valentineESP.sendPacket(packet);
	}
	
	/**
	 * Gets the version of the currently connected Valentine One.
	 * Requires a function with a String parameter:  public void function( String _parameter).
	 * 
	 * @param _callbackObject The object that has the function on it to be notified when it has the version of the Valentine One.
	 * @param _function The function to call when the version is available.
	 */
	public void getV1Version(Object _callbackObject, String _function)
	{
		RequestVersion packet;
		if (isLibraryInDemoMode())
		{
			packet = new RequestVersion(Devices.VALENTINE1_WITH_CHECKSUM, Devices.VALENTINE1_WITH_CHECKSUM);
			m_versionCallbackObject.put(Devices.VALENTINE1_WITH_CHECKSUM, _callbackObject);
			m_versionCallbackFunction.put(Devices.VALENTINE1_WITH_CHECKSUM, _function);
		}
		else
		{
			m_versionCallbackObject.put(m_valentineType, _callbackObject);
			m_versionCallbackFunction.put(m_valentineType, _function);
			packet = new RequestVersion(m_valentineType, m_valentineType);
		}
		
		if ( !m_valentineESP.isRegisteredForPacket(PacketId.respVersion, this) ){
			// Only register if not already registered		
			m_valentineESP.registerForPacket(PacketId.respVersion, this, "getVersionCallback");
		}
		m_valentineESP.sendPacket(packet);
	}
	
	/**
	 * Gets the serial number of the requested device.  Don't use this one for getting the Valentine One serial number, 
	 * use the getV1SerialNumber call instead.
	 * Requires a function with a String parameter:  public void function( String _parameter).
	 * 
	 * @param _callbackObject The object that has the function on it to be notified when it has the serial number of the device.
	 * @param _function The function to call when the serial number is available.
	 * @param _destination The device to query for its serial number.
	 */
	public void getSerialNumber(Object _callbackObject, String _function, Devices _destination)
	{
		m_serialNumberCallbackObject.put(_destination, _callbackObject);
		m_serialNumberCallbackFunction.put(_destination, _function);
		
		RequestSerialNumber packet = new RequestSerialNumber(m_valentineType, _destination);
		if ( !m_valentineESP.isRegisteredForPacket(PacketId.respSerialNumber, this) ){
			// Only register if not already registered		
			m_valentineESP.registerForPacket(PacketId.respSerialNumber, this, "getSerialNumberCallback");
		}
		m_valentineESP.sendPacket(packet);
	}
	
	/** 
	 * Gets the serial number of the currently connected Valentine One.
	 * Requires a function with a String parameter:  public void function( String _parameter).
	 * 
	 * @param _callbackObject The object that has the function on it to be notified when it has the serial number of the Valentine One.
	 * @param _function The function to call when the serial number is available.
	 */
	public void getV1SerialNumber(Object _callbackObject, String _function)
	{
		RequestSerialNumber packet;
		if (isLibraryInDemoMode())
		{
			packet = new RequestSerialNumber(Devices.VALENTINE1_WITH_CHECKSUM, Devices.VALENTINE1_WITH_CHECKSUM);
			m_serialNumberCallbackObject.put(Devices.VALENTINE1_WITH_CHECKSUM, _callbackObject);
			m_serialNumberCallbackFunction.put(Devices.VALENTINE1_WITH_CHECKSUM, _function);
		}
		else
		{
			packet = new RequestSerialNumber(m_valentineType, m_valentineType);
			m_serialNumberCallbackObject.put(m_valentineType, _callbackObject);
			m_serialNumberCallbackFunction.put(m_valentineType, _function);
		}
		if ( !m_valentineESP.isRegisteredForPacket(PacketId.respSerialNumber, this) ){
			// Only register if not already registered		
			m_valentineESP.registerForPacket(PacketId.respSerialNumber, this, "getSerialNumberCallback");
		}
		m_valentineESP.sendPacket(packet);
	}
	
	/**
	 * Retrieves the current set of user settings (programming settings) from the Valentine One.
	 * Requires a function with a UserSettings parameter:  public void function( UserSettings _parameter).
	 *  
	 * @param _callbackObject  Object which has the function to be called when the UserSettings is available.
	 * @param _function Function to be called when the UserSettings is available.
	 */
	public void getUserSettings(Object _callbackObject, String _function)
	{
		m_userBytesCallbackObject = _callbackObject;
		m_userBytesCallbackFunction = _function;

		RequestUserBytes packet = new RequestUserBytes(m_valentineType);
		m_valentineESP.registerForPacket(PacketId.respUserBytes, this, "getUserBytesCallback");
		m_valentineESP.sendPacket(packet);
	}
	
	/** 
	 * Writes the given User Settings to the Valentine One.
	 * 
	 * @param _userSettings The user settings to be written to the device.
	 */
	public void writeUserSettings(UserSettings _userSettings)
	{
		RequestWriteUserBytes packet = new RequestWriteUserBytes(_userSettings, m_valentineType);
		m_valentineESP.sendPacket(packet);
	}
	
	/**
	 * Gets the current battery level of the power source the Valentine One is connected to.
	 * Requires a function with a String parameter:  public void function( String _parameter).
	 * 
	 * @param _callbackObject Object that has the function to be called when the battery level is available.
	 * @param _function Function to be called when the battery level is available.
	 */
	public void getBatteryVoltage(Object _callbackObject, String _function)
	{
		m_voltageCallbackObject = _callbackObject;
		m_voltageCallbackFunction = _function;

		RequestBatteryVoltage packet = new RequestBatteryVoltage(m_valentineType);
		m_valentineESP.registerForPacket(PacketId.respBatteryVoltage, this, "getBatteryVoltageCallback");
		m_valentineESP.sendPacket(packet);
	}	
	
	/**
	 * This is the callback from the ESP client to handle the version response from the requested device
	 * and sends this to the requesting object.
	 * Do not call this directly.
	 * 
	 * @param _resp The ResponseVersion packet from the Valentine One.
	 */
	public void localV1connVerCallback(ResponseVersion _resp)
	{
		if ( _resp.getOrigin() == Devices.V1CONNECT && _resp.getOrigin() == Devices.V1CONNECT ){
			m_lastV1ConnVer = (String)_resp.getResponseData();
			m_valentineESP.deregisterForPacket(PacketId.respVersion, this, "localV1connVerCallback");
		}
	}
	
	/**
	 * This is the callback from the ESP client to handle the version response from the requested device
	 * and sends this to the requesting object.
	 * Do not call this directly.
	 * 
	 * @param _resp The ResponseVersion packet from the Valentine One.
	 */
	public void getVersionCallback(ResponseVersion _resp)
	{
		// Cache the V1connection version whenever it is received
		if ( _resp.getOrigin() == Devices.V1CONNECT && _resp.getOrigin() == Devices.V1CONNECT ){
			m_lastV1ConnVer = (String)_resp.getResponseData();
		}
		
		Object callbackObj = m_versionCallbackObject.get(_resp.getOrigin());
		String callbackFunction = m_versionCallbackFunction.get(_resp.getOrigin());
		
		m_versionCallbackObject.remove(_resp.getOrigin());
		m_versionCallbackFunction.remove(_resp.getOrigin());
		
		if (m_versionCallbackObject.size() == 0)
		{
			m_valentineESP.deregisterForPacket(PacketId.respVersion, this);
		}
		
		PacketQueue.removeFromBusyPacketIds(PacketId.reqVersion);
		
		if ( callbackObj != null && callbackFunction != null ){		
			Utilities.doCallback(callbackObj, callbackFunction, String.class, (String)_resp.getResponseData());
		}
		
		// Update the V1 version based frequency lookup object
		if ( _resp.getOrigin() == Devices.VALENTINE1_WITHOUT_CHECKSUM || _resp.getOrigin() == Devices.VALENTINE1_WITH_CHECKSUM ){
			m_settingLookup.setV1Version ( (String)_resp.getResponseData() );
		}
	}
	
	/**
	 * This is the callback from the ESP client to handle the serial number response from the requested device
	 * and sends this to the requesting object.
	 * Do not call this directly.
	 * 
	 * @param _resp The ResponseSerialNumber packet from the Valentine One.
	 */
	public void getSerialNumberCallback(ResponseSerialNumber _resp)
	{
		Object callbackObj = m_serialNumberCallbackObject.get(_resp.getOrigin());
		String callbackFunction = m_serialNumberCallbackFunction.get(_resp.getOrigin());
		
		m_serialNumberCallbackObject.remove(_resp.getOrigin());
		m_serialNumberCallbackFunction.remove(_resp.getOrigin());
		
		if (m_serialNumberCallbackObject.size() == 0)
		{
			m_valentineESP.deregisterForPacket(PacketId.respSerialNumber, this);
		}
		
		PacketQueue.removeFromBusyPacketIds(PacketId.reqSerialNumber);
		
		Utilities.doCallback(callbackObj, callbackFunction, String.class, (String)_resp.getResponseData());
	}
	
	/**
	 * This is the callback from the ESP client to handle the UserBytes response from the Valentine One
	 * and sends this to the requesting object.
	 * Do not call this directly.
	 * 
	 * @param _resp The ResponseUserBytes packet from the Valentine One.
	 */
	public void getUserBytesCallback(ResponseUserBytes _resp)
	{
		m_valentineESP.deregisterForPacket(PacketId.respUserBytes, this);
		
		PacketQueue.removeFromBusyPacketIds(PacketId.reqUserBytes);
		
		Utilities.doCallback(m_userBytesCallbackObject, m_userBytesCallbackFunction, UserSettings.class, (UserSettings)_resp.getResponseData());
	}
	
	/**
	 * This is the callback from the ESP client to handle the Battery Voltage response from the Valentine One
	 * and sends this to the requesting object.
	 * Do not call this directly.
	 * 
	 * @param _resp The ResponseUserBytes packet from the Valentine One.
	 */
	public void getBatteryVoltageCallback(ResponseBatteryVoltage _resp)
	{
		m_valentineESP.deregisterForPacket(PacketId.respBatteryVoltage, this);
		
		PacketQueue.removeFromBusyPacketIds(PacketId.reqBatteryVoltage);
		Utilities.doCallback(m_voltageCallbackObject, m_voltageCallbackFunction, Float.class, (Float)_resp.getResponseData());
	}	
	
	/** 
	 * Mutes the Valentine one.  True turns the muting on.
	 * 
	 * @param _onOff True Mutes the device.
	 */
	public void mute(boolean _onOff)
	{
		if (_onOff)
		{
			RequestMuteOn packet;
			
			packet = new RequestMuteOn(m_valentineType);
			
			m_valentineESP.sendPacket(packet);
		}
		else
		{
			RequestMuteOff packet;
			
			packet = new RequestMuteOff(m_valentineType);
			
			m_valentineESP.sendPacket(packet);
		}
	}
	
	/** 
	 * Turns off and on the main display on the Valentine One.  True is on, false is off.
	 * 
	 * @param _onOff  True is on, false is off/
	 */
	public void turnMainDisplayOnOff(boolean _onOff)
	{
		if (_onOff)
		{
			RequestTurnOnMainDisplay packet = new RequestTurnOnMainDisplay(m_valentineType);
			m_valentineESP.sendPacket(packet);
		}
		else
		{
			RequestTurnOffMainDisplay packet = new RequestTurnOffMainDisplay(m_valentineType);
			m_valentineESP.sendPacket(packet);
		}
	}
	
	/**
	 * Turns off and on the flow of Alert Data from the Valentine One. To process this data, use registerForAlertData
	 * to register a callback to process the data.
	 * 
	 * @param _send True starts the data, false stops the data.
	 */
	public void sendAlertData(boolean _send)
	{
		if (_send)
		{
			sendStartAlertData();
		}
		else
		{
			sendStopAlertData();
		}
	}	
	
	/** 
	 * Sends a RequestFactoryDefault packet to the indicated device.
	 * 
	 * @param _device The device to send a RequestFactoryDefault to.
	 */
	public void doFactoryDefault(Devices _device)
	{
		RequestFactoryDefault packet = new RequestFactoryDefault(m_valentineType, _device);
		m_valentineESP.sendPacket(packet);
	}
	
	//***// Savvy
	
	/**
	 * Requests the status from the connected Savvy device.
	 * Requires a function with a SavvyStatus parameter:  public void function( SavvyStatus _parameter).
	 * 
	 * @param _callbackObject The object which has the function to call when the status is available.
	 * @param _function The function to call when the status is available.
	 */
	public void getSavvyStatus(Object _callbackObject, String _function)
	{
		m_savvyStatusObject = _callbackObject;
		m_savvyStatusFunction = _function;

		RequestSavvyStatus packet = new RequestSavvyStatus(m_valentineType, Devices.SAVVY);
		m_valentineESP.registerForPacket(PacketId.respSavvyStatus, this, "getSavvyStatusCallback");
		m_valentineESP.sendPacket(packet);
	}
	
	/**
	 * Requests the vehicles speed from the connected Savvy device.
	 * Requires a function with a Integer parameter:  public void function( Integer _parameter).
	 * 
	 * @param _callbackObject The object which has the function to call when the speed is available.
	 * @param _function The function to call when the speed is available.
	 */
	public void getVehicleSpeed(Object _callbackObject, String _function)
	{
		m_vehicleSpeedObject = _callbackObject;
		m_vehicleSpeedFunction = _function;

		RequestVehicleSpeed packet = new RequestVehicleSpeed(m_valentineType, Devices.SAVVY);
		m_valentineESP.registerForPacket(PacketId.respVehicleSpeed, this, "getVehicleSpeedCallback");
		m_valentineESP.sendPacket(packet);
	}	
	
	/** 
	 * This is the call back from the ESP client to call to handle the ResponseSavvyStatus packet.
	 *  Do not call directly.
	 *  
	 *  @param _resp	The Savvy Response data received from the V1.
	 */
	public void getSavvyStatusCallback(ResponseSavvyStatus _resp)
	{
		if(ESPLibraryLogController.LOG_WRITE_DEBUG){
			Log.d("Valentine/SavvyStatus", "callback function = " + m_savvyStatusFunction);
		}
		m_valentineESP.deregisterForPacket(PacketId.respSavvyStatus, this);
		PacketQueue.removeFromBusyPacketIds(PacketId.reqSavvyStatus);
		if ((m_savvyStatusObject != null) && (m_savvyStatusFunction != null))
		{
			Utilities.doCallback(m_savvyStatusObject, m_savvyStatusFunction, SavvyStatus.class, (SavvyStatus)_resp.getResponseData());
		}
		
		//m_savvyStatusObject = null;
		//m_savvyStatusFunction = null;
	}
	
	/**
	 * This is the call back from the ESP client to call to handle the ResponseVehicleSpeed packet.
	 * Do not call directly.
	 * 
	 * @param _resp		The Vehicle speed response data received from the V1.
	 */
	public void getVehicleSpeedCallback(ResponseVehicleSpeed _resp)
	{
		m_valentineESP.deregisterForPacket(PacketId.respVehicleSpeed, this);
		PacketQueue.removeFromBusyPacketIds(PacketId.reqVehicleSpeed);
		Utilities.doCallback(m_vehicleSpeedObject, m_vehicleSpeedFunction, Integer.class, (Integer)_resp.getResponseData());
	}
	
	
	/** 
	 * Overrides the thumb wheel on the Savvy to the given speed.
	 * 
	 * @param _speed The speed to set the override to, 0-255.
	 */
	public void setOverrideThumbwheel(byte _speed)
	{
		RequestOverrideThumbwheel packet = new RequestOverrideThumbwheel(m_valentineType, _speed, Devices.SAVVY);
		m_valentineESP.sendPacket(packet);
	}
	
	/** 
	 * Overrides the thumb wheel on the Savvy to the None setting.
	 */
	public void setOverrideThumbwheelToNone()
	{
		RequestOverrideThumbwheel packet = new RequestOverrideThumbwheel(m_valentineType, (byte) 0x00, Devices.SAVVY);
		m_valentineESP.sendPacket(packet);
	}
	
	/**
	 *  Overrides the thumb wheel on the Savvy to the Auto setting.
	 */
	public void setOverrideThumbwheelToAuto()
	{
		RequestOverrideThumbwheel packet = new RequestOverrideThumbwheel(m_valentineType, (byte) 0xff, Devices.SAVVY);
		m_valentineESP.sendPacket(packet);
	}
	
	/**
	 *  Sets the Savvy Mute functionality to on or off.
	 * 
	 * @param _enableMute True enables the functionality, false disables it.
	 */
	public void setSavvyMute(Boolean _enableMute)
	{
		RequestSetSavvyUnmute packet = new RequestSetSavvyUnmute(m_valentineType, _enableMute, Devices.SAVVY);
		m_valentineESP.sendPacket(packet);
	}
	
	//***//Custom Sweeps
	
	/**
	 * Requests current set sweeps on the Valentine One.
	 * Requires a function with a ArrayList<SweepDefinition> parameter:  public void function( ArrayList<SweepDefinition> _parameter).
	 * 
	 * @param _callbackObject The object which has the function to call when the sweeps are available.
	 * @param _function The function to call when the sweeps are available.
	 */
	public void getAllSweeps(Object _callbackObject, String _function)
	{
		// Always clear the cache when reading the sweep data using this method. If the caller does not want the
		// cache cleared they should call the other method.
		getAllSweeps (true, _callbackObject, _function);
	}
	
	/** 
	 * Requests current set sweeps on the Valentine One.
	 * Requires a function with a ArrayList<SweepDefinition> parameter:  public void function( ArrayList<SweepDefinition> _parameter).
	 * 
	 * @param _clearCache if true, the sweep data cache will be cleared before the sweep data is read.
	 * @param _callbackObject The object which has the function to call when the sweeps are available.
	 * @param _function The function to call when the sweeps are available.
	 */
	public void getAllSweeps(boolean _clearCache, Object _callbackObject, String _function)
	{
		// This method does not provide an error callback for handling sweep read errors
		m_getAllSweepsMachine = new GetAllSweeps(_clearCache, m_valentineType, m_valentineESP, _callbackObject, _function, null, null);
		m_getAllSweepsMachine.getSweeps();
	}
	
	/**
	 * This method will force the stop the sweep reading state machine.
	 */
	public void abortSweepRequest ()
	{
		if ( m_getAllSweepsMachine != null ){
			m_getAllSweepsMachine.abort();
		}
	}
	
	/** 
	 * Write the supplied custom sweep definitions to the Valentine One.  This does not do any checking of the definitions before 
	 * writing them to the device.
	 * Both the success callback and the error call back require a function with a String parameter:  public void function( String _parameter).
	 * 
	 * @param _definitions A list of definitions for the sweeps.
	 * @param _callbackObject The object that has the function to call when the sweeps are written correctly.
	 * @param _function The function to call on success.
	 * @param _errorObject The object that has the function to call when the sweeps writing fails.
	 * @param _errorFunction The function to call on failure.
	 */
	public void setCustomSweeps(ArrayList<SweepDefinition> _definitions, Object _callbackObject, String _function, Object _errorObject, String _errorFunction)
	{
		m_writeCustomSweepsMachine = new WriteCustomSweeps(_definitions, m_valentineType, m_valentineESP, _callbackObject, _function, _errorObject, _errorFunction);
		m_writeCustomSweepsMachine.Start();
	}
	
	/**
	 * Requests the current sweep sections from the Valentine One.
	 * Requires a function with a ArrayList<SweepSections> parameter:  public void function( ArrayList<SweepSections> _parameter).
	 * 
	 * @param _callbackObject Object that has the function to call when the sweep sections are available.
	 * @param _function Function to call when the sections are available.
	 */
	public void getSweepSections(Object _callbackObject, String _function)
	{
		m_getSweepsObject = _callbackObject;
		m_getSweepsFunction = _function;
		
		RequestSweepSections packet = new RequestSweepSections(m_valentineType);
		m_valentineESP.registerForPacket(PacketId.respSweepSections, this, "getSweepSectionsCallback");
		m_valentineESP.sendPacket(packet);		
	}
	
	/**
	 * Tells the Valentine One to set the current Sweeps to the default ones. 
	 */
	public void setSweepsToDefault()
	{
		RequestSetSweepsToDefault packet = new RequestSetSweepsToDefault(m_valentineType);
		m_valentineESP.sendPacket(packet);
	}
	
	//**//CustomSweepCallbacks
	/** 
	 * This is the call back from the ESP client to call to handle the RequestSweepSections packets.
	 * Do not call directly. 
	 * 
	 * @param _resp		The Sweep Section response data received from the V1.
	 */
	public void getSweepSectionsCallback(ResponseSweepSections _resp)
	{
		//get sweep sections back
		m_valentineESP.deregisterForPacket(PacketId.respSweepSections, this);
		SweepSection[] sections = (SweepSection[]) _resp.getResponseData();

		ArrayList<SweepSection> temp = new ArrayList<SweepSection>();
		for (int i = 0; i < sections.length; i++)
		{
			temp.add(sections[i]);
		}
		
		setCachedSweepSections(temp);
		
		PacketQueue.removeFromBusyPacketIds(PacketId.reqSweepSections);
		
		Utilities.doCallback(m_getSweepsObject, m_getSweepsFunction, SweepSection[].class, (SweepSection[])sections);
	}
	
	/** 
	 * Starts the ESP Library in demo mode. This will disconnect the client from a connected V1Connection.  
	 * 
	 * @param _demoData 	The demo data of bytes from packets, comments, and display messages read into a single string from a file.
	 * @param _repeat 		Repeat the data from the beginning when it reaches the end of the data.
	 */
	public void startDemoMode(String _demoData, boolean _repeat)
	{
		// Start the callbacks into the local methods whenever we start demo mode.
		registerLocalCallbacks();
				
		m_valentineESP.startDemo(_demoData, _repeat);
		
		// Turn on demo mode in the frequency lookup object.		
		m_settingLookup.setDemoMode(true);
	}
	
	/** 
	 * Exit's the ESP Library from Demo mode, with the option to attempt a connection with the last V1Connection.
	 * 
	 * @param _restart 		If true, the ESP Library will attempt to connect with the last V1Connection once demo Mode is exited.
	 * The connection attempt will be performed synchronously, not safe to perform on the UI thread.
	 * 
	 * @return	Returns true, if @param _restart is true, the return value will indicate if a Bluetooth connection was established with the V1Connection.
	 */
	public boolean stopDemoMode( boolean _restart)
	{
		boolean rc = m_valentineESP.stopDemo( _restart);
		if ( _restart)
		{
			if (rc)
			{
				doSearch();
			}
		}		
		
		// Turn off demo mode in the frequency lookup object.
		m_settingLookup.setDemoMode(false);
		
		return rc;
	}
	
	/**
	 * Registers a function to handle notification messages from the Demo mode data playback.
	 * Requires a function with a String parameter:  public void function( String _parameter).
	 * 
	 * Only one Notification callback is allowed at a time, registering a second will overwrite the first.
	 * 
	 * @param _notificationObject The object that handles the notification and has the function on it.
	 * @param _function The function to be called.
	 */
	public void registerNotificationFunction(Object _notificationObject, String _function)
	{
		m_notificationObject = _notificationObject;
		m_notificationFunction = _function;
	}
	
	/** 
	 * Removes the notification callback data.
	 */
	public void deregisterNotificationFunction()
	{
		m_notificationObject = null;
		m_notificationFunction = null;
	}
	
	/**
	 * Performs the notification callback to the registered notification object.
	 * This should never be called from outside of the ESP Library.
	 * 
	 * @param _notification The notification to be handled by the notification object.
	 */
	public void doNotification(String _notification)
	{
		if ((m_notificationObject != null) && (m_notificationFunction != null))
		{
			Utilities.doCallback(m_notificationObject, m_notificationFunction, String.class, _notification);
		}
	}	
	
	//**// Mode Change
	
	/** 
	 * Changes the logic mode of the Valentine One.
	 * 
	 * @param _mode The value of the mode you want to set the valentine one to.
	 * <pre>
	 * Value    Mode
	 *          US                      Euro Non Custom Sweeps      Euro Custom Sweeps 
	 * 1        All Bogeys              K & Ka(Photo)               K & Ka Custom Sweeps
	 * 2        Logic Mode              Ka(Photo Only)              Ka Custom Sweeps
	 * 3        Advanced Logic Mode     n/a                         n/a
	 * </pre>
	 */
	public void changeMode(byte _mode)
	{
		RequestChangeMode packet = new RequestChangeMode(_mode, m_valentineType);
		m_valentineESP.sendPacket(packet);
	}
	
	/**
	 * Utility function that will clear all callbacks for the Request Version packets.  This is to clear outstanding
	 * callbacks if a device is not connected when an activity is dismissed. 
	 */
	public void clearVersionCallbacks()
	{
		m_valentineESP.clearCallbacks( PacketId.respVersion);
	}
	
	/**
	 * Utility function that will clear all callbacks for all packets types. This is to clear outstanding
	 * callbacks if a device is not connected when an activity is dismissed.
	 * 
	 */
	public void clearAllCallbacks()
	{
		m_valentineESP.clearAllCallbacks();
		
		// We do not need to call stop on the alert data callbacks because the call to 
		// m_valentineESP.clearAllCallbacks() will do that for us.
		m_getAlertDataMachineMap.clear();
		
		m_infCallbackCallbackData.clear();		
		m_versionCallbackObject.clear();
		m_versionCallbackFunction.clear();
		m_serialNumberCallbackObject.clear();
		m_serialNumberCallbackFunction.clear();
		
		m_userBytesCallbackObject = null;
		m_userBytesCallbackFunction = null;
		m_voltageCallbackObject = null;
		m_voltageCallbackFunction = null;
		m_savvyStatusObject = null;
		m_savvyStatusFunction = null;
		m_vehicleSpeedObject = null;
		m_vehicleSpeedFunction = null;
		m_getSweepsObject = null;
		m_getSweepsFunction = null;
		m_getMaxSweepIndexObject = null;
		m_getMaxSweepIndexFunction = null;
		
		m_notificationObject = null;
		m_notificationFunction = null;

		m_stopObject = null;
		m_stopFunction = null;
		
		m_unsupportedDeviceObject = null;
		m_unsupportedDeviceFunction = null;
		
		m_ConnectionCallbackObject = null;
		m_ConnectionCallbackName = null;

		m_unsupportedPacketObject = null;
		m_unsupportedPacketFunction = null;
		m_requestNotProcessedObject = null;
		m_requestNotProcessedFunction = null;
		m_dataErrorObject = null;
		m_dataErrorFunction = null;
		m_dataErrorObjectRaw = null;
		m_dataErrorFunctionRaw = null;
	}
	
	/** 
	 * Register a function to be notified if the ESP client has not received any data from the Valentine One after X seconds.
	 * Example case of this being called, the valentine one has been turned off for more than X seconds.
	 * Requires a function with a String parameter:  public void function( String _parameter).
	 *  
	 * Only one Notification callback is allowed at a time, registering a second will overwrite the first.
	 *  
	 * @param _owner The object with the function to be notified when there is no data from the Valentine One.
	 * @param _function The function to call.
	 */
	public void registerNoDataCallback(Object _owner, String _function)
	{
		m_valentineESP.registerForNoDataNotification(_owner, _function);
	}
	
	/**
	 * Removes the no data notification callback.
	 */
	public void deregisterNoDataCallback()
	{
		m_valentineESP.deregisterForNoDataNotification();
	}

	/**
	 * Sets the callback data for when the demo data processing finds a User Settings packet in it. 
	 * Requires a function with a UserSettings parameter:  public void function( UserSettings _parameter).
	 * 
	 * @param _owner Object to be notified when there is a demo data user settings object in the demo data.
	 * @param _function Function to be called.
	 */
	public void setDemoConfigurationCallbackData(Object _owner, String _function)
	{
		m_valentineESP.setDemoConfigurationCallbackData(_owner, _function);
	}
	
	/**
	 * Returns the current UserSettings from the demo data outside of the callback. 
	 * @return the demo data user settings.
	 */
	public UserSettings getDemoUserSettings()
	{
		return m_valentineESP.getDemoData().getUserSettings();
	}
	
	/**
	 * The callback for the when the ESP client tries to connect to the Valentine One with an unsupported phone.
	 * Requires a function with a String parameter:  public void function( String _parameter).
	 * 
	 * @param _obj The object to be notified when there is an unsupported phone.
	 * @param _function The function to be called.
	 */
	public void setUnsupportedDeviceCallback(Object _obj, String _function)
	{
		m_unsupportedDeviceObject = _obj;
		m_unsupportedDeviceFunction = _function;
	}
	
	/**
	 * Does the notification when there is a unsupported device detected.
	 * 
	 * @param _error Error to pass to the notification object.
	 */
	public void unsupportedDeviceCallback(String _error)
	{
		if ((m_unsupportedDeviceObject != null) && (m_unsupportedDeviceFunction != null))
		{
			Utilities.doCallback(m_unsupportedDeviceObject, m_unsupportedDeviceFunction, String.class, _error);
		}
	}
	
	/**
	 * Sets the callback object to be notified when the Valentine One sends a Unsupported packet to the client.
	 * Requires a function with a Integer parameter: public void function( Integer _parameter).
	 * 
	 * @param _obj The object to be notified when there is an UnsupportedPacket sent.
	 * @param _function The function to be called.
	 */
	public void registerUnsupportedPacketCallback(Object _obj, String _function)
	{
		m_unsupportedPacketObject = _obj;
		m_unsupportedPacketFunction = _function;
	}
	
	/** 
	 * Does the notification when there is a unsupported packet detected.
	 * 
	 * @param _error Error to pass to the notification object.
	 */
	public void unsupportedPacketCallback(ResponseUnsupported _error)
	{
		if ((m_unsupportedPacketObject != null) && (m_unsupportedPacketFunction != null))
		{
			Utilities.doCallback(m_unsupportedPacketObject, m_unsupportedPacketFunction, Integer.class, _error.getResponseData());
		}
	}
	
	/** 
	 * Sets the callback object to be notified when the Valentine One sends a RequestNotProcessed packet to the client.
	 * Requires a function with a Integer parameter:  public void function( Integer _parameter).
	 * 
	 * @param _obj The object to be notified when there is an RequestNotProcessed object
	 * @param _function The function to be called
	 */
	public void registerRequestNotProcessedCallback(Object _obj, String _function)
	{
		m_requestNotProcessedObject = _obj;
		m_requestNotProcessedFunction = _function;
	}
	
	/** 
	 * Does the notification when the Valentine One sends a RequestNotProcessed packet.
	 * 
	 * @param _error Error to pass to the notification object.
	 */
	public void RequestNotProcessedCallback(ResponseRequestNotProcessed _error)
	{
		if ((m_requestNotProcessedObject != null) && (m_requestNotProcessedFunction != null))
		{
			Utilities.doCallback(m_requestNotProcessedObject, m_requestNotProcessedFunction, Integer.class, _error.getResponseData());
		}
	}
	
	/**
	 * Sets the callback object to be notified when the Valentine One sends a DataError packet to the client.
	 * Requires a function with a Integer parameter:  public void function( Integer _parameter).
	 * 
	 * @param _obj The object to be notified when there is an DataError packet.
	 * @param _function The function to be called.
	 */
	public void registerDataErrorCallback(Object _obj, String _function)
	{
		m_dataErrorObject = _obj;
		m_dataErrorFunction = _function;
	}
	
	/**
	 * Sets the callback object to be notified when the Valentine One sends a DataError packet to the client.
	 * Requires a function with a ResponseDataError parameter:  public void function( String _parameter).
	 * This sends the raw packet to the app to handle.
	 * 
	 * @param _obj The object to be notified when there is an DataError packet.
	 * @param _function The function to be called.
	 */
	public void registerDataErrorRawCallback(Object _obj, String _function)
	{
		m_dataErrorObjectRaw = _obj;
		m_dataErrorFunctionRaw = _function;
	}
	
	/**
	 *  Performs a callback to the object registered for data errors.
	 *  This should never be called from outside of the ESP Library.
	 *  
	 * @param _error Error to pass to the notification object.
	 */
	public void dataErrorCallback(ResponseDataError _error)
	{
		if ((m_dataErrorObject != null) && (m_dataErrorFunction != null))
		{
			Utilities.doCallback(m_dataErrorObject, m_dataErrorFunction, Integer.class, _error);
		}
		if ((m_dataErrorObjectRaw != null) && (m_dataErrorFunctionRaw != null))
		{
			Utilities.doCallback(m_dataErrorObjectRaw, m_dataErrorFunctionRaw, ResponseDataError.class, _error);
		}
		
	}
	
	/** 
	 * Gets the max sweep index from the Valentine One.
	 * Requires a function with a Integer parameter:  public void function( Integer _parameter).
	 * 
	 * @param _obj The object to be notified when the max sweep index has been retrieved.
	 * @param _function The function to be called.
	 */
	public void getMaxSweepIndex(Object _obj, String _function)
	{
		m_getMaxSweepIndexObject = _obj;
		m_getMaxSweepIndexFunction = _function;
		
		RequestMaxSweepIndex packet = new RequestMaxSweepIndex(m_valentineType);
		m_valentineESP.registerForPacket(PacketId.respMaxSweepIndex, this, "maxSweepIndexCallback");
		m_valentineESP.sendPacket(packet);		
	}
	
	/** 
	 * Performs a callback to the object registered for Max Sweep Index data.
	 * This should never be called from outside of the ESP Library.
	 * 
	 * @param _maxSweep The maximum number of Sweeps.
	 */
	public void maxSweepIndexCallback(ResponseMaxSweepIndex _maxSweep)
	{
		m_valentineESP.deregisterForPacket(PacketId.respMaxSweepIndex, this);
		PacketQueue.removeFromBusyPacketIds(PacketId.reqMaxSweepIndex);
	
		m_maxSweepIndex = (Integer)_maxSweep.getResponseData();
		
		Utilities.doCallback(m_getMaxSweepIndexObject, m_getMaxSweepIndexFunction, Integer.class, _maxSweep.getResponseData());
	}

	/**
	 * Send a request to the V1 to start sending alert data.
	 */
	private void sendStartAlertData() {
		if(ESPLibraryLogController.LOG_WRITE_DEBUG){
			Log.d(LOG_TAG, "Sending Start Alert Data Packet");
		}
		RequestStartAlertData packet = new RequestStartAlertData(m_valentineType);
		m_valentineESP.sendPacket(packet);
	}

	/**
	 * Send a request to the V1 to stop sending alert data.
	 */
	private void sendStopAlertData() {
		if(ESPLibraryLogController.LOG_WRITE_DEBUG){
			Log.d(LOG_TAG, "Sending Stop Alert Data Packet");
		}
		RequestStopAlertData packet = new RequestStopAlertData(m_valentineType);
		m_valentineESP.sendPacket(packet);
	}
	
	/**
	 * This method will request the V1connection version and caches it.
	 */
	private void getV1connectionVerison ()
	{
		RequestVersion packet = new RequestVersion(m_valentineType, Devices.V1CONNECT);
		// Only register if not already registered
		m_valentineESP.registerForPacket(PacketId.respVersion, this, "localV1connVerCallback");
		// Write the request packet to the V1.
		m_valentineESP.sendPacket(packet);
	}
	
	/**
	 * This method returns the {@link BluetoothSocket} of the current Bluetooth connection (IF SUPPORTED).
	 * 
	 * @return  Returns the {@link BluetoothSocket} from the current Bluetooth connection if available, otherwise, null.
	 * (For Bluetooth LE connections, this method will always returns null)
	 * 
	 * @deprecated DO NOT USE. THIS METHOD WILL BE REMOVED IN THE NEXT API RELEASE.
	 */
	BluetoothSocket getSocket() {
		return m_valentineESP.getSocket();
	}
	
	/**
	 * Pass through method to the ValentineESP object to determine if the object passed in is registered for
	 * the packet type passed in.
	 * 
	 * @param _type - The packet type to look for.
	 * @param _object - The object to look for.
	 * 
	 * @return true if the object passed in is registered for the packet type passed in, else false.
	 */
	public boolean isRegisteredForPacket (PacketId _type, Object _object)
	{
		return m_valentineESP.isRegisteredForPacket (_type, _object);
	}
	
	/**
	 * Pass through method to the ValentineESP object to register for a packet.
	 * 
	 * @param _type - The packet type to register for.
	 * @param _callBackObject - The object to register.
	 * @param _method - The method to register.
	 * 
	 * @return true if the object passed in is registered for the packet type passed in, else false.
	 */
	public void registerForPacket(PacketId _type, Object _callBackObject, String _method)
	{
		m_valentineESP.registerForPacket(_type, _callBackObject, _method);
	}	
}
