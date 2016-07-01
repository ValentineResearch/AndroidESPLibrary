/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.utilities;

import android.util.Log;
import com.valentine.esp.ValentineClient;
import com.valentine.esp.constants.ESPLibraryLogController;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class Utilities 
{
	/** 
	 * This utility function is the heart of the callbacks used in this library.  It uses reflection to find the given function on the 
	 * _owner with the _data type as parameter.  It will log an error if it can't invoke the function for some reason.  All callbacks
	 * run in their own thread not on the ui thread. 
	 * 
	 * @param _owner The object to look for the function on, and invoke the function on
	 * @param _function The function name to look for and invoked
	 * @param _dataType The data type of the parameter for the function to look for
	 * @param _data the data to pass into the function when its invoked
	 */
	@SuppressWarnings("unchecked")
	public static <T> void doCallback(final Object _owner, final String _function, final Class<T> _dataType, final Object _data) 
	{
		new Runnable()
		{
			public void run()
			{
				try 
				{
					if ( _owner == null || _function == null ){
						// We can't do anything with this.  
						// This will typically happen when the ValentineClient receives an echo and processes
						// it as a response.
						if(ESPLibraryLogController.LOG_WRITE_WARNING){
							Log.w("Valentine", "Found null when attempting callback in Utilities.java");
						}
						return;
					}
					if ((_dataType == null) || (_data == null))
					{
						_owner.getClass().getMethod(_function).invoke(_owner);
						return;
					}
					else
					{
						Class<?> dataClass = _data.getClass();
						if (dataClass.isArray())
						{
							ArrayList<T> list = new ArrayList<T>();
							for ( T obj: ((T[]) _data) ) 
							{
								list.add(obj);
							}
							dataClass = list.getClass();
							
							_owner.getClass().getMethod(_function, dataClass).invoke(_owner, list);
							return;
						}
						_owner.getClass().getMethod(_function, dataClass).invoke(_owner, _data);
					}
				} 
				catch ( InvocationTargetException e ) 
				{
					e.printStackTrace();
					ValentineClient.getInstance().reportError(e.toString());
					if(ESPLibraryLogController.LOG_WRITE_INFO){
						Log.i("Valentine",  _owner.toString() + " " + _function + " There was an invoke error calling back to owner: " + e.getTargetException().toString());
					}
					e.printStackTrace();
					
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
					ValentineClient.getInstance().reportError(e.toString());
					if(ESPLibraryLogController.LOG_WRITE_INFO){
						Log.i("Valentine", _owner.toString() + " " + _function + " There was an error calling back to owner: " + e.toString());
					}
					e.printStackTrace();
				}
			}
		}.run();
	}
	
	/** 
	 * Converts a dp value to a pixel value for the device
	 * @param _dp the dp value to convert
	 * @param _scale the value returned by a call getResources().getDisplayMetrics().density, passed in from the calling activity
	 * @return the number of pixels represented by the dp values
	 */
	public static int getPixelFromDp(int _dp, float _scale) 
	{	// '0.5f' is added to round the px value to the neares whole number.
		return (int) (_dp * _scale + 0.5f);
	}
	

}
