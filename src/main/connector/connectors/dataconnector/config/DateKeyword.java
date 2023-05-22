/*
 * Copyright 2015 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.connectors.dataconnector.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The supported keywords for date based queries.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public enum DateKeyword
{
  /**
   * Yesterday.
   */
  Yesterday("Yesterday"),
  /**
   * Yesterday minus 1 day.
   */
  Yesterday_minus_one("Yesterday-1"),
  /**
   * Yesterday ninus 2 days.
   */
  Yesterday_minus_two("Yesterday-2"),
  /**
   * Today.
   */
  Today("Today"),
  /**
   * Now.
   */
  Now("Now"),
  /**
   * Now minus 1 hour.
   */
  Now_minus_one("Now-1h");

  private static final List<String> VALUES = new ArrayList<>(Arrays.asList(Yesterday.theValue,
          Yesterday_minus_one.theValue, Yesterday_minus_two.theValue, Today.theValue,
          Now.theValue, Now_minus_one.theValue));

  private String theValue;

  DateKeyword(String myValue)
  {
    theValue = myValue;
  }

  /**
   * Determines whether the keyword is a valid value.
   *
   * @param aKeywordvalue the keyword
   * @return boolean
   */
  public static boolean isValid(String aKeywordvalue)
  {
    boolean myIsValid = false;
    for (DateKeyword myValue : values())
    {
      if (myValue.theValue.equalsIgnoreCase(aKeywordvalue))
      {
        myIsValid = true;
      }
    }
    return myIsValid;
  }

  /**
   * Get the list of supported terms.
   *
   * @return List of Strings
   */
  public static List<String> getValues()
  {
    return VALUES;
  }

  public String getValue()
  {
    return theValue;
  }
}
