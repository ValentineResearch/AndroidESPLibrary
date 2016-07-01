/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.constants;

/**  This enum has all the different packet types and their identifiers, alog with an easy to read string of the type
 * 
 *
 */
public enum PacketId 
{ 
	reqVersion 					((byte)0x01, "reqVersion"),
	respVersion 				((byte)0x02, "respVersion"),
	reqSerialNumber 			((byte)0x03, "reqSerialNumber"),
	respSerialNumber			((byte)0x04, "respSerialNumber"),
	reqUserBytes 				((byte)0x11, "reqUserBytes"),
	respUserBytes 				((byte)0x12, "respUserBytes"),
	reqWriteUserBytes 			((byte)0x13, "reqWriteUserBytes"),
	reqFactoryDefault 			((byte)0x14, "reqFactoryDefault"),
	reqWriteSweepDefinition 	((byte)0x15, "reqWriteSweepDefinition"),
	reqAllSweepDefinitions 		((byte)0x16, "reqAllSweepDefinitions"),
	respSweepDefinition 		((byte)0x17, "respSweepDefinition"),
	reqSetSweepsToDefault 		((byte)0x18, "reqSetSweepsToDefault"),
	reqMaxSweepIndex 			((byte)0x19, "reqMaxSweepIndex"),
	respMaxSweepIndex 			((byte)0x20, "respMaxSweepIndex"),
	respSweepWriteResult		((byte)0x21, "respSweepWriteResult"),
	reqSweepSections			((byte)0x22, "reqSweepSections"),
	respSweepSections 			((byte)0x23, "respSweepSections"),
	reqDefaultSweepDefinitions	((byte)0x24, "reqDefaultSweepDefinitions"),
	respDefaultSweepDefinition 	((byte)0x25, "respDefaultSweepDefinition"),	
	infDisplayData 				((byte)0x31, "infDisplayData"),
	reqTurnOffMainDisplay 		((byte)0x32, "reqTurnOffMainDisplay"),
	reqTurnOnMainDisplay 		((byte)0x33, "reqTurnOnMainDisplay"),
	reqMuteOn 					((byte)0x34, "reqMuteOn"),
	reqMuteOff 					((byte)0x35, "reqMuteOff"),
	reqChangeMode 				((byte)0x36, "reqChangeMode"),
	reqStartAlertData 			((byte)0x41, "reqStartAlertData"),
	reqStopAlertData 			((byte)0x42, "reqStopAlertData"),
	respAlertData 				((byte)0x43, "respAlertData"),
	respDataReceived 			((byte)0x61, "respDataReceived"), 
	reqBatteryVoltage 			((byte)0x62, "reqBatteryVoltage"),
	respBatteryVoltage 			((byte)0x63, "respBatteryVoltage"),
	respUnsupportedPacket 		((byte)0x64, "respUnsupportedPacket"),
	respRequestNotProcessed 	((byte)0x65, "respRequestNotProcessed"),
	infV1Busy 					((byte)0x66, "infV1Busy"),
	respDataError 				((byte)0x67, "respDataError"),
	reqSavvyStatus 				((byte)0x71, "reqSavvyStatus"),
	respSavvyStatus 			((byte)0x72, "respSavvyStatus"),
	reqVehicleSpeed 			((byte)0x73, "reqVehicleSpeed"),
	respVehicleSpeed 			((byte)0x74, "respVehicleSpeed"),
	reqOverrideThumbwheel 		((byte)0x75, "reqOverrideThumbwheel"),
	reqSetSavvyUnmuteEnable 	((byte)0x76, "reqSetSavvyUnmuteEnable"),
	
	unknownPacketType 			((byte)0x100, "UnknownPacketType");
	
	byte m_value;
	String m_name;
		
	PacketId(byte _value, String _name)
	{
		m_value = _value;
		m_name = _name;
	}
	
	/**
	 * Gets the byte values of a particular PacketId.
	 * 	
	 * @return m_value		The byte value of the packetId.
	 */
	public byte toByteValue()
	{
		return m_value;
	}
	
	/**
	 * Returns the string name of the packetId.
	 * 
	 * @return m_name	The string name of the packetId.
	 */
	public String toString()
	{
		return m_name;
	}
};
