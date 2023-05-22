/*
 * Copyright 2008 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.web.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Report a version identity for ENERGYSYS and it's hosted applications.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public final class Version
{
  private static final String VERSION = "version";
  private static final String BUILD_DATE = "build.date";
  private static final Properties PROPERTIES = new Properties();

  private Version()
  {
  }

  private static Properties getProperties()
  {
    if (PROPERTIES.isEmpty())
    {
      try
      {
        try (InputStream stream
          = Version.class.getClassLoader().getResourceAsStream("Version.properties"))
        {
          if (stream == null)
          {
            setDefaults();
          }
          else
          {
            PROPERTIES.load(stream);
          }
        }
      }
      catch (IOException myEx)
      {
        setDefaults();
      }
    }
    return PROPERTIES;
  }

  private static void setDefaults()
  {
    PROPERTIES.setProperty(VERSION, "ESYS 00_00_00_00");
    PROPERTIES.setProperty(BUILD_DATE, "1970-01-01");    
  }
  
  /**
   * Get Gamma build name - the software version.
   *
   * @return String the software version
   */
  public static String getGammaBuildName()
  {
    String myVersion = getProperties().getProperty(VERSION);
    // Check build number if of form 2_0_1_3
    // if so remove the final _3
    if (myVersion.matches(".*\\d+_\\d+_\\d+_\\d+$"))
    {
      return (myVersion.substring(0, myVersion.lastIndexOf('_'))
        + " build " + myVersion.substring(myVersion.lastIndexOf('_') + 1)).replaceAll("_", ".");
    }
    return myVersion.replaceAll("_", ".");
  }

  /**
   * Get the version description.
   *
   * @return The build version details.
   *
   * currently the version as hard coded.
   */
  public static String getVersionDescription()
  {
    return "Release " + getProperties().getProperty(VERSION) + " built on " + getProperties().getProperty(BUILD_DATE);
  }
  
  /**
   * Main method for testing only.
   * @param someArgs arguments.
   */
  public static void main(String[] someArgs)
  {
    System.out.println(getVersionDescription());
    System.out.println(getGammaBuildName());
  }  
}
