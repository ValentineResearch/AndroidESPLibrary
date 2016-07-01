/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.packets.response;

import com.valentine.esp.constants.Devices;
import com.valentine.esp.constants.PacketId;
import com.valentine.esp.constants.PacketIdLookup;
import com.valentine.esp.packets.ESPPacket;

public class ResponseVersion extends ESPPacket 
{
	public ResponseVersion(Devices _destination)
	{
		m_destination = _destination.toByteValue();
		m_timeStamp = System.currentTimeMillis();
		buildPacket();
	}
	
	@Override
	/**
	 * Returns the Version of the V1's software using this packets payload data.
	 * @return A string containing the V1's version.
	 */
	public Object getResponseData()
	{
		if (PacketIdLookup.getConstant(packetIdentifier) != PacketId.respVersion)
		{
			return null;
		}
		
		/*
		 0 The version identification letter for the responding device
			‘V’ for Valentine One
			‘C’ for Concealed Display
			‘R’ for Remote Audio
			‘S’ for Savvy
		 1 ASCII value of the major version number.
		 2 Decimal point (‘.’)
		 3 ASCII value of the minor version number.
		 4 ASCII value of the first digit of the revision number.
		 5 ASCII value of the second digit of the revision number.
		 6 ASCII value of the Engineering Control Number.
		*/

		String rc = "";
		
		int length = payloadLength;
		
		for (int i = 0; i < length; i++)
		{
			char temp = (char)payloadData[i];
			rc = rc + temp;
		}
		
		return rc;
	}

	@Override
	/**
	 * See parent for default implementation.
	 */
	protected void buildPacket() 
	{
		
	}
}
