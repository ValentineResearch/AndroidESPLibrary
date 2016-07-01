/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.packets;

import com.valentine.esp.constants.Devices;
import com.valentine.esp.data.AuxilaryData;
import com.valentine.esp.data.BandAndArrowIndicatorData;
import com.valentine.esp.data.BogeyCounterData;
import com.valentine.esp.data.InfDisplayInfoData;
import com.valentine.esp.data.SignalStrengthData;

public class InfDisplayData extends ESPPacket 
{
	public InfDisplayData(Devices _destination)
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
		InfDisplayInfoData rc = new InfDisplayInfoData();
		BogeyCounterData bogey1 = new BogeyCounterData();
		BogeyCounterData bogey2 = new BogeyCounterData();
		SignalStrengthData signal = new SignalStrengthData();
		BandAndArrowIndicatorData bandAndArrow1 = new BandAndArrowIndicatorData();
		BandAndArrowIndicatorData bandAndArrow2 = new BandAndArrowIndicatorData();
		AuxilaryData auxData = new AuxilaryData();
		
		bogey1.setFromByte(payloadData[0]);
		bogey2.setFromByte(payloadData[1]);
		signal.setFromByte(payloadData[2]);
		bandAndArrow1.setFromByte(payloadData[3]);
		bandAndArrow2.setFromByte(payloadData[4]);
		auxData.setFromByte(payloadData[5]);
		
		rc.setAux1Data(payloadData[6]);
		rc.setAux2Data(payloadData[7]);
				
		rc.setBogeyCounterData1(bogey1);
		rc.setBogeyCounterData2(bogey2);
		rc.setSignalStrengthData(signal);
		rc.setBandAndArrowIndicator1(bandAndArrow1);
		rc.setBandAndArrowIndicator2(bandAndArrow2);
		rc.setAuxData(auxData);
		
		return rc;
	}
}
