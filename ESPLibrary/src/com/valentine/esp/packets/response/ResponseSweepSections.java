/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.packets.response;

import com.valentine.esp.constants.Devices;
import com.valentine.esp.data.SweepSection;
import com.valentine.esp.packets.ESPPacket;

public class ResponseSweepSections extends ESPPacket 
{
	public ResponseSweepSections(Devices _destination)
	{
		m_destination = _destination.toByteValue();
		m_timeStamp = System.currentTimeMillis();
		buildPacket();
	}
	
	@Override
	/**
	 * Returns an array of SweepSections from the V1 using this packets payload data.
	 * @return An array of SweepSections.
	 */
	public Object getResponseData()
	{
		SweepSection[] rc;

		SweepSection temp = new SweepSection();
		
		// Get the number of sections in the data coming from the V1.
		temp.buildFromBytes(payloadData, 0);
		int v1Sections = temp.getSweepDefinitionIndex().getNumberOfSectionsAvailable();
		
		// Determine how many of the sweeps are valid
		int numValidSections = 0;
		
		for ( int i = 0; i < v1Sections; i++ ){
			temp.buildFromBytes(payloadData, 5*i);
			if ( temp.getLowerFrequencyEdgeInteger() != 0 && temp.getUpperFrequencyEdgeInteger() != 0 ){
				numValidSections ++;
			}
		}
		
		// Allocate the array to hold only the valid sections		
		rc = new SweepSection[numValidSections];
		
		// Add the sections to the array
		int curIdx = 1;
		for ( int i = 0; i < v1Sections; i++ ){
			temp = new SweepSection();				
			temp.buildFromBytes(payloadData, 5*i);
			if ( temp.getLowerFrequencyEdgeInteger() != 0 && temp.getUpperFrequencyEdgeInteger() != 0 ){
			// Only use the sweep section if it is not zero.
				// Use '<< 4' because the index is stored in the upper nibble of the index byte.
				temp.setSweepDefinition(curIdx << 4, numValidSections);
				curIdx ++;
				
				rc[i] = temp;
			}			
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
