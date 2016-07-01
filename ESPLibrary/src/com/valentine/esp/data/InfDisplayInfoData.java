/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

public class InfDisplayInfoData 
{
	private BogeyCounterData m_bogeyCounterData1;
	private BogeyCounterData m_bogeyCounterData2;
	private SignalStrengthData m_signalStrengthData;
	private BandAndArrowIndicatorData m_bandAndArrowIndicatorData1;
	private BandAndArrowIndicatorData m_bandAndArrowIndicatorData2;
	private AuxilaryData m_auxData;
	private byte m_auxData1;
	private byte m_auxData2;
	
	public InfDisplayInfoData ()
	{
		// Nothing to do in the empty constructor
	}
	
	/**
	 *  Use the copy constructor to make a deep copy of this object
	 * @param src InfDisplayInfoData object to make an exact copy of.
	 */
	public InfDisplayInfoData (InfDisplayInfoData src)
	{
		m_bogeyCounterData1 = new BogeyCounterData(src.m_bogeyCounterData1);
		m_bogeyCounterData2 = new BogeyCounterData(src.m_bogeyCounterData2);
		m_signalStrengthData = new SignalStrengthData(src.m_signalStrengthData);
		m_bandAndArrowIndicatorData1 = new BandAndArrowIndicatorData(src.m_bandAndArrowIndicatorData1);
		m_bandAndArrowIndicatorData2 = new BandAndArrowIndicatorData(src.m_bandAndArrowIndicatorData2);
		m_auxData = new AuxilaryData(src.m_auxData);
		m_auxData1 = src.m_auxData1;
		m_auxData2 = src.m_auxData2;
	}
		
	/**
	 * Clears the display data for this object. This will produce a blank display
	 */
	public void clear()
	{
		if ( m_bogeyCounterData1 == null ){
			// Create the objects
			noDataInit();
		}
		
		m_bogeyCounterData1.clear();
		m_bogeyCounterData2.clear();
		m_signalStrengthData.clear();
		m_bandAndArrowIndicatorData1.clear();
		m_bandAndArrowIndicatorData2.clear();
		m_auxData.clear();
		m_auxData1 = 0x00;
		m_auxData2 = 0x00;
	}
	
	/**
	 * Gets the seven segment image 0.
	 * 
	 * @return	Returns {@link BogeyCounterData} that represents this packets bogey counter image 1.
	 */
	public BogeyCounterData getBogeyCounterData1()
	{
		return m_bogeyCounterData1;
	}
	
	/**
	 * Gets the seven segment image 1.
	 * 
	 * @return	Returns {@link BogeyCounterData} that represents this packets bogey counter image 2.
	 */
	public BogeyCounterData getBogeyCounterData2()
	{
		return m_bogeyCounterData2;
	}
	
	/**
	 * Gets the Signal Strength Data from this packets.
	 * 
	 * @return	Returns {@link SignalStrengthData} that represents this packets signal strength.
	 */
	public SignalStrengthData getSignalStrengthData()
	{
		return m_signalStrengthData;
	}
	
	/**
	 * Gets the Band Arrow Indicator image 1.
	 * 
	 * @return	Returns {@link BandAndArrowIndicatorData} that represents this packets band and arrow data.
	 */
	public BandAndArrowIndicatorData getBandAndArrowIndicator1()
	{
		return m_bandAndArrowIndicatorData1;
	}
	
	/**
	 * Gets the Band Arrow Indicator image 2.
	 * 
	 * @return	Returns {@link BandAndArrowIndicatorData} that represents this packets band and arrow data.
	 */
	public BandAndArrowIndicatorData getBandAndArrowIndicator2()
	{
		return m_bandAndArrowIndicatorData2;
	}
	
	/**
	 * Gets the Aux data byte 0.
	 * 
	 * @return	Returns {@link AuxilaryData} that represents this packets aux byte 0.
	 */
	public AuxilaryData getAuxData()
	{
		return m_auxData;
	}
	
	/**
	 * Gets the Aux data byte 1.
	 * 
	 * @return	Returns a byte that contains this packets aux byte 1 data. As of now, this byte is reserved.
	 */
	public byte getAux1Data()
	{
		return m_auxData1;
	}
	
	/**
	 * Gets the Aux data byte 2.
	 * 
	 * @return	Returns a byte that contains this packets aux byte 2 data. As of now, this byte is reserved.
	 */
	public byte getAux2Data()
	{
		return m_auxData2;
	}
	
	/**
	 * Sets the first seven segment image.
	 * 
	 * @param	_data containing data about the first seven segment image.
	 */
	public void setBogeyCounterData1(BogeyCounterData _data)
	{
		m_bogeyCounterData1 = _data;
	}
	
	/**
	 * Sets the second seven segment image.
	 * 
	 * @param	_data containing data about the second seven segment image.
	 */
	public void setBogeyCounterData2(BogeyCounterData _data)
	{
		m_bogeyCounterData2 = _data;
	}
	
	/**
	 * Sets the signal strength.
	 * 
	 * @param _data
	 */
	public void setSignalStrengthData(SignalStrengthData _data)
	{
		m_signalStrengthData = _data;
	}
	
	/**
	 * Sets the first band arrow indicator image.
	 * 
	 * @param _data
	 */
	public void setBandAndArrowIndicator1(BandAndArrowIndicatorData _data)
	{
		m_bandAndArrowIndicatorData1 = _data;
	}
	
	/**
	 * Sets the second band arrow indicator image.
	 * 
	 * @param _data
	 */
	public void setBandAndArrowIndicator2(BandAndArrowIndicatorData _data)
	{
		m_bandAndArrowIndicatorData2 = _data;
	}
	
	/**
	 * Sets the first auxillary data byte
	 * 
	 * @param _data
	 */
	public void setAuxData(AuxilaryData _data)
	{
		m_auxData = _data;
	}
	
	/**
	 * Sets the second auxillary data byte.
	 * 
	 * @param _data
	 */
	public void setAux1Data(byte _data)
	{
		m_auxData1 = _data;
	}
	
	/**
	 * Sets the third auxillary data byte.
	 * 
	 * @param _data
	 */
	public void setAux2Data(byte _data)
	{
		m_auxData2 = _data;
	}
	
	/***
	 * Helper method for determining if any alerts are present.
	 * 
	 * @return 	True if there are any alerts otherwise, false.
	 */
	public boolean isActiveAlerts() {
		return getBandAndArrowIndicator1().getFront() || getBandAndArrowIndicator1().getSide() || getBandAndArrowIndicator1().getRear();
	}
	
	/**
	 * Convenience method to extract the V1 mode from the display data represented by this object.
	 * 
	 * @param _previousModeData		Mode data.
	 * 
	 * @return	Returns the ModeData contained in this objects data.
	 */
	public ModeData getMode(ModeData _previousModeData)
	{
		ModeData rc = new ModeData();
		
		byte data = m_bogeyCounterData1.getRawDataWithOutDecimalPoint();
		
		switch (data) {
			case (byte) 0x77:
				rc.setUsaMode(true);
				rc.setAllBogeysMode(true);
				break;
			case 0x18:
				rc.setUsaMode(true);
				rc.setLogicMode(true);
				break;
			case 0x38:
				rc.setUsaMode(true);
				rc.setAdvLogicMode(true);
				break;
			case 0x39:
			case 0x3E:
				rc.setEuroMode(true);
				rc.setAllBogeysMode(true);
				break;
			case 0x1C:
			case 0x58:
				rc.setEuroMode(true);
				rc.setLogicMode(true);
				break;
			default:
				rc.setEuroMode(m_auxData.getEuroMode());
				rc.setCustomSweeps(m_auxData.getCustomSweep());
				break;
		}
		 
		if (rc.getEuroMode())
		{
			rc.setCustomSweeps(m_auxData.getCustomSweep());
		}
		
		return rc;
	}
	
	/**
	 * Method to initialize this object as a blank display
	 */
	public void noDataInit()
	{
		m_bogeyCounterData1 = new BogeyCounterData();
		m_bogeyCounterData2 = new BogeyCounterData();
		m_signalStrengthData = new SignalStrengthData();
		m_bandAndArrowIndicatorData1 = new BandAndArrowIndicatorData();
		m_bandAndArrowIndicatorData2 = new BandAndArrowIndicatorData();
		m_auxData = new AuxilaryData();
	}
	
	/**
	 * This method will compare the contents of this object to the object passed in to see if all of the contents are equal.
	 * 
	 * @param src -The source object to use for the comparison.
	 * 
	 * @return true if ALL data in this object is equal to the object passed in, else false. 
	 */
	public boolean isEqual(InfDisplayInfoData src)
	{
		
		if ( m_auxData1 != src.m_auxData1 ){
			return false;
		}
		
		if ( m_auxData2 != src.m_auxData2 ){
			return false;
		}
		
		if ( !m_bogeyCounterData1.isEqual (src.m_bogeyCounterData1) ){
			return false;
		}
		
		if ( !m_bogeyCounterData2.isEqual (src.m_bogeyCounterData2) ){
			return false;
		}
		
		if ( !m_signalStrengthData.isEqual (src.m_signalStrengthData) ){
			return false;
		}
		
		if ( !m_bandAndArrowIndicatorData1.isEqual (src.m_bandAndArrowIndicatorData1) ){
			return false;
		}
		
		if ( !m_bandAndArrowIndicatorData2.isEqual (src.m_bandAndArrowIndicatorData2) ){
			return false;
		}
		
		if ( !m_auxData.isEqual (src.m_auxData) ){
			return false;
		}
		
		return true;	
	}
}
