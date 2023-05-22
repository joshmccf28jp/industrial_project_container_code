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

import java.io.File;

/**
 * Encapsulates the results of a Tag List Query execution.
 *
 * @author EnergySys Limited
 * @version $Revision$
 * Last modified by: $Author$
 */
public class TagListQueryResult 
{
  private File theResultsFile;
  private Notification theNotification;
  
  /**
   * Constructor.
   * @param aResultsFile the results file.
   * @param aNotification the notification.
   */
  public TagListQueryResult(File aResultsFile, Notification aNotification)
  {
    theResultsFile = aResultsFile;
    theNotification = aNotification;
  }

  /**
   * getResultsFile.
   * @return the results file.
   */
  public File getResultsFile() 
  {
    return theResultsFile;
  }

  /**
   * setResultsFile.
   * @param aResultsFile  the results file.
   */
  public void setResultsFile(File aResultsFile) 
  {
    this.theResultsFile = aResultsFile;
  }



  /**
   * getNotification.
   * @return the notification.
   */
  public Notification getNotification() 
  {
    return theNotification;
  }

  /**
   * setNotification.
   * @param aNotification the notification.
   */
  public void setNotification(Notification aNotification) 
  {
    this.theNotification = aNotification;
  }
  
}
