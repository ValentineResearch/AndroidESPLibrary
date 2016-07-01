/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.valentine.esp.PacketQueue;
import com.valentine.esp.packets.ESPPacket;
/**
 * Interface to be implemented by classes that want to function as a Bluetooth Wrapper,
 * ex. {@link VR_BluetoothLEWrapper}, and {@link VR_BluetoothSPPWrapper}.
 */
public interface IVR_BluetoothWrapper {
	/**
	 * Synchronously attempts a Bluetooth connection.
	 *  
	 * <br><br> DO NOT CALL ON THE UI THREAD.
	 * 
	 * @return True if connection was established, false otherwise.
	 */
	boolean		startSync();
	
	/**
	 * Asynchronously attempts a Bluetooth connection.
	 * 
	 * @return Int indicating the result of the connection attempt. Valid return values = {
	 *	ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_NO_DEVICE,
	 *	ValentineClient.RESULT_OF_CONNECTION_EVENT_NO_CONNECTION_ATTEMPT,
	 *	ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_LE_NOT_SUPPORTED,
	 *	ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_NO_CALLBACK,
	 *	ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_UNSUPPORTED_CONNTYPE,
	 *	ValentineClient.RESULT_OF_CONNECTION_EVENT_CONNECTING,
	 *	ValentineClient.RESULT_OF_CONNECTION_EVENT_CONNECTING_DELAY }
	 */
	int		startAsync();
	
	/**
	 * Synchronously attempts to close a Bluetooth connection.
	 * 
	 * @return True if connection was terminated, false otherwise.
	 */
	boolean 	stopSync();
	/**
	 * Asynchronously attempts to close a Bluetooth connection. 
	 * 
	 */
	void 		stopAsync();
	
	/**
	 * Returns if there is a Bluetooth connection.
	 * 
	 * @return	True if there if connected to a V1Connection false, otherwise.
	 */
	boolean 	isConnected();
	
	/**
	 * Send a {@link ESPPacket} to the {@link PacketQueue}, to be written to the {@link BluetoothDevice}. 
	 * @param packet	The {@link ESPPacket} to be queued.
	 */
	void 		sendPacket(ESPPacket packet);
}
