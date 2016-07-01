/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.packets.response;

import com.valentine.esp.constants.Devices;
import com.valentine.esp.packets.ESPPacket;

public class ResponseMaxSweepIndex extends ESPPacket 
{
	public ResponseMaxSweepIndex(Devices _destination)
	{
		m_destination = _destination.toByteValue();
		m_timeStamp = System.currentTimeMillis();
		buildPacket();
	}

	@Override
	/**
	 * See parent for default implementation.
	 */
	protected void buildPacket()
	{

	}

	@Override
	/**
	 * Returns an Integer indicating the max sweep index.
	 * @return Max sweep index.
	 */
	public Object getResponseData() 
	{
		Integer rc;
		
		rc = (int) payloadData[0];
		
		return rc;
	}
}
