/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.packets.request;

import com.valentine.esp.constants.Devices;
import com.valentine.esp.constants.PacketId;
import com.valentine.esp.data.UserSettings;
import com.valentine.esp.packets.ESPPacket;

public class RequestWriteUserBytes extends ESPPacket 
{
	UserSettings m_settings;
	
	public RequestWriteUserBytes(UserSettings _settings, Devices _destination)
	{
		m_destination = _destination.toByteValue();
		m_valentineType = _destination;
		m_settings = _settings;
		m_timeStamp = System.currentTimeMillis();
		buildPacket();
	}

	@Override
	/**
	 * Initializes data using logic thats specific to this packet.
	 * See parent for default implementation.
	 */
	protected void buildPacket()
	{
		super.buildPacket();
		packetIdentifier = PacketId.reqWriteUserBytes.toByteValue();
		
		if (m_settings != null)
		{
			payloadData = m_settings.buildBytes();
		}
		else
		{
			payloadData =  new byte[6];
		}
		payloadLength = 6;
		
		if ((m_valentineType == Devices.VALENTINE1_LEGACY) || (m_valentineType == Devices.VALENTINE1_WITHOUT_CHECKSUM))
		{
			checkSum = 0;
		}
		else
		{
			checkSum = makeMessageChecksum();
		}
		
		// Sets the packet checksum and packet length values.  
		setPacketInfo();
	}

	@Override
	/**
	 * See parent for default implementation.
	 */
	public Object getResponseData() 
	{
		return null;
	}
}
