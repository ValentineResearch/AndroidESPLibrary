/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

public class AuxilaryData 
{
	private boolean m_soft;
	private boolean m_TSHoldOff;
	private boolean m_sysStatus;
	private boolean m_displayOn;
	private boolean m_euroMode;
	private boolean m_customSweep;
	private boolean m_legacy;
	private boolean m_reserved2;
	
	public AuxilaryData ()
	{
		// Nothing to do in the empty constructor
	}
	
	/**
	 *  Use the copy constructor to make a deep copy of this object
	 * @param src
	 */
	public AuxilaryData (AuxilaryData src)
	{
		 m_soft = src.m_soft;
		 m_TSHoldOff = src.m_TSHoldOff;
		 m_sysStatus = src.m_sysStatus;
		 m_displayOn = src.m_displayOn;
		 m_euroMode = src.m_euroMode;
		 m_customSweep = src.m_customSweep;
		 m_legacy = src.m_legacy;
		 m_reserved2 = src.m_reserved2;
	}

	/**
	 * Method to clear all bits in this object
	 */
	public void clear()
	{
		setFromByte ( (byte)0x00 );
	}
	
	/**
	 * This method will compare the contents of this object to the object passed in to see if all of the contents are equal.
	 * 
	 * @param src -The source object to use for the comparison.
	 * 
	 * @return true if ALL data in this object is equal to the object passed in, else false. 
	 */
	public boolean isEqual(AuxilaryData src)
	{
		if (  m_soft != src.m_soft) { return false;	}
		if (  m_TSHoldOff != src.m_TSHoldOff) { return false;	}
		if (  m_sysStatus != src.m_sysStatus) { return false;	}
		if (  m_displayOn != src.m_displayOn) { return false;	}
		if (  m_euroMode != src.m_euroMode) { return false;	}
		if (  m_customSweep != src.m_customSweep) { return false;	}
		if (  m_legacy != src.m_legacy) { return false;	}
		if (  m_reserved2 != src.m_reserved2) { return false;	}
		
		return true;
	}
	
	/**
	 * Getter to retrieve the soft bit.
	 * 
	 * @return Returns true if the soft bit is set, otherwise false is returned.
	 */
	public boolean getSoft()
	{
		return m_soft;
	}
	
	/**
	 * Getter to retrieve the time slice holdoff bit.
	 * 
	 * @return Returns true if the TS holdoff bit is set, otherwise false is returned.
	 */
	public boolean getTSHoldOff()
	{
		return m_TSHoldOff;
	}
	
	/**
	 * Getter to retrieve the system status bit.
	 * 
	 * @return Returns true of the system status bit is set, otherwise false is returned.
	 */
	public boolean getSysStatus()
	{
		return m_sysStatus;
	}
	
	/**
	 * Getter to retrieve the display on bit.
	 * 
	 * @return Returns true of the display on bit is set, otherwise false is returned.
	 */
	public boolean getDisplayOn()
	{
		return m_displayOn;
	}
	
	/**
	 * Getter to retrieve the euro mode bit.
	 * 
	 * @return returns true if the Euro Mode bit is set, otherwise false is returned.
	 */
	public boolean getEuroMode()
	{
		return m_euroMode;
	}
	
	/**
	 * Getter to retrieve the custom sweep bit. 
	 * 
	 * @return Returns true if the custom sweep bit is set, otherwise false is returned.
	 */
	public boolean getCustomSweep()
	{
		return m_customSweep;
	}
	
	/**
	 * Getter to retrieve the legacy bit.
	 * 
	 * @return	Returns true of the legacy bit is set, otherwise false is returned.
	 */
	public boolean getLegacy()
	{
		return m_legacy;
	}
	
	/**
	 * Getter to retrieve the Reserved2 bit.
	 * 
	 * @return	Returns true if the reserved by is set, otherwise false is returned.
	 */
	public boolean getReserved2()
	{
		return m_reserved2;
	}
	
	/**
	 * Method to set the value of this object using the byte passed in.
	 * 
	 * @param _data	packed byte containing the auxiliary data.
	 */
	public void setFromByte(byte _data)
	{
		/*
		07 06 05 04 03 02 01 00
		|  |  |  |  |  |  |  |
		|  |  |  |  |  |  |  \-- Soft
		|  |  |  |  |  |  \----- TS Holdoff
		|  |  |  |  |  \-------- Sys. Status
		|  |  |  |  \----------- Display On
		|  |  |  \-------------- Euro Mode
		|  |  \----------------- Custom Sweep
		|  \-------------------- ESP/Legacy
		\----------------------- Reserved
		*/
		
		if ((_data & 1) > 0)
		{
			m_soft = true;
		}
		else
		{
			m_soft = false;
		}
		
		if ((_data & 2) > 0)
		{
			m_TSHoldOff = true;
		}
		else
		{
			m_TSHoldOff = false;
		}
		
		if ((_data & 4) > 0)
		{
			m_sysStatus = true;
		}
		else
		{
			m_sysStatus = false;
		} 
		
		if ((_data & 8) > 0)
		{
			m_displayOn = true;
		}
		else
		{
			m_displayOn = false;
		} 
		
		if ((_data & 16) > 0)
		{
			m_euroMode = true;
		}
		else
		{
			m_euroMode = false;
		} 
		
		if ((_data & 32) > 0)
		{
			m_customSweep = true;
		}
		else
		{
			m_customSweep = false;
		}
		
		if ((_data & 64) > 0)
		{
			m_legacy = true;
		}
		else
		{
			m_legacy = false;
		}
		
		if ((_data & 128) > 0)
		{
			m_reserved2 = true;
		}
		else
		{
			m_reserved2 = false;
		}
	}
}
