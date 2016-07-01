/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.constants;

/** This enum lists all the different types of devices that can be connected to the ESP bus and their origin addresses.
 *  This also has a easy to read string and two utility devices the Valentine_legacy, and the Unknown types to address
 *  issues where the difference between a without checksum Valentine One and a legacy Valentine One is needed to be
 *	known.
 */
public enum Devices 
{
	CONCEALED_DISPAY ((byte)0x00, "Concealed Display"),
	REMOTE_AUDIO ((byte)0x01, "Remote Audio"),
	SAVVY ((byte)0x02, "Savvy"),
	THIRD_PARTY_1 ((byte)0x03, "Third Party 1"),
	THIRD_PARTY_2 ((byte)0x04, "Third Party 2"),
	THIRD_PARTY_3 ((byte)0x05, "Third Party 3"),
	V1CONNECT ((byte)0x06, "V1Connect"),
	RESERVED ((byte)0x07, "Reserved for Valentine Research"),
	GENERAL_BROADCAST ((byte)0x08, "General Broadcast"),
	VALENTINE1_WITHOUT_CHECKSUM ((byte)0x09, "Valentine1 without checksum"),
	VALENTINE1_WITH_CHECKSUM ((byte)0x0A, "Valentine1 with checksum"),
	
	VALENTINE1_LEGACY ((byte)(0x98), "Legacy Valentine1"),
	UNKNOWN ((byte)0x99, "Unknown");
	
	byte m_value;
	String m_name;
		
	/** Devices constructor
	 * 
	 */
	Devices(byte _value, String _name)
	{
		m_value = _value;
		m_name = _name;
	}
	
	/** Method to convert this enumeration to a byte value
	 * 
	 * @return the byte representation of this object
	 */
	public byte toByteValue()
	{
		return m_value;
	}
	
	/** Method to convert this enumeration to a string
	 * 
	 * @return the string representation of this object
	 */
	public String toString()
	{
		return m_name;
	}
	
	/**
	 * Returns a {@link Device} enum that corresponds to val.
	 * 
	 * @param val	The byte value for which you want to return a {@link Device} enum.
	 * 
	 * @return Returns a {@link Device} enum representation of val.
	 */
	public static Devices fromByteValue (byte val)
	{
		// Trap special cases
		if ( val == Devices.VALENTINE1_LEGACY.toByteValue() ){
			return Devices.VALENTINE1_LEGACY;
		}
		
		if ( val == Devices.UNKNOWN.toByteValue() ){
			return Devices.UNKNOWN;
		}
		
		return Devices.values()[val]; 
	}
}
