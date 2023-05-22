/*
 * Copyright 2018 EnergySys Limited. All Rights Reserved.
 * 
 * This software is the proprietary information of EnergySys Limited.
 * Use is subject to licence terms.
 * This software is not designed or supplied to be used in circumstances where
 * personal injury, death or loss of or damage to property could result from any
 * defect in the software.
 * In no event shall the developers or owners be liable for personal injury,
 * death or loss or damage to property, loss of business, revenue, profits, use,
 * data or other economic advantage or for any indirect, punitive, special,
 * incidental, consequential or exemplary loss or damage resulting from the use
 * of the software or documentation.
 * Developer and owner make no warranties, representations or undertakings of
 * any nature in relation to the software and documentation.
 */
package com.energysys.connector.util;

import com.energysys.connector.config.ConnectorConfig;
import com.energysys.connector.exception.ConnectorException;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * App scoped bean that supplies system time zone info for screens.
 * Also supplies static methods allowing default time zone to be set at jetty start up.
 * @author EnergySys Limited
 */
@ManagedBean(name = "TimeZoneUtil")
@ApplicationScoped
public class TimeZoneUtil
{

  private static final Logger LOG = Logger.getLogger(TimeZoneUtil.class.getName());

  /**
   * Default constructor.
   */
  public TimeZoneUtil()
  {

  }

  /**
   * Sets the default time zone to a given timezone.
   * @param aTimeZone the timezone id
   */
  public static void setDefaultTimeZone(TimeZone aTimeZone)
  {
    TimeZone.setDefault(aTimeZone);
    LOG.info("TimeZone set to: " + TimeZone.getDefault().toString());
  }

  /**
   * Can be called to load the default timezone from the standard system property.
   * This allows the system property to be set during jetty startup instead of as a JVM option.
   */
  public static void loadTimeZoneFromCredentials() throws ConnectorException
  {
    setDefaultTimeZone(ConnectorConfig.loadFromKeystore().getConnectorTimezone());
  }

  /**
   * Gets the short name of the default timezone.
   * @return short name
   */
  public String getTimeZoneShort()
  {
    return ZoneId.systemDefault().getDisplayName(TextStyle.SHORT, Locale.getDefault());
  }

  /**
   * Gets the long name of the default timezone.
   * @return long name
   */
  public String getTimeZoneFull()
  {
    return ZoneId.systemDefault().getDisplayName(TextStyle.FULL, Locale.getDefault());
  }
}
