/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

public class SignalStrengthData 
{
	private boolean m_b0;
	private boolean m_b1;
	private boolean m_b2;
	private boolean m_b3;
	private boolean m_b4;
	private boolean m_b5;
	private boolean m_b6;
	private boolean m_b7;

	private byte m_rawData;
	
	public SignalStrengthData ()
	{
		// Nothing to do in the empty constructor
	}
	
	/**
	 *  Use the copy constructor to make a deep copy of this object
	 * @param src SignalStrenghData object used to make an exact copy.
	 */
	public SignalStrengthData (SignalStrengthData src)
	{
		m_b0 = src.m_b0;
		m_b1 = src.m_b1;
		m_b2 = src.m_b2;
		m_b3 = src.m_b3;
		m_b4 = src.m_b4;
		m_b5 = src.m_b5;
		m_b6 = src.m_b6;
		m_b7 = src.m_b7;

		m_rawData = src.m_rawData;
	}
		
	/**
	 * This method will compare the contents of this object to the object passed in to see if all of the contents are equal.
	 * 
	 * @param src -The source object to use for the comparison.
	 * 
	 * @return true if ALL data in this object is equal to the object passed in, else false. 
	 */
	public boolean isEqual(SignalStrengthData src)
	{
		if (  m_rawData != src.m_rawData){
			return false;
		}
		if (  m_b0 != src.m_b0) { return false;	}
		if (  m_b1 != src.m_b1) { return false;	}
		if (  m_b2 != src.m_b2) { return false;	}
		if (  m_b3 != src.m_b3) { return false;	}
		if (  m_b4 != src.m_b4) { return false;	}
		if (  m_b5 != src.m_b5) { return false;	}
		if (  m_b6 != src.m_b6) { return false;	}
		if (  m_b7 != src.m_b7) { return false;	}
		
		return true;
	}
	
	/**
	 *	Clears signal strength data represented by this object.
	 */
	public void clear()
	{
		setFromByte ( (byte)0x00 );
	}
	
	/**
	 * Getter to determine whether or not the left most signal strength led is turned on.
	 * 
	 * @return	Returns true if the signal strength led is on, otherwise false is returned.
	 */
	public boolean getB0()
	{
		return m_b0;
	}
	
	/**
	 * Getter to determine whether or not signal strength led #1 is turned on.
	 * 
	 * @return	Returns true if the signal strength led is on, otherwise false is returned.
	 */
	public boolean getB1()
	{
		return m_b1;
	}
	
	/**
	 * Getter to determine whether or not signal strength led #2 is turned on.
	 * 
	 * @return	Returns true if the signal strength led is on, otherwise false is returned.
	 */
	public boolean getB2()
	{
		return m_b2;
	}
	
	/**
	 * Getter to determine whether or not signal strength led #3 is turned on.
	 * 
	 * @return	Returns true if the signal strength led is on, otherwise false is returned.
	 */
	public boolean getB3()
	{
		return m_b3;
	}
	
	/**
	 * Getter to determine whether or not signal strength led #4 is turned on.
	 * 
	 * @return	Returns true if the signal strength led is on, otherwise false is returned.
	 */
	public boolean getB4()
	{
		return m_b4;
	}
	
	/**
	 * Getter to determine whether or not signal strength led #5 is turned on.
	 * 
	 * @return	Returns true if the signal strength led is on, otherwise false is returned.
	 */
	public boolean getB5()
	{
		return m_b5;
	}
	
	/**
	 * Getter to determine whether or not signal strength led #6 is turned on.
	 * 
	 * @return	Returns true if the signal strength led is on, otherwise false is returned.
	 */
	public boolean getB6()
	{
		return m_b6;
	}
	
	/**
	 * Getter to determine whether or not the right most signal strength led is turned on.
	 * 
	 * @return	Returns true if the signal strength led is on, otherwise false is returned.
	 */
	public boolean getB7()
	{
		return m_b7;
	}
	
	/**
	 * Getter to retrieve the raw data byte this object represents.
	 * 
	 * @return	Returns the raw byte data that represents the signal strength.
	 */
	public byte getRawData()
	{
		return m_rawData;
	}
	
	/**
	 * Gets the number of LEDs that are turned on.
	 * 
	 * @return Returns the number of lit LEDs.
	 */
	public int getNumberOfLEDs()
	{
		if (m_rawData == 0x00)
		{
			return 0;
		}
		else if (m_rawData == 0x01)
		{
			return 1;
		}
		else if (m_rawData == 0x03)
		{
			return 2;
		}
		else if (m_rawData == 0x07)
		{
			return 3;
		}
		else if (m_rawData == 0x0F)
		{
			return 4;
		}
		else if (m_rawData == 0x1f)
		{
			return 5;
		}
		else if (m_rawData == 0x3f)
		{
			return 6;
		}
		else if (m_rawData == 0x7f)
		{
			return 7;
		}
		else
		{
			return 8;
		}
	}
	
	/**
	 * Set the signal strength values using the data inside of the passed in byte.
	 * 
	 * @param _data 	Packed byte containing signal strength data.
	 */
	public void setFromByte(byte _data)
	{
		/*
		07 06 05 04 03 02 01 00
		|  |  |  |  |  |  |  |
		|  |  |  |  |  |  |  \-- b0 (left)
		|  |  |  |  |  |  \----- b1
		|  |  |  |  |  \-------- b2
		|  |  |  |  \----------- b3
		|  |  |  \-------------- b4
		|  |  \----------------- b5
		|  \-------------------- b6
		\----------------------- b7 (right)
		*/
		
		m_rawData = _data;
		
		if ((_data & 1) > 0)
		{
			m_b0 = true;
		}
		else
		{
			m_b0 = false;
		}
		
		if ((_data & 2) > 0)
		{
			m_b1 = true;
		}
		else
		{
			m_b1 = false;
		}
		
		if ((_data & 4) > 0)
		{
			m_b2 = true;
		}
		else
		{
			m_b2 = false;
		} 
		
		if ((_data & 8) > 0)
		{
			m_b3 = true;
		}
		else
		{
			m_b3 = false;
		} 
		
		if ((_data & 16) > 0)
		{
			m_b4 = true;
		}
		else
		{
			m_b4 = false;
		} 
		
		if ((_data & 32) > 0)
		{
			m_b5 = true;
		}
		else
		{
			m_b5 = false;
		}
		
		if ((_data & 64) > 0)
		{
			m_b6 = true;
		}
		else
		{
			m_b6 = false;
		}
		
		if ((_data & 128) > 0)
		{
			m_b7 = true;
		}
		else
		{
			m_b7 = false;
		}
	}
}
