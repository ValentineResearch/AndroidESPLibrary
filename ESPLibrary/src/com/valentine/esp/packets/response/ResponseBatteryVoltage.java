/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.packets.response;

import com.valentine.esp.constants.Devices;
import com.valentine.esp.packets.ESPPacket;

public class ResponseBatteryVoltage extends ESPPacket 
{
	public ResponseBatteryVoltage(Devices _destination)
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
	 * Returns the Float indicating the battery voltage.
	 * @returns Float indicating the battery.
	 */
	public Object getResponseData() 
	{
		/*
		 0 Integer portion of the battery voltage
		 1 Payload Bytes Decimal portion of the battery voltage
		*/
		
		Byte integerPart = payloadData[0];
		Byte decimalPart = payloadData[1];
		
		String temp = integerPart.toString() + ".";
		
		if (decimalPart < 10)
		{
			temp += "0";
		}
			
		temp += decimalPart.toString();
		
		return Float.parseFloat(temp);
	}
}
