/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

public class BogeyCounterData 
{
	private byte m_rawData;
	private byte m_rawDataWithOutDp;
	private boolean m_segA;
	private boolean m_segB;
	private boolean m_segC;
	private boolean m_segD;
	private boolean m_segE;
	private boolean m_segF;
	private boolean m_segG;
	private boolean m_dp;
	
	public BogeyCounterData ()
	{
		// Nothing to do in the empty constructor
	}
	
	/**
	 *  Use the copy constructor to make a deep copy of this object
	 *  
	 * @param src BogeyCountwerData object to make an exact copy of.
	 */
	public BogeyCounterData (BogeyCounterData src)
	{
		m_rawData = src.m_rawData;
		m_rawDataWithOutDp = src.m_rawDataWithOutDp;
		
		 m_segA = src.m_segA;
		 m_segB = src.m_segB;
		 m_segC = src.m_segC;
		 m_segD = src.m_segD;
		 m_segE = src.m_segE;
		 m_segF = src.m_segF;
		 m_segG = src.m_segG;
		 m_dp = src.m_dp;
	}
	
	/**
	 * This method will compare the contents of this object to the object passed in to see if all of the contents are equal.
	 * 
	 * @param src -The source object to use for the comparison.
	 * 
	 * @return true if ALL data in this object is equal to the object passed in, else false. 
	 */
	public boolean isEqual(BogeyCounterData src)
	{
		if (  m_rawData != src.m_rawData){
			return false;
		}
		if (  m_rawDataWithOutDp != src.m_rawDataWithOutDp){
			return false;
		}
		if (  m_segA != src.m_segA){
			return false;
		}
		if (  m_segB != src.m_segB){
			return false;
		}
		if (  m_segC != src.m_segC){
			return false;
		}
		if (  m_segD != src.m_segD){
			return false;
		}
		if (  m_segE != src.m_segE){
			return false;
		}
		if (  m_segF != src.m_segF){
			return false;
		}
		if (  m_segG != src.m_segG){
			return false;
		}
		if (  m_dp != src.m_dp){
			return false;
		}
		
		return true;
	}
	
	/**
	 * Method to clear the data in this object
	 */
	public void clear()
	{
		setFromByte ( (byte)0x00 );
	}

	/**
	 * Indicates if Segment A is active.
	 * 
	 * @return	Returns true if the segment is active, otherwise false is returned.
	 */
	public boolean getSegA()
	{
		return m_segA;
	}
	
	/**
	 * Indicates if Segment B is active.
	 * 
	 * @return	Returns true if the segment is active, otherwise false is returned.
	 */
	public boolean getSegB()
	{
		return m_segB;
	}
	
	/**
	 * Indicates if Segment C is active.
	 * 
	 * @return	Returns true if the segment is active, otherwise false is returned.
	 */
	public boolean getSegC()
	{
		return m_segC;
	}
	
	/**
	 * Indicates if Segment D is active.
	 * 
	 * @return	Returns true if the segment is active, otherwise false is returned.
	 */
	public boolean getSegD()
	{
		return m_segD;
	}
	
	/**
	 * Indicates if Segment E is active.
	 * 
	 * @return	Returns true if the segment is active, otherwise false is returned.
	 */
	public boolean getSegE()
	{
		return m_segE;
	}
	
	/**
	 * Indicates if Segment F is active.
	 * 
	 * @return	Returns true if the segment is active, otherwise false is returned.
	 */
	public boolean getSegF()
	{
		return m_segF;
	}
	
	/**
	 * Indicates if Segment G is active.
	 * 
	 * @return	Returns true if the segment is active, otherwise false is returned.
	 */
	public boolean getSegG()
	{
		return m_segG;
	}
	
	/**
	 * Indicates if the Dp segment is active.
	 * 
	 * @return	Returns true if the Dp segment is active, otherwise false is returned. 
	 */
	public boolean getDp()
	{
		return m_dp;
	}
	
	/**
	 * Returns the Bogey counter raw byte data.
	 * 
	 * @return	A byte containing the raw bogey counter data (seven segment value + decimal point).
	 */
	public byte getRawData()
	{
		return m_rawData;
	}
	/**
	 * Returns the modified Bogey counterbyte data.
	 * 
	 * @return	A byte containing the bogey counter data (seven segment value - decimal point).
	 */
	public byte getRawDataWithOutDecimalPoint()
	{
		return m_rawDataWithOutDp;
	}
	
	/**
	 * Method to set the value of this object using the byte passed in.
	 * 
	 * @param _data packed byte containing Bogey counter data.
	 */
	public void setFromByte(byte _data)
	{
		/*
		07 06 05 04 03 02 01 00
		|  |  |  |  |  |  |  |
		|  |  |  |  |  |  |  \-- Seg a
		|  |  |  |  |  |  \----- Seg b
		|  |  |  |  |  \-------- Seg c
		|  |  |  |  \----------- Seg d
		|  |  |  \-------------- Seg e
		|  |  \----------------- Seg f
		|  \-------------------- Seg g
		\----------------------- dp
		*/
		
		m_rawData = _data;
		m_rawDataWithOutDp = (byte) (_data & 0x7f);
		
		if ((_data & 1) > 0)
		{
			m_segA = true;
		}
		else
		{
			m_segA = false;
		}
		
		if ((_data & 2) > 0)
		{
			m_segB = true;
		}
		else
		{
			m_segB = false;
		}
		
		if ((_data & 4) > 0)
		{
			m_segC = true;
		}
		else
		{
			m_segC = false;
		} 
		
		if ((_data & 8) > 0)
		{
			m_segD = true;
		}
		else
		{
			m_segD = false;
		} 
		
		if ((_data & 16) > 0)
		{
			m_segE = true;
		}
		else
		{
			m_segE = false;
		} 
		
		if ((_data & 32) > 0)
		{
			m_segF = true;
		}
		else
		{
			m_segF = false;
		}
		
		if ((_data & 64) > 0)
		{
			m_segG = true;
		}
		else
		{
			m_segG = false;
		}
		
		if ((_data & 128) > 0)
		{
			m_dp = true;
		}
		else
		{
			m_dp = false;
		}
	}
	
	/**
	 * Method to convert the segments turned on into a letter. The string "?" is returned if the segments do not represent a known letter.
	 * 
	 * @return	Returns the string representation of the active segments.
	 */
	public String convertToLetter()
	{
		String rc = "?";
		
		if (getSegA() && getSegB() && getSegC() && getSegD() && getSegE() && getSegF() && !getSegG())
		{
			rc = "0";
		}
		else if (!getSegA() && getSegB() && getSegC() && !getSegD() && !getSegE() && !getSegF() && !getSegG())
		{
			rc = "1";
		}
		else if (getSegA() && getSegB() && !getSegC() && getSegD() && getSegE() && !getSegF() && getSegG())
		{
			rc = "2";
		}
		else if (getSegA() && getSegB() && getSegC() && getSegD() && !getSegE() && !getSegF() && getSegG())
		{
			rc = "3";
		}
		else if (!getSegA() && getSegB() && getSegC() && !getSegD() && !getSegE() && getSegF() && getSegG())
		{
			rc = "4";
		}
		else if (getSegA() && !getSegB() && getSegC() && getSegD() && !getSegE() && getSegF() && getSegG())
		{
			rc = "5";
		}
		else if (getSegA() && !getSegB() && getSegC() && getSegD() && getSegE() && getSegF() && getSegG())
		{
			rc = "6";
		}
		else if (getSegA() && getSegB() && getSegC() && !getSegD() && !getSegE() && !getSegF() && !getSegG())
		{
			rc = "7";
		}
		else if (getSegA() && getSegB() && getSegC() && getSegD() && getSegE() && getSegF() && getSegG())
		{
			rc = "8";
		}
		else if (getSegA() && getSegB() && getSegC() && getSegD() && !getSegE() && getSegF() && getSegG())
		{
			rc = "9";
		}
		else if (getSegA() && getSegB() && getSegC() && !getSegD() && getSegE() && getSegF() && getSegG())
		{
			rc = "A";
		}
		else if (!getSegA() && !getSegB() && getSegC() && getSegD() && getSegE() && getSegF() && getSegG())
		{
			rc = "b";
		}
		else if (getSegA() && !getSegB() && !getSegC() && getSegD() && getSegE() && getSegF() && !getSegG())
		{
			rc = "C";
		}
		else if (!getSegA() && getSegB() && getSegC() && getSegD() && getSegE() && !getSegF() && getSegG())
		{
			rc = "d";
		}
		else if (getSegA() && !getSegB() && !getSegC() && getSegD() && getSegE() && getSegF() && getSegG())
		{
			rc = "E";
		}
		else if (getSegA() && !getSegB() && !getSegC() && !getSegD() && getSegE() && getSegF() && getSegG())
		{
			rc = "F";
		}
		
		else if (getSegA() && !getSegB() && !getSegC() && getSegD() && !getSegE() && !getSegF() && getSegG())
		{
			rc = "#";
		}
		else if (!getSegA() && !getSegB() && !getSegC() && getSegD() && getSegE() && !getSegF() && !getSegG())
		{
			rc = "&";
		}
		else if (!getSegA() && !getSegB() && !getSegC() && getSegD() && getSegE() && getSegF() && !getSegG())
		{
			rc = "L";
		}
		else if (!getSegA() && getSegB() && getSegC() && getSegD() && getSegE() && !getSegF() && !getSegG())
		{
			rc = "J";
		}
		
		else if (getSegA() && !getSegB() && !getSegC() && getSegD() && getSegE() && getSegF() && !getSegG())
		{
			rc = "C";
		}
		else if (!getSegA() && !getSegB() && !getSegC() && getSegD() && getSegE() && !getSegF() && getSegG())
		{
			rc = "c";
		}
		else if (!getSegA() && getSegB() && getSegC() && getSegD() && getSegE() && getSegF() && !getSegG())
		{
			rc = "U";
		}
		else if (!getSegA() && !getSegB() && getSegC() && getSegD() && getSegE() && !getSegF() && !getSegG())
		{
			rc = "u";
		}
		
		else if (!getSegA() && !getSegB() && !getSegC() && !getSegD() && !getSegE() && !getSegF() && !getSegG())
		{
			rc = " ";
		}
		
		if (getDp())
		{
			rc += ".";
		}
					
		return rc;
	}
	
}
