/*
 * Copyright 2017 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.exception;

import java.util.logging.Logger;

/**
 * System exception thrown by file connector.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class ConnectorSystemException extends Error
{

  private static final Logger LOG = Logger.getLogger(ConnectorSystemException.class.getName());

  /**
   * Basic contructor for creating exception.
   *
   * @param aMessage a message
   */
  public ConnectorSystemException(String aMessage)
  {
    super(aMessage);
    LOG.severe(aMessage);
  }

  /**
   * Basic constructor for wrapping an exception.
   *
   * @param aMessage a message
   * @param anEx the wrapped exception
   */
  public ConnectorSystemException(String aMessage, Exception anEx)
  {
    super(aMessage, anEx);
    LOG.severe(aMessage);
  }
}
