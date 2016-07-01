/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.statemachines;

import java.util.ArrayList;


import android.util.Log;

import com.valentine.esp.PacketQueue;
import com.valentine.esp.ValentineClient;
import com.valentine.esp.ValentineESP;
import com.valentine.esp.constants.Devices;
import com.valentine.esp.constants.ESPLibraryLogController;
import com.valentine.esp.constants.PacketId;
import com.valentine.esp.data.SweepDefinition;
import com.valentine.esp.data.SweepWriteResult;
import com.valentine.esp.packets.request.RequestAllSweepDefinitions;
import com.valentine.esp.packets.request.RequestWriteSweepDefinition;
import com.valentine.esp.packets.response.ResponseSweepDefinitions;
import com.valentine.esp.packets.response.ResponseSweepWriteResult;
import com.valentine.esp.utilities.Utilities;

/** This class writes the custom sweeps to the Valentine one.  Is used internally by the ValentineClient Class.
 * 
 *
 */
public class WriteCustomSweeps 
{
	ArrayList<SweepDefinition> m_userDefintions;
	Object m_finalCallbackObject;
	String m_finalCallbackFunction;
	Object m_errorCallbackObject;
	String m_errorCallbackFunction;
	ValentineESP m_valentineESP;
	Devices m_valentineType;
	
	GetAllSweeps m_sweepReader;
	Integer m_maxSweepIndex;
	SweepDefinition[] m_finalDefinitions;
	
	/**
	 * Initializes the WriteCustomSweeps state machine with the custom sweeps,
	 * ValentineESP object, and the success/failure callbacks.
	 * 
	 * @param _userDefintions			ArrayList of custom sweeps to be written to the V1.
	 * @param _valentineType			V1 type.
	 * @param _valentineESP				The ValentineESP object to register for various callbacks.
	 * @param _finalCallbackObject		The object thats will notified once the custom sweeps have been written.
	 * @param _finalCallbackFunction	The function that will be called once the custom sweeps have been written. 
	 * 	{Argument must be ArrayList<SweepDefinition> _defs}
	 * @param _errorCallbackObject		The object that will receive the error callback.
	 * @param _errorCallbackFunction	The function to call if there is an error writing the custom sweeps.
	 */
	public WriteCustomSweeps(ArrayList<SweepDefinition> _userDefintions, 
			Devices _valentineType, 
			ValentineESP _valentineESP, 
			Object _finalCallbackObject, 
			String _finalCallbackFunction, 
			Object _errorCallbackObject, 
			String _errorCallbackFunction)
	{
		m_userDefintions = _userDefintions;
		m_valentineESP = _valentineESP;
		m_finalCallbackObject = _finalCallbackObject;
		m_finalCallbackFunction = _finalCallbackFunction;
		m_errorCallbackObject = _errorCallbackObject;
		m_errorCallbackFunction = _errorCallbackFunction;
		m_valentineType = _valentineType;
		m_sweepReader = null;
	}
	
	/**
	 * Starts writing the custom Sweeps to the V1.
	 */
	public void Start()
	{
		// use the GetAllSweeps state machine to handle the sweep info reading for us. This will force an update to 
		// the ValentineClient cache any time sweeps are written to the V1.
		m_sweepReader = new GetAllSweeps (true, m_valentineType, m_valentineESP, this, "sweepReaderSuccessCallback", this, "sweepReaderErrorCallback");
		m_sweepReader.getSweeps();
	}
	
	/**
	 * Notifies the callback objects error callback  method that an error has occurred while attempting to read the
	 * custom sweep and passes in the error code.
	 * 
	 * (Do not call directly)
	 * 
	 * @param errCode	The error code returned from the V1.
	 */
	public void sweepReaderErrorCallback (Integer errCode)
	{
		// Tell the caller that an error occurred
		if ((m_errorCallbackObject != null) && (m_errorCallbackFunction != null)){
			Utilities.doCallback(m_errorCallbackObject,	m_errorCallbackFunction, Integer.class, errCode);
		}
	}
	
	/**
	 * Callback fires once we've first successfully read the sweeps from the V1. This triggers the users custom sweeps
	 * to be written to the V1.
	 * (Do not call directly)
	 * 
	 * @param _defs	The custom sweeps that are currently in the V1 before the write.
	 */
	public void sweepReaderSuccessCallback(ArrayList<SweepDefinition> _defs)
	{
		// Get the max sweep index from the ValentineClient cache and start writing
		
		m_maxSweepIndex = ValentineClient.getInstance().getCachedMaxSweepIndex();
		
		// Register for the sweep write result before starting the write
		m_valentineESP.registerForPacket(PacketId.respSweepWriteResult, this, "getSweepWriteResultCallback");
		
		ArrayList<SweepDefinition> empty = new ArrayList<SweepDefinition>();
		ArrayList<SweepDefinition> notEmpty = new ArrayList<SweepDefinition>();
		
		for (int i = 0; i < m_userDefintions.size(); i++)
		{	
			SweepDefinition sweepDef = m_userDefintions.get(i);
			
			if ((sweepDef.getLowerFrequencyEdge() == 0) || (sweepDef.getUpperFrequencyEdge() == 0))
			{
				empty.add(sweepDef);
			}
			else
			{
				notEmpty.add(sweepDef);
			}
		}
		
		//write sweeps (N times, last one with commit = 1)
		for (int i = 0; i < notEmpty.size(); i++)
		{
			SweepDefinition sweepDef = notEmpty.get(i);
			
			sweepDef.setIndex(i);
			if(ESPLibraryLogController.LOG_WRITE_INFO){
				Log.i("ValentineDebug", "Write Lower - " + sweepDef.getLowerFrequencyEdge().toString() + " Upper - " + sweepDef.getUpperFrequencyEdge().toString());
			}
			
			if (i == notEmpty.size()-1)
			{
				sweepDef.setCommit(true);
				i = m_userDefintions.size();
			}
			
			RequestWriteSweepDefinition packet = new RequestWriteSweepDefinition(sweepDef, m_valentineType);
			m_valentineESP.sendPacket(packet);
		}
	}
	
	/**
	 * Callback that fires once the custom sweeps have been written to the V1 and returns result of the CustomSweep write.
	 * If there was an error during the write, it will notify the callback object of the error result index.
	 * 
	 * (Do not call directly)
	 * 
	 * @param _resp	 The result of the custom sweep write.
	 */
	public void getSweepWriteResultCallback(ResponseSweepWriteResult _resp)
	{
		m_valentineESP.deregisterForPacket(PacketId.respSweepWriteResult, this);
		SweepWriteResult result = (SweepWriteResult) _resp.getResponseData();
		
		PacketQueue.removeFromBusyPacketIds(PacketId.reqWriteSweepDefinition);
		
		if (result.getSuccess())
		{
			RequestAllSweepDefinitions packet = new RequestAllSweepDefinitions(m_valentineType);
			m_valentineESP.registerForPacket(PacketId.respSweepDefinition, this, "getSweepDefinitionsFinalCallback");
			m_valentineESP.sendPacket(packet);	
		}
		else
		{
			if ((m_errorCallbackObject != null) && (m_errorCallbackFunction != null))
			{
				Utilities.doCallback(m_errorCallbackObject,	m_errorCallbackFunction, Integer.class, result.getErrorIndex());
			}
		}
	}
	
	/**
	 * Final Callback that receives the custom sweeps written to the V1 and stores them inside of an array list before sending them to
	 * the calling objects final callback method.
	 * 
	 * @param _resp The sweep definition response that contains a sweep definition from the V1.
	 */
	public void getSweepDefinitionsFinalCallback(ResponseSweepDefinitions _resp)
	{
		SweepDefinition rc = (SweepDefinition) _resp.getResponseData();

		PacketQueue.removeFromBusyPacketIds(PacketId.reqAllSweepDefinitions);
		
		
		if ( m_finalDefinitions == null ){
			m_finalDefinitions = new SweepDefinition[m_maxSweepIndex+1];
		}
		
		if ( rc.getIndex() <= m_maxSweepIndex ){		
			m_finalDefinitions[rc.getIndex()] = rc;
		}
		
		if (rc.getIndex() == m_maxSweepIndex)
		{
			m_valentineESP.deregisterForPacket(PacketId.respSweepDefinition, this);
			
			Utilities.doCallback(m_finalCallbackObject, m_finalCallbackFunction, SweepDefinition[].class, m_finalDefinitions);
		}
	}
}
