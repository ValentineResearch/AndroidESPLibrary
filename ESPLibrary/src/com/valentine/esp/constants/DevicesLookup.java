/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.constants;

import java.util.HashMap;
import java.util.Map;

/** This utility class converts a byte value to a Devices value
 * 
 */
public class DevicesLookup 
{
	static Map<Byte, Devices> m_values = new HashMap<Byte, Devices>();  
	
	/** Converts a byte value to a Devices value
	 * @param _value the byte value to return the Devices value for
	 * @return the device that this value represents
	 */
	public static Devices getConstant(byte _value)
	{
		if (m_values.size() == 0)
		{
			setUpValues();
		}
		
		// Trap special cases
		if ( _value == Devices.VALENTINE1_LEGACY.toByteValue() ){
			return Devices.VALENTINE1_LEGACY;
		}
		if ( _value == Devices.UNKNOWN.toByteValue() ){
			return Devices.UNKNOWN;
		}
		
		// Cast to char so it isn't treated as a signed value.
		if ( (char)_value > (char)0x0F ){
			_value &= (byte)0x0F;
		}
		
		Devices retDev = m_values.get(_value);
		
		if ( retDev == null ){
			retDev = Devices.UNKNOWN;
		}
		
		return retDev; 
	}
	
	/**
	 * Sets up a map containing the all the Devices values using the Devices byte value as the key.
	 */
	private static void setUpValues()
	{
		Devices[] array = Devices.values();
		
		for (int i = 0; i < array.length; i++)
		{
			m_values.put(array[i].toByteValue(), array[i]);
		}
	}
}
