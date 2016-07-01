/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

import java.nio.ByteBuffer;

public class AlertData 
{
	
	public enum SignalDirection {
		// The QuadscreenFragment is dependent on the order of this enum.
		ORIENTATION_FRONT(0),
		ORIENTATION_SIDE(1),
		ORIENTATION_REAR(2),
		ORIENTATION_INVALID(3);
		
		private final int index;
		
		SignalDirection (int index){
			this.index = index;
		}
		
		public int getValue(){
			return index;
		}
		
		public static SignalDirection getEnumForPos(int pos){
			return values()[pos];
		}
	};
	
	private AlertIndexAndCount m_alertIndexAndCount;
	private byte m_frequencyMSB;
	private byte m_frequencyLSB;
	private int m_frequency;
	private int m_frontSignalStrength;
	private int m_rearSignalStrength;
	private byte m_frontSignalStrengthByte;
	private byte m_rearSignalStrengthByte;
	private BandArrowData m_bandArrowData;
	private boolean m_priorityAlert;
	
	/**
	 * Returns the AlertIndex count
	 * 
	 * @return m_alertIndexAndCount 	The AlertIndexCount.
	 */
	public AlertIndexAndCount getAlertIndexAndCount()
	{
		return m_alertIndexAndCount;
	}
	
	/**
	 * Returns the current frequency of the signal
	 * 
	 * @return m_frequency		The current signal's frequency
	 */
	public int getFrequency()
	{
		return m_frequency;
	}

	/**
	 * Returns the signal strength for the front side of the V1
	 * 
	 * @return m_frontSignalStrength		The signal strength from the front of the device
	 */
	public int getFrontSignalStrength()
	{
		return m_frontSignalStrength;
	}
	
	/**
	 * Returns the signal strength for the rear side of the V1
	 * 
	 * @return m_rearSignalStrength			The signal strength from the rear of the device
	 */
	public int getRearSignalStrength()
	{
		return m_rearSignalStrength;
	}

	/**
	 * Returns the band arrow data
	 * 
	 * @return m_bandArrowData	The data of the arrow bands of the device
	 */
	public BandArrowData getBandArrowData()
	{
		return m_bandArrowData;
	}
	
	/**
	 * Returns the priority of the alert
	 * 
	 * @return m_priorityAlert		The priority of the alert
	 */
	public boolean getPriorityAlert()
	{
		return m_priorityAlert;
	}
	
	/**
	 * Sets the priority of the alert
	 * 
	 * @param _priority		Flag that set if this alert is priority.
	 */
	public void setPriorityAlert(boolean _priority)
	{
		m_priorityAlert = _priority;
	}
	
	/**
	 * Returns the number of front LED's currently receiving a signal
	 * 
	 * @return m_frontSignalStrengthByte		The front number of LED's receiving a signal
	 */
	public int getFrontSignalNumberOfLEDs()
	{
		return getNumberOfLEDs(m_frontSignalStrengthByte);
	}
	
	/**
	 * Returns the number of rear LED's currently receiving a signal
	 * 
	 * @return m_rearSignalStrengthByte		The rear number of LED's receiving a signal
	 */
	public int getRearSignalNumberOfLEDs()
	{
		return getNumberOfLEDs(m_rearSignalStrengthByte);
	}
	
	/**
	 * Returns the number of LED's
	 * 
	 * @param _data 	A byte array containing data about the LED's
	 * @return int 		Number of LED's
	 */
	private int getNumberOfLEDs(byte _data)
	{
		int unsigned = _data & 0xff;
		if (unsigned == 0x00)
		{
			return 0;
		}
		
		if (m_bandArrowData.getXBand())
		{
			if (unsigned >= 0xd0)
			{
				return 8;
			}
			else if (unsigned >= 0xc5)
			{
				return 7;
			}
			else if (unsigned >= 0xbd)
			{
				return 6;
			}
			else if (unsigned >= 0xb4)
			{
				return 5;
			}
			else if (unsigned >= 0xaa)
			{
				return 4;
			}
			else if (unsigned >= 0xa0)
			{
				return 3;
			}
			else if (unsigned >= 0x96)
			{
				return 2;
			}
			else if (unsigned >= 0x01)
			{
				return 1;
			}
		}
		else if ((m_bandArrowData.getKuBand()) || (m_bandArrowData.getKBand()))
		{
			if (unsigned >= 0xc2)
			{
				return 8;
			}
			else if (unsigned >= 0xb8)
			{
				return 7;
			}
			else if (unsigned >= 0xae)
			{
				return 6;
			}
			else if (unsigned >= 0xa4)
			{
				return 5;
			}
			else if (unsigned >= 0x9a)
			{
				return 4;
			}
			else if (unsigned >= 0x90)
			{
				return 3;
			}
			else if (unsigned >= 0x88)
			{
				return 2;
			}
			else if (unsigned >= 0x01)
			{
				return 1;
			}
		}
		else if (m_bandArrowData.getKaBand())
		{
			if (unsigned >= 0xba)
			{
				return 8;
			}
			else if (unsigned >= 0xb3)
			{
				return 7;
			}
			else if (unsigned >= 0xac)
			{
				return 6;
			}
			else if (unsigned >= 0xa5)
			{
				return 5;
			}
			else if (unsigned >= 0x9e)
			{
				return 4;
			}
			else if (unsigned >= 0x97)
			{
				return 3;
			}
			else if (unsigned >= 0x90)
			{
				return 2;
			}
			else if (unsigned >= 0x01)
			{
				return 1;
			}
		}
	
		return 0;
	}
	
	/**
	 *  Sets up the alertdata using the data stored inside of passed in byte array.
	 * 	
	 * @param _bytes		The byte array containing data about an alert.
	 */
	public void buildFromData(byte[] _bytes)
	{
		m_alertIndexAndCount = new AlertIndexAndCount();
		m_bandArrowData = new BandArrowData();
		
		m_alertIndexAndCount.buildFromByte(_bytes[0]);
		m_frequencyMSB = _bytes[1];
		m_frequencyLSB = _bytes[2];
		
		ByteBuffer b = ByteBuffer.allocate(4);
		b.put((byte) 0);
		b.put((byte) 0);
		b.put(m_frequencyMSB);
		b.put(m_frequencyLSB);
		int temp = b.getInt(0);
		
		m_frequency = temp;
		
		m_frontSignalStrength = (int)(_bytes[3] & 0xff);
		m_rearSignalStrength = (int)(_bytes[4] & 0xff);
		
		m_frontSignalStrengthByte = _bytes[3];
		m_rearSignalStrengthByte = _bytes[4];
		
		m_bandArrowData.buildFromByte(_bytes[5]);
		
		if ((_bytes[6] & 128) > 0)
		{
			m_priorityAlert = true;
		}
		else
		{
			m_priorityAlert = false;
		}
	}
	
	/**
	 * Returns the orientation of the signal. Such as front, side or rear.
	 * 
	 * @return		The orientation of the signal.
	 */
	public SignalDirection getOrientation()
	{
		if (m_bandArrowData.getFront()) {
			return SignalDirection.ORIENTATION_FRONT;
		}
		else if (m_bandArrowData.getSide()) {
			return SignalDirection.ORIENTATION_SIDE;
		}
		else if (m_bandArrowData.getRear()) {
			return SignalDirection.ORIENTATION_REAR;
		}
		return SignalDirection.ORIENTATION_INVALID;
	}

	/**
	 * Returns the number of LED's (signal strength) for a particular direction.
	 * 
	 * @param orientation	The orientation of the signal.
	 * 
	 * @return The number of active LED's for the given signal orientation.
	 */
	public int getNumBarGraphLED(SignalDirection orientation)
	{
		if(orientation == SignalDirection.ORIENTATION_FRONT){
			return getFrontSignalNumberOfLEDs();
		}
		else if(orientation == SignalDirection.ORIENTATION_SIDE){	
			if (getFrontSignalNumberOfLEDs() > getRearSignalNumberOfLEDs())
				return getFrontSignalNumberOfLEDs();
			else
				return getRearSignalNumberOfLEDs();		
		}
		else if(orientation == SignalDirection.ORIENTATION_REAR){
			return getRearSignalNumberOfLEDs();
		}
		return 4;
	}
}
