/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * A Wrapper to bundle a {@link BluetoothDevice}, and it's RSSI value.
 * 
 * @author jdavis
 */
public class BluetoothDeviceBundle implements Parcelable {
	
	private final BluetoothDevice 	mDevice;
	
	// The default value meaning there is no RSSI value.
	private int 					mRssi = 0;
	// The last time the BluetoothDevice was discovered.
	private long 					mTimeDiscovered;
	
	private boolean 				mPaired = false;
	private ConnectionType			mDeviceType;

	/**
	 * Create a BluetoothDeviceBundle that holds a {@link BluetoothDevice}, an RSSI (Received signal strength indication) value, and discovery time.
	 * 
	 * @param device			{@link BluetoothDevice} to store. Not allowed to be null.
	 * @param rssi				The last RSSI value of the {@link BluetoothDevice}. Can be 0.
	 * @param timeDiscovered	The time of discovery of the {@link BluetoothDevice}. Can be 0.
	 * @param deviceType		The connection type of the Bluetooth discovery.
	 */
	public BluetoothDeviceBundle(BluetoothDevice device, int rssi, long timeDiscovered, ConnectionType deviceType) {
		mDevice = device;
		mRssi = rssi;
		mTimeDiscovered = timeDiscovered;
		mDeviceType = deviceType;
	}
	/**
	 * Retrieve the {@link BluetoothDevice} device, this {@link BluetoothDeviceBundle} represents.
	 * @return	The {@link BluetoothDevice} contained in the {@link BluetoothDeviceBundle}.
	 */
	public BluetoothDevice getDevice() {
		return mDevice;
	}
	/**
	 * Retrieve the last RSSI value for the contained {@link BluetoothDevice}, received from the Bluetooth framework.
	 * 
	 * @return	The last received RSSI value for the {@link BluetoothDevice}.
	 */
	public int getRssi() {
		return mRssi;
	}
	
	/**
	 * Set the RSSI value for the contained {@link BluetoothDevice}, received from the Bluetooth framework.
	 * 
	 * @param mRssi 	The new RSSI value of the contained {@link BluetoothDevice}.
	 */
	public void setRssi(int mRssi) {
		this.mRssi = mRssi;
	}	
	
	/**
	 * Returns the last time the {@link BluetoothDevice} was discovered.
	 * 
	 * @return	The last time the {@link BluetoothDevice} was discovered.
	 */
	public long getTimeDiscovered() {
		return mTimeDiscovered;
	}
	
	/**
	 * Sets the last time the {@link BluetoothDevice} was discovered.
	 * @param mTimeDiscovered	The last time the device was discovered.
	 */
	public void setTimeDiscovered(long mTimeDiscovered) {
		this.mTimeDiscovered = mTimeDiscovered;
	}
	
	public void setIsPaired(boolean paired){
		this.mPaired = paired;
	}
	
	public boolean isPaired() {
		return this.mPaired;
	}
	
	/**
	 * Check equality based on the {@link BluetoothDevice}'s MAC address.
	 */
	@Override
	public boolean equals(Object o) {
		if(o == this) {
			return true;
		}
		if(o instanceof BluetoothDeviceBundle) {
			return this.mDevice.getAddress().equals(((BluetoothDeviceBundle) o).mDevice.getAddress());
		}		
		return false;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// Store the BluetoothDeviceBundle instance's data inside of Parcel. 
		dest.writeParcelable(mDevice, flags);
		dest.writeInt(mRssi);
		dest.writeLong(mTimeDiscovered);
	}
	
	public static final Parcelable.Creator<BluetoothDeviceBundle> CREATOR = new Parcelable.Creator<BluetoothDeviceBundle>() {
		public BluetoothDeviceBundle createFromParcel(Parcel in) {
		    return new BluetoothDeviceBundle(in);
		} 
		
		public BluetoothDeviceBundle[] newArray(int size) {
		    return new BluetoothDeviceBundle[size];
		} 
	};
	
	/**
	 * Private constructor only to be used by the {@link Parcelable.Creator} to re-instantiate a instance of this class.
	 *  
	 * @param in	Parcel object received by the {@link Parcelable.Creator} containing data to recreate an instance of this
	 * class.
	 */
	private BluetoothDeviceBundle(Parcel in) {
		// Restore the BluetoothDeviceBundle using the passed in Parcel data.
		mDevice = (BluetoothDevice) in.readParcelable(BluetoothDevice.class.getClassLoader());
		mRssi = in.readInt();
		mTimeDiscovered = in.readLong();
    }	
}

