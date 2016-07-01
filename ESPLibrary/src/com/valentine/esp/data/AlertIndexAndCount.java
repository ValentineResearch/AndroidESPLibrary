/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

public class AlertIndexAndCount 
{
	private int m_count;
	private int m_index;
	
	/**
	 * Gets the number of current alerts present.
	 * @return	The number of present alerts.
	 */
	public int getCount()
	{
		return m_count;
	}
	/**
	 * Gets the index of the alert.
	 * 
	 * @return The index in the alertlist of this alert. 
	 */
	public int getIndex()
	{
		return m_index;
	}
	
	/**
	 * Gets the alert count and the index of the alert from the passed in byte.'
	 * 
	 * @param _data	The byte containing data about the alert.
	 */
	public void buildFromByte(byte _data)
	{
		/*
		 07 06 05 04 03 02 01 00
		 |  |  |  |  |  |  |  |
		 |  |  |  |  |  |  |  \-- Count B0 (Number of alerts present)
		 |  |  |  |  |  |  \----- Count B1
		 |  |  |  |  |  \-------- Count B2
		 |  |  |  |  \----------- Count B3
		 |  |  |  \-------------- Index B0 (Index of this alert)
		 |  |  \----------------- Index B1
		 |  \-------------------- Index B2
		 \----------------------- Index B3
		*/
		
		m_count = _data & 0x0F;
		m_index = _data & 0xF0;
		m_index = m_index >> 4;
	}
}


