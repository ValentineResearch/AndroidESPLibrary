# **ESP Library v3.0 – June 30, 2016** 
* Added support for Bluetooth Low Energy (BLE).

    WARNING – Android ESP Library developers should encourage their users not to use the Android device’s Bluetooth Settings screen to pair with the V1connection LE. All connections to the V1connection LE should be done through the ESP library. This is considered best practice for all versions of V1connection LE, but it is especially important for units built before March 8, 2016. On those devices, using that screen will cause a fatal firmware conflict. V1connection LEs built on or after March 8, 2016 will have a Bluetooth device name with the format “V1C-XXXX” (XXXX will be a set of numbers and letters specific to your device) and firmware version L1.0010 or higher. V1connection LEs built before March 8, 2016 will have a Bluetooth device name of “V1connection LE” and firmware version L1.0007 or lower.
    * Use the ValentineClient.checkBluetoothLESupport method to determine if the Android device will support BLE.
    * Use the ValentineClient.getConnectionType method to determine what connection type is currently being used by the library.
    * In addition to the notes below, refer to the Hello V1 App example for usage.
    * Added a button to allow the user to select the search type when using the library’s ListBluetoothDevice screen.  The ListBluetoothScreen has been improved to show the RSSI for the found devices and to remove devices from the list if they are not seen for 30 seconds.
    * Refactored the library’s Bluetooth implementation to allow for easily switching between SPP and BLE. 
* Added support for ESP Specification 3.003, which is being released simultaneously with library version 3.0.
    * Added the reqDefaultSweepDefinitions and respDefaultSweepDefintions packets to read the default custom sweeps from the Valentine One (V1). This feature is only available on V1s running firmware 3.8952 and higher.
* Added the ConnectionType enum for indicating the connection type the ESPLibrary should use.  The enum value ConnectionType.V1Connection refers to SPP connections and the enum value ConnectionType.V1Connection LE refers to Bluetooth Low energy connections.
* Added additional support to hold off sending packets when the V1 is not accepting ESP packets. 
* ValentineClient interface changes
    * Added the ability to have the Bluetooth connection process run asynchronously instead of making it a blocking operation. The startUp method has been deprecated and renamed to startUpSync to indicate that it is a blocking method. It is recommended that the startUpAsync method be used for connecting to BLE and SPP devices.
    * Added the ability to have the Bluetooth connection close process run asynchronously instead of making it a blocking operation. The shutdown method has been deprecated and renamed to ShutdownSync to indicate that it is a blocking method. It is recommended that the ShutdownUpAsync method be used for disconnecting from BLE and SPP devices.
    * Added two new methods to allow for discovering SPP and BLE devices, scanForDevices and stopScanningForDevice.
    * Added the method hasPreviousDevice, to indicate if there is previously connected device.
    * Added the method clearPreviousDevice to remove the previous device. Safe to call multiple times. 
* Added VRScanCallback to handle reporting SPP and BLE scan results. 
* Made internal library changes to handle multiple Bluetooth protocols. Typical implementations will not use these interfaces.
    * Fixed a bug that was causing an incorrect return value from ESPPacket.isSamePacket.Modified ESPPacket interface to use BLE or SPP.
    * Removed the checksum from the payload data stored in the ESPPacket object.
    * Added the class BluetoothDeviceBundle. It contains the ConnectionType of the BluetoothDevice, either V1Connection or V1Connection LE. It also contains the RSSI value of BluetoothDevice and discovery time in milliseconds.
    * Made major changes to the ValentineESP object to support multiple Bluetooth protocols and to act as an intermediary between the ValentineClient and the VR_BluetoothWrapper objects.

# **ESPLibrary v2.0 – January 27, 2015**
* Added the ESPLogController class to control how log entries are made.
* Renamed the reqDefaultSweeps packet to reqSetSweepsToDefault.
* Added methods to the AlertData object to consolidate parsing of the direction and number of LEDs for each alert. Also added a SignalDirection enumeration to the AlertData object.
* Added a copy constructor, clear method and isEqual method to the AuxilaryData, BandAndArrowIndicatorData , SignalStrengthData and BogeyCounterData objects.
* Added a copy constructor and clear methods to the InfDisplayInfoData object. Also added a method called isActiveAlerts to the InfDisplayInfoData object to determine if there are any active alerts on the V1 front panel.
* Added set methods to the SweepDefinitionIndex object to allow changing the sweep index and the number of sweep sections.
* The SweepSections object was renamed SweepSection to reflect the fact that it only represents a single sweep section.
* Removed the float representation of a sweep section from the SweepSection object.
* Changed the logic inside of UserSettings. setMuteVolumeAsBoolean and UserSettings. setPostMuteBogeyLockVolumeAsBoolean to more accurately reflect the V1 settings.
* Renamed the RequestDefaultSweeps packet to RequestDefaultSweepDefinitions.
* Added destination and timestamp initialization to all response packets.
* Added getter and setter methods for manipulating the V1 type used in an ESPPacket. This change coincides with the way the destination and V1 type are handled inside the ESPPacket object.
* Override the ESPPacket.toString method to return a space delimited string of hex bytes.
* Modified the ESPPacke.makeFromBuffer method so it is better at detecting and handling corrupted packets.
* Removed the destination from the GetAlertData constructor.
* Renamed GetAlerts.getAlerts to GetAlerts.start.
* Added error callback parameters to the GetAllSweeps constructor.
* Removed GetAllSweeps.doSweepsCall. The GetAllSweeps.getSweeps method should be used instead. Also added GetAllSweeps.abort to abort a sweep read. 
* Removed getSweepSectionsCallback, getNumberOfSweepsCallback and getSweepDefintionsCallback from WriteCustomSweeps.
* Modified the ESPReaderThread object to clear the packet queue before starting the read loop. 
* Added a timeout parameter to the ESPReaderThread constructor.
* Improved the logic for resending failed packets.
* Added checks to prevent sending duplicate packets.
* Removed the CustomSweepChecker object.
* Added the Range object.
* Added the V1VersionSettingLookup object to provide the default frequencies in cases where the app does not know what frequency to use. This object will also be used to provide information about version specific V1 settings. For example, the V1VersionSettingLookup object can be used to determine if the currently attached V1 should have TMF & Junk-K Fighter turned on or off by default. The ValentineClient will instantiate a V1VersionSettingLookup instance and will update the version every time the version number is read. Applications are expected to access the V1VersionSettingLookup object via the ValentineClient helper methods.
* Removed the PacketQueue.clearBusyFlags method.
* Added the PacketQueue. initInputQueue method which should be called before packets are added to the queue.
* Made the following modifications to the ValentineClient object:
    * The ValentineClient will now accept a timeout value in its constructor. The timeout value specifies how long the DataReaderThread will wait without receiving data before it reports a V1 data loss.
    * Changed how the library selects the V1 type to prevent accidentally switching between V1 types.
    * The method getCachedSweepSections, returns either cached sweep sections from the V1 or default values.
    * The method getCachedMaxSweepIndex was added to the class. This method returns the last known max sweep index for the custom sweeps.
    * The method clearSweepCache was added to the class. This method will clear the cache of sweep section information read from the V1.
    * The method getDevice was added to the class. This method will gets the current Bluetooth Device.
    * The method StartUp registers for local callbacks before attempting to connect.
    * The method Shutdown clears all local callbacks when fired.
    * Added the isLibraryInDemoMode  method to determine if the library is in Demo mode was added called, isLibraryInDemoMode().
    * Functionality was added to control whether or not the DataWriterThread is going to skip writing ESP packets when the V1 mode selection is Legacy Mode. It is recommended that Legacy Mode protection is always enabled.
    * Checks have been added to prevent duplicate callback registration for various parts of the ESP functionality.
    * Allow muting of the V1 in all communication modes.
    * The method getSavvyStatusCallback no longer sets the callback object to null once its fired.
    * A second getAllSweeps has been added that accepts a boolean flag to clear the previous custom sweeps.
    * A method called abortSweepRequest has been added that will stop the sweep reading state machine from finishing.
    * The method setDefaultSweeps has been renamed to setSweepsToDefault.
    * The method clearAllCallbacks was added to help clear all callbacks for all packets in the library.
    * The method registerUnsupportedPacketCallback was added to the class. This method sets the callback object to be notified when the Valentine One sends a Unsupported packet to the client.
    * The method unsupportedPacketCallback was added to the class.
    * The method registerRequestNotProcessedCallback was added to the class. This method sets the callback object to be notified when the Valentine One sends a RequestNotProcessed packet to the client.
    * The method registerDataErrorCallback was added to the class. This method sets the callback object to be notified when the Valentine One sends a DataError response packet to the client. Use this method if you are only interested in getting the error code from the packet.
    * The method registerDataErrorRawCallback was added to the class. Sets the callback object to be notified when the Valentine One sends a DataError packet to the client. Use this method if you want the entire ResponseDataError packet.
    * The method getMaxSweepIndex was added to the class.
    * The methods sendStartAlertData and sendStopAlertData were added to the class to help control the V1 alert data output.
    * The method getSocket was added to the class as a convenience method to get the current Bluetooth connection socket from the valentineESP object.
    * The method registerForPacket was added as a pass through to register a packet with the ValentineESP object.
* Added a timeout parameter to the ValentineESP object. The timeout value specifies how long the DataReaderThread will wait without receiving data before it reports a V1 data loss.
* Added the ValentineESP.setLockedPacket method. This method will set the locked packet type for this object. A packet type that is locked cannot have any new callbacks registered and it cannot have any existing callbacks deregistered. This is needed to prevent modifying the array list while it is being traversed.
* Removed the getStatus startupDemoMode and startProcessingDemoMode methods from ValentineESP.
* The ValentineESP.doCallback method has been renamed and made private.
* Added ValentineESP.clearAllCallbacks. This method can be used to clear callbacks when pausing or exiting the app. This method is typically accessed through ValentineClient.clearAllCallbacks.
* Added protection from writing ESP bytes while in Legacy Mode.
* Fixed miscellaneous bugs as well as code inconsistencies and inefficiencies.

# **ESPLibrary v1.0 – April 4, 2013**
* Initial ESP Library release.

