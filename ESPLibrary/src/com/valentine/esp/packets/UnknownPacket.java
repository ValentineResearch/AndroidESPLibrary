/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.packets;

import com.valentine.esp.constants.Devices;

public class UnknownPacket extends ESPPacket 
{
	public UnknownPacket(Devices _destination)
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
		return null;
	}
}
