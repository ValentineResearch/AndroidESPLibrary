/*
 * Copyright(c) 2016 Valentine Research, Inc
 * This file is part of the ESP Library, which is licensed under the MIT license. 
 * You should have received a copy of the MIT license along with this file. If not, see http://opensource.org/licenses/MIT
 */
package com.valentine.esp.data;

import java.util.ArrayList;

import com.valentine.esp.utilities.V1VersionSettingLookup;

public class UserSettings 
{
	public enum Constants 
	{
		ON,
		OFF,
		NORMAL,
		RESPONSIVE,
		LEVER,
		ZERO,
		KNOB
	}
	
	Constants m_XBand;
	Constants m_KBand;
	Constants m_KaBand;
	Constants m_laser;
	
	Constants m_bargraph;
	
	Constants m_kaFalseGuard;
	Constants m_featureBGKMuting;

	Constants m_muteVolume;
	Constants m_postmuteBogeyLockVolume;
	
	String m_kMuteTimer;
	
	Constants m_KInitialUnmute4Lights;
	Constants m_KPersistentUnmute6Lights;
	Constants m_KRearMute;
	Constants m_KuBand;
	Constants m_Pop;
	Constants m_Euro;
	Constants m_EuroXBand;
	Constants m_Filter;
	Constants m_ForceLegacy;
	
	/**
	 * Gets X band constant.
	 * 
	 * @return X Band constant.
	 */
	public Constants getXBand()
	{
		return m_XBand;
	}
	
	/**
	 * Gets K band constant.
	 * 
	 * @return K Band constant.
	 */
	public Constants getKBand()
	{
		return m_KBand;
	}

	/**
	 * Gets Ka band constant.
	 * 
	 * @return Ka Band constant.
	 */
	public Constants getKaBand()
	{
		return m_KaBand;
	}

	/**
	 * Gets Laser constant.
	 * 
	 * @return Laser constant.
	 */
	public Constants getLaser()
	{
		return m_laser;
	}

	/**
	 * Gets Bargraph constant.
	 * 
	 * @return Bargraph constant.
	 */
	public Constants getBargraph()
	{
		return m_bargraph;
	}

	/**
	 * Gets KaFalseGuard constant.
	 * 
	 * @return KaFalseGuard constant.
	 */
	public Constants getKaFalseGuard()
	{
		return m_kaFalseGuard;
	}

	/**
	 * Gets FeatureBGKMuting constant.
	 * 
	 * @return FeatureBGKMuting constant.
	 */
	public Constants getFeatureBGKMuting()
	{
		return m_featureBGKMuting;
	}

	/**
	 * Gets MuteVolume constant.
	 * 
	 * @return MuteVolume constant.
	 */
	public Constants getMuteVolume()
	{
		return m_muteVolume;
	}

	/**
	 * Gets PostmuteBogeyLockVolume constant.
	 * 
	 * @return PostmuteBogeyLockVolume constant.
	 */
	public Constants getPostmuteBogeyLockVolume()
	{
		return m_postmuteBogeyLockVolume;
	}

	/**
	 * Gets KMuteTimer constant.
	 * 
	 * @return KMuteTimer constant.
	 */
	public String getKMuteTimer()
	{
		return m_kMuteTimer;
	}

	/**
	 * Gets KInitialUnmute4Lights constant.
	 * 
	 * @return KInitialUnmute4Lights constant.
	 */
	public Constants getKInitialUnmute4Lights()
	{
		return m_KInitialUnmute4Lights;
	}

	/**
	 * Gets KPersistentUnmute6Lights constant.
	 * 
	 * @return KPersistentUnmute6Lights constant.
	 */
	public Constants getKPersistentUnmute6Lights()
	{
		return m_KPersistentUnmute6Lights;
	}

	/**
	 * Gets KRearMute constant.
	 * 
	 * @return KRearMute constant.
	 */
	public Constants getKRearMute()
	{
		return m_KRearMute;
	}

	/**
	 * Gets KuBand constant.
	 * 
	 * @return KuBand constant.
	 */
	public Constants getKuBand()
	{
		return m_KuBand;
	}

	/**
	 * Gets Pop constant.
	 * 
	 * @return Pop constant.
	 */
	public Constants getPop()
	{
		return m_Pop;
	}

	/**
	 * Gets Euro constant.
	 * 
	 * @return Euro constant.
	 */
	public Constants getEuro()
	{
		return m_Euro;
	}
	
	/**
	 * Gets Euro X band constant.
	 * 
	 * @return Euro X Band constant.
	 */
	public Constants getEuroXBand()
	{
		return m_EuroXBand;
	}

	/**
	 * Gets Filter constant.
	 * 
	 * @return Filter constant.
	 */
	public Constants getFilter()
	{
		return m_Filter;
	}
	
	public Constants getForceLegacy()
	{
		return m_ForceLegacy;
	}
	
	/**
	 * Gets X band as boolean.
	 * 
	 * @return True if X band is on otherwise false.
	 */
	public boolean getXBandAsBoolean()
	{
		return getOnOffAsBoolean(m_XBand);
	}

	/**
	 * Gets K band as boolean.
	 * 
	 * @return True if K band is on otherwise false.
	 */
	public boolean getKBandAsBoolean()
	{
		return getOnOffAsBoolean(m_KBand);
	}

	/**
	 * Gets Ka band as boolean.
	 * 
	 * @return True if Ka band is on otherwise false.
	 */
	public boolean getKaBandAsBoolean()
	{
		return getOnOffAsBoolean(m_KaBand);
	}

	/**
	 * Gets laser as boolean.
	 * 
	 * @return True if laser is on otherwise false.
	 */
	public boolean getLaserAsBoolean()
	{
		return getOnOffAsBoolean(m_laser);
	}

	/**
	 * Gets Ka False guard as boolean.
	 * 
	 * @return True if Ka false guard is on otherwise false.
	 */
	public boolean getKaFalseGuardAsBoolean()
	{
		return getOnOffAsBoolean(m_kaFalseGuard);
	}

	/**
	 * Gets Feature BGK Muting as boolean.
	 * 
	 * @return True if Feature BGK Muting is on otherwise false.
	 */
	public boolean getFeatureBGKMutingAsBoolean()
	{
		return getOnOffAsBoolean(m_featureBGKMuting);
	}
	
	/**
	 * Gets K Initial Unmute 4 Lights as boolean.
	 * 
	 * @return True if K Initial Unmute 4 Lights is on otherwise false.
	 */
	public boolean getKInitialUnmute4LightsAsBoolean()
	{
		return getOnOffAsBoolean(m_KInitialUnmute4Lights);
	}
	
	/**
	 * Gets K Persistent Unmute 6 Lights as boolean.
	 * 
	 * @return True if K Persistent Unmute 6 Lights is on otherwise false.
	 */
	public boolean getKPersistentUnmute6LightsAsBoolean()
	{
		return getOnOffAsBoolean(m_KPersistentUnmute6Lights);
	}
	
	/**
	 * Gets K Rear Mute as boolean.
	 * 
	 * @return True if K Rear Mute is on otherwise false.
	 */
	public boolean getKRearMuteAsBoolean()
	{
		return getOnOffAsBoolean(m_KRearMute);
	}
	
	/**
	 * Gets Ku Band as boolean.
	 * 
	 * @return True if Ku Band is on otherwise false.
	 */
	public boolean getKuBandAsBoolean()
	{
		return getOnOffAsBoolean(m_KuBand);
	}
	
	/**
	 * Gets Pop as boolean.
	 * 
	 * @return True if Pop is on otherwise false.
	 */
	public boolean getPopAsBoolean()
	{
		return getOnOffAsBoolean(m_Pop);
	}
	
	/**
	 * Gets Euro as boolean.
	 * 
	 * @return True if Euro is on otherwise false.
	 */
	public boolean getEuroAsBoolean()
	{
		return getOnOffAsBoolean(m_Euro);
	}
	
	/**
	 * Gets Euro X Band as boolean.
	 * 
	 * @return True if Euro X Band is on otherwise false.
	 */
	public boolean getEuroXBandAsBoolean()
	{
		return getOnOffAsBoolean(m_EuroXBand);
	}
	
	/**
	 * Gets Filter as boolean.
	 * 
	 * @return True if Filter is on otherwise false.
	 */
	public boolean getFilterAsBoolean()
	{
		return getOnOffAsBoolean(m_Filter);
	}
	
	/**
	 * Gets Force Legacy as boolean.
	 * 
	 * @return True if Force Legacy is on otherwise false.
	 */
	public boolean getForceLegacyAsBoolean()
	{
		return getOnOffAsBoolean(m_ForceLegacy);
	}
	
	/**
	 * Returns whether or not a specified Constant is on.
	 * 
	 * @param _value	The constant to check if active or not.
	 * 
	 * @return	True if the constant is on otherwise false.
	 */
	private boolean getOnOffAsBoolean(Constants _value)
	{
		if (_value == Constants.ON)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Returns whether the Bar Graph is responsive or not.
	 *  
	 * @return True if the bar graph is responsive. 
	 */
	public boolean getBargraphAsBoolean()
	{
		if (m_bargraph == Constants.RESPONSIVE)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Returns whether the volume muting is using 'Lever' or not.
	 *  
	 * @return True if the volume muting is using 'Lever', otherwise false. 
	 */
	public boolean getMuteVolumeAsBoolean()
	{
		if (m_muteVolume == Constants.LEVER)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Returns whether bogey volume mute lock is using 'Knob' or not.
	 * 
	 * @return True if the mute lock is using 'Knob', otherwise false.
	 */
	public boolean getPostMuteBogeyLockVolumeAsBoolean()
	{
		if (m_postmuteBogeyLockVolume == Constants.KNOB)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Sets the X band constant to on.
	 * 
	 * @param _XBand	The constant to set to X band.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setXBand(Constants _XBand)
	{
		if ((_XBand != Constants.ON) && (_XBand != Constants.OFF))
		{
			return false;
		}
			
		m_XBand = _XBand;
		return true;
	}

	/**
	 * Sets the K band constant to on.
	 * 
	 * @param _KBand	The constant to set to K band.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setKBand(Constants _KBand)
	{
		if ((_KBand != Constants.ON) && (_KBand != Constants.OFF))
		{
			return false;
		}
		m_KBand = _KBand;
		return true;
	}

	/**
	 * Sets the Ka band constant to on.
	 * 
	 * @param _KaBand	The constant to set to Ka band.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setKaBand(Constants _KaBand)
	{
		if ((_KaBand != Constants.ON) && (_KaBand != Constants.OFF))
		{
			return false;
		}
		m_KaBand = _KaBand;
		return true;
	}
	
	/**
	 * Sets the Laser constant to on.
	 * 
	 * @param _laser	The constant to set to Laser.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setLaser(Constants _laser)
	{
		if ((_laser != Constants.ON) && (_laser != Constants.OFF))
		{
			return false;
		}
		m_laser = _laser;
		return true;
	}
	
	/**
	 * Sets the bargraph constant to on.
	 * 
	 * @param _bargraph	The constant to set to _bargraph.
	 * 
	 * @return True if the constant both not 'NORMAL' or 'RESPONSIVE'.
	 */
	public boolean setBargraph(Constants _bargraph)
	{
		if ((_bargraph != Constants.NORMAL) && (_bargraph != Constants.RESPONSIVE))
		{
			return false;
		}
		m_bargraph = _bargraph;
		return true;
	}
	
	/**
	 * Sets the KaFalse constant to on.
	 * 
	 * @param _kaFalseGuard	The constant to set to KaFalse.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setKaFalseGuard(Constants _kaFalseGuard)
	{
		if ((_kaFalseGuard != Constants.ON) && (_kaFalseGuard != Constants.OFF))
		{
			return false;
		}
		m_kaFalseGuard = _kaFalseGuard;
		return true;
	}
	
	/**
	 * Sets the Feature BGK Muting constant to on.
	 * 
	 * @param _featureBGKMuting	The constant to set to Feature BGK Muting.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setFeatureBGKMuting(Constants _featureBGKMuting)
	{
		if ((_featureBGKMuting != Constants.ON) && (_featureBGKMuting != Constants.OFF))
		{
			return false;
		}
		m_featureBGKMuting = _featureBGKMuting;
		return true;
	}
	
	/**
	 * Sets the Mute Volume constant to on.
	 * 
	 * @param _muteVolume	The constant to set to the  Mute Volume constant.
	 * 
	 * @return True if the constant both not 'LEVER' or 'ZERO'.
	 */
	public boolean setMuteVolume(Constants _muteVolume)
	{
		if ((_muteVolume != Constants.LEVER) && (_muteVolume != Constants.ZERO))
		{
			return false;
		}
		m_muteVolume = _muteVolume;
		return true;
	}
	
	/**
	 * Sets the Postmute Bogey Lock Volume constant to on.
	 * 
	 * @param _postmuteBogeyLockVolume	The constant to set to the Postmute Bogey Lock Volume constant.
	 * 
	 * @return True if the constant both not 'LEVER' or 'KNOB'.
	 */
	public boolean setPostmuteBogeyLockVolume(Constants _postmuteBogeyLockVolume)
	{
		if ((_postmuteBogeyLockVolume != Constants.LEVER) && (_postmuteBogeyLockVolume != Constants.KNOB))
		{
			return false;
		}
		m_postmuteBogeyLockVolume = _postmuteBogeyLockVolume;
		return true;
	}
	
	/**
	 * Sets K mute Timer to the passed in string.
	 * @param _kMuteTimer
	 * @return	true.
	 */
	public boolean setKMuteTimer(String _kMuteTimer)
	{
		m_kMuteTimer = _kMuteTimer;
		return true;
	}
	
	/**
	 * Sets K Initial Unmute 4 Lights constant to on.
	 * 
	 * @param _KInitialUnmute4Lights	The constant to set to the Initial Unmute 4 Lights constant.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setKInitialUnmute4Lights(Constants _KInitialUnmute4Lights)
	{
		if ((_KInitialUnmute4Lights != Constants.ON) && (_KInitialUnmute4Lights != Constants.OFF))
		{
			return false;
		}
		m_KInitialUnmute4Lights = _KInitialUnmute4Lights;
		return true;
	}
	
	/**
	 * Sets K Persistent Unmute 6 Lights constant to on.
	 * 
	 * @param _KPersistentUnmute6Lights	The constant to set to the K Persistent Unmute 6 Lights constant.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setKPersistentUnmute6Lights(Constants _KPersistentUnmute6Lights)
	{
		if ((_KPersistentUnmute6Lights != Constants.ON) && (_KPersistentUnmute6Lights != Constants.OFF))
		{
			return false;
		}
		m_KPersistentUnmute6Lights = _KPersistentUnmute6Lights;
		return true;
	}
	
	/**
	 * Sets K Rear Mute constant to on.
	 * 
	 * @param _KRearMute	The constant to set to the K Rear Mute constant.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setKRearMute(Constants _KRearMute)
	{
		if ((_KRearMute != Constants.ON) && (_KRearMute != Constants.OFF))
		{
			return false;
		}
		m_KRearMute = _KRearMute;
		return true;
	}
	
	/**
	 * Sets Ku Band constant to on.
	 * 
	 * @param _KuBand	The constant to set to the Ku Band constant.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setKuBand(Constants _KuBand)
	{
		if ((_KuBand != Constants.ON) && (_KuBand != Constants.OFF))
		{
			return false;
		}
		m_KuBand = _KuBand;
		return true;
	}
	
	/**
	 * Sets Pop constant to on.
	 * 
	 * @param _Pop	The constant to set to the Pop constant.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setPop(Constants _Pop)
	{
		if ((_Pop != Constants.ON) && (_Pop != Constants.OFF))
		{
			return false;
		}
		m_Pop = _Pop;
		return true;
	}
	
	/**
	 * Sets Euro constant to on.
	 * 
	 * @param _Euro	The constant to set to the Euro constant.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setEuro(Constants _Euro)
	{
		if ((_Euro != Constants.ON) && (_Euro != Constants.OFF))
		{
			return false;
		}
		m_Euro = _Euro;
		return true;
	}
	
	/**
	 * Sets Pop constant to on.
	 * 
	 * @param _EuroXBand	The constant to set to the Pop constant.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setEuroXBand(Constants _EuroXBand)
	{
		if ((_EuroXBand != Constants.ON) && (_EuroXBand != Constants.OFF))
		{
			return false;
		}
		m_EuroXBand = _EuroXBand;
		return true;
	}
	
	/**
	 * Sets Filter constant to on.
	 * 
	 * @param _Filter	The constant to set to the Filter constant.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setFilter(Constants _Filter)
	{
		if ((_Filter != Constants.ON) && (_Filter != Constants.OFF))
		{
			return false;
		}
		m_Filter = _Filter;
		return true;
	}
	
	/**
	 * Sets ForceLegacy constant to on.
	 * 
	 * @param _ForceLegacy	The constant to set to the ForceLegacy constant.
	 * 
	 * @return True if the constant is neither 'ON' or 'OFF'.
	 */
	public boolean setForceLegacy(Constants _ForceLegacy)
	{
		if ((_ForceLegacy != Constants.ON) && (_ForceLegacy != Constants.OFF))
		{
			return false;
		}
		m_ForceLegacy = _ForceLegacy;
		return true;
	}
	
	/**
	 * Sets the X band constant to on OR OFF.
	 * 
	 * @param _value	The state to set the X band constant to.
	 */
	public void setXBandAsBoolean(boolean _value)
	{
		m_XBand = getOnOffFromBoolean(_value);
	}

	/**
	 * Sets the K band constant to on OR OFF.
	 * 
	 * @param _value	The state to set the K band constant to.
	 */
	public void setKBandAsBoolean(boolean _value)
	{
		m_KBand = getOnOffFromBoolean(_value);
	}

	/**
	 * Sets the Ka band constant to on OR OFF.
	 * 
	 * @param _value	The state to set the Ka band constant to.
	 */
	public void setKaBandAsBoolean(boolean _value)
	{
		m_KaBand = getOnOffFromBoolean(_value);
	}

	/**
	 * Sets the Laser constant to on OR OFF.
	 * 
	 * @param _value	The state to set Laser constant to.
	 */
	public void setLaserAsBoolean(boolean _value)
	{
		m_laser = getOnOffFromBoolean(_value);
	}

	/**
	 * Sets the Ka False Guard constant to on OR OFF.
	 * 
	 * @param _value	The state to set the KaFalseGuard constant to.
	 */
	public void setKaFalseGuardAsBoolean(boolean _value)
	{
		m_kaFalseGuard = getOnOffFromBoolean(_value);
	}

	/**
	 * Sets the Feature BGK Muting constant to on OR OFF.
	 * 
	 * @param _value	The state to set the  Feature BGK Muting constant to.
	 */
	public void setFeatureBGKMutingAsBoolean(boolean _value)
	{
		m_featureBGKMuting = getOnOffFromBoolean(_value);
	}

	/**
	 * Sets the K Initial Unmute 4 Lights constant to on OR OFF.
	 * 
	 * @param _value	The state to set the K Initial Unmute 4 Lights constant to.
	 */
	public void setKInitialUnmute4LightsAsBoolean(boolean _value)
	{
		m_KInitialUnmute4Lights = getOnOffFromBoolean(_value);
	}
	
	/**
	 * Sets the K Persistent Unmute 6 Lights constant to on OR OFF.
	 * 
	 * @param _value	The state to set the K Persistent Unmute 6 Lights constant to.
	 */
	public void setKPersistentUnmute6LightsAsBoolean(boolean _value)
	{
		m_KPersistentUnmute6Lights = getOnOffFromBoolean(_value);
	}
	
	/**
	 * Sets the K Rear Mute constant to on OR OFF.
	 * 
	 * @param _value	The state to set the K Rear Mute constant to.
	 */
	public void setKRearMuteAsBoolean(boolean _value)
	{
		m_KRearMute = getOnOffFromBoolean(_value);
	}

	/**
	 * Sets the Ku Band constant to on OR OFF.
	 * 
	 * @param _value	The state to set the Ku Band constant to.
	 */
	public void setKuBandAsBoolean(boolean _value)
	{
		m_KuBand = getOnOffFromBoolean(_value);
	}

	/**
	 * Sets the Pop Mute constant to on OR OFF.
	 * 
	 * @param _value	The state to set the Pop constant to.
	 */
	public void setPopAsBoolean(boolean _value)
	{
		m_Pop = getOnOffFromBoolean(_value);
	}

	/**
	 * Sets the Euro constant to on OR OFF.
	 * 
	 * @param _value	The state to set the Euro constant to.
	 */
	public void setEuroAsBoolean(boolean _value)
	{
		m_Euro = getOnOffFromBoolean(_value);
	}

	/**
	 * Sets the Euro X Band constant to on OR OFF.
	 * 
	 * @param _value	The state to set the Euro X Band constant to.
	 */
	public void setEuroXBandAsBoolean(boolean _value)
	{
		m_EuroXBand = getOnOffFromBoolean(_value);
	}
	
	/**
	 * Sets the Filter constant to on OR OFF.
	 * 
	 * @param _value	The state to set the Filter constant to.
	 */
	public void setFilterAsBoolean(boolean _value)
	{
		m_Filter = getOnOffFromBoolean(_value);
	}
	
	/**
	 * Sets the Force Legacy constant to on OR OFF.
	 * 
	 * @param _value	The state to set the Force Legacy constant to.
	 */
	public void setForceLegacyAsBoolean(boolean _value)
	{
		m_ForceLegacy = getOnOffFromBoolean(_value);
	}
	
	/**
	 * Gets the on/off state based on on the boolean value passed in.
	 * 
	 * @param _value	The boolean value of the constant to check the state of.
	 * 
	 * @return Constant.ON if the value is true otherwise Constant.OFF.
	 */
	private Constants getOnOffFromBoolean(boolean _value)
	{
		if (_value)
		{
			return Constants.ON;
		}
		else
		{
			return Constants.OFF;
		}
	}
	
	/**
	 * Sets the Bar graph constant to RESPONSIVE OR NORMAL, based on the passed in boolean value.
	 * 
	 * @param _value	Sets the bar graph to RESPONSIVE if true, otherwise NORMAL.
	 */
	public void setBargraphAsBoolean(boolean _value)
	{
		if (_value)
		{
			m_bargraph = Constants.RESPONSIVE;
		}
		else
		{
			m_bargraph = Constants.NORMAL;
		}
	}
	
	/**
	 * Sets the Mute Volume constant to LEVER OR ZERO, based on the passed in boolean value.
	 * 
	 * @param _value	Sets the Mute Volume to LEVER if true, otherwise ZERO.
	 */
	public void setMuteVolumeAsBoolean(boolean _value)
	{
		if (_value)
		{
			m_muteVolume = Constants.LEVER;
		}
		else
		{
			m_muteVolume = Constants.ZERO;
		}
	}
	
	/**
	 * Sets the Post Mute Bogey Lock Volume constant to LEVER OR ZERO, based on the passed in boolean value.
	 * 
	 * @param _value	Sets the Post Mute Bogey Lock Volume to LEVER if true, otherwise ZERO.
	 */
	public void setPostMuteBogeyLockVolumeAsBoolean(boolean _value)
	{
		if (_value)
		{
			m_postmuteBogeyLockVolume = Constants.KNOB;
		}
		else
		{
			m_postmuteBogeyLockVolume = Constants.LEVER;
		}
	}
	
	/**
	 * Sets up the user settings based off the data from the passed in byte array.
	 * 
	 * @param _data byte array containing data about the user settings.
	 */
	public void buildFromBytes(byte[] _data)
	{
		/*
		 PayloadBytes
		 0 User Byte 0
		 1 User Byte 1
		 2 User Byte 2
		 3 User Byte 3
		 4 User Byte 4
		 5 User Byte 5
		 
		 Feature 	Name						Values				Bytes 		Bit		Factory Default
		 1 			X band 						On/Off				User 0 		0 		On
		 2 			K band 						On/Off 				User 0 		1 		On
		 3 			Ka band 					On/Off 				User 0 		2 		On
		 4 			Laser 						On/Off 				User 0 		3 		On
		 5 			Bargraph 					Normal/Responsive 	User 0 		4 		Normal
		 6 			Ka False Guard 				On/Off 				User 0 		5 		On
		 7 			Feature bG(K Muting) 		On/Off 				User 0 		6 		Off
		 8 			Mute Volume 				Lever/Zero 			User 0 		7 		Lever
		 A 			Postmute Bogey Lock Volume 	Lever/Knob 			User 1 		0 		Knob
		 b 			K Mute Timer 									User 1 		1 		10 sec
		 C 			“ 												User 1 		2
		 d 			“ 												User 1 		3
		 E 			K Initial Unmute 4 lights 						User 1 		4 		On
		 F 			K Persistent Unmute 6 lights 					User 1 		5 		On
		 G 			K Rear Mute 				On/Off 				User 1 		6 		Off
		 H 			Ku band 					On/Off 				User 1 		7 		Off
		 J 			Pop 						On/Off 				User 2 		0 		On
		 u 			Euro 						On/Off 				User 2 		1 		Off
		 u bar 		Euro X band 				On/Off 				User 2 		2 		Off
		 t 			Filter 						On/Off 				User 2 		3 		Off
		 L 			Force Legacy CD 								User 2 		4 		Off
		*/
		
		byte userByte0 = _data[0];
		byte userByte1 = _data[1];
		byte userByte2 = _data[2];
		
		setXBand(getOnOffValue(getBitFromByte(userByte0, 0)));
		setKBand(getOnOffValue(getBitFromByte(userByte0, 1)));
		setKaBand(getOnOffValue(getBitFromByte(userByte0, 2)));
		setLaser(getOnOffValue(getBitFromByte(userByte0, 3)));
		
		setBargraph(getNormalResponsiveValue(getBitFromByte(userByte0, 4)));
		
		setKaFalseGuard(getOnOffValue(getBitFromByte(userByte0, 5)));
		setFeatureBGKMuting(getOnOffValueReverse(getBitFromByte(userByte0, 6)));
		
		setMuteVolume(getLeverZero(getBitFromByte(userByte0, 7)));
		
		setPostmuteBogeyLockVolume(getLeverKnobValue(getBitFromByte(userByte1, 0)));
		
		setKMuteTimer(convertTimeFromBytes(getBitFromByte(userByte1, 1), getBitFromByte(userByte1, 2), getBitFromByte(userByte1, 3)));
		
		setKInitialUnmute4Lights(getOnOffValue(getBitFromByte(userByte1, 4)));
		setKPersistentUnmute6Lights(getOnOffValue(getBitFromByte(userByte1, 5)));
		setKRearMute(getOnOffValueReverse(getBitFromByte(userByte1, 6)));
		setKuBand(getOnOffValueReverse(getBitFromByte(userByte1, 7)));
		
		setPop(getOnOffValue(getBitFromByte(userByte2, 0)));
		setEuro(getOnOffValueReverse(getBitFromByte(userByte2, 1)));
		setEuroXBand(getOnOffValueReverse(getBitFromByte(userByte2, 2)));
		setFilter(getOnOffValueReverse(getBitFromByte(userByte2, 3)));
		setForceLegacy(getOnOffValueReverse(getBitFromByte(userByte2, 4)));
	}
	
	/**
	 * Retrieves a specified bit from the supplied byte.
	 *  
	 * @param _byte			The byte to get the bit located at #_whichBit.
	 * @param _whichBit		The location of the desired bit.
	 * 
	 * @return				byte containing the desired bit.
	 */
	private byte getBitFromByte(byte _byte, int _whichBit)
	{
		byte mask;
		
		switch (_whichBit)
		{
		case 0:
			mask = 1;
			break;
		case 1:
			mask = 2;
			break;
		case 2:
			mask = 4;
			break;
		case 3:
			mask = 8;
			break;
		case 4:
			mask = 16;
			break;
		case 5:
			mask = 32;
			break;
		case 6:
			mask = 64;
			break;
		case 7:
			mask = (byte) 128;
			break;
		default:
			mask = 0;
		}
		
		byte newValue = (byte) (_byte & mask);
		
		newValue = (byte) (newValue >>> _whichBit);
		
		return newValue;
	}
	
	/**
	 * Gets a Constants ON/OFF value for the passed in byte.
	 * 
	 * param _value		The byte to check the constants on/Off value.	
	 * @return			Constant.ON if the byte is not equal to '0', otherwise Constant.OFF.
	 */
	private UserSettings.Constants getOnOffValue(byte _value)
	{
		UserSettings.Constants rc;
		if (_value == 0)
		{
			rc =  UserSettings.Constants.OFF;
		}
		else
		{
			rc = UserSettings.Constants.ON;
		}
		
		return rc;
	}
	
	/**
	 * Does exact same logic as getOnOffValue  in reverse.
	 *  
	 * @param _value	See {@link #getOnOffValue(byte)}
	 * @return			See {@link #getOnOffValue(byte)}
	 */
	private UserSettings.Constants getOnOffValueReverse(byte _value)
	{
		UserSettings.Constants rc;
		if (_value == 0)
		{
			rc =  UserSettings.Constants.ON;
		}
		else
		{
			rc = UserSettings.Constants.OFF;
		}
		
		return rc;
	}
	
	/**
	 * Gets a Constants responsive value from the passed in byte.
	 * 
	 * @param _value		The byte to check the constants responsive value.	
	 * @return			Constant.RESPONSIVE if the byte is not equal to '0', otherwise Constant.NORMAL .
	 */
	private UserSettings.Constants getNormalResponsiveValue(byte _value)
	{
		UserSettings.Constants rc;
		if (_value == 0)
		{
			rc =  UserSettings.Constants.RESPONSIVE;
		}
		else
		{
			rc = UserSettings.Constants.NORMAL;
		}
		
		return rc;
	}
	
	/**
	 * Gets a Constants LEVER/KNOB state from the passed in byte.
	 * 
	 * @param _value		The byte to check the constants LEVER/KNOB state.	
	 * @return			Constant.LEVER if the byte is not equal to '0', otherwise Constant.KNOB .
	 */
	private UserSettings.Constants getLeverKnobValue(byte _value)
	{
		UserSettings.Constants rc;
		if (_value == 0)
		{
			rc =  UserSettings.Constants.LEVER;
		}
		else
		{
			rc = UserSettings.Constants.KNOB;
		}
		
		return rc;
	}
	
	/**
	 * Gets a Constants ZERO/LEVER state from the passed in byte.
	 * 
	 * @param _value		The byte to check the constants ZERO/LEVER state.	
	 * @return			Constant.ZERO if the byte is not equal to '0', otherwise Constant.LEVER .
	 */
	private UserSettings.Constants getLeverZero(byte _value)
	{
		UserSettings.Constants rc;
		if (_value == 0)
		{
			rc =  UserSettings.Constants.ZERO;
		}
		else
		{
			rc = UserSettings.Constants.LEVER;
		}
		
		return rc;
	}
	
	/**
	 * Returns the user setting as a byte array.
	 * 
	 * @return byte array reflecting the user settings.
	 */
	public byte[] buildBytes()
	{
		/*
		 PayloadBytes
		 0 User Byte 0
		 1 User Byte 1
		 2 User Byte 2
		 3 User Byte 3
		 4 User Byte 4
		 5 User Byte 5
		 
		 Feature 	Name						Values				Bytes 		Bit		Factory Default
		 1 			X band 						On/Off				User 0 		0 		On
		 2 			K band 						On/Off 				User 0 		1 		On
		 3 			Ka band 					On/Off 				User 0 		2 		On
		 4 			Laser 						On/Off 				User 0 		3 		On
		 
		 5 			Bargraph 					Normal/Responsive 	User 0 		4 		Normal
		 
		 6 			Ka False Guard 				On/Off 				User 0 		5 		On
		 7 			Feature bG(K Muting) 		On/Off 				User 0 		6 		Off
		 8 			Mute Volume 				Lever/Zero 			User 0 		7 		Lever
		 A 			Postmute Bogey Lock Volume 	Lever/Knob 			User 1 		0 		Knob
		 b 			K Mute Timer 									User 1 		1 		10 sec
		 C 			“ 												User 1 		2
		 d 			“ 												User 1 		3
		 E 			K Initial Unmute 4 lights 						User 1 		4 		On
		 F 			K Persistent Unmute 6 lights 					User 1 		5 		On
		 G 			K Rear Mute 				On/Off 				User 1 		6 		Off
		 H 			Ku band 					On/Off 				User 1 		7 		Off
		 J 			Pop 						On/Off 				User 2 		0 		On
		 u 			Euro 						On/Off 				User 2 		1 		Off
		 u bar 		Euro X band 				On/Off 				User 2 		2 		Off
		 t 			Filter 						On/Off 				User 2 		3 		Off
		 L 			Force Legacy CD 								User 2 		4 		Off
		*/
	
		byte[] rc = new byte[6];
		rc[0] = setBit(rc[0], 0, getOnOffValue(getXBand()));
		rc[0] = setBit(rc[0], 1, getOnOffValue(getKBand()));
		rc[0] = setBit(rc[0], 2, getOnOffValue(getKaBand()));
		rc[0] = setBit(rc[0], 3, getOnOffValue(getLaser()));
		rc[0] = setBit(rc[0], 4, getNormalResponsiveValue(getBargraph()));
		rc[0] = setBit(rc[0], 5, getOnOffValue(getKaFalseGuard()));
		rc[0] = setBit(rc[0], 6, getOnOffValueReverse(getFeatureBGKMuting()));
		rc[0] = setBit(rc[0], 7, getLeverZeroBoolean(getMuteVolume()));
		
		
		rc[1] = setBit(rc[1], 0, getLeverKnobValueBoolean(getPostmuteBogeyLockVolume()));
		
		rc[1] = setTimeoutBitsBits(rc[1]);
		
		rc[1] = setBit(rc[1], 4, getOnOffValue(getKInitialUnmute4Lights()));
		rc[1] = setBit(rc[1], 5, getOnOffValue(getKPersistentUnmute6Lights()));
		rc[1] = setBit(rc[1], 6, getOnOffValueReverse(getKRearMute()));
		rc[1] = setBit(rc[1], 7, getOnOffValueReverse(getKuBand()));
		
		rc[2] = setBit(rc[2], 0, getOnOffValue(getPop()));
		rc[2] = setBit(rc[2], 1, getOnOffValueReverse(getEuro()));
		rc[2] = setBit(rc[2], 2, getOnOffValueReverse(getEuroXBand()));
		rc[2] = setBit(rc[2], 3, getOnOffValueReverse(getFilter()));
		rc[2] = setBit(rc[2], 4, getOnOffValueReverse(getForceLegacy()));
		
		//setting unused bits
		rc[2] = setBit(rc[2], 5, true);
		rc[2] = setBit(rc[2], 6, true);
		rc[2] = setBit(rc[2], 7, true);
		
		rc[3] = setBit(rc[3], 0, true);
		rc[3] = setBit(rc[3], 1, true);
		rc[3] = setBit(rc[3], 2, true);
		rc[3] = setBit(rc[3], 3, true);
		rc[3] = setBit(rc[3], 4, true);
		rc[3] = setBit(rc[3], 5, true);
		rc[3] = setBit(rc[3], 6, true);
		rc[3] = setBit(rc[3], 7, true);
		
		rc[4] = setBit(rc[4], 0, true);
		rc[4] = setBit(rc[4], 1, true);
		rc[4] = setBit(rc[4], 2, true);
		rc[4] = setBit(rc[4], 3, true);
		rc[4] = setBit(rc[4], 4, true);
		rc[4] = setBit(rc[4], 5, true);
		rc[4] = setBit(rc[4], 6, true);
		rc[4] = setBit(rc[4], 7, true);
		
		rc[5] = setBit(rc[5], 0, true);
		rc[5] = setBit(rc[5], 1, true);
		rc[5] = setBit(rc[5], 2, true);
		rc[5] = setBit(rc[5], 3, true);
		rc[5] = setBit(rc[5], 4, true);
		rc[5] = setBit(rc[5], 5, true);
		rc[5] = setBit(rc[5], 6, true);
		rc[5] = setBit(rc[5], 7, true);
		
		return rc;
	}
	
	/**
	 * Set the specified bit in the given byte.
	 * 
	 * @param _byte			The byte to set the bit in.
	 * 
	 * @param _whichBit		The location of the bit to set.
	 * @param value			The value of the bit to set.
	 * 
	 * @return				The modified byte.
	 */
	private byte setBit(byte _byte, int _whichBit, boolean value)
	{
		byte mask;
		
		if (value)
		{
			switch (_whichBit)
			{
			case 0:
				mask = 1;
				break;
			case 1:
				mask = 2;
				break;
			case 2:
				mask = 4;
				break;
			case 3:
				mask = 8;
				break;
			case 4:
				mask = 0x10;
				break;
			case 5:
				mask = 0x20;
				break;
			case 6:
				mask = 0x40;
				break;
			case 7:
				mask = (byte) 0x80;
				break;
			default:
				mask = 0;
			}
			
			_byte = (byte) (_byte | mask);
		
			return _byte;
		}
		
		return _byte;
	}
	
	/**
	 * Gets a constants ON/Off state.
	 * 
	 * @param _value	The constant to get the ON/OFF state.
	 * 
	 * @return True if the constant is ON, otherwise false.
	 */
	private boolean getOnOffValue(UserSettings.Constants _value)
	{
		boolean rc;
		if (_value == UserSettings.Constants.OFF)
		{
			rc =  false;
		}
		else
		{
			rc = true;
		}
		
		return rc;
	}
	
	/**
	 * Does the exact same logic as {@link #getOnOffValue(Constants)} but in reverse.
	 * 
	 * @param _value	See {@link #getOnOffValue(Constants)}.
	 * @return			{@link #getOnOffValue(Constants)}.
	 */
	private boolean getOnOffValueReverse(UserSettings.Constants _value)
	{
		boolean rc;
		if (_value == UserSettings.Constants.ON)
		{
			rc =  false;
		}
		else
		{
			rc = true;
		}
		
		return rc;
	}
	
	/**
	 * Gets the constants responsive value.
	 * 
	 * @param _value The constant to get the responsive value from.
	 * 
	 * @return True if the constant is NORMAL, otherwise false. 
	 */
	private boolean getNormalResponsiveValue(UserSettings.Constants _value)
	{
		boolean rc;
		if (_value == UserSettings.Constants.NORMAL)
		{
			rc = true;
		}
		else
		{
			rc = false;
		}
		
		return rc;
	}
	
	/**
	 * Gets the constants responsive value.
	 * 
	 * @param _value The constant to get the Lever/Knob value from.
	 * 
	 * @return True if the constant is LEVER, otherwise false. 
	 */
	private boolean getLeverKnobValueBoolean(UserSettings.Constants _value)
	{
		boolean rc;
		if (_value == UserSettings.Constants.LEVER)
		{
			rc = false;
		}
		else
		{
			rc = true;
		}
		
		return rc;
	}
	
	/**
	 * Gets the constants responsive value.
	 * 
	 * @param _value The constant to get the Lever/Zero value from.
	 * 
	 * @return True if the constant is LEVER, otherwise false. 
	 */
	private boolean getLeverZeroBoolean(UserSettings.Constants _value)
	{
		boolean rc;
		if (_value == UserSettings.Constants.LEVER)
		{
			rc =  true;
		}
		else
		{
			rc = false;
		}
		
		return rc;
	}

	/**
	 * Converts the time from byte.
	 * @param _one
	 * @param _two
	 * @param _three
	 * 
	 * @return	String containing the converted time.
	 */
	private String convertTimeFromBytes(byte _one, byte _two, byte _three)
	{
		String rc = "10";
		
		int setting = getSetting(_one, _two, _three);
		
		rc = getTimeForSettingDefault(setting).toString();
		
		return rc;
	}
	
	/**
	 * Gets the time from the user setting.
	 * @param _setting	The setting to extract the time from.
	 * 
	 * @return Interger containing the time.
	 */
	private Integer getTimeForSettingDefault(int _setting)
	{
		switch (_setting)
		{
		case 1:
			return 10;
		case 2:
			return 30;
		case 3:
			return 20;
		case 4:
			return 15;
		case 5:
			return 7;
		case 6:
			return 5;
		case 7:
			return 4;
		case 8:
			return 3;
		}
		
		return 0;
	}
	
	/**
	 * Gets the setting from the three passed in bytes.
	 * 
	 * @param _one
	 * @param _two
	 * @param _three
	 * @return			Int containing the user time settings.
	 */
	private int getSetting(byte _one, byte _two, byte _three)
	{
		boolean one = false;
		boolean two = false;
		boolean three = false;
		
		if (_one > 0)
		{
			one = true;
		}
		
		if (_two > 0)
		{
			two = true;
		}
		
		if (_three > 0)
		{
			three = true;
		}
		
		if (one & two & three)
		{
			return 1;
		}
		else if (!one & two & three)
		{
			return 2;
		}
		else if (one & !two & three)
		{
			return 3;
		}
		else if (!one & !two & three)
		{
			return 4;
		}
		else if (one & two & !three)
		{
			return 5;
		}
		else if (!one & two & !three)
		{
			return 6;
		}
		else if (one & !two & !three)
		{
			return 7;
		}
		else //if (!one & !two & !three)
		{
			return 8;
		}
	}
	
	/**
	 * Gets a list containing allow timeout values.
	 * 
	 * @return List containing allowable timeouts.
	 */
	public static ArrayList<String> getAllowedTimeoutValues()
	{
		ArrayList<String> rc = new ArrayList<String>();		
		
		rc.add("10");
		rc.add("30");
		rc.add("20");
		rc.add("15");
		rc.add("7");
		rc.add("5");
		rc.add("4");
		rc.add("3");
		
		return rc;
	}
	
	/**
	 * Sets the the timeout bits.
	 * 
	 * @param _byte	The byte to set the timeout with.
	 * 
	 * @return	The modified timeout byte.
	 */
	private byte setTimeoutBitsBits(byte _byte)
	{
		int which = 0;
		
		which = getSettingForTimeForDefault(getKMuteTimer());
		
		switch (which)
		{
		case 1:
			_byte = (byte) (_byte | 0x0E);
			break;
		case 2:
			_byte = (byte) (_byte | 0x0C);
			break;
		case 3:
			_byte = (byte) (_byte | 0x0A);
			break;
		case 4:
			_byte = (byte) (_byte | 0x08);
			break;
		case 5:
			_byte = (byte) (_byte | 0x06);
			break;
		case 6:
			_byte = (byte) (_byte | 0x04);
			break;
		case 7:
			_byte = (byte) (_byte | 0x02);
			break;
		case 8:
			_byte = (byte) (_byte | 0x00);
		}
		
		return _byte;
	}
	
	/**
	 * Gets the setting for the default timeout.
	 * 
	 * @param _time	The time to get the default timeout for.
	 * 
	 * @return	Integer containing default time out.
	 */
	private Integer getSettingForTimeForDefault(String _time)
	{
		int timeInt = Integer.parseInt(_time);
		switch (timeInt)
		{
		case 10:
			return 1;
		case 30:
			return 2;
		case 20:
			return 3;
		case 15:
			return 4;
		case 7:
			return 5;
		case 5:
			return 6;
		case 4:
			return 7;
		case 3:
			return 8;
		}
		
		return 0;
	}
	
	/**
	 * Creates an UserSettings with factory defaults settings.
	 * 
	 * @return	UserSettings containing factory default settings.
	 */
	public static UserSettings GenerateFactoryDefaults()
	{
		UserSettings rc = new UserSettings();
		
		byte[] data = { (byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff,(byte) 0xff};
		
		rc.buildFromBytes(data);
		
		if ( V1VersionSettingLookup.defaultTMFState() ){
			// The 0xFF value set above will turn off the TMF feature, so we only need to take action
			// if the TMF feature should be turned on by default
			rc.setFilter(Constants.ON);
		}
		
		return rc;
	}
}
