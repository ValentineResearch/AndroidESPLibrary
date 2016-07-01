/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.bluetooth;

/**
 * This enum list all of the Bluetooth connection types the library supports.
 * @author jdavis
 *
 */
public enum ConnectionType {
	UNKNOWN(0, "UNKNOWN"),
	/**
	 * The V1Connection enum represents a Bluetooth SPP connection.
	 */
	V1Connection(1, "V1Connection"),
	/**
	 * The V1Connection enum represents a Bluetooth LE connection.
	 */
	V1Connection_LE(2, "V1Connection LE");
	
	private final int mValue;
	private final String mName;
	
	private ConnectionType(int value, String name) {
		this.mValue = value;
		this.mName = name;
	}	
		
	@Override
	public String toString() {
		return mName;
	}


	public int getValue() {
		return mValue;
	}	
}
