/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.utilities;

public class Range {

	public int LoFreq;
	public int HiFreq;
	
	/**
	 * Constructor for the Range object.
	 * 
	 * @param loFreq - The low frequency for the new range.
	 * @param hiFreq - The high frequency for the new range.
	 */
	public Range(int loFreq, int hiFreq) {
		LoFreq = loFreq;
		HiFreq = hiFreq;	
	}
	
	/**
	 * Constructor for the new range. This will initialize the range to 0 - 0.
	 */
	public Range() 
	{
		LoFreq = 0;
		HiFreq = 0;
	}

	/**
	 * This method will return the width of this range. The range unit (MHz, GHz, etc) of the frequencies 
	 * is not defined by this object, so the return value of this method is unitless.
	 *  
	 * @return This method returns HiFreq - LowFreq.
	 */
	public double getWidth() {
		return (HiFreq - LoFreq);
	}
	
	/**
	 * Method to clone this object
	 */
	public Range clone()
	{
		return new Range(LoFreq, HiFreq);
	}
	
	/**
	 * This method will compare the range passed in to see if it is contained within this object.
	 * 
	 * @param r - The range to use for the comparison.
	 * 
	 * @return True if the ranges passed in is completely within the range defined by this object.
	 */
	public boolean isContained(Range r)
	{
		if ( (r.LoFreq >= LoFreq) && (r.LoFreq <= HiFreq) && (r.HiFreq >= LoFreq) && (r.HiFreq <= HiFreq) ){
			return true;
		}
		
		return false;		
	}
	
	/**
	 * This method will compare the low frequency and the high frequency passed in to see if it is contained within this range.
	 * 
	 * @param _loFreq - The low frequency to check.
	 * @param _hiFreq - The high frequency to check.
	 * 
	 * @return True if the ranges passed in is completely within the range defined by this object.
	 */
	public boolean isContained(int _loFreq, int _hiFreq)
	{
		if ( (_loFreq >= LoFreq) && (_loFreq <= HiFreq) && (_hiFreq >= LoFreq) && (_hiFreq <= HiFreq) ){
			return true;
		}
		
		return false;		
	}
	
	/**
	 * This method will compare the frequency passed in to see if it is contained within this range.
	 * 
	 * @param frequency - The frequency to check.
	 * 
	 * @return True if the ranges passed in is completely within the range defined by this object.
	 */
	public boolean isContained(int frequency)
	{
		if( (frequency >= this.LoFreq) && (frequency <= this.HiFreq) ) {
			return true;
		}
		return false;
	}
}
