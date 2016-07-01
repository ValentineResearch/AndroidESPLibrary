/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

import com.valentine.esp.constants.Devices;
import com.valentine.esp.constants.ESPLibraryLogController;
import com.valentine.esp.constants.PacketId;
import com.valentine.esp.packets.ESPPacket;

/** This class encapsulates the input and output packet queues used by the ESP client and the reading and 
 * 	writing threads.  Should not be needed to be directly used.
 *
 */
public class PacketQueue 
{
	private static ReentrantLock m_inlock = new ReentrantLock();
	private static ReentrantLock m_outlock = new ReentrantLock(); 
	
	private static LinkedList<ESPPacket> m_inputQueue = new LinkedList<ESPPacket>();
	private static LinkedList<ESPPacket> m_outputQueue = new LinkedList<ESPPacket>();
	
	private static boolean m_holdoffOutput = true;						// If true, getNextOutputPacket will return null
	private static Devices m_v1Type = Devices.UNKNOWN;					// This is used to rebuild ESP packets before they are written to the hardware if the V1 type changes while there are packets in the queue.
	
	private static ArrayList<Byte> m_busyPacketIds = new ArrayList<Byte>();
	
	private static Map<PacketId, ESPPacket> m_lastSentPacket = new HashMap<PacketId, ESPPacket>();
	
	private static ArrayList<ESPPacket> m_toSendAfterBusyClear = new ArrayList<ESPPacket>();
	
	/** Returns the next packet from the Valentine One
	 * 
	 * @return ESPPacket	The next packet from the Valentine One
	 */
	public static ESPPacket getNextInputPacket()
	{
		ESPPacket rc;
		m_inlock.lock();
		if (m_inputQueue.size() == 0)
		{
			rc = null;
		}
		else
		{
			rc = m_inputQueue.remove();
		}
		m_inlock.unlock();
		return rc;
	}
	
	/** Pushes a packet from the Valentine One onto the queue to be processed
	 * 
	 * @param packet The packet from the Valentine One
	 */
	public static void pushInputPacketOntoQueue(ESPPacket packet)
	{
		m_inlock.lock();
		m_inputQueue.addLast(packet);
		m_inlock.unlock();
	}
	
	/** Initialize the output queue. The initialization is done in a single lock, which makes this method preferable to using the individual methods.
	 * 
	 * @param v1Type 			The current type of the V1. This is typically initialized to Devices.Unknown and then refreshed with a call to setNewV1Type once the app receives an infDisplayData packet.
	 * @param clearOutputQueue 	If true, the output queue will be cleared.
	 * @param holdoffOutput 	If true, no output packets will be returned from getNextOutputPacket() until setHoldoffOutput() is called with the holdoffOutput parameter set to false.
	 */
	public static void initOutputQueue (Devices v1Type, boolean clearOutputQueue, boolean holdoffOutput)
	{
		m_outlock.lock();
		if ( clearOutputQueue ){
			if ( m_outputQueue.size() != 0 ){
				if(ESPLibraryLogController.LOG_WRITE_DEBUG){
					Log.d("Valentine", "Deleting " + m_outputQueue.size() + " packets from output queue." );
				}
			}
			m_outputQueue.clear();			
		}		
		m_v1Type = v1Type;
		m_holdoffOutput = holdoffOutput;
		m_outlock.unlock();		
	}
	
	/** Initialize the input queue. The initialization is done in a single lock, which makes this method preferable to using the individual methods.
	 * 
	 * @param clearOutputQueue 	If true, the output queue will be cleared.
	 */
	public static void initInputQueue (boolean clearOutputQueue)
	{
		m_inlock.lock();
		if ( clearOutputQueue ){
			if ( m_inputQueue.size() != 0 ){
				if(ESPLibraryLogController.LOG_WRITE_DEBUG){
					Log.d("Valentine", "Deleting " + m_inputQueue.size() + " packets from input queue." );
				}
			}
			m_inputQueue.clear();			
		}	
		m_inlock.unlock();
	}
	
	
	/** Gets the next packet off the queue to be written to the Valentine One
	 * 
	 * @return ESPPacket The next packet to be written to the Valentine One
	 */
	public static ESPPacket getNextOutputPacket()
	{
		ESPPacket rc = null;
		m_outlock.lock();
		if (m_outputQueue.size() == 0 ){
			// No packets to send or we don't have a V1 type yet			
			rc = null;
		}
		else if ( m_holdoffOutput ){
			rc = null;
			
			// Check for packets destined for the V1connection and allow them to override the holdoff
			for ( int i = 0; i < m_outputQueue.size(); i++ ){
				ESPPacket p = (ESPPacket)m_outputQueue.get(i);
				
				if ( p.getDestination() == Devices.V1CONNECT && p.getOrigin() == Devices.V1CONNECT ){
					rc = m_outputQueue.remove(i);
				}
			}
		}
		else{
			int location = 0;
			boolean found = false;
			
			while (rc == null)
			{
				if (!found)
				{
					rc = m_outputQueue.remove(location);
				}
				else
				{
					location++;
				}
				
				if (location == m_outputQueue.size())
				{
					break;
				}
			}
		}
		
		if ( rc != null ){
			// Change the packet type if it doesn't match the last type specified.
			// Don't change if the last type specified is UNKNOWN because there is not a valid type to change it to.			
			if ( rc.getV1Type() != m_v1Type && m_v1Type != Devices.UNKNOWN ){
				rc.setNewV1Type(m_v1Type);
			}
		}
		
		m_outlock.unlock();
		
		return rc;
	}
	
	/** Pushes a packet onto the queue to write to the Valentine One. If the packet was build with a different V1 than the V1 type specified in setNewV1Type() or initOutputQueue(), the packet will be rebuilt with the correct V1 type. 
	 * 
	 * @param packet The next packet to push on the output queue to write to the Valentine One
	 */
	public static void pushOutputPacketOntoQueue(ESPPacket packet)
	{
		m_outlock.lock();			
		boolean addPacketToQueue = true;
		
		for ( int i = 0; i < m_outputQueue.size(); i++ ){
			ESPPacket curPacket = m_outputQueue.get(i);
			if ( packet.isSamePacket(curPacket) ){
				// Don't put this packet into the queue
				addPacketToQueue = false;
				break;
			}
		}
		
		if ( addPacketToQueue ){
			m_outputQueue.addLast(packet);		
		}
		
		m_outlock.unlock();
	}
	
	/** 
	 * Tells the write queue to allow or prevent packets to be sent to the hardware.
	 * 
	 * @param holdoffOutput		Flag to control writing ESPPackets to the Valentine One.
	 */
	public static void setHoldoffOutput (boolean holdoffOutput)
	{
		m_outlock.lock();
		m_holdoffOutput = holdoffOutput;
		m_outlock.unlock();	
	}
	
	/** Determine if the write queue is allowed to send packets to the hardware.
	 * 
	 * @return true if packets can be sent to the hardware, else false
	 */
	public static boolean getHoldoffOutput ()
	{
		m_outlock.lock();
		boolean retVal = m_holdoffOutput;
		m_outlock.unlock();	
		
		return retVal;
	}
	
	/** Tells the output queue to change the V1 type. 
	 * 
	 * @param v1Type	The new V1 type for the output queue.
	 */
	public static void setNewV1Type (Devices v1Type)
	{
		m_outlock.lock();
		m_v1Type = v1Type;
		m_outlock.unlock();
	}
	
	/**
	 * Get the current V1 type.
	 * 
	 * @return The current V1 type.
	 */
	public static Devices getV1Type ()
	{
		m_outlock.lock();
		byte typeByte = m_v1Type.toByteValue();
		m_outlock.unlock();
		
		return Devices.fromByteValue(typeByte);
	}
	
	
	/** Sets the packets the Valentine One is working on from the InfBusyPacket it sends.
	 * 
	 * @param newPacket the InfBusyPacket the Valentine One send.
	 */
	public static void setBusyPacketIds(ESPPacket newPacket) 
	{
		m_busyPacketIds.clear();
		String log = "";
		
		if (newPacket != null)
		{
			byte[] payload = newPacket.getPayload();
			for (int i = 0; i < payload.length; i++)
			{
				Byte newId = payload[i];
				m_busyPacketIds.add(newId);
			}
			
			/*
			for (int i = 0; i < m_busyPacketIds.size(); i++)
			{
				log = log + "[" + Byte.toString(m_busyPacketIds.get(i)) + "] ";
			}
			*/
		}
		
		if(ESPLibraryLogController.LOG_WRITE_INFO){
			Log.i("Valentine", log);
		}
	}
	
	/** Removes the packets the Valentine One is working on from the InfBusyPacket it sends.
	 * 
	 * @param _id the id of the packet to remove.
	 */
	public static void removeFromBusyPacketIds(PacketId _id)
	{
		if (m_busyPacketIds.size() != 0)
		{
			byte id =  _id.toByteValue();
			int toRemove = -1;
			
			for (int i = 0; i < m_busyPacketIds.size(); i++)
			{
				if (id == m_busyPacketIds.get(i) )
				{
					toRemove = 0;
					break;
				}
			}
			
			if (toRemove != -1)
			{
				m_busyPacketIds.remove(toRemove);
			}
		}
	}
	
	/**
	 * Check for a specific packet id in the list of items the V1 is busy working on.
	 * 
	 * @param _id The id of the packet to check for.
	 * 
	 * @return True if the packet id is in the list of items the V1 is busy working on, else false.
	 */
	public static boolean isPacketIdInBusyList(PacketId _id)
	{
		byte packetIdByte = _id.toByteValue();
		
		if (m_busyPacketIds.size() == 0)
		{
			return false;
		}
		
		for (int i = 0; i < m_busyPacketIds.size(); i++)
		{
			if (m_busyPacketIds.get(i) == packetIdByte)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Add a packet to the list of packets that were sent. 
	 * 
	 * @param _packet The packet that was sent.
	 */
	public static void putLastWrittenPacketOfType(ESPPacket _packet)
	{
		m_lastSentPacket.put(_packet.getPacketIdentifier(), _packet);
	}
	
	/**
	 * Get the last packet sent that has the packet id provided.
	 * 
	 * @param _id The packet id to search for.
	 * 
	 * @return If a packet was sent with the id passed in, return the last packet sent that has the packet id provided, else return null.
	 */
	public static ESPPacket getLastWrittenPacketOfType(PacketId _id)
	{
		if (m_lastSentPacket.containsKey(_id))
		{
			return m_lastSentPacket.get(_id);
		}
		else
		{
			return null;
		}
	}
	
	/** 
	 * Add a packet to send to the V1 as soon as it is done being busy.
	 * 
	 * @param _packet The packet to send.
	 */
	public static void pushOnToSendAfterBusyQueue(ESPPacket _packet)
	{
		m_outlock.lock();
		boolean addToQueue = true;
		
		for (int i = 0; i < m_toSendAfterBusyClear.size(); i++){
			ESPPacket curPacket = m_toSendAfterBusyClear.get(i);
			if ( curPacket.getPacketIdentifier().toByteValue() == _packet.getPacketIdentifier().toByteValue() ){
				// This packet is already in the queue, so don't resend it after we are busy
				addToQueue = false;
			}
		}
		
		if ( addToQueue ){
			for ( int i = 0; i < m_busyPacketIds.size(); i++ ){
				if ( _packet.getPacketIdentifier().toByteValue() == m_busyPacketIds.get(i).byteValue() ){
					// This packet is in the list of packets the V1 is working on, so don't add it to the queue
					addToQueue = false;
				}
			}
		}
		
		if ( addToQueue ){
			m_toSendAfterBusyClear.add(_packet);
		}
		m_outlock.unlock();
	}
	
	/**
	 * This method will add all packets passed into pushOnToSendAfterBusyQueue() to the active write queue. 
	 */
	public static void sendAfterBusyQueue()
	{
		m_outlock.lock();		
		if ( m_toSendAfterBusyClear.size() > 0 ){
			Log.i("Valentine", "V1 not busy. Trying to resend " + m_toSendAfterBusyClear.size() + " packets");
		}		
		for (int i = 0; i < m_toSendAfterBusyClear.size(); i++)
		{
			ESPPacket packet = m_toSendAfterBusyClear.get(i);
			PacketQueue.pushOutputPacketOntoQueue(packet);
		}
		m_outlock.unlock();
		m_toSendAfterBusyClear.clear();
	}
	
	/**
	 * This method will clear the send after busy queue.
	 */
	public static void clearSendAfterBusyQueue ()
	{
		m_outlock.lock();		
		m_toSendAfterBusyClear.clear();
		m_outlock.unlock();
	}
}
