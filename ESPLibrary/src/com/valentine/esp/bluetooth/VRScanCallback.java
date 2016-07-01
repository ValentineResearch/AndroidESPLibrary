/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.bluetooth;

/**
 * An abstract class that should be implemented to receive discovered BluetoothDevices, notifications that a discovery process has completed.
 * 
 */
public abstract class VRScanCallback {
	/**
	 * This function is called when a BluetoothDevices is discovered.
	 * 
	 * Only V1Connection {@link BluetoothDevice}s are returned.
	 * 
	 * @param bluetoothDeviceBundle	A bundle consisting of a discovered BluetoothDevice, rssi and a human friendly name for the device.
	 * @param connectionType	The connectionType of the scanning process.
	 */
	public abstract void onDeviceFound(BluetoothDeviceBundle bluetoothDeviceBundle, ConnectionType connectionType);
	/**
	 * This function is called once the scanning process completes.
	 * @param connectionType	The connectionType of the scanning process.
	 */
	public abstract void onScanComplete(ConnectionType connectionType);
}
