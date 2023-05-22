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

import com.energysys.connector.config.ConnectorConfig;
import com.energysys.connector.connectors.dataconnector.odata.TagList;
import com.energysys.connector.connectors.dataconnector.config.TagListQueryConfiguration;
import com.energysys.connector.exception.ConnectorException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * IAdaptorInterface.
 *
 * @author EnergySys Limited
 * @version $Revision$
 * Last modified by: $Author$
 */
public interface IAdaptorInterface 
{

  /**
   * Execute the query.
   * @param aTagListQuery the query
   * @param aTagList the tag list
   * @param aConnectorConfig the connector config
   * @param aTimezone
   * @param aTargetRunTime
   * @return TagListQueryResult the result
   * @throws ConnectorException on error
   */
  TagListQueryResult execute(TagListQueryConfiguration aTagListQuery, TagList aTagList,
                             ConnectorConfig aConnectorConfig,
                             TimeZone aTimezone, Date aTargetRunTime)
          throws ConnectorException;

  /**
   * Gets the query types the adaptor supports.
   * @return query types
   * @throws UnsupportedOperationException
   */
  List<String> getQueryTypes() throws UnsupportedOperationException;

  /**
   * Gets the summary types the adaptor supports.
   * @return the summary types
   * @throws UnsupportedOperationException
   */
  List<String> getSummaryTypes() throws UnsupportedOperationException;

  /**
   * Gets the Adaptor name.
   * @return the name
   * @throws UnsupportedOperationException
   */
  String getAdapterName() throws UnsupportedOperationException;

  /**
   * Retuend whether this Adaptor supports re-runs.
   * @return if supported
   */
  Boolean supportsReRuns();
}
