/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

public class ModeData 
{
	private boolean m_usaMode;
	private boolean m_euroMode;
	private boolean m_allBogeysMode;
	private boolean m_logicMode;
	private boolean m_advLogicMode;
	private boolean m_customSweeps;
	
	public ModeData()
	{
		m_usaMode = false;
		m_euroMode = false;
		m_allBogeysMode = false;
		m_logicMode = false;
		m_advLogicMode = false;
		m_customSweeps = false;
	}
	
	/**
	 * Returns whether USA mode is currently active or not.
	 * 
	 * @return True is USA mode is active, otherwise false.
	 */
	public boolean getUsaMode()
	{
		return m_usaMode;
	}
	
	/**
	 * Returns whether euro mode is currently active or not.
	 * 
	 * @return	True is Euro mode is active, otherwise false.
	 */
	public boolean getEuroMode()
	{
		return m_euroMode;
	}
	
	/**
	 * Returns whether AllBogeyMode is currently active or not.
	 *  
	 * @return	True if AllBogeyMode is active, otherwise false.
	 */
	public boolean getAllBogeysMode()
	{
		return m_allBogeysMode;
	}

	/**
	 * Returns whether LogicMode is currently active or not.
	 *  
	 * @return	True if LogicMode is active, otherwise false.
	 */
	public boolean getLogicMode()
	{
		return m_logicMode;
	}
	
	/**
	 * Returns whether AdvLogicMode is currently active or not.
	 *  
	 * @return	True if AdvLogicMode is active, otherwise false.
	 */
	public boolean getAdvLogicMode()
	{
		return m_advLogicMode;
	}
	
	/**
	 * Returns whether CustomSweeps are active.
	 * 
	 * @return True if custom sweeps are active, otherwise false.
	 */
	public boolean getCustomSweeps()
	{
		return m_customSweeps;
	}
	
	/**
	 * Sets whether Usa mode is active.
	 * @param _usaMode	True to turn usa mode on otherwise false to turn usa mode off.
	 */
	public void setUsaMode( boolean _usaMode)
	{
		m_usaMode = _usaMode;
	}
	
	/**
	 * Sets whether Euro mode is active.
	 * @param _euroMode	True to turn Euro mode on otherwise false to turn Euro mode off.
	 */
	public void setEuroMode( boolean _euroMode)
	{
		m_euroMode = _euroMode;
	}
	
	/**
	 * Sets whether AllBogeysMode is active.
	 * @param _allBogeysMode	True to turn AllBogeysMode mode on otherwise false to turn AllBogeysMode off.
	 */
	public void setAllBogeysMode( boolean _allBogeysMode)
	{
		m_allBogeysMode = _allBogeysMode;
	}
	
	/**
	 * Sets whether LogicMode is active.
	 * @param _logicMode	True to turn LogicMode on otherwise false to turn LogicMode off.
	 */
	public void setLogicMode( boolean _logicMode)
	{
		m_logicMode = _logicMode;
	}
	
	/**
	 * Sets whether AdvLogicMode is active.
	 * @param _advLogicMode	True to turn AdvLogicMode on otherwise false to turn AdvLogicMode off.
	 */
	public void setAdvLogicMode( boolean _advLogicMode)
	{
		m_advLogicMode = _advLogicMode;
	}
	
	/**
	 * Sets whether CustomSweeps  is active.
	 * @param _customSweeps	True to turn CustomSweeps mode on otherwise false to turn CustomSweeps mode off.
	 */
	public void setCustomSweeps( boolean _customSweeps)
	{
		m_customSweeps = _customSweeps;
	}
}
