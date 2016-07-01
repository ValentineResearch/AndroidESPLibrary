/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.valentine.esp.PacketQueue;
import com.valentine.esp.constants.Devices;
import com.valentine.esp.data.UserSettings;
import com.valentine.esp.packets.ESPPacket;
import com.valentine.esp.packets.response.ResponseBatteryVoltage;
import com.valentine.esp.packets.response.ResponseMaxSweepIndex;
import com.valentine.esp.packets.response.ResponseSavvyStatus;
import com.valentine.esp.packets.response.ResponseSerialNumber;
import com.valentine.esp.packets.response.ResponseSweepDefinitions;
import com.valentine.esp.packets.response.ResponseSweepSections;
import com.valentine.esp.packets.response.ResponseUserBytes;
import com.valentine.esp.packets.response.ResponseVehicleSpeed;
import com.valentine.esp.packets.response.ResponseVersion;
import com.valentine.esp.utilities.Utilities;

/** This is the class that handles all the demo packets sent to it from the ESP Client.  It stores the response packets and then
 * puts them on the input queue as it receives request packets for the particular type of packet.
 * 
 * InfDisplayData and AlertData Packets are put right on the queue with out storage.
 *
 * This effectively simulates a Valentine One with Checksum being plugged into the ESP buss.
 */
public class DemoData 
{
	private Map<Devices, ResponseVersion> m_versionPackets;
	
	private Map<Devices, ResponseSerialNumber> m_V1SerialPackets;
	
	private ResponseSavvyStatus m_SavvyConfiguration;
	private ResponseUserBytes m_V1Configuration;
	private ArrayList<ResponseSweepSections> m_CustomSweepSections;
	private ArrayList<ResponseSweepDefinitions> m_ResponseSweepDefinitions;
	private ResponseMaxSweepIndex m_MaximumCustomSweepIndex;
	private ResponseBatteryVoltage m_BatteryVoltage;
	private ResponseVehicleSpeed m_VehicleSpeed;
	
	private Object m_V1ConfigObject;
	private String m_V1ConfigFunction;
	
	
	public DemoData()
	{
		m_versionPackets = new HashMap<Devices, ResponseVersion>();
		m_V1SerialPackets = new HashMap<Devices, ResponseSerialNumber>();
		m_ResponseSweepDefinitions = new ArrayList<ResponseSweepDefinitions>();
		m_CustomSweepSections = new ArrayList<ResponseSweepSections>();
	}
	
	/**
	 * Sets the callback data for when we get user settings in the demo data packets.
	 * Requires a function with a UserSettings parameter, void function (UserSettings_data)
	 * 
	 * @param _owner Object that is to be notified
	 * @param _function The function to call on the _owner
	 */
	public void setConfigCallbackData(Object _owner, String _function)
	{
		m_V1ConfigObject = _owner;
		m_V1ConfigFunction = _function;
	}
	
	/**
	 * Parses through the passes in ESP packet and stores the responses and place the request into the input queues to be processed.
	 * 
	 * @param _packet The packet to handle.
	 */
	public void handleDemoPacket(ESPPacket _packet)
	{
		switch (_packet.getPacketIdentifier())
		{
			case respVersion:
				m_versionPackets.put(_packet.getOrigin(), (ResponseVersion) _packet);
				break;
			case respSerialNumber:
				m_V1SerialPackets.put(_packet.getOrigin(), (ResponseSerialNumber) _packet);
				break;
			case respSavvyStatus:
				m_SavvyConfiguration = (ResponseSavvyStatus) _packet;
				break;
			case respUserBytes:
				m_V1Configuration = (ResponseUserBytes) _packet;
				if ((m_V1ConfigObject != null) && (m_V1ConfigFunction != null))
				{
					Utilities.doCallback(m_V1ConfigObject, m_V1ConfigFunction, UserSettings.class, (UserSettings) m_V1Configuration.getResponseData());
				}
				break;
			case respSweepSections:
				m_CustomSweepSections.add((ResponseSweepSections) _packet);
				break;
			case respSweepDefinition:
				m_ResponseSweepDefinitions.add((ResponseSweepDefinitions) _packet);
				break;
			case respMaxSweepIndex:
				m_MaximumCustomSweepIndex = (ResponseMaxSweepIndex) _packet;
				break;
			case respBatteryVoltage:
				m_BatteryVoltage = (ResponseBatteryVoltage) _packet;
				break;
			case respVehicleSpeed:
				m_VehicleSpeed = (ResponseVehicleSpeed) _packet;
				break;
			case reqVersion:
				PacketQueue.pushInputPacketOntoQueue(m_versionPackets.get(_packet.getDestination()));
				break;
			case reqSerialNumber:
				PacketQueue.pushInputPacketOntoQueue(m_V1SerialPackets.get(_packet.getDestination()));
				break;
			case reqUserBytes:
				PacketQueue.pushInputPacketOntoQueue(m_V1Configuration);
				break;
			case reqAllSweepDefinitions:
			case reqSetSweepsToDefault:
				for (int i = 0; i < m_ResponseSweepDefinitions.size(); i++)
				{
					PacketQueue.pushInputPacketOntoQueue(m_ResponseSweepDefinitions.get(i));
				}
				break;				
			case reqDefaultSweepDefinitions:
			case respDefaultSweepDefinition:
				// Ignore these packets in the demo mode file.
				break;				
			case reqMaxSweepIndex:
				PacketQueue.pushInputPacketOntoQueue(m_MaximumCustomSweepIndex);
				break;
			case reqSweepSections:
				for (int i = 0; i < m_CustomSweepSections.size(); i++)
				{
					PacketQueue.pushInputPacketOntoQueue(m_CustomSweepSections.get(i));
				}
				break;
			case reqBatteryVoltage:
				PacketQueue.pushInputPacketOntoQueue(m_BatteryVoltage);
				break;
			case reqSavvyStatus:
				PacketQueue.pushInputPacketOntoQueue(m_SavvyConfiguration);
				break;
			case reqVehicleSpeed:
				PacketQueue.pushInputPacketOntoQueue(m_VehicleSpeed);
				break;
			
				
			
			case reqTurnOffMainDisplay:
			case reqTurnOnMainDisplay:
			case respDataReceived: 
			case reqStartAlertData:
			case reqStopAlertData:
			case reqChangeMode:	
			case reqMuteOn:
			case reqMuteOff:
			case reqFactoryDefault:
			case reqWriteUserBytes:
			case reqWriteSweepDefinition:
			case reqOverrideThumbwheel:
			case reqSetSavvyUnmuteEnable:	
			case respDataError:
			case respUnsupportedPacket:
			case respRequestNotProcessed:	
			case infV1Busy:
			case respSweepWriteResult:
				break;
			
			case respAlertData:
			case infDisplayData:
				PacketQueue.pushInputPacketOntoQueue(_packet);
				break;
			
			case unknownPacketType:
				// There is nothing to do with an unknown packet type in demo mode
				break;
		}
	}
	
	/**
	 *  Gets the current demo mode UserSettings.
	 *  
	 * @return The current Demo mode user settings if it has been encountered yet, null if it has not.
	 */
	public UserSettings getUserSettings()
	{
		return (UserSettings)m_V1Configuration.getResponseData();
	}
}
