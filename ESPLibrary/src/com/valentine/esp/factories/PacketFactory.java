/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.factories;

import com.valentine.esp.constants.Devices;
import com.valentine.esp.constants.PacketId;
import com.valentine.esp.packets.ESPPacket;
import com.valentine.esp.packets.InfDisplayData;
import com.valentine.esp.packets.InfV1Busy;
import com.valentine.esp.packets.UnknownPacket;
import com.valentine.esp.packets.request.RequestAllSweepDefinitions;
import com.valentine.esp.packets.request.RequestBatteryVoltage;
import com.valentine.esp.packets.request.RequestChangeMode;
import com.valentine.esp.packets.request.RequestDefaultSweepDefinitions;
import com.valentine.esp.packets.request.RequestSetSweepsToDefault;
import com.valentine.esp.packets.request.RequestFactoryDefault;
import com.valentine.esp.packets.request.RequestMaxSweepIndex;
import com.valentine.esp.packets.request.RequestMuteOff;
import com.valentine.esp.packets.request.RequestMuteOn;
import com.valentine.esp.packets.request.RequestOverrideThumbwheel;
import com.valentine.esp.packets.request.RequestSavvyStatus;
import com.valentine.esp.packets.request.RequestSerialNumber;
import com.valentine.esp.packets.request.RequestSetSavvyUnmute;
import com.valentine.esp.packets.request.RequestStartAlertData;
import com.valentine.esp.packets.request.RequestStopAlertData;
import com.valentine.esp.packets.request.RequestSweepSections;
import com.valentine.esp.packets.request.RequestTurnOffMainDisplay;
import com.valentine.esp.packets.request.RequestTurnOnMainDisplay;
import com.valentine.esp.packets.request.RequestUserBytes;
import com.valentine.esp.packets.request.RequestVehicleSpeed;
import com.valentine.esp.packets.request.RequestVersion;
import com.valentine.esp.packets.request.RequestWriteSweepDefinition;
import com.valentine.esp.packets.request.RequestWriteUserBytes;
import com.valentine.esp.packets.response.ResponseAlertData;
import com.valentine.esp.packets.response.ResponseBatteryVoltage;
import com.valentine.esp.packets.response.ResponseDataError;
import com.valentine.esp.packets.response.ResponseDataReceived;
import com.valentine.esp.packets.response.ResponseDefaultSweepDefinitions;
import com.valentine.esp.packets.response.ResponseMaxSweepIndex;
import com.valentine.esp.packets.response.ResponseRequestNotProcessed;
import com.valentine.esp.packets.response.ResponseSavvyStatus;
import com.valentine.esp.packets.response.ResponseSerialNumber;
import com.valentine.esp.packets.response.ResponseSweepDefinitions;
import com.valentine.esp.packets.response.ResponseSweepSections;
import com.valentine.esp.packets.response.ResponseSweepWriteResult;
import com.valentine.esp.packets.response.ResponseUnsupported;
import com.valentine.esp.packets.response.ResponseUserBytes;
import com.valentine.esp.packets.response.ResponseVehicleSpeed;
import com.valentine.esp.packets.response.ResponseVersion;

public class PacketFactory 
{
	/**
	 * Returns a new ESP packet with the Device initialized to Unknown based on the id of the passed in PacketId.
	 * 
	 * @param _id	The packetId of the desired ESP packet.
	 * 
	 * @return 		The desired packet if PacketId is valid otherwise 'UnknownPacket'.
	 */
	public static ESPPacket getPacket(PacketId _id)
	{
		ESPPacket rc = null;
		if (_id != null) {
			switch (_id)
			{
					//basic data
				case reqVersion:
					rc = new RequestVersion(Devices.UNKNOWN, Devices.UNKNOWN);
					break;
				case respVersion:
					rc = new ResponseVersion(Devices.UNKNOWN);
					break;
				case reqSerialNumber:
					rc = new RequestSerialNumber(Devices.UNKNOWN, Devices.UNKNOWN);
					break;
				case respSerialNumber:
					rc = new ResponseSerialNumber(Devices.UNKNOWN);
					break;
				case reqUserBytes:
					rc = new RequestUserBytes(Devices.UNKNOWN);
					break;
				case respUserBytes:
					rc = new ResponseUserBytes(Devices.UNKNOWN);
					break;
				case reqWriteUserBytes:
					rc = new RequestWriteUserBytes(null, Devices.UNKNOWN);
					break;
				case reqFactoryDefault:
					rc = new RequestFactoryDefault(Devices.UNKNOWN, Devices.UNKNOWN);
					break;
				case reqDefaultSweepDefinitions:
					rc = new RequestDefaultSweepDefinitions(Devices.UNKNOWN);
					break;
				case respDefaultSweepDefinition:
					rc = new ResponseDefaultSweepDefinitions(Devices.UNKNOWN);
					break;
					//custom sweep data
				case reqWriteSweepDefinition:
					rc = new RequestWriteSweepDefinition(null, Devices.UNKNOWN);
					break;
				case reqAllSweepDefinitions:
					rc = new RequestAllSweepDefinitions(Devices.UNKNOWN);
					break;
				case respSweepDefinition:
					rc = new ResponseSweepDefinitions(Devices.UNKNOWN);
					break;
				case reqSetSweepsToDefault:
					rc = new RequestSetSweepsToDefault(Devices.UNKNOWN);
					break;
				case reqMaxSweepIndex:
					rc = new RequestMaxSweepIndex(Devices.UNKNOWN);
					break;
				case respMaxSweepIndex:
					rc = new ResponseMaxSweepIndex(Devices.UNKNOWN);
					break;
				case respSweepWriteResult:
					rc = new ResponseSweepWriteResult(Devices.UNKNOWN);
					break;
				case reqSweepSections:
					rc = new RequestSweepSections(Devices.UNKNOWN);
					break;
				case respSweepSections:
					rc = new ResponseSweepSections(Devices.UNKNOWN);
					break;
					
					//informational packets
				case infDisplayData:
					rc = new InfDisplayData(Devices.UNKNOWN);
					break;
				case reqTurnOffMainDisplay:
					rc = new RequestTurnOffMainDisplay(Devices.UNKNOWN);
					break;
				case reqTurnOnMainDisplay:
					rc = new RequestTurnOnMainDisplay(Devices.UNKNOWN);
				case reqMuteOn:
					rc = new RequestMuteOn(Devices.UNKNOWN);
					break;
				case reqMuteOff:
					rc = new RequestMuteOff(Devices.UNKNOWN);
					break;	
				case reqChangeMode:
					rc = new RequestChangeMode((byte) 0, Devices.UNKNOWN);
					break;
				case reqStartAlertData:
					rc = new RequestStartAlertData(Devices.UNKNOWN);
					break;
				case reqStopAlertData:
					rc = new RequestStopAlertData(Devices.UNKNOWN);
					break;
				case respAlertData:
					rc = new ResponseAlertData(Devices.UNKNOWN);
					break;
				case respDataReceived:
					rc = new ResponseDataReceived(Devices.UNKNOWN);
					break;
				case reqBatteryVoltage:
					rc = new RequestBatteryVoltage(Devices.UNKNOWN);
					break;
				case respBatteryVoltage:
					rc = new ResponseBatteryVoltage(Devices.UNKNOWN);
					break;
					
					//unspported and error
				case respUnsupportedPacket:
					rc = new ResponseUnsupported(Devices.UNKNOWN);
					break;
				case respRequestNotProcessed:
					rc = new ResponseRequestNotProcessed(Devices.UNKNOWN);
					break;
				case infV1Busy:
					rc = new InfV1Busy(Devices.UNKNOWN);
					break;
				case respDataError:
					rc = new ResponseDataError(Devices.UNKNOWN);
					break;
					
					//Savvy
				case reqSavvyStatus:
					rc = new RequestSavvyStatus(Devices.UNKNOWN, Devices.UNKNOWN);
					break;
				case respSavvyStatus:
					rc = new ResponseSavvyStatus(Devices.UNKNOWN);
					break;
				case reqVehicleSpeed:
					rc = new RequestVehicleSpeed(Devices.UNKNOWN, Devices.UNKNOWN);
					break;
				case respVehicleSpeed:
					rc = new ResponseVehicleSpeed(Devices.UNKNOWN);
					break;
				case reqOverrideThumbwheel:
					rc = new RequestOverrideThumbwheel(Devices.UNKNOWN, (byte) 0, Devices.UNKNOWN);
					break;
				case reqSetSavvyUnmuteEnable:
					rc = new RequestSetSavvyUnmute(Devices.UNKNOWN, false, Devices.UNKNOWN);
					break;
				default:
					rc = new UnknownPacket(Devices.UNKNOWN);
			}
		}
		return rc;
	}
}
