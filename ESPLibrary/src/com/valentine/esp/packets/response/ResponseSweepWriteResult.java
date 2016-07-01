/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.packets.response;

import com.valentine.esp.constants.Devices;
import com.valentine.esp.data.SweepWriteResult;
import com.valentine.esp.packets.ESPPacket;

public class ResponseSweepWriteResult extends ESPPacket 
{
	public ResponseSweepWriteResult(Devices _destination)
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
	 * Returns SweepWriteResult from the V1 using this packets payload data.
	 * @return	SweepWriteResult from the V1.
	 */
	public Object getResponseData() 
	{
		SweepWriteResult rc = new SweepWriteResult();
		
		rc.buildFromByte(payloadData[0]);
		
		return rc;
	}
}
