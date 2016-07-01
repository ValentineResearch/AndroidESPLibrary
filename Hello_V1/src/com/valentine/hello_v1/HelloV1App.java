/*
 * The HelloV1App object extends the Application. This object is used to the ValentineClient object can
 * be globally accessible.
 */

package com.valentine.hello_v1;

// The ValentineClient is the class that handles all the interfacing with the ESP bus.
// All the calls to the device are done through this class. 
import android.app.Application;
import android.bluetooth.BluetoothDevice;

import com.valentine.esp.ValentineClient;
import com.valentine.esp.bluetooth.ConnectionType;

public class HelloV1App extends Application
{
	private static HelloV1App m_instance;
	
	public static ValentineClient valentineClient;
	
	public static ConnectionType connectionType;
	
	public static BluetoothDevice device;
	
	public static ConnectionType type;
	
	public void onCreate() 
    {
		super.onCreate();
		m_instance = this;
		valentineClient = new ValentineClient(this);
	}
	
	public HelloV1App()
	{
	}
		
	/* 
	 * Returns the current instance of the app 
	 */
	public static HelloV1App getInstance()
	{
		return m_instance;
	}
}
