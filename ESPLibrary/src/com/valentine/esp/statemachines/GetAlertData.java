/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.statemachines;

//import android.util.Log;
import com.valentine.esp.PacketQueue;
import com.valentine.esp.ValentineESP;
import com.valentine.esp.constants.PacketId;
import com.valentine.esp.data.AlertData;
import com.valentine.esp.packets.response.ResponseAlertData;
import com.valentine.esp.utilities.Utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** This is the class that aggregates the Alert Data responses into an ArrayList representing all of the current alerts.
 * Used internally by the Valentine Client.
 *
 */
public class GetAlertData 
{
	//private static final String LOG_TAG = "ValentineESP/GetAlertData";
	private ValentineESP m_valentineESP;
	private Object       m_callbackObject;
	private String       m_callbackFunction;

	private Map<Integer, AlertData> m_currentResponses;
	int m_received;
	
	/**
	 * Constructor that sets up the ValentineESP object, the obCallback object and callback function.
	 * 
	 * @param _valentineESP			A ValentineESP object to to register with the library for a AlertData.
	 * @param _callbackObject		The object which wants to receive the AlertData.
	 * @param _callbackFunction		the function inside of the Object that will be receiving the AlertData.
	 */
	public GetAlertData(ValentineESP _valentineESP, Object _callbackObject, String _callbackFunction) {
		m_valentineESP = _valentineESP;
		m_callbackObject = _callbackObject;
		m_callbackFunction = _callbackFunction;
	}

	/**
	 * Tells the V1 to start sending AlertData.
	 */
	public void start() {
		m_valentineESP.registerForPacket(PacketId.respAlertData, this, "getAlertDataCallback");
	}
	
	/**
	 * Tells the V1 to stop sending AlertData.
	 */
	public void stop() {
		//stop listening to alerts from V1
		m_valentineESP.deregisterForPacket(PacketId.respAlertData, this);
	}
	
	/**
	 * Callback that receives the ResponseAlertData and converts it to AlertData.
	 * 
	 * @param _resp the ResponseAlertData that will be converted to AlertData.
	 */
	public void getAlertDataCallback(ResponseAlertData _resp)
	{
		AlertData alert = (AlertData) _resp.getResponseData();
		int index = alert.getAlertIndexAndCount().getIndex();
		int count = alert.getAlertIndexAndCount().getCount();
		
		PacketQueue.removeFromBusyPacketIds(PacketId.reqStartAlertData);
		
		if ((index == 1) && (count > 0))
		{
			m_currentResponses = new HashMap<Integer, AlertData>();
			m_received = 0;
		}
		
		if (count == 0)
		{
			if (m_currentResponses != null)
			{
				m_currentResponses.clear();
			}
			else
			{
				m_currentResponses = new HashMap<Integer, AlertData>();
			}
		}
		
		if ((count > 0) && (m_currentResponses != null))
		{
			m_currentResponses.put(index, alert);
			m_received++;
		}


		if (m_currentResponses != null)
		{
			if ((count == 0) || (m_currentResponses.size() == count))
			{
				ArrayList<AlertData> data = new ArrayList<AlertData>();

				for (int i = 0; i <= m_currentResponses.size(); i++)
				{
					AlertData item = m_currentResponses.get(i);
					if (item != null)
					{
						data.add(item);
					}
				}
				
				AlertData rc[] = new AlertData[data.size()];
				rc = data.toArray(rc);
				
				Utilities.doCallback(m_callbackObject, m_callbackFunction, AlertData[].class, rc);
			}
		}
	}
}
