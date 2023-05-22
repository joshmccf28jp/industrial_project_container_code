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
package com.energysys.connector.connectors.dataconnector.adaptor;

/**
 * Encapsulate notifications.
 *
 * @author EnergySys Limited
 * @version $Revision$
 * Last modified by: $Author$
 */
public class Notification 
{
  private String theWarningMessage;
  private String theRetrievedDataMessage;
  
  /**
   * Constructor.
   */
  public Notification()
  {
    
  }
  
  /**
   * Constructor.
   * @param aWarningMessage warning message
   * @param aRetrievedDataMessage retrieved data notification
   */
  public Notification(String aWarningMessage, String aRetrievedDataMessage)
  {
    theWarningMessage = aWarningMessage;
    theRetrievedDataMessage = aRetrievedDataMessage;
  }

  /**
   * getWarningMessage.
   * @return warning
   */
  public String getWarningMessage() 
  {
    return theWarningMessage;
  }

  /**
   * setWarningMessage.
   * @param aWarningMessage message
   */
  public void setWarningMessage(String aWarningMessage) 
  {
    this.theWarningMessage = aWarningMessage;
  }
  
  /**
   * setRetrievedDataMessage.
   * @param aRetrievedDataMessage message
   */
  public void setRetrievedDataMessage(String aRetrievedDataMessage) 
  {
    this.theRetrievedDataMessage = aRetrievedDataMessage;
  }


  /**
   * getRetrievedDataMessage for display.
   * @return message
   */
  public String getRetrievedDataMessage() 
  {
    return theRetrievedDataMessage;
  }
  
 /**
   * hasWarningMessage.
   * @return true if message for display.
   */
  public boolean hasWarningMessage()
  {
    return (theWarningMessage != null && !theWarningMessage.isEmpty());
  }
  
  /**
   * hasRetrievedDataMessage.
   * @return true if message for display.
   */
  public boolean hasRetrievedDataMessage()
  {
    return (theRetrievedDataMessage != null && !theRetrievedDataMessage.isEmpty());
  }
}
