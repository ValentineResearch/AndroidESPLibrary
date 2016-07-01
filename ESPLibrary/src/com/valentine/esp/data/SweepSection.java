/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

import java.nio.ByteBuffer;

public class SweepSection 
{
	private SweepDefinitionIndex m_sweepDefinition;
	
	private int m_upperFrequencyEdgeInt;	
	private int m_lowerFrequencyEdgeInt;
	
	public SweepSection ()
	{
		// Allow the default constructor.
	}
	
	public SweepSection (int lowerFrequencyEdgeInt, int upperFrequencyEdgeInt)
	{
		m_upperFrequencyEdgeInt = upperFrequencyEdgeInt;
		m_lowerFrequencyEdgeInt = lowerFrequencyEdgeInt;
	}

	/**
	 * Gets the sweep definition index.
	 * @return	Index of this sweep definition.
	 */
	public SweepDefinitionIndex getSweepDefinitionIndex()
	{
		return m_sweepDefinition;
	}
	
	/**
	 * Get the upper edge frequency of this sweep definition.
	 * 	
	 * @return The upper edge frequency of this sweep definition.
	 */
	public int getUpperFrequencyEdgeInteger()
	{
		return m_upperFrequencyEdgeInt;
	}	

	/**
	 * Get the lower edge frequency of this sweep definition.
	 * 	
	 * @return The lower edge frequency of this sweep definition.
	 */
	public int getLowerFrequencyEdgeInteger()
	{
		return m_lowerFrequencyEdgeInt;
	}
	
	/**
	 * Sets the upper edge frequency of the sweep definition. 
	 * 
	 * @param _freq	The frequency to set the sweep definitions' upper edge at.
	 */
	public void setUpperFrequencyEdgeInteger(int _freq)
	{
		m_upperFrequencyEdgeInt = _freq;
	}
	
	/**
	 * Sets the lower edge frequency of the sweep definition.
	 * 
	 * @param _freq	The frequency to set the sweep definitions' lower edge at.
	 */
	public void setLowerFrequencyEdgeInteger(int _freq)
	{
		m_lowerFrequencyEdgeInt = _freq;
	}
	
	/**
	 * Sets the index and number of available sections for this sweeps.
	 * 
	 * @param index	The index of the sweep definition.
	 * @param numSections	The number of available section.
	 */
	public void setSweepDefinition (int index, int numSections)
	{
		m_sweepDefinition.setCurrentSweepIndex(index);
		m_sweepDefinition.setNumberOfSectionsAvailable(numSections);
	}
	
	/**
	 * Sets the sweep section data using the bytes stored inside of the passed in byte array.
	 * 
	 * @param _data	byte array containing data about about this sweep section.
	 * @param _startIndex	The starting index used to retrieve the bytes containing data about this sweep section.
	 */
	public void buildFromBytes(byte[] _data, int _startIndex)
	{
		m_sweepDefinition = new SweepDefinitionIndex();
		m_sweepDefinition.buildFromByte(_data[_startIndex]);
		
		byte msbUpperFrequencyEdge = _data[_startIndex+1];
		byte lsbUpperFrequencyEdge = _data[_startIndex+2];
		
		ByteBuffer b = ByteBuffer.allocate(4);
		b.put((byte) 0);
		b.put((byte) 0);
		b.put(msbUpperFrequencyEdge);
		b.put(lsbUpperFrequencyEdge);
		int temp = b.getInt(0);
		
		//m_upperFrequencyEdge = temp / 1000.0f;
		
		m_upperFrequencyEdgeInt = temp;
		
		byte msbLowerFrequencyEdge = _data[_startIndex+3];
		byte lsbLowerFrequencyEdge = _data[_startIndex+4];
		
		b = ByteBuffer.allocate(4);
		b.put((byte) 0);
		b.put((byte) 0);
		b.put(msbLowerFrequencyEdge);
		b.put(lsbLowerFrequencyEdge);
		temp = b.getInt(0);
		
		//m_lowerFrequencyEdge = temp / 1000.0f;
		
		m_lowerFrequencyEdgeInt = temp;
	}	
}
