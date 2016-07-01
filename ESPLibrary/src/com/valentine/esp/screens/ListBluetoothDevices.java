/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.screens;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.valentine.esp.ValentineClient;
import com.valentine.esp.ValentineESP;
import com.valentine.esp.bluetooth.BluetoothDeviceBundle;
import com.valentine.esp.bluetooth.ConnectionType;
import com.valentine.esp.bluetooth.VRScanCallback;
import com.valentine.esp.bluetooth.VR_BluetoothLEWrapper;
import com.valentine.esp.bluetooth.VR_BluetoothSPPWrapper;
import com.valentine.esp.bluetooth.VR_BluetoothWrapper;
import com.valentine.esp.constants.ESPLibraryLogController;
import com.valentine.esp.utilities.Utilities;

public class ListBluetoothDevices extends Activity {
	
	private static final String CHANGE_BTN_TXT = "Change Search type";

	private static final String LOG_TAG = "ListBluetoothDevices LOG";
	
	private static final int 		FINISH_ACTIVITY_DELAY = 1000;
	/**
	 * Used as a Serializable {@link ConnectionType} extra field inside of the intent that started this activity.
	 */
	public static final String 		EXTRA_CONNECTION_TYPE = "EXTRA_CONNECTION_TYPE";
	/**
	 * Used as a Parcelable {@link BluetoothDevice} extra field inside of the intent that started this activity.
	 */
	public static final String		EXTRA_SELECTED_DEVICE = "EXTRA_SELECTED_DEVICE";
	
	/**
	 * Used as a boolean extra field to indicate if demo mode should start inside of the intent that started this activity.
	 */
	public static final String 		EXTRA_DEMO_MODE = "EXTRA_DEMO_MODE";
		
	private static final int 		CHECK_DEVICES_INTERVAL = 5000; 

	public static int 				REQUEST_ENABLE_BT = 1;

	private ListView 				m_listView;

	boolean 						demoDevice;
	TextView 						m_searchingBar;

	private ConnectionType 			mConnectionType = null;
	private BluetoothDeviceAdapter 	adapter;
	
	private VR_BluetoothWrapper 	mBluetoothWrapper;
	
	private AlertDialog				mPopup = null;
	
	private Handler 				mHandler;
	protected boolean				mRun = true;

	private AlertDialog mUnsupportedDialog;
	private BluetoothAdapter mBluetoothAdapter = null;
	
	/**
	 * Item Click listener that handles selections from the Listview.
	 * Handles pairing the {@link BluetoothDevice} if it has never been paired before.
	 * 
	 * 				***ONLY SPP DEVICES CAN PAIR WITH THE PHONE***
	 */
	OnItemClickListener itemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			// Get the bluetooth device from the ListView's adapter.
			BluetoothDevice device = ((BluetoothDeviceBundle) parent.getItemAtPosition(position)).getDevice();
			if (mConnectionType == ConnectionType.V1Connection) {
				if (device.getBondState() == BluetoothDevice.BOND_NONE) {
					try {
						Method m = device.getClass().getMethod("createBond", (Class[]) null);
						m.invoke(device, (Object[]) null);
					} catch (SecurityException e) {
						ValentineClient.getInstance().reportError(
								"Unable to connect to " + device.getName());
					} catch (IllegalArgumentException e) {
						ValentineClient.getInstance().reportError(
								"Unable to connect to " + device.getName());
					} catch (Exception e) {
						ValentineClient.getInstance().reportError(
								"Unable to connect to " + device.getName());
					}
					// Hold off returning the result to calling activity until we become paired with the bluetooth device.
					return;
				}
			}
			Intent response = new Intent();
			response.putExtra(EXTRA_CONNECTION_TYPE, mConnectionType);
			response.putExtra(EXTRA_DEMO_MODE, false);
			response.putExtra(EXTRA_SELECTED_DEVICE, device);
			setResult(Activity.RESULT_OK, response);
			finish();
		}
		
	};
	
	private Runnable notSupportedRunnable = new Runnable() {
		
		@Override
		public void run() {
			// Something is unsupported so set the result to CANCELLED and finish the activity.
			setResult(Activity.RESULT_CANCELED, new Intent());
			finish();			
		}
	};
	
	/**
	 * Handles updating the ListView's dataset.
	 * 
	 * SHOULD ONLY BE CALLED ON THE UI(MAIN) THREAD. 
	 */
	private Runnable updateListViewRunnable = new Runnable() {
		
		@Override
		public void run() {
			if(mRun){
				if(adapter != null) {
					// Force the Listview's adapter to update the data set.
					adapter.mUpdateListView();
					mHandler.postDelayed(this, CHECK_DEVICES_INTERVAL);
				}			
			}
		}		
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Try to Retrieve the passed in ConnectionType.
		mConnectionType = (ConnectionType) getIntent().getSerializableExtra(EXTRA_CONNECTION_TYPE);
		//if null or it's value is ConnectionType.UNKNOWN, default to V1Connection.		
		if(mConnectionType == null || mConnectionType.equals(ConnectionType.UNKNOWN)) {
			mConnectionType = ConnectionType.V1Connection;	
		}
		mHandler = new Handler();
		// The parent layout of the listscreen.
		LinearLayout parentLayout = new LinearLayout(this);
		parentLayout.setOrientation(LinearLayout.VERTICAL);
		// Set the layoutparams to fill the entire screen,
		LayoutParams parentParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		parentLayout.setLayoutParams(parentParams);
		// Create the nested linearlayout to contain the dialog and change type button.
		LinearLayout childLinearLayout = new LinearLayout(this);
		childLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
		// Set the nested linearlayout's layoutparams to match the parent's width and set the height to wrap_content.
		LayoutParams childLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		childLayoutParams.setMargins(2, 15, 2, 5);
		childLinearLayout.setLayoutParams(childLayoutParams);
		// Create the textview that display the Bluetooth scan in progress.
		m_searchingBar = new TextView(this);
		m_searchingBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);
		LayoutParams textViewParams = new LayoutParams(Utilities.getPixelFromDp(0, getResources().getDisplayMetrics().density), LayoutParams.WRAP_CONTENT, .7f);
		m_searchingBar.setLayoutParams(textViewParams);
		// Create the button that handles changing the bluetooth connection type.
		Button mChangeType = new Button(this);
		mChangeType.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				performClick();				
			}
		});
		mChangeType.setText(CHANGE_BTN_TXT);
		LayoutParams buttonParams = new LayoutParams(Utilities.getPixelFromDp(0, getResources().getDisplayMetrics().density), LayoutParams.WRAP_CONTENT, .3f);
		buttonParams.gravity = Gravity.RIGHT;
		mChangeType.setLayoutParams(buttonParams);		
		childLinearLayout.addView(m_searchingBar);
		childLinearLayout.addView(mChangeType);			

		// Add the textview and button into the nested linearlayout.
		ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
		progressBar.setIndeterminate(true);
		LayoutParams progressBarParams = new LayoutParams(LayoutParams.MATCH_PARENT, Utilities.getPixelFromDp(15, getResources().getDisplayMetrics().density));
		progressBarParams.setMargins(0, 10, 0, 5);
		progressBar.setLayoutParams(progressBarParams);
		
		// Create the listview that displays the discovered BluetoothDevices.
		m_listView = new ListView(this);
		m_listView.setLayoutParams(parentParams);
		
		// Add the nested Linearlayout and listview to the parent layout.
		parentLayout.addView(childLinearLayout);
		parentLayout.addView(progressBar);
		parentLayout.addView(m_listView);		
		setContentView(parentLayout);
		// Always use an application context so we don't ever leak and activity.
		Context context = getApplicationContext();
		View demoView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, m_listView, false);
		((TextView)demoView.findViewById(android.R.id.text1)).setText("Demo Mode");
		// Set a custom onClick listener for when the demo mode option is selected from the listview.
		demoView.setOnClickListener(new OnClickListener() { 
			@Override 
			public void onClick(View view) {
				demoDevice = true;
				Intent response = new Intent();
				response.putExtra(EXTRA_DEMO_MODE, true);
				// Pass in the current ConnectionType of the current scanning process even if the library will be launching into Demo Mode.
				response.putExtra(EXTRA_CONNECTION_TYPE, mConnectionType);
				setResult(Activity.RESULT_OK, response);
				m_listView.setEnabled(true);
				finish();
				
			}
		});
		m_listView.addHeaderView(demoView);
		m_listView.setOnItemClickListener(itemClickListener);
		// Recommend way of getting the BluetoothAdapter for API level 18 and above.
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
			mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
		}
		else {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}		
	}
	
	/**
	 * Launches an AlertDialog that prompts the user to pick a {@link ConnectionType} used to discover {@link BluetoothDevice}.
	 */
	protected void performClick() {
		 AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 final ConnectionType [] connectionTypes = {ConnectionType.V1Connection, ConnectionType.V1Connection_LE};
		 // Creates a custom listview adapter that will enable/disable the V1Connection LE option based on the device support.
		 ArrayAdapter<Object> adapter = new ArrayAdapter<Object>(this, 0, 0, connectionTypes) {
			 	@Override
				public boolean isEnabled(int position) {
					// If Bluetooth LE is not supported return false so the list item is not selectable.
					if(!ValentineClient.checkBluetoothLESupport(getApplicationContext()) && getItem(position).toString().contains("V1Connection LE")) {
						return false;
					}			
					return true;
				}

				@Override
				public View getView(int position, View convertView, ViewGroup parent) {
					if(convertView == null) {
						LayoutInflater inflater = LayoutInflater.from(getContext());
						convertView = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);				
					}
					
					String text = getItem(position).toString();
					
					if(!isEnabled(position)) {
						convertView.setEnabled(false);
						text += " (Not Supported)";
					}
					else {
						convertView.setEnabled(true);
					}
					
					((TextView) convertView.findViewById(android.R.id.text1)).setText(text);
					
					return convertView;
				}
				
		 };
		 // Get the current ConnectionType position so the AlertDialog's listview will mark the current Connectiontype.
		 int checkedItem = mConnectionType == ConnectionType.V1Connection ? 0 : 1;
		 AlertDialog dialog = builder.setSingleChoiceItems(adapter, checkedItem, new DialogInterface.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				// If user selection is not the same as the connection type, we want to update 
				// its value and restart the bluetooth scanning process in order to reflect the new selection.
				if(mConnectionType != connectionTypes[which]) {
					mConnectionType = connectionTypes[which];
					mBluetoothWrapper.stopScanningForDevices();
					setUpBluetooth(true);
				}
			}
			
		}).create();
		 // OnDismissListeners cannot be set on AlertDialog.Builders prior to Android 17, so we must add it directly on the AlertDialog object.
		 dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {			
				@Override
				public void onDismiss(DialogInterface dialog) {
					// When the dialog is dismissed set its reference to null.
					mPopup = null;
				}
		 });
		 
		 mPopup = dialog;
		 mPopup.show();
	}	
	
	@Override
	protected void onResume() {
		super.onResume();
		// Keeps the screen on.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);	
		// If null, the device does not support bluetooth so we just bail.
		if (mBluetoothAdapter == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setTitle("Bluetooth Unsupported")
			.setMessage("This devices does not support Bluetooth, and this app's functionality is dependent on Bluetooth.")
			.setCancelable(false)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
					finish();
					
				}
			});
			mUnsupportedDialog = builder.create();
			mUnsupportedDialog.show();
			return;
		}
		// If bluetooth is not enabled we want to enable it.
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			return;
		}
		// Register the Broadcast receiver for the BluetoothDevice pairing event.
		registerReceiver(m_pairingReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
		// If ConnectionType equal LE and the devices does not support  
		if(ConnectionType.V1Connection_LE.equals(mConnectionType) && !ValentineClient.checkBluetoothLESupport(this)){
			if(ESPLibraryLogController.LOG_WRITE_ERROR) {
				Log.e(LOG_TAG, "ConnectionType = " + mConnectionType.toString() + " is not supported on this device.");
			}
			// Set the title bar to display ConnectionType + 'unsupported'.
			m_searchingBar.setText(mConnectionType.toString() + " unsupported");
			// Force the ConnectionType and BluetoothWrapper references to null so no one tries to operate on them.
			mConnectionType = null;
			mBluetoothWrapper = null;
			mHandler.postDelayed(notSupportedRunnable, FINISH_ACTIVITY_DELAY);
			return;
		}		
		setupBluetoothWrapper();
		// Prepares the Bluetooth scan process.
		setUpBluetooth(false);
		mRun = true; 
		// Repost the runnable that handles updating the Listview's dataset.
		mHandler.postDelayed(updateListViewRunnable, CHECK_DEVICES_INTERVAL);
	}
	
	private void setupBluetoothWrapper() {
		
		// Create a bluetooth wrapper object, for the Bluetooth scanning process, based off of the ConnectionType.
		if(mConnectionType.equals(ConnectionType.V1Connection_LE)) {
			mBluetoothWrapper = new VR_BluetoothLEWrapper(new ValentineESP(0, this), null, 0, this);
		}
		else {
			// Default to using SPP if connectionType is not LE.
			mBluetoothWrapper = new VR_BluetoothSPPWrapper(new ValentineESP(0, this), null, 0, this);
		}
	}
		
	/**
	 * Helper method that will handle enabling Bluetooth on the phone and starting 
	 * device discovery, and setting up the 'devices' listview. As well as registering the Discovery broadcast receivers.
	 * 
	 * @param connTypeChanged	A flag to determine if the connection type has been changed and a new {@link VR_BluetoothWrapper} object needs to be created.
	 */
	@SuppressLint("NewApi")
	private void setUpBluetooth(boolean connTypeChanged) {
		
		ArrayList<BluetoothDeviceBundle> pairedDevices = new ArrayList<BluetoothDeviceBundle>();;
		// If the ConnectionType is V1Connection we want to list the paired devices in the listview.
		// V1Connection_LE devices are not paired, so this is not necessary.
		if(mConnectionType == ConnectionType.V1Connection) {		
			// Store the paired BluetoothDevices inside of the devices list.
			for(BluetoothDevice device : mBluetoothAdapter.getBondedDevices()) {
				// Only add V1connection paired BluetoothDevice.
				BluetoothDeviceBundle bundle = new BluetoothDeviceBundle(device, 0, System.currentTimeMillis(), ConnectionType.V1Connection);
				if(VR_BluetoothSPPWrapper.isV1ConnectionSPP(device)){
					pairedDevices.add(bundle);
				}
			}
		}
		
		adapter = new BluetoothDeviceAdapter(pairedDevices, getApplicationContext(), m_listView);		
		m_listView.setAdapter(adapter);
		
		if(connTypeChanged) {
			setupBluetoothWrapper();
		}
		
		// We want to scan infinitely so pass in a zero for time to scan.
		mBluetoothWrapper.scanForDevices(vrScanCallback, 0);
		
		String deviceType = "V1Connection";
		if(mConnectionType == ConnectionType.V1Connection_LE) {
			deviceType = "V1Connection LE";
		}
		
		// Set the title bar to display that the app is searching for bluetooth devices.
		m_searchingBar.setText("Searching for " + deviceType + "s");	
	}

	@Override
	public void onPause() {
		super.onPause();		
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);		
		if(mBluetoothWrapper != null) {

			if(mBluetoothWrapper instanceof VR_BluetoothSPPWrapper){
				((VR_BluetoothSPPWrapper) mBluetoothWrapper).unregisterBroadcastReceiver();
			}
			// Stop scanning when an activity pauses.
			mBluetoothWrapper.stopScanningForDevices();
		}		

		//Stop the update listview runnable.
		mRun = false;		
		
		mHandler.removeCallbacks(updateListViewRunnable);
		mHandler.removeCallbacks(notSupportedRunnable);
		
		// Clear all dialogs when entering pause so we don't leak Activity context.
		if(mPopup != null) {
			mPopup.dismiss();
			mPopup = null;
		}

		if(!isFinishing()) {
			// If the activity is pausing and the unsupported dialog is not null, we should finish here because bluetooth is unsupportedo
			if(mUnsupportedDialog != null) {
				finish();
			}
		}
		// Unregister the Broadcast receiver.
		unregisterReceiver(m_pairingReceiver);
	}
	
	/**
	 * Callback that receives discovered {@link BluetoothDeviceBundle}s.
	 */
	VRScanCallback vrScanCallback = new VRScanCallback() {
		
		@Override
		public void onScanComplete(ConnectionType connectionType) {}
		
		@Override
		public void onDeviceFound(final BluetoothDeviceBundle bluetoothDeviceBundle, ConnectionType connectionType) {
			// Notify the adapters dataset has changed on the UI thread inorder for the ListView to reflect the changes.s
			runOnUiThread(new Runnable() {
				public void run() {
					// Add the scanned bluetooth device bundle to the adapter.
					// Processing must be done inside of the adapter to make sure to not add duplicates.
					if(adapter.addBluetoothDevice(bluetoothDeviceBundle)) {
						adapter.notifyDataSetChanged();
					}
				}
			});
		}
	};
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			setUpBluetooth(false);
		} else if (resultCode == Activity.RESULT_CANCELED) {
			ValentineClient.getInstance().reportError("Turning on bluetooth canceled");
		}
	}

	/**
	 * BroadcastReceiver that triggered when a pairing attempt between the phone and the selected {@link BluetoothDevice} occurs.
	 * Handles notifying the user if the pairing attempt has failed.
	 */
	private BroadcastReceiver m_pairingReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
				int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
				int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (prevBondState == BluetoothDevice.BOND_BONDING) {
					// check for both BONDED and NONE here because in some error cases the bonding fails and we need to fail gracefully.
					if (bondState == BluetoothDevice.BOND_BONDED) {
						m_listView.setEnabled(true);
						Intent response = new Intent();
						response.putExtra(EXTRA_CONNECTION_TYPE, mConnectionType);
						response.putExtra(EXTRA_DEMO_MODE, false);
						response.putExtra(EXTRA_SELECTED_DEVICE, device);
						ListBluetoothDevices.this.setResult(Activity.RESULT_OK, response);
						finish();
					} else if (bondState == BluetoothDevice.BOND_NONE) {
						m_listView.setEnabled(true);
						AlertDialog.Builder builder = new AlertDialog.Builder(ListBluetoothDevices.this);
						builder.setTitle("Unable to connect")
						       .setMessage(
								       "Please check the notification area for a pairing request, enter 1234 for the pin and please try again")
						       .setCancelable(false)
						       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
							       public void onClick(DialogInterface dialog, int id) {
								       dialog.cancel();
							       }
						       })
						       .show();
					}
				}
			}
		}
	};	
	
	/**
	 * Custom ListView adapter for displaying {@link BluetoothDevice}s.
	 * 
	 * @author jdavis
	 *
	 */
	public class BluetoothDeviceAdapter extends BaseAdapter {
			
        private static final int LISTVIEW_ITEM_REGULAR_SECTION = 0;
        private static final int LISTVIEW_ITEM_REGULAR = 1;        
        private static final String LIST_ITEM_SECTION_HEADER_PAIRED = "Paired Devices";
        private static final String LIST_ITEM_SECTION_HEADER_NEARBY = "Nearby Device";
        private static final String RSSI_STRING = "rssi: "; 
        private boolean indicate_rssi = true;
		/**
		 * Holds all {@link BluetoothDevice}'s, all discovered and paired devices.
		 */
		private ArrayList<BluetoothDeviceBundle> mDevices = new ArrayList<BluetoothDeviceBundle>();
		private Object mLock = new Object();
		private long mMostRecentDeviceDisc = 0L;
		private TypedValue mTypedValLarge;
		private TypedValue mTypedValSmall;
		private int mTextViewMinHeightLarge = 0;
		private int mTextViewMinHeightSmall = 0;
		private int mListItemDefaultHeight = 0;
		private ListView mListView = null;
		private int mPairedCount = 0;
		
		public BluetoothDeviceAdapter(ArrayList<BluetoothDeviceBundle> initial, Context context, ListView listview) {
			super();
			mListView = listview;
			if(!initial.isEmpty()) {
				// Make the first item null.
				mDevices.add(null);
				mDevices.addAll(initial);
				mPairedCount = initial.size();
			}
			// NOTIFYDATASETCHANGED() IS INTENTIONALLY NOT CALLED HERE BECAUSE THE ADAPTER HAS NOT BEEN ADDED TO A LISTVIEW.
			if(context != null) {
				// Create the TypedValue used to setup the listview items textviews.
				createTypedValues(context);
			}
		}
		
		private void createTypedValues(Context context) {
			mTypedValLarge = new TypedValue();
			mTypedValSmall = new TypedValue();
			// Convert the desired textview minimum height into DIP.
			mTextViewMinHeightLarge = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 38, context.getResources().getDisplayMetrics());
			mTextViewMinHeightSmall = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, context.getResources().getDisplayMetrics());
			mListItemDefaultHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, context.getResources().getDisplayMetrics());
			// Resolve the desired TextApperance and store it in a TypedValue object.
			context.getTheme().resolveAttribute(android.R.attr.textAppearanceLarge, mTypedValLarge, true);
			context.getTheme().resolveAttribute(android.R.attr.textAppearanceSmall, mTypedValSmall, true);
		}
		
		/**
		 * Adds a bluetooth device to the adapter updating it's RSSI value, if already present.
		 * 
		 * @param deviceBundle	A {@link BluetoothDeviceBundle} that pairs a {@link BluetoothDevice}, a rssi value, and discovery time. 
		 * 
		 * @return 	Returns true if the deviceBundle was successfully added to the adapter. Returns false, 
		 * if called in any thread other than the Main (UI) thread or if the bundle was already inside of the adapter. 
		 */
		public boolean addBluetoothDevice(BluetoothDeviceBundle deviceBundle) {
			// Make sure that this method is only called on the UI thread.
			if(Thread.currentThread() != Looper.getMainLooper().getThread()) {
				Log.e("ListBluetoothDevices - addBluetoothDevice()", "The BluetoothDeviceAdapter's dataset can only be modified on the UI thread.");
				return false;
			}			
			synchronized (mLock) {
				mMostRecentDeviceDisc = deviceBundle.getTimeDiscovered();			
				int index = mDevices.indexOf(deviceBundle);
				if(index != -1) {
					BluetoothDeviceBundle bndle = mDevices.get(index);
					bndle.setTimeDiscovered(mMostRecentDeviceDisc);
					// We only want to update the rssi text view only if the flag is activated.
					if(indicate_rssi) {
						bndle.setRssi(deviceBundle.getRssi());
						// Update the rssi TextView.
						updateRssiTextView(index, deviceBundle.getRssi());
			        }					
				}
				else {
					// If the list is empty or if he number of devices equals the number of paired devices plus the header view then add a new null element.
					if(mDevices.isEmpty() || mPairedCount + 1 == mDevices.size()) {
						mDevices.add(null);
					}
					// Add the bluetooth device to the Front of the list.
					mDevices.add(deviceBundle);
					return true;
				}
			}
			return false;
		}
		
		private void updateRssiTextView(int index, int rssi) {
			synchronized (mLock) {
				if(mListView != null) {
					/*
					 * Using the difference of the first visible view on screen and the position 
					 * of the view that wants to be updated get the child from inside of the listview.
					 */
					View v = mListView.getChildAt((index + mListView.getHeaderViewsCount()) - mListView.getFirstVisiblePosition());
					if(v != null) {
						((ViewHolder)v.getTag()).rssi.setText( RSSI_STRING + String.valueOf(rssi));
					}
				}
			}			
		}
		
		private void mUpdateListView() {
			boolean modified = false;
			synchronized(mLock) {
				long time = System.currentTimeMillis();
				// We are going through the list in reverse and removing the items and we want to stop at the header sections. Since the paired count is not zero based it will always account for a header index.
				for(int i = mDevices.size() - 1; i > mPairedCount ; i--) {				
					BluetoothDeviceBundle bundle = mDevices.get(i);
					if(bundle != null && (time - bundle.getTimeDiscovered() > 29000)) {
						// Remove this index because it has exceeded the timeout.
						mDevices.remove(i);
						modified = true;
					}
				}
				// If the last time is null, remove the header view.
				if(!mDevices.isEmpty() && mDevices.get(mDevices.size() - 1) == null){
					mDevices.remove(mDevices.size() - 1);
				}
			}
			if(modified){
				notifyDataSetChanged();
			}
		}
		
		/**
		 * Removes all of the contained {@link BluetoothDevice}s.
		 */
		public void clear() {
			mDevices.clear();
			mPairedCount = 0;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			/*
			 * Return the number of header sections, paired and discovered bluetooth devices.
			 * The number of discovered devices is adjusted when a device is add or removed.
			 * The number of paired devices is immutable.
			 */
			return mDevices.size();
		}
			
		@Override
		public Object getItem(int position) {
			
			int offset;
			/*
			 * If the number of header sections is less than two (by default there is always one) or
			 * if the position is less than or equal to the number of header sections (max of two), offset the
			 * position by one, to account for the header section that is not actually contained in
			 * mBluetoothDeviceBundles.
			 *
			 * This works because getItem(int position) is never called for positions that correspond with the header sections.
			 * So if the number of header sections is 1 or position is less than or equal to the paired devices
			 * we only need to adjust the position for one additional index added by the getView();
			 */
			return mDevices.get(position);
		}
		
		private String getHeaderText(int position) {
			if(mDevices.get(position) == null) {
				if(position == 0 && mPairedCount > 0) {
					return LIST_ITEM_SECTION_HEADER_PAIRED;
				}
				return LIST_ITEM_SECTION_HEADER_NEARBY;
			}
			return "";
		}
	
		@Override
		public long getItemId(int position) {
			return position;
		}

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) == LISTVIEW_ITEM_REGULAR;
        } 
        
        
        @Override
		public int getViewTypeCount() {
        	// The max number of view types for this adapter. One for section headers and one for regular items.
			return 2;
		}

		@Override
        public int getItemViewType(int position) {
        	/*
        	 *  If the position is zero or if position is greater than then number of paired 
        	 *  devices and less than the number of paired devices plus the number of section headers.
        	 */
        	if(mDevices.get(position) == null) {
        		return LISTVIEW_ITEM_REGULAR_SECTION;
        	}
        	return LISTVIEW_ITEM_REGULAR;
        }
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int itemType = getItemViewType(position);
			Context context = parent.getContext();
			ViewHolder holder;
			if(convertView == null) {
				convertView = new LinearLayout(context);				
				// Make sure our typevalues exist.
				if(mTypedValLarge == null) {
					createTypedValues(context);
				}				
				holder = getHolder(context, itemType, (LinearLayout) convertView);
								
				convertView.setTag(holder);	
			}
			// Fetch the viewholder from ConvertView			
			holder = (ViewHolder) convertView.getTag();
			
			// The view we are inflating is a 
			if(itemType == LISTVIEW_ITEM_REGULAR_SECTION){
				String text = getHeaderText(position);				
				holder.title.setText(text);
			}
			else {
				BluetoothDeviceBundle bundle = (BluetoothDeviceBundle) getItem(position);
				holder.title.setText(bundle.getDevice().getName());
				if(indicate_rssi) {
		        	holder.rssi.setText(RSSI_STRING + String.valueOf(bundle.getRssi()));
		        }
			}			
			return convertView;
		}
		
		private ViewHolder getHolder(Context context, int itemType, LinearLayout item) {			
			ViewHolder holder = new ViewHolder();
			holder.layout = item;
			int itemHeight = LayoutParams.WRAP_CONTENT;
			// If the current view is is a regular item apply the default listview item height.
			if(itemType == LISTVIEW_ITEM_REGULAR) {
				itemHeight = mListItemDefaultHeight;
			}
			item.setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, itemHeight));
			item.setOrientation(LinearLayout.VERTICAL);
			item.setPadding(6, 0, 0, 0);
			
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lp.setMargins(0, 0, 0, 0);
			// title textview
			TextView title = new TextView(context);
			title.setLayoutParams(lp);
			title.setPadding(0, 0, 0, 0);
			item.addView(title);
			
			holder.title = title;				
			
			if(itemType == LISTVIEW_ITEM_REGULAR) {
				// RSSI view				
				if(indicate_rssi) {
					TextView rssi = new TextView(context);
					rssi.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
					rssi.setPadding(0, 0, 0, 0);
					lp.setMargins(0, 0, 0, 0);
					rssi.setLayoutParams(lp);
					item.addView(rssi);
					holder.rssi = rssi;

					rssi.setTextAppearance(context, mTypedValSmall.data);
					rssi.setMinHeight(mTextViewMinHeightSmall);
				}
				
				// Apply a text appearance.
				title.setTextAppearance(context, mTypedValLarge.data);				
				title.setMinHeight(mTextViewMinHeightLarge);
			}
			else {
				// Apply a text appearance.
				title.setTextAppearance(context, mTypedValSmall.data);				
				title.setMinHeight(mTextViewMinHeightSmall);
			}
			return holder;
		}
		
		/**
		 * Simple Data-structure for holding references to the ListItem's views.
		 * ALWAYS USE THIS PATTERN. WHEN USING LISTVIEWS.
		 * @author jdavis
		 *
		 */
		public class ViewHolder {
			LinearLayout layout;
			TextView title;
			TextView rssi;
		}
	}
}