/* 
 * The purpose of the MainActivity is to launch the ListBluetoothDevices screen from the ESP library so the user can
 * select the desired Bluetooth device. Once the user has selected a device, this activity will launch the V1Screen,
 * which is where all communication with the V1 takes place in this app. 
 * 
 * Demo Mode is not supported by this app.
 */

package com.valentine.hello_v1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;

import com.valentine.esp.bluetooth.ConnectionType;
import com.valentine.esp.screens.ListBluetoothDevices;

public class MainActivity extends Activity implements DialogInterface.OnClickListener
{
	// Request codes for activities launched by this screen
	private final int GET_DEVICE	 = 10;
	final private int SHOWNODEMOMODE = 11;
	
	private AlertDialog connTypeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);        
    }    
    
    @Override
	protected void onResume() {
		super.onResume();
		if(connTypeDialog == null) {
			String message;
			// Create a Alert Dialog that prompt
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setCancelable(false);
			builder.setTitle("Choose Connection Type");
			builder.setNegativeButton(ConnectionType.V1Connection.toString(), this);
			// Check to see if the device supports Bluetooth LE.
			if(HelloV1App.valentineClient.isBluetoothLESupported()){
				message = "This device supports Bluetooth LE, choose the connection type for the Bluetooth discovery process.";
				builder.setPositiveButton(ConnectionType.V1Connection_LE.toString(), this);
			}
			else {
				// Set the special message fror device that do not support Bluetooth LE. 
				message = "This device does not support Bluetooth LE.";
			}
			builder.setMessage(message);
			connTypeDialog = builder.create();
		}
		// Display the AlertDialog.
		connTypeDialog.show();								
	}
    
    @Override
    public void onClick(DialogInterface dialog, int which) {
    	ConnectionType type;
    	// Based on which button was selected assign the appropriate connectiontype.
    	if(which == DialogInterface.BUTTON_NEGATIVE){
    		type = ConnectionType.V1Connection;
    	}
    	else {
    		type = ConnectionType.V1Connection_LE;			
    	}		
    	launchListVBluetootDevicesActivity(type);		
    }
    
    /**
     * Helper method for starting the ListBluetoothDevice activity.
     * 
     * @param type	The {@link ConnectionType} to pass to the {@link ListBluetoothDevices} activity. This will control the type of 
     * V1Connection devices scanned. 
     */
    private void launchListVBluetootDevicesActivity(ConnectionType type) {
    	// Use the Bluetooth devices screen from the ESP Library to select a device.
    	Intent newIntent = new Intent(MainActivity.this, ListBluetoothDevices.class);
    	// Put the ConnectionType as a Serializable extra.
    	newIntent.putExtra(ListBluetoothDevices.EXTRA_CONNECTION_TYPE, type);
    	startActivityForResult(newIntent, GET_DEVICE);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	// Always dismiss the dialog when pausing the activity.
    	if(connTypeDialog != null) {
    		connTypeDialog.cancel();
    	}
    }

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if ( requestCode == SHOWNODEMOMODE && resultCode == Activity.RESULT_OK ){
    		// The user acknowledged that DEMO mode is not supported
    		Intent newIntent = new Intent(this, ListBluetoothDevices.class); 
        	startActivityForResult(newIntent, GET_DEVICE);
    	}
    	if (requestCode == GET_DEVICE)
    	{
    		if (resultCode == Activity.RESULT_OK)
    		{
    			// Get the demo mode flag from the ActivityResult intent using the ListBluetoothDevices extra field
    			boolean isDemoMode = data.getBooleanExtra(ListBluetoothDevices.EXTRA_DEMO_MODE, false);
    			// Get the ConnectionType from the ActivityResult intent using the ListBluetoothDevices extra field
    			// This is necessary top initiate a Bluetooth connection with the selected device.
    			HelloV1App.type = (ConnectionType) data.getSerializableExtra(ListBluetoothDevices.EXTRA_CONNECTION_TYPE);
    			if ( isDemoMode ){
    				// Demo Mode is not supported in this app
    				Intent newIntent = new Intent(this, NoDemoModeScreen.class); 
    	        	startActivityForResult(newIntent, SHOWNODEMOMODE);
    			}
    			else{
    				// Get the selected BluetoothDevice from the ActivityResult intent using the ListBluetoothDevices extra field.
    				HelloV1App.device = data.getParcelableExtra(ListBluetoothDevices.EXTRA_SELECTED_DEVICE);
    				HelloV1App.connectionType = (ConnectionType) data.getSerializableExtra(ListBluetoothDevices.EXTRA_CONNECTION_TYPE);
	    			Intent intent = new Intent(this, V1Screen.class);	    			
	    			startActivity(intent);	    			
	    			finish();	    			
    			}
    		}	
    		else if (resultCode == Activity.RESULT_CANCELED)
    		{
    			finish();
    		}
    	}
    }
}