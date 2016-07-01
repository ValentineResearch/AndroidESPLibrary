/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;


public class BandArrowData 
{
	private boolean m_laser;
	private boolean m_kaBand;
	private boolean m_kBand;
	private boolean m_xBand;
	private boolean m_kuBand;
	
	private boolean m_front;
	private boolean m_side;
	private boolean m_rear;
	
	/**
	 * This is for future use, use the laser information from the BandAndArrowIndicator1 from the most recent 
	 * InfoDisplayInfoData object to know if laser alerts are present.
	 * 
	 * @return	Returns true if the laser bit is set, otherwise false is returned.
	 */
	public boolean getLaser()
	{
		return m_laser;
	}
	
	/**
	 * Returns if the Ka band is active.
	 * 
	 * @return	True if ka band is active otherwise false.
	 */
	public boolean getKaBand()
	{
		return m_kaBand;
	}
	

	/**
	 * Returns if the K band is active.
	 * 
	 * @return	True if K band is active otherwise false.
	 */
	public boolean getKBand()
	{
		return m_kBand;
	}
	

	/**
	 * Returns if the X band is active.
	 * 
	 * @return	True if X band is active otherwise false.
	 */
	public boolean getXBand()
	{
		return m_xBand;
	}
	

	/**
	 * Returns if the Ku band is active.
	 * 
	 * @return	True if Ku band is active otherwise false.
	 */
	public boolean getKuBand()
	{
		return m_kuBand;
	}
	

	/**
	 * Returns if an alert is active in the front.
	 * 
	 * @return	True if there is a an alert in the front.
	 */
	public boolean getFront()
	{
		return m_front;
	}
	
	/**
	 * Returns if an alert is active in the side.
	 * 
	 * @return	True if there is a an alert in the side.
	 */
	public boolean getSide()
	{
		return m_side;
	}
	
	/**
	 * Returns if an alert is active in the rear.
	 * 
	 * @return	True if there is a an alert in the rear.
	 */
	public boolean getRear()
	{
		return m_rear;
	}
	
	/**
	 * Sets this band arrow state information using the data inside of the passed in byte.
	 * 
	 * @param _data		Packed byte that contains band arrow information.
	 */
	public void buildFromByte(byte _data)
	{
		/*	
		 Band/Arrow definition
		 07 06 05 04 03 02 01 00
		 |  |  |  |  |  |  |  |
		 |  |  |  |  |  |  |  \-- LASER
		 |  |  |  |  |  |  \----- Ka BAND
		 |  |  |  |  |  \-------- K BAND
		 |  |  |  |  \----------- X BAND
		 |  |  |  \-------------- Ku Band
		 |  |  \----------------- FRONT
		 |  \-------------------- SIDE
		 \----------------------- REAR
		*/
		
		if ((_data & 1) > 0)
		{
			m_laser = true;
		}
		else
		{
			m_laser = false;
		}
		
		if ((_data & 2) > 0)
		{
			m_kaBand = true;
		}
		else
		{
			m_kaBand = false;
		}
		
		if ((_data & 4) > 0)
		{
			m_kBand = true;
		}
		else
		{
			m_kBand = false;
		} 
		
		if ((_data & 8) > 0)
		{
			m_xBand = true;
		}
		else
		{
			m_xBand = false;
		} 
		
		if ((_data & 16) > 0)
		{
			m_kuBand = true;
		}
		else
		{
			m_kuBand = false;
		} 
		
		if ((_data & 32) > 0)
		{
			m_front = true;
		}
		else
		{
			m_front = false;
		}
		
		if ((_data & 64) > 0)
		{
			m_side = true;
		}
		else
		{
			m_side = false;
		}
		
		if ((_data & 128) > 0)
		{
			m_rear = true;
		}
		else
		{
			m_rear = false;
		}
	}
}
