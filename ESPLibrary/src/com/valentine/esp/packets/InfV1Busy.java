/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.packets;

import java.util.ArrayList;
import com.valentine.esp.constants.Devices;

public class InfV1Busy extends ESPPacket 
{
	public InfV1Busy(Devices _destination)
	{
		m_destination = _destination.toByteValue();
		m_timeStamp = System.currentTimeMillis();
	}

	@Override
	/**
	 *  Handles setting constant values.
	 *  See super.buildPacket() for implementation details.
	 */
	protected void buildPacket()
	{

	}

	@Override
	/**
	 *  Gets the data embedded into the packet.  Should not need to call directly, data returned directly from the Valentine Client.
	 * @return An object representing the data in the packet.  Cast to the correct type for the packet. 
	 */
	public Object getResponseData() 
	{
		ArrayList<Integer> rc = new ArrayList<Integer>();
		
		for (int i = 0; i < payloadLength; i++)
		{
			rc.add((int) payloadData[i]);
		}
		
		return rc;
	}
}
