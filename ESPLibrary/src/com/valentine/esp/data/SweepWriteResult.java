/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

public class SweepWriteResult 
{
	private boolean m_success;
	private int m_errorIndex;
	
	/**
	 * Returns if the attempted sweep write was successful.
	 * 
	 * @return	True fi the sweep write succeeded, otherwise false.
	 */
	public boolean getSuccess() 
	{
		return m_success;
	}
	
	/**
	 * Get then error index of the sweep write.
	 * 
	 * @return Returns -1 if the sweep write failed, otherwise the index of the error.
	 */
	public int getErrorIndex()
	{
		return m_errorIndex;
	}
	
	/**
	 * Set up the sweep write results using the data from the passed in byte.
	 * 
	 * @param _data	byte containing data about the sweep write.
	 */
	public void buildFromByte(byte _data)
	{
		if (_data == 0)
		{
			m_success = true;
			m_errorIndex = -1;
		}
		else
		{
			m_success = false;
			m_errorIndex = _data;
		}
	}
}
