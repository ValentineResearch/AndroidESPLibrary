/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class SweepDefinition 
{
	private int m_index;
	private boolean m_commit;
	
	private byte m_msbUpperFrequencyEdge;
	private byte m_lsbUpperFrequencyEdge;
	
	private Integer m_upperFrequencyEdge;
	
	private byte m_msbLowerFrequencyEdge;
	private byte m_lsbLowerFrequencyEdge;
	
	private Integer m_lowerFrequencyEdge;
	
	/**
	 * Gets the index of the sweep.
	 * 
	 * @return	The index of the sweep.
	 */
	public int getIndex()
	{
		return m_index;
	}
	/**
	 * Returns whether the sweep definition has been committed or not.
	 * 
	 * @return	True if the sweep definition has been committed, otherwise false.s
	 */
	public boolean getCommit()
	{
		return	m_commit;
	}
	
	/**
	 * Gets the sweeps definitions upper edge frequency.
	 * 
	 * @return	The sweep definitions upper edge frequency.
	 */
	public Integer getUpperFrequencyEdge()
	{
		return m_upperFrequencyEdge;
	}
	
	/**
	 * Gets the sweep definitions lower edge frequency.
	 * 
	 * @return The sweep definitions lower edge frequency.
	 */
	public Integer getLowerFrequencyEdge()
	{
		return m_lowerFrequencyEdge;
	}
	
	/**
	 * Sets the sweep definitions' index.
	 * 
	 * @param _index	The index of the sweep index.
	 */
	public void setIndex(int _index)
	{
		m_index = _index;
	}
	
	/**
	 * Sets the sweep definition commit state.
	 * @param _commit true if the sweep definition has been commited, otherwise false. 
	 */
	public void setCommit(boolean _commit)
	{
		m_commit = _commit;
	}
	
	/**
	 * Sets the sweep definition upper edge frequency.
	 * 
	 * @param _upperFrequencyEdge	The frequency of the sweep definitions upper edge.
	 */
	public void setUpperFrequencyEdge(Integer _upperFrequencyEdge)
	{
		m_upperFrequencyEdge = _upperFrequencyEdge;
	}
	
	/**
	 * Sets the sweep definition lower edge frequency.
	 * 
	 * @param _lowerFrequencyEdge	The frequency of the sweep definitions lower edge.
	 */
	public void setLowerFrequencyEdge(Integer _lowerFrequencyEdge)
	{
		m_lowerFrequencyEdge = _lowerFrequencyEdge;
	}
	
	/**
	 * Sets the sweeps definitions values using data from the passed in byte.
	 * @param _data	The byte containing data about this sweep definitions.
	 */
	public void buildFromBytes(byte[] _data)
	{
		/*
		Sweep Index byte definition
		07 06 05 04 03 02 01 00
		|  |  |  |  |  |  |  |
		|  |  |  |  |  |  |  \-- Index bit 0
		|  |  |  |  |  |  \----- Index bit 1
		|  |  |  |  |  \-------- Index bit 2
		|  |  |  |  \----------- Index bit 3
		|  |  |  \-------------- Index bit 4
		|  |  \----------------- Index bit 5
		|  \-------------------- Commit Changes (see Custom Sweeps)
		\----------------------- Reserved
		*/
		
		m_index = _data[0] & 0x3f;
		if ((_data[0] & 64) > 1)
		{
			m_commit = true;
		}
		else
		{
			m_commit = false;
		}
		
		m_msbUpperFrequencyEdge = _data[1];
		m_lsbUpperFrequencyEdge = _data[2];
		
		ByteBuffer b = ByteBuffer.allocate(4);
		b.put((byte) 0);
		b.put((byte) 0);
		b.put(m_msbUpperFrequencyEdge);
		b.put(m_lsbUpperFrequencyEdge);
		int temp = b.getInt(0);
		
		
		NumberFormat nf = DecimalFormat.getInstance();
		
		nf.setMaximumIntegerDigits(2);
		nf.setMaximumFractionDigits(3);
		
		m_upperFrequencyEdge = temp;
		
		m_msbLowerFrequencyEdge = _data[3];
		m_lsbLowerFrequencyEdge = _data[4];

		b = ByteBuffer.allocate(4);
		b.put((byte) 0);
		b.put((byte) 0);
		b.put(m_msbLowerFrequencyEdge);
		b.put(m_lsbLowerFrequencyEdge);
		temp = b.getInt(0);

		m_lowerFrequencyEdge = temp;
	}
	
	/**
	 * Creates a byte array reflecting this sweep definition.
	 * 
	 * @return	byte array reflecting this sweep definition.
	 */
	public byte[] buildBytes()
	{
		/*
		Sweep Index byte definition
		07 06 05 04 03 02 01 00
		|  |  |  |  |  |  |  |
		|  |  |  |  |  |  |  \-- Index bit 0
		|  |  |  |  |  |  \----- Index bit 1
		|  |  |  |  |  \-------- Index bit 2
		|  |  |  |  \----------- Index bit 3
		|  |  |  \-------------- Index bit 4
		|  |  \----------------- Index bit 5
		|  \-------------------- Commit Changes (see Custom Sweeps)
		\----------------------- Reserved
		*/
		
		byte[] data = new byte[5]; 
		
		data[0] = (byte) (m_index & 0x3f);

		if (m_commit)
		{
			data[0] = (byte) (data[0] | 64);
		}
		
		data[0] = (byte) (data[0] | 0x80);
		
		ByteBuffer b = ByteBuffer.allocate(4);
		int temp = m_upperFrequencyEdge;
		b.putInt(temp);

		byte[] result = b.array();
		
		data[1] = result[2];
		data[2] = result[3];
		
		b = ByteBuffer.allocate(4);
		temp = m_lowerFrequencyEdge;
		b.putInt(temp);

		result = b.array();
		
		data[3] = result[2];
		data[4] = result[3];
		
		return data;
	}
}
