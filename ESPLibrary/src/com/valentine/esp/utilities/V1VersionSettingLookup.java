/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.utilities;

import java.util.ArrayList;

import com.valentine.esp.data.SweepSection;

public class V1VersionSettingLookup 
{
	/**
	 *  Enumeration used to extract band ranges and lower/upper edge frequencies.
	 * @author jdavis
	 *
	 */
	public enum V1_Band {
		X,
		Ku,
		K,
		Ka,
		Ka_Lo,
		Ka_Mid,
		Ka_Hi,
		POP,
		No_Band;		
	}
	
	// Default to not in demo mode.
	protected static boolean isInDemoMode = false;
	
	// Use version V3.8930 as the default version for this app. 
	protected final static double DEFAULT_VERSION = 3.8930;	
	
	// The ability to read the default sweeps from the V1 was added in V3.8950.
	protected final static double READ_SWEEP_DEFAULTS_START_VER = 3.8950;
	
	// The TMF & Junk-K Fighter feature was turned on by default in verison 3.8945
	protected final static double START_TMF_ON_BY_DEFAULT = 3.8945;
	
	protected static double currentVersion = DEFAULT_VERSION;
	
	/*************************************************************************************************************
	 *************************************************************************************************************
	 ******************************                                              *********************************
	 ******************************		   Demo Mode Definitions	             *********************************
	 ******************************                                              *********************************
	 *************************************************************************************************************
	 ************************************************************************************************************/
	/* Demo mode sweep section frequencies */
	private static final int V3_8920_DEMO_SWEEP_SECTION_LO 	 = 33383;
	private static final int V3_8920_DEMO_SWEEP_SECTION_HI	 = 36072;	
	
	
	/*************************************************************************************************************
	 *************************************************************************************************************
	 ******************************                                              *********************************
	 ******************************			V3.8920 Definitions		             *********************************
	 ******************************                                              *********************************
	 *************************************************************************************************************
	 ************************************************************************************************************/
	// V3.8920 is the first production release that supports the ESP specification.
	/* V3.9820 Custom sweep ranges */
	private static final Range[] V3_8920_CUSTOM_SWEEPS =
	{
		new Range(33900, 34106),
		new Range(34180, 34475),
		new Range(34563, 34652),
		new Range(35467, 35526),
		new Range(0, 0),
		new Range(0, 0)
	};
	
	/* V3.9820 maximum number of custom sweep ranges */
	private static final int V38920_MAX_SWEEP_INDEX 		  = 5; 
	
	/* V3.9820 Band frequencies */
	private static final double V3_8920_BAND_X_LO             = 10.477;
	private static final double V3_8920_BAND_X_HI             = 10.566;
	private static final double V3_8920_BAND_X_POLICE_LO      = 10.500;
	private static final double V3_8920_BAND_X_POLICE_HI      = 10.550;
	private static final double V3_8920_BAND_KU_LO            = 13.394;
	private static final double V3_8920_BAND_KU_HI            = 13.512;
	private static final double V3_8920_BAND_KU_POLICE_LO     = 13.400;
	private static final double V3_8920_BAND_KU_POLICE_HI     = 13.500;
	private static final double V3_8920_BAND_K_LO             = 24.036;
	private static final double V3_8920_BAND_K_HI             = 24.272;
	private static final double V3_8920_BAND_K_POLICE_LO      = 24.050;
	private static final double V3_8920_BAND_K_POLICE_HI      = 24.250;
	private static final double V3_8920_BAND_KA_LO_LO         = 33.400;
	private static final double V3_8920_BAND_KA_LO_HI         = 34.300;
	private static final double V3_8920_BAND_KA_LO_POLICE_LO  = 33.700;
	private static final double V3_8920_BAND_KA_LO_POLICE_HI  = 33.900;
	private static final double V3_8920_BAND_KA_MID_LO        = 34.301;
	private static final double V3_8920_BAND_KA_MID_HI        = 35.100;
	private static final double V3_8920_BAND_KA_MID_POLICE_LO = 34.600;
	private static final double V3_8920_BAND_KA_MID_POLICE_HI = 34.800;
	private static final double V3_8920_BAND_KA_HI_LO         = 35.101;
	private static final double V3_8920_BAND_KA_HI_HI         = 36.000;
	private static final double V3_8920_BAND_KA_HI_POLICE_LO  = 35.400;
	private static final double V3_8920_BAND_KA_HI_POLICE_HI  = 35.600;
	private static final double V3_8920_BAND_POP_LO           = 33.700;
	private static final double V3_8920_BAND_POP_HI           = 33.900;
	
	/* Default sweep section frequencies */
	private static final int V3_8920_SWEEP_SECTION_1_LO		  = 33380;
	private static final int V3_8920_SWEEP_SECTION_1_HI	 	  = 34770;
	private static final int V3_8920_SWEEP_SECTION_2_LO		  = 34774;
	private static final int V3_8920_SWEEP_SECTION_2_HI	 	  = 36072;	
	
	/**
	 * This method will convert the version string to a floating point value
	 * 
	 * @param _version The string received from the hardware
	 */
	public void setV1Version (final String _version)
	{
		if ( _version.substring(0, 1).equals(new String("V")) ) {
			try{
				// The version is in the format V0.1234 so we need to start with the second character. 
				currentVersion = Double.parseDouble(_version.substring(1));					
			}
			catch (Exception e){
				currentVersion = DEFAULT_VERSION;
			}
		}
	}
	
	/**
	 * This method will put this object into or out of Demo Mode.
	 * 
	 * @param inDemoMode - The new Demo Mode state for this object.
	 */
	public void setDemoMode (boolean inDemoMode)
	{
		isInDemoMode = inDemoMode;
	}
	
	/**
	 * Gets the default custom sweeps based on the V1 version.
	 * 
	 * @return An array of ranges that define the custom sweeps for the current V1 version.
	 */
	public Range[] getV1DefaultCustomSweeps() {
		
		int maxSweepIndex;
		Range [] customSweeps;
		
		// Use the sweeps for the initial ESP release
		maxSweepIndex = V38920_MAX_SWEEP_INDEX;
		customSweeps = V3_8920_CUSTOM_SWEEPS;
			
		// Create a new array that can be modified by the caller without affecting this object.
		// Use '+ 1' because the sweep index is zero based so there are maxSweepIndex + 1 sweeps available.
		Range[] returnRange = new Range[maxSweepIndex + 1];
		for ( int i = 0; i <= maxSweepIndex; i++ ){
			returnRange[i] = new Range (customSweeps[i].LoFreq, customSweeps[i].HiFreq);
		}	
		return returnRange;
	}
	
	/**
	 * Retrieves the range for the specified band.
	 * 
	 * @param band	The band to retrieve the range for.
	 * 
	 * @return A range containing the correct frequency for the supplied band.
	 */
	public Range getDefaultRangeForBand(V1_Band band) {
		// All versions use the same band ranges so just use the default values from V3.8920.
		switch(band) {
			case Ka:
				return new Range((int) (V3_8920_BAND_KA_LO_LO * 1000), (int) (V3_8920_BAND_KA_HI_HI * 1000));
			case Ka_Hi:
				return new Range((int) (V3_8920_BAND_KA_HI_LO * 1000), (int) (V3_8920_BAND_KA_HI_HI * 1000));
			case Ka_Lo:
				return new Range((int) (V3_8920_BAND_KA_LO_LO * 1000), (int) (V3_8920_BAND_KA_LO_HI * 1000));
			case Ka_Mid:
				return new Range((int) (V3_8920_BAND_KA_MID_LO * 1000), (int) (V3_8920_BAND_KA_MID_HI * 1000));
			case K:
				return new Range((int) (V3_8920_BAND_K_LO * 1000), (int) (V3_8920_BAND_K_HI * 1000));
			case Ku:
				return new Range((int) (V3_8920_BAND_KU_LO * 1000), (int) (V3_8920_BAND_KU_HI * 1000));
			case X:
				return new Range((int) (V3_8920_BAND_X_LO * 1000), (int) (V3_8920_BAND_X_HI * 1000));
			case POP:
				return new Range((int) (V3_8920_BAND_POP_LO * 1000), (int) (V3_8920_BAND_POP_HI * 1000));
			case No_Band:
			default:
				return new Range();	
		
		}
	}
	
	/**
	 * Retrieves the police/box range for the specified band.
	 * 
	 * @param band	The band to retrieve the range for.
	 * 
	 * @return A range containing the correct frequency for the supplied band.
	 */
	public Range getDefaultRangeForPolice(V1_Band band) {
		// All versions use the same box ranges so just use the default values from V3.8920.
		switch(band){
			case K:
				return new Range((int) (V3_8920_BAND_K_POLICE_LO * 1000), (int) (V3_8920_BAND_K_POLICE_HI * 1000));
			case Ka_Hi:
				return new Range((int) (V3_8920_BAND_KA_HI_POLICE_LO * 1000), (int) (V3_8920_BAND_KA_HI_POLICE_HI * 1000));
			case Ka_Mid:
				return new Range((int) (V3_8920_BAND_KA_MID_POLICE_LO * 1000), (int) (V3_8920_BAND_KA_MID_POLICE_HI * 1000));
			case Ka_Lo:
				return new Range((int) (V3_8920_BAND_KA_LO_POLICE_LO * 1000), (int) (V3_8920_BAND_KA_LO_POLICE_HI * 1000));
			case Ku:
				return new Range((int) (V3_8920_BAND_KU_POLICE_LO * 1000), (int) (V3_8920_BAND_KU_POLICE_HI * 1000));
			case X:
				return new Range((int) (V3_8920_BAND_X_POLICE_LO * 1000), (int) (V3_8920_BAND_X_POLICE_HI * 1000));
			case POP:
			case Ka:
			case No_Band:
			default:
				return new Range();	
		}
	}
	
	/**
	 * Retrieves the lower edge frequency of the specified band.
	 * 
	 * @param band		The band to retrieve the lower edge frequency for.
	 * @return			the Frequency of the lower edge for the specified band.
	 */
	public double getDefaultLowerEdge(V1_Band band) {
		// All versions use the same band ranges so just use the default values from V3.8920.
		switch(band){
			case Ka:
				return V3_8920_BAND_KA_LO_LO;
			case Ka_Hi:
				return V3_8920_BAND_KA_HI_LO;
			case Ka_Lo:
				return V3_8920_BAND_KA_LO_LO;
			case Ka_Mid:
				return V3_8920_BAND_KA_MID_LO;
			case K:
				return V3_8920_BAND_K_LO;
			case Ku:
				return V3_8920_BAND_KU_LO;
			case X:
				return V3_8920_BAND_X_LO;
			case POP:
				return  V3_8920_BAND_POP_LO;
			case No_Band:
			default:
				return 0.0;	
		}
	}
	
	/**
	 * Retrieves the upper edge frequency of the specified band.
	 * 
	 * @param band		The band to retrieve the upper edge frequency for.
	 * @return			the Frequency of the upper edge for the specified band.
	 */
	public double getDefaultUpperEdge(V1_Band band) {
		// All versions use the same band ranges so just use the default values from V3.8920.
		switch(band){
			case Ka:
				return V3_8920_BAND_KA_HI_HI;
			case Ka_Hi:
				return V3_8920_BAND_KA_HI_HI;
			case Ka_Mid:
				return V3_8920_BAND_KA_MID_HI;
			case Ka_Lo:
				return V3_8920_BAND_KA_LO_HI;
			case K:
				return V3_8920_BAND_K_HI;
			case Ku:
				return V3_8920_BAND_KU_HI;
			case X:
				return V3_8920_BAND_X_HI;
			case POP:
				return V3_8920_BAND_POP_HI;
			case No_Band:
			default:
				return 0.0;	
		}
	}
	
	/**
	 * Retrieves the lower edge frequency of the police detection box (Box) for the specified band.
	 * 
	 * @param band		The band to retrieve the lower edge frequency for.
	 * @return			the Frequency of the lower edge for the specified band.
	 */
	public double getDefaultPoliceLowerEdge(V1_Band band) {
		// All versions use the same box ranges so just use the default values from V3.8920.
		switch(band){
			case K:
				return V3_8920_BAND_K_POLICE_LO;
			case Ka_Hi:
				return V3_8920_BAND_KA_HI_POLICE_LO;
			case Ka_Mid:
				return V3_8920_BAND_KA_MID_POLICE_LO;
			case Ka_Lo:
				return V3_8920_BAND_KA_LO_POLICE_LO;
			case Ku:
				return V3_8920_BAND_KU_POLICE_LO;
			case X:
				return V3_8920_BAND_X_POLICE_LO;
			case Ka:
			case POP:
			case No_Band:
			default:
				return 0.0;				
		}
	}
	
	/**
	 * Retrieves the upper edge frequency of the police detection box (Box) for the specified band.
	 * 
	 * @param band		The band to retrieve the upper edge frequency for.
	 * @return			the Frequency of the upper edge for the specified band.
	 */
	public double getDefaultPoliceUpperEdge(V1_Band band) {
		// All versions use the same box ranges so just use the default values from V3.8920.
		switch(band){
			case K:
				return V3_8920_BAND_K_POLICE_HI;
			case Ka_Hi:
				return V3_8920_BAND_KA_HI_POLICE_HI;
			case Ka_Mid:
				return V3_8920_BAND_KA_MID_POLICE_HI;
			case Ka_Lo:
				return V3_8920_BAND_KA_LO_POLICE_HI;
			case Ku:
				return V3_8920_BAND_KU_POLICE_HI;
			case X:
				return V3_8920_BAND_X_POLICE_HI;
			case Ka:
			case POP:
			case No_Band:
			default:
				return 0.0;				
		}
	}
	
	/**
	 * Gets the default sweep section for the current V1 version
	 * 
	 * @return The default sweep section.
	 */
	public ArrayList<SweepSection> getV1DefaultSweepSections()
	{
		ArrayList<SweepSection> retObj = new ArrayList<SweepSection>();
		
		if ( isInDemoMode ){
			// Use a full sweep as the only section when in demo mode
			retObj.add(new SweepSection (V3_8920_DEMO_SWEEP_SECTION_LO, V3_8920_DEMO_SWEEP_SECTION_HI)); 
		}
		else{
			// Use the V3.8920 defaults when not in demo mode
			retObj.add(new SweepSection (V3_8920_SWEEP_SECTION_1_LO, V3_8920_SWEEP_SECTION_1_HI));
			retObj.add(new SweepSection (V3_8920_SWEEP_SECTION_2_LO, V3_8920_SWEEP_SECTION_2_HI));	
		}		
		
		return retObj;
	}
	
	/**
	 * This method will return the default max sweep index based on the current V1 version.
	 * 
	 * @return The default max sweep index based on the current V1 version.
	 */
	public int getV1DefaultMaxSweepIndex()
	{
		// Use the index for the initial ESP release
		return V38920_MAX_SWEEP_INDEX;
		
	}	
	/** This helper method will return an empty range. This method can be used to provide an app-wide definition of 
	 * precisely what an empty range is.
	 * 
	 * @return A range with both endpoints set to zero.
	 */
	public Range getEmptyRange() {
		return new Range();
	}
	
	/**
	 * This method will determine if the current version of the V1 supports reading the custom sweep defaults.
	 * 
	 * @return true if the last version read from the V1 indicates the V1 supports reading the custom sweep defaults, else false.
	 */
	public boolean allowDefaulSweepDefRead()
	{
		return ( currentVersion >= READ_SWEEP_DEFAULTS_START_VER );
	}
	
	/**
	 * This method will determine if the current version of the V1 will have the TMF/Junk-K Fighter feature 
	 * turned on by default or if the feature is off by default. 
	 * 
	 * @return true if the TMF/Junk-K Fighter feature is turned on by default, else return false
	 */
	public static boolean defaultTMFState()
	{
		if ( isInDemoMode ){
			// Turn on TMF & Junk-K Fighter by default when in demo mode.
			return true;
		}
		
		if ( currentVersion >= START_TMF_ON_BY_DEFAULT ){
			// This version has TMF on by default as the unit is shipped from the factory.
			return true;
		}
		
		// This version does not have TMF on by default
		return false;
	}
	
}
