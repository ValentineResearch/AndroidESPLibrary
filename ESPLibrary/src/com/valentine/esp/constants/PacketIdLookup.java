/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.constants;

import java.util.HashMap;
import java.util.Map;

public class PacketIdLookup 
{
	static Map<Byte, PacketId> m_values = new HashMap<Byte, PacketId>();  
	
	/**
	 * Gets a packetId for a given byte value.
	 * 
	 * @param _value	The byte value to retrieve a packetId for.
	 * 
	 * @return	retId	The packetId for the given byte value.
	 */
	public static PacketId getConstant(byte _value)
	{
		if (m_values.size() == 0)
		{
			setUpValues();
		}
		
		PacketId retId = m_values.get(_value);
		
		if ( retId == null ){
			retId = PacketId.unknownPacketType;			
		}
		
		return retId;
	}
	
	/**
	 * Sets up the packetId map, used for retrieving packetIds from byte values.
	 */
	private static void setUpValues()
	{
		PacketId[] array = PacketId.values();
		
		for (int i = 0; i < array.length; i++)
		{
			m_values.put(array[i].toByteValue(), array[i]);
		}
	}
}
