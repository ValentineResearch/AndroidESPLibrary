/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.packets.response;

import com.valentine.esp.constants.Devices;
import com.valentine.esp.data.SweepDefinition;
import com.valentine.esp.packets.ESPPacket;

public class ResponseDefaultSweepDefinitions extends ESPPacket {

	public ResponseDefaultSweepDefinitions(Devices _destination) {
		m_destination = _destination.toByteValue();
		m_timeStamp = System.currentTimeMillis();
		buildPacket();
	}

	@Override
	/**
	 * See parent for default implementation.
	 */
	protected void buildPacket() {
		super.buildPacket();
	}

	@Override
	/**
	 * Returns the SweepDefinition created from the payload data of this packet.
	 * @returns The sweep definition.
	 */
	public Object getResponseData() {
		SweepDefinition rc = new SweepDefinition();

		rc.buildFromBytes(payloadData);

		return rc;
	}

}
