/*  
 * The V1Screen activity is where all communication with the ESP bus takes place. When this activity Resumes, it will
 * attempt to connect to the V1connection and register for display data. Once the display data stream is active and the
 * V1 is actively searching for signals, this activity will register for alert data and send a request to the V1 for the
 * alert data. When this app pauses, it will de-register for all callbacks and disconnect from the V1connection. It does
 * this so other apps can use the V1connection if necessary. This activity also demonstrates a simple V1 request and 
 * response by including a button that will allow the user to ask the V1 for the firmware version.  
 */
package com.valentine.hello_v1;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.valentine.esp.ValentineClient;
import com.valentine.esp.data.AlertData;
import com.valentine.esp.data.InfDisplayInfoData;
import com.valentine.esp.data.SweepDefinition;

public class V1Screen extends Activity {

	private TextView							m_tvStatus;
	private TextView							m_tvAlertStatus;
	private Button								m_btV1Ver;
	private Button								m_btV1Sweeps;
	private boolean								m_active;
	private boolean								m_alertDataOn;
	
	private static Handler						m_handler = new Handler();
	
	// Use this task to handle a V1 data loss 
	private Runnable m_dataTimeoutTask = new Runnable() {
	   public void run() {
		   m_tvStatus.setText (getString(R.string.v1_data_lost));
		   m_tvAlertStatus.setText (getString(R.string.alert_status_unknown));
		   m_btV1Ver.setEnabled(false);
		   m_btV1Sweeps.setEnabled (false);
		   m_active = false;
		   m_alertDataOn = false;
		   errorReportingFunction (getString(R.string.v1_data_lost));	   
	   }
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_v1_screen);		
		
		// Do not let the phone go to sleep when running this app
        getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
        
        // Show the app version name
 		String versionName = "";
 		try {
 		    final PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);		   
 		    versionName = getString(R.string.hello_v1_ver) + packageInfo.versionName;
 		} catch (NameNotFoundException e) {
 		    e.printStackTrace();
 		    versionName = getString(R.string.hello_v1_ver) + "???";
 		}
 		TextView tv = (TextView) findViewById(R.id.tvHelloV1Version);
 		tv.setText(versionName);
 		
 		m_tvStatus = (TextView) findViewById(R.id.tvStatus);
 		m_tvAlertStatus = (TextView) findViewById(R.id.tvAlertStatus);
 		m_btV1Ver = (Button) findViewById(R.id.btV1Ver);
 		m_btV1Sweeps = (Button) findViewById (R.id.btReadSweeps);
	}
	
	/*
	 * Connect to the V1connection when this activity resumes
	 */
	@Override
	public void onResume () {		
		super.onResume ();
		m_tvStatus.setText (getString(R.string.status_connecting));
		m_tvAlertStatus.setText (getString(R.string.alert_status_unknown));
		m_btV1Ver.setEnabled(false);
		m_btV1Sweeps.setEnabled(false);
		
		m_active = false;
		m_alertDataOn = false;
		handleConnectionResult(HelloV1App.valentineClient.startUpAsync(HelloV1App.device, HelloV1App.connectionType, "onConnectionAttemptCompleted", this));
	}
	
	/**
	 * Helper method for processing the connection result.
	 * 
	 * @param result	The result of the connection attempt.
	 */
	private void handleConnectionResult(int result) {
		String message = null;
		switch(result) {
			case ValentineClient.RESULT_OF_CONNECTION_EVENT_NO_CONNECTION_ATTEMPT:
				message = "Not intiating a connection attempt, not connecting!";
				break;
			case ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_NO_DEVICE:
				message = "A null BluetoothDevice passed into startUpAsync!";
				break;
			case ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_LE_NOT_SUPPORTED:
				message = "Bluetooth LE is not supported, not connecting!";
				break;
			case ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_NO_CALLBACK:
				message = "The callback method or object was null, not connecting!";
				break;
			case ValentineClient.RESULT_OF_CONNECTION_EVENT_FAILED_UNSUPPORTED_CONNTYPE:
				message = "The connection type passed in is unsupported, not connecting!";
				break;
			case ValentineClient.RESULT_OF_CONNECTION_EVENT_CONNECTING:
				message = "ESPLibrary is attempting a connection!";
				break;
			case ValentineClient.RESULT_OF_CONNECTION_EVENT_CONNECTING_DELAY:
				message = "ESPLibrary is attempting a connection with a DELAY!.";
				break;
		}
		Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * Callback method triggered by the ESPLibrary.
	 * 
	 * @param connected	The result of the connection event, true if a connection was made, false otherwise. 
	 * Connection callbacks are issued on the UI thread.
	 */
	public void onConnectionAttemptCompleted(Boolean connected) {
		String message;
		if(connected) {
			m_handler.sendEmptyMessage(0);			
			message = "We've connected with the V1Connection";
			// Good connection, so register for error and display callbacks
			HelloV1App.valentineClient.registerForDisplayData(this,"displayCallback");
			HelloV1App.valentineClient.setErrorHandler(this, "errorReportingFunction");
			m_tvStatus.setText (getString(R.string.status_ready));
			m_tvAlertStatus.setText (getString(R.string.alert_status));
		}
		else {
			message = "We failed to connect";
			// Failed connection
			m_tvStatus.setText (getString(R.string.status_connect_failed));
			m_tvAlertStatus.setText (getString(R.string.alert_status_unavail));
			errorReportingFunction ( "Unable to connect to " + HelloV1App.device.getName());			
		}
		Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
	}	
	
	/**
	 * Callback that gets triggered when the library has closed the Bluetooth connection.
	 * Connection callbacks are issued on the UI thread.
	 */
	public void onShutdown() {
		Toast.makeText(getBaseContext(), "The ESPLibrary has been shutdown", Toast.LENGTH_SHORT).show();
	}
    	
	/*
	 * Disconnect from the V1connection when this activity is not being used.
	 */
	@Override
	public void onPause () {	
		// Tell the V1 to stop sending alert data and stop getting alert and display data from the library
		HelloV1App.valentineClient.sendAlertData(false);
		HelloV1App.valentineClient.deregisterForAlertData(this);
		HelloV1App.valentineClient.deregisterForDisplayData(this);
		m_handler.removeCallbacks(m_dataTimeoutTask);
        		
		// Pass in a callback method and reference to the callback object to receive the disconnected event
		
		HelloV1App.valentineClient.ShutdownAsync("onShutdown", this);
		
		super.onPause();
	}
	
	/*
	 *  Handle the display data from the V1
	 */
	public void displayCallback( InfDisplayInfoData _dispData)
	{
		// TODO Add your own display data processing here		
		if ( !m_active ) {
			// This is the first time we have received display data
			final String status;
			if  ( !_dispData.getAuxData().getSysStatus() ){
				// The V1 is not actively searching for alerts 
				// This can happen if the V1 is initializing or has a fatal error
				status = getString(R.string.status_initializing);
			}
			else{
				// The V1 is actively searching for alerts
				status = getString(R.string.status_ready);
				m_active = true;				
			}
			
			runOnUiThread( new Runnable()
			{
				public void run()
				{	
					m_tvStatus.setText (status);
					m_btV1Ver.setEnabled(m_active);
					m_btV1Sweeps.setEnabled(m_active);
				}
			});
		}
		else{		
			if ( !m_alertDataOn ){
				// Ask for alert data if we haven't yet and the V1 is actively searching for alerts 
				HelloV1App.valentineClient.registerForAlertData(this, "alertDataCallback");
				HelloV1App.valentineClient.sendAlertData(true);				
				m_alertDataOn = true;
			}
		}
		
		if ( m_active ){
			// Use a handler to check for the timeout of the display data.
			m_handler.removeCallbacks(m_dataTimeoutTask);
			m_handler.postDelayed(m_dataTimeoutTask, 3000);
		}
	}	
	
	/*
	 * This function is called by the ESP library every time it receives a full alert table
	 */
	public void alertDataCallback(ArrayList<AlertData> _data)
	{
		final String msg;
		
		// TODO Add your own alert processing code here
		switch ( _data.size() ){
			case 0 :	msg = getString(R.string.no_alerts);											break;
			case 1 :	msg = getString(R.string.single_alert);											break;
			default :	msg = Integer.toString ( _data.size() ) + getString(R.string.alerts_suffix);	break;
		}
		
		runOnUiThread( new Runnable()
		{
			public void run()
			{	
				m_tvAlertStatus.setText (msg);
			}
		});		
	}
	
	/*
	 * This function will be called when the ValentineClient encounters an error 
	 */
	public void errorReportingFunction(final String _error)
	{
		m_ShowMessage ( _error, getString(R.string.error) );
	}
	
	
	/*
	 * Button click event to ask for the V1 version
	 */
	public void onBtV1VerClick(View view)
	{
		// This call will initiate a request to the V1 and tell the library what function to call when the V1 version is received 
		HelloV1App.valentineClient.getV1Version(this, "versionCallback");		
	}
	
	/*
	 * Button click event to ask for the V1 sweeps
	 */
	public void onBtReadSweepsClick(View view)
	{
		// This call will initiate a request to the V1 and tell the library what function to call when the V1 version is received 
		HelloV1App.valentineClient.getAllSweeps(this,  "getAllSweepsCallback");
	}	
	
	/*
	 * This function will be called when the ESP library receives the V1 version from the V1
	 */
	public void versionCallback( String _ver)
	{
		m_ShowMessage ( _ver, getString(R.string.esp_response) );
	}
	
	/*
	 * This function will be called when the ESP library receives the sweep definitions from the V1. 
	 */
	public void getAllSweepsCallback (ArrayList<SweepDefinition> _sweeps)
	{
		StringBuilder str = new StringBuilder();
		
		str.append("V1 Custom Sweeps: \n");
		
		for ( int i = 0; i < _sweeps.size(); i++ ){
			str.append((i+1) + ": ");
			SweepDefinition def = _sweeps.get(i);
			
			if ( def.getLowerFrequencyEdge() == 0 && def.getUpperFrequencyEdge() == 0 ){
				str.append ("disabled\n");
			}
			else{
				str.append (def.getLowerFrequencyEdge() + " - " + def.getUpperFrequencyEdge() + "\n");
			}
		}
		
		m_ShowMessage ( str.toString(), "Custom Sweeps");		
	}
	
	/*
	 * This is a helper function to show an AlertDialog box
	 */
	private void m_ShowMessage (final String _msg, final String _title)
	{
		runOnUiThread( new Runnable()
		{
			public void run()
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(V1Screen.this);
				builder.setTitle(_title)
				.setMessage(_msg)
				.setCancelable(false)
				.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() 
				{
					@Override
					public void onClick(DialogInterface dialog, int which) 
					{
						dialog.cancel();
					}
				})
				.create()
				.show();
			}
		});
	}
}

