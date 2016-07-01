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

public class ResponseSerialNumber extends ESPPacket 
{
	public ResponseSerialNumber(Devices _destination)
	{
		m_destination = _destination.toByteValue();
		m_timeStamp = System.currentTimeMillis();
		buildPacket();
	}
	
	@Override
	/**
	 * Returns the V1 serial number using this packets payload data.
	 * @return A string containing then V1's serial number. 
	 */
	public Object getResponseData()
	{
		if (PacketIdLookup.getConstant(packetIdentifier) != PacketId.respSerialNumber)
		{
			return null;
		}
		
		/*
		0 The first character of the serial number string, in ASCII.
		1 The second character of the serial number string, in ASCII
		2 The third character of the serial number string, in ASCII
		3 The fourth character of the serial number string, in ASCII
		4 The fifth character of the serial number string, in ASCII
		5 The sixth character of the serial number string, in ASCII
		6 The seventh character of the serial number string, in ASCII
		7 The eighth character of the serial number string, in ASCII
		8 The ninth character of the serial number string, in ASCII
		9 The tenth character of the serial number string, in ASCII
		*/

		String rc = "";
		// This is the serial number length per ESP Specification version 3.002
		int snLength = 10;
		
		if (snLength > payloadLength){
			// This must be an different format so just use the payload data
			snLength = payloadLength;
		}
		
		for (int i = 0; i < snLength; i++)
		{
			char temp = (char)payloadData[i];
			if (temp == 0)
			{
				break;
			}
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
