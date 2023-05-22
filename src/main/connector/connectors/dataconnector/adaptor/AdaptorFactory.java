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

import com.energysys.connector.exception.ConnectorException;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Adaptor Factory.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public final class AdaptorFactory
{

  private static final Logger LOG = Log.getLogger(AdaptorFactory.class.getName());
  private static final String ADAPTOR_CLASS_NAME = "com.energysys.execution.Adaptor";

  /**
   * Constructor.
   */
  private AdaptorFactory()
  {

  }

  /**
   * Returns an implementation which matches the class name 'com.energysys.execution.Adaptor'.
   *
   * @return IAdaptorInterface implementation
   * @throws ExecutionException on error
   */
  public static IAdaptorInterface getAdaptor() throws ConnectorException
  {
    IAdaptorInterface myAdaptor = null;
    Class myClass;

    try
    {
      myClass = Class.forName(ADAPTOR_CLASS_NAME);
      myAdaptor = (IAdaptorInterface) myClass.newInstance();
    }
    catch (ClassNotFoundException | InstantiationException | IllegalAccessException myEx)
    {
      String myMessage = "The Adaptor could not be created, please check the configuration.";
      throw new ConnectorException(myMessage, myEx);
    }

    return myAdaptor;
  }

}
