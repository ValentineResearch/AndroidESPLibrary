/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.statemachines;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;


import android.util.Log;

import com.valentine.esp.PacketQueue;
import com.valentine.esp.ValentineClient;
import com.valentine.esp.ValentineESP;
import com.valentine.esp.constants.Devices;
import com.valentine.esp.constants.ESPLibraryLogController;
import com.valentine.esp.constants.PacketId;
import com.valentine.esp.data.SweepDefinition;
import com.valentine.esp.data.SweepSection;
import com.valentine.esp.packets.request.RequestAllSweepDefinitions;
import com.valentine.esp.packets.request.RequestDefaultSweepDefinitions;
import com.valentine.esp.packets.request.RequestMaxSweepIndex;
import com.valentine.esp.packets.request.RequestSweepSections;
import com.valentine.esp.packets.response.ResponseDefaultSweepDefinitions;
import com.valentine.esp.packets.response.ResponseMaxSweepIndex;
import com.valentine.esp.packets.response.ResponseSweepDefinitions;
import com.valentine.esp.packets.response.ResponseSweepSections;
import com.valentine.esp.utilities.Utilities;

/** 
 * This is the class that combines all the currently set custom sweeps into one ArrayList
 *	Used internally by the ValentineClient Class 
 *
 */
public class GetAllSweeps 
{
	public static final int ABORT_ERR_CODE = -64353;
	private ValentineESP m_valentineESP;
	private Object m_callbackObject;
	private String m_callbackFunction;
	private Object m_errCallbackObject;
	private String m_errCallbackFunction;
	//private ArrayList<SweepDefinition> m_currentResponses;
	private SweepDefinition[] m_sweepDefs = null;
	private SweepDefinition[] m_defaultSweepDefs = null;
	private Devices m_valentineType;
	private boolean m_abort;
	private boolean m_clearCacheBeforeRead;
	
	private Integer m_maxSweepIndex;
	SweepSection[] m_sections;
	
	private ReentrantLock m_lock;
	
	/**
	 * Constructor for the GetAllSweeps state machine.
	 * 
	 * @param _clearCacheBeforeRead - If true, the ValentineClient sweep data cache will be cleared before data is read.
	 * @param _valentineType - The V1 type to use when building requests.
	 * @param _valentineESP - The connected ValentineESP object to use for all requests and packet registrations 
	 * @param _callbackObject - The object to use in the callback once the state machine completes all of its steps.
	 * @param _callbackFunction - The method in _callbackObject to use for the callback once the state machine completes all of its steps. 
	 * @param _errCallbackObject - The callback object to receive error callbacks.
	 * @param _errCallbackFunction - The name of the method that will receive error notifications.
	 * 
	 */
	public GetAllSweeps(boolean _clearCacheBeforeRead, Devices _valentineType, ValentineESP _valentineESP, Object _callbackObject, String _callbackFunction, Object _errCallbackObject, String _errCallbackFunction)
	{
		m_valentineESP = _valentineESP;
		m_callbackObject = _callbackObject;
		m_callbackFunction = _callbackFunction;
		m_errCallbackObject = _errCallbackObject;
		m_errCallbackFunction = _errCallbackFunction;
		m_valentineType = _valentineType;
		m_sweepDefs = null;
		m_defaultSweepDefs = null;
		m_abort = false;
		m_clearCacheBeforeRead = _clearCacheBeforeRead;
		m_lock = new ReentrantLock();
	}
	
	/**
	 * This is the entry point for the GetAllSweeps state machine.
	 */
	public void getSweeps()
	{
		// Clear any sweeps we received previously
		m_lock.lock();
		m_sweepDefs = null;
		m_defaultSweepDefs = null;
		m_abort = false;
		m_lock.unlock();
		
		if ( m_clearCacheBeforeRead ){
		// Clear the ValentineClient sweep information cache before we start reading
			ValentineClient.getInstance().clearSweepCache();
		}
		
		//get sweep sections
		RequestSweepSections packet = new RequestSweepSections(m_valentineType);
		m_valentineESP.registerForPacket(PacketId.respSweepSections, this, "getSweepSectionsCallback");
		m_valentineESP.sendPacket(packet);		
	}
	
	/**
	 * This method will set a flag to abort the state machine. The state machine will be stopped on the next data callback after this method is called.
	 */
	public void abort()
	{
		m_lock.lock();
		m_abort = true;
		
		// Deregister all callbacks when an abort is called
		m_valentineESP.deregisterForPacket(PacketId.respSweepSections, this);
		m_valentineESP.deregisterForPacket(PacketId.respMaxSweepIndex, this);
		m_valentineESP.deregisterForPacket(PacketId.respDefaultSweepDefinition, this);
		m_valentineESP.deregisterForPacket(PacketId.respSweepDefinition, this);
		
		m_lock.unlock();
		
		if ( m_errCallbackObject != null && m_errCallbackFunction != null ){
			// Tell the owner that we were aborted
			Utilities.doCallback(m_callbackObject, m_callbackFunction, Integer.class, ABORT_ERR_CODE);
		}
	}
	
	/**
	 * This callback method is used to advance the state machine. DO NOT CALL THIS METHOD DIRECTLY.
	 * 
	 * @param _resp - The Sweep Sections received from the V1
	 */
	public void getSweepSectionsCallback(ResponseSweepSections _resp)
	{
		// Deregister once we have received the data.
		m_valentineESP.deregisterForPacket(PacketId.respSweepSections, this);
		
		m_lock.lock();
		boolean doAbort = m_abort;
		m_lock.unlock();
		
		if ( doAbort ){
			// Nothing more to do here
			return;
		}
		
		m_sections = (SweepSection[]) _resp.getResponseData();
		
		// Add the sweep sections to the client
		ArrayList<SweepSection> temp = new ArrayList<SweepSection>();
		for (int i = 0; i < m_sections.length; i++)
		{
			temp.add(m_sections[i]);
		}
		
		ValentineClient.getInstance().setCachedSweepSections(temp);
		
		PacketQueue.removeFromBusyPacketIds(PacketId.reqSweepSections);
		
		//get allowed number of sweeps
		RequestMaxSweepIndex packet = new RequestMaxSweepIndex(m_valentineType);
		m_valentineESP.registerForPacket(PacketId.respMaxSweepIndex, this, "getNumberOfSweepsCallback");
		m_valentineESP.sendPacket(packet);		
	}
	
	/**
	 * This callback method is used to advance the state machine. DO NOT CALL THIS METHOD DIRECTLY.
	 * 
	 * @param _resp The maximum sweep index received from the V1.
	 */
	public void getNumberOfSweepsCallback(ResponseMaxSweepIndex _resp)
	{
		//get allowed number of sweeps back
		m_valentineESP.deregisterForPacket(PacketId.respMaxSweepIndex, this);
		
		m_lock.lock();
		boolean doAbort = m_abort;
		m_lock.unlock();
		
		if ( doAbort ){
			// Nothing more to do here
			return;
		}
		
		m_maxSweepIndex = (Integer) _resp.getResponseData();
		
		ValentineClient.getInstance().setCachedMaxSweepIndex(m_maxSweepIndex);
		
		PacketQueue.removeFromBusyPacketIds(PacketId.reqMaxSweepIndex);
		
		if ( ValentineClient.getInstance().allowDefaulSweepDefRead() ){			
			// Get all default sweep definitions
			RequestDefaultSweepDefinitions packet = new RequestDefaultSweepDefinitions(m_valentineType);
			
			// Create a new array in for the sweep definitions 
			m_lock.lock();
			m_defaultSweepDefs = new SweepDefinition[m_maxSweepIndex + 1];
			m_lock.unlock();
			
			m_valentineESP.registerForPacket(PacketId.respDefaultSweepDefinition, this, "getDefaultSweepDefsCallback");
			m_valentineESP.sendPacket(packet);	
		}
		else{
			// The current V1 does not support reading the default sweeps so just skip to the current sweep read
			privStartSweepDefRead();
		}
	}
		
	/**
	 * This method will initiate a read of the CURRENT sweep definitions and set up the callback.
	 */
	private void privStartSweepDefRead ()
	{
		//get all sweep definitions
		RequestAllSweepDefinitions packet = new RequestAllSweepDefinitions(m_valentineType);
		
		// Create a new array in for the sweep definitions 
		m_lock.lock();
		m_sweepDefs = new SweepDefinition[m_maxSweepIndex + 1];
		m_lock.unlock();
		
		m_valentineESP.registerForPacket(PacketId.respSweepDefinition, this, "getSweepsCallback");
		m_valentineESP.sendPacket(packet);	
	}
	
	/**
	 * This callback method is used to advance the state machine. DO NOT CALL THIS METHOD DIRECTLY.
	 * This is the last step in the state machine. Once all sweep definitions are received, the callback set up in the constructor will be called.
	 * 
	 * @param _resp The sweep definition received from the V1.
	 */
	public void getDefaultSweepDefsCallback(ResponseDefaultSweepDefinitions _resp)
	{
		PacketQueue.removeFromBusyPacketIds(PacketId.reqDefaultSweepDefinitions);
		
		m_lock.lock();
		boolean doAbort = m_abort;
		m_lock.unlock();
		
		if ( doAbort ){
			// Nothing more to do here
			return;
		}
		
		SweepDefinition rc = (SweepDefinition) _resp.getResponseData();
		int index = rc.getIndex();
		boolean allSweepsFound = false;
		
		if(ESPLibraryLogController.LOG_WRITE_INFO){
			Log.i("ValentineDebug", "Default Sweep index: " + index + "  Final Lower - " + rc.getLowerFrequencyEdge().toString() + " Upper - " + rc.getUpperFrequencyEdge().toString());
		}
		
		m_lock.lock();
		if ( m_defaultSweepDefs != null ){
			// Received out of order...do nothing
			if ( index < m_defaultSweepDefs.length ){
				m_defaultSweepDefs[index] = rc;
				
				// Add this default sweep to the library cache if it will be used
				ValentineClient.getInstance().addDefaultSweepDefintionToCache(rc);
				
				// Check for all sweeps received
				allSweepsFound = true;
								
				for ( int i = 0; i < m_defaultSweepDefs.length; i++ ){
					if ( m_defaultSweepDefs[i] == null ) {
						// We haven't received this sweep yet
						allSweepsFound = false;
					}
				}
			}
			else{
				if(ESPLibraryLogController.LOG_WRITE_INFO){
					Log.i("ValentineDebug", "Found unusable sweep index. Received " + index + " but max is " + m_defaultSweepDefs.length);
				}
			}			
		}				
		m_lock.unlock();
		
		if ( allSweepsFound ){
			// We are done reading the sweeps
			m_valentineESP.deregisterForPacket(PacketId.respDefaultSweepDefinition, this);
			
			privStartSweepDefRead();
		}
		
		
	}
	
	/**
	 * This callback method is used to advance the state machine. DO NOT CALL THIS METHOD DIRECTLY.
	 * This is the last step in the state machine. Once all sweep definitions are received, the callback set up in the constructor will be called.
	 * 
	 * @param _resp The sweep definition received from the V1.
	 */
	public void getSweepsCallback(ResponseSweepDefinitions _resp)
	{
		PacketQueue.removeFromBusyPacketIds(PacketId.reqAllSweepDefinitions);
		
		m_lock.lock();
		boolean doAbort = m_abort;
		m_lock.unlock();
		
		if ( doAbort ){
			// Nothing more to do here
			return;
		}
		
		SweepDefinition rc = (SweepDefinition) _resp.getResponseData();
		int index = rc.getIndex();
		boolean allSweepsFound = false;
		
		if(ESPLibraryLogController.LOG_WRITE_INFO){
			Log.i("ValentineDebug", "Sweep index: " + index + "  Final Lower - " + rc.getLowerFrequencyEdge().toString() + " Upper - " + rc.getUpperFrequencyEdge().toString());
		}
		
		m_lock.lock();
		if ( m_sweepDefs != null ){
			// Received out of order...do nothing
			if ( index < m_sweepDefs.length ){
				m_sweepDefs[index] = rc;
				
				// Check for all sweeps received
				allSweepsFound = true;
								
				for ( int i = 0; i < m_sweepDefs.length; i++ ){
					if ( m_sweepDefs[i] == null ) {
						// We haven't received this sweep yet
						allSweepsFound = false;
					}
				}
			}
			else{
				if(ESPLibraryLogController.LOG_WRITE_INFO){
					Log.i("ValentineDebug", "Found unusable sweep index. Received " + index + " but max is " + m_sweepDefs.length);
				}
			}			
		}				
		m_lock.unlock();
		
		if ( allSweepsFound ){
			// We are done reading the sweeps
			m_valentineESP.deregisterForPacket(PacketId.respSweepDefinition, this);
			
			Utilities.doCallback(m_callbackObject, m_callbackFunction, SweepDefinition[].class, m_sweepDefs);
		}
		
	}
}
