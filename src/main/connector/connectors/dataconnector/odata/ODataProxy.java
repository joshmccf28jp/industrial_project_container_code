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
package com.energysys.connector.connectors.dataconnector.odata;

import com.energysys.connector.config.ConnectorConfig;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.schedulers.quartz.RemoteQueryExecution;
import com.energysys.connector.util.odata.client.ODataConsumerHelper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;

/**
 * Proxy calls to EnergySys OData feed.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public class ODataProxy
{

  private static final Logger LOG = Logger.getLogger(ODataProxy.class.getName());
  private static final String NO_ID = null;
  private static final String NO_PATH = null;
  private static final String NO_QUERY = null;
  private static final String APPLICATION_ATOM_XML = "application/atom+xml";
  private static final String METER = "METER";
  private static final String NAME = "NAME";
  private static final String ALIAS = "ALIAS";
  private static final String ES_ASSET = "ES_ASSET";

  /**
   * Constructor.
   */
  public ODataProxy()
  {

  }

  /**
   * Gets the asset timezone from the Configuration. (Unless overriden in config).
   * @param aConnectorConfig
   * @param aConfigItem
   * @return the timezone
   * @throws ConnectorException
   */
  public TimeZone getAssetTimezone(ConnectorConfig aConnectorConfig,
                                   EsysOdataConnectionCredentials aConfigItem) throws ConnectorException
  {
    if (aConfigItem.getAssetTimeZoneOverride() != null)
    {
      LOG.info("TimeZone ID override set in OData credentials: " + aConfigItem.getAssetTimeZoneOverride());
      return TimeZone.getTimeZone(aConfigItem.getAssetTimeZoneOverride());
    }
    else
    {
      // Attempt to get TimeZone from ES_ASSET
      TimeZone myTimeZone = getTimeZoneFromESAsset(aConnectorConfig, aConfigItem);

      // Otherwise default to UTC
      if (myTimeZone == null)
      {
        LOG.info("TimeZone ID defaulting to UTC");
        myTimeZone = TimeZone.getTimeZone("UTC");
      }
      return myTimeZone;
    }
  }

  /**
   * Retrieve the tag lists from the OData service.
   *
   * @param aConnectorConfig
   * @param aConfigItem the connection config.
   * @return Map of Name/TagLists
   * @throws ConnectorException on error
   */
  public Map<String, TagList> getTagLists(ConnectorConfig aConnectorConfig,
                                          EsysOdataConnectionCredentials aConfigItem) throws ConnectorException
  {
    Map<String, TagList> myTagListMap = new HashMap<>();

    ODataConsumerHelper myODataConsumerHelper = new ODataConsumerHelper(aConnectorConfig);
    // get initial query for paginated records
    String myQuery = getTopQuery(aConfigItem);
    // retrieve feed and process entries
    ODataFeed myTagListFeed = myODataConsumerHelper.readFeed(aConfigItem.getServiceURL(),
            aConfigItem.getTagListObjectName(), NO_ID, NO_PATH, myQuery, APPLICATION_ATOM_XML);
    processTagLists(myTagListMap, myTagListFeed);
    // retrieve feeds for following pages and process entries
    while (myTagListFeed.getFeedMetadata().getNextLink() != null)
    {
      myQuery = extractNextLinkQuery(myTagListFeed.getFeedMetadata().getNextLink());
      myTagListFeed = myODataConsumerHelper.readFeed(aConfigItem.getServiceURL(),
          aConfigItem.getTagListObjectName(), NO_ID, NO_PATH, myQuery, APPLICATION_ATOM_XML);
      processTagLists(myTagListMap, myTagListFeed);
    }

    return myTagListMap;
  }

  private TimeZone getTimeZoneFromESAsset(ConnectorConfig aConnectorConfig,
                                          EsysOdataConnectionCredentials aConfigItem)
  {
    try
    {
      if (aConfigItem.getServiceURL() == null)
      {
        LOG.info("TimeZone ID not be retrieved from EnergySys. "
                + "esys-odata-cred configuration file missing service_url.");
        return null;
      }

      String myTimeZoneID = null;
      ODataConsumerHelper myODataConsumerHelper = new ODataConsumerHelper(aConnectorConfig);
      String myQuery = getTopQuery(aConfigItem);

      ODataEntry myAssetEntry = myODataConsumerHelper.readFeed(aConfigItem.getServiceURL(),
          ES_ASSET, NO_ID, NO_PATH,
          myQuery, APPLICATION_ATOM_XML).getEntries().get(0);

      if (myAssetEntry.getProperties().containsKey("TIMEZONE"))
      {
        myTimeZoneID = (String) myAssetEntry.getProperties().get("TIMEZONE");
        myTimeZoneID = myTimeZoneID.replace('|', '/');
        LOG.info("TimeZone ID retrieved from ES_ASSET: " + myTimeZoneID);
        return TimeZone.getTimeZone(myTimeZoneID);
      }
      else
      {
        LOG.info("No TIMEZONE entry found in ES_ASSET");
        return null;
      }
    }
    catch (ConnectorException e)
    {
      LOG.info("Could not retrieve TimeZone ID from ES_ASSET: " + e.getMessage());
      return null;
    }
  }


  private String extractNextLinkQuery(String aNextLink)
  {
    return aNextLink.substring(aNextLink.indexOf("?") + 1);
  }

  private void processTagLists(Map<String, TagList> aTagListMap, ODataFeed aTagListFeed)
  {
    List<ODataEntry> myEntries = aTagListFeed.getEntries();
    for (ODataEntry myEntry : myEntries)
    {
      addTagEntry(aTagListMap, (String) myEntry.getProperties().get(NAME), (String) myEntry.getProperties().get(METER),
          (String) myEntry.getProperties().get(ALIAS));
    }
  }

  private void addTagEntry(Map<String, TagList> aTagListMap, String aTagListName, String aTagName, String aTagAlias)
  {
    if (aTagListMap.containsKey(aTagListName))
    {
      TagList myTagList = aTagListMap.get(aTagListName);
      myTagList.put(aTagName);
      myTagList.setTagId(aTagName, aTagAlias);
    }
    else
    {
      TagList myTagList = new TagList(aTagListName);
      myTagList.put(aTagName);
      aTagListMap.put(aTagListName, myTagList);
      myTagList.setTagId(aTagName, aTagAlias);
    }
  }

  /**
   * Gets the list of remote query executions from EnergySys via OData.
   * @param aConnectorConfig
   * @param myODataCredentials
   * @param anAssetTimezone
   * @return the list of remote query executions
   * @throws ConnectorException
   */
  public List<RemoteQueryExecution> 
    getRemoteQueryExecutions(ConnectorConfig aConnectorConfig,
                             EsysOdataConnectionCredentials myODataCredentials, TimeZone anAssetTimezone)
      throws ConnectorException
  {
    List<RemoteQueryExecution> myRemoteQueryExecutions = new ArrayList<>();

    ZoneId myTimezone = anAssetTimezone.toZoneId();
    
    LOG.info("Using Timezone:" + myTimezone);

    // retrieve feed and process entries
    ODataConsumerHelper myODataConsumerHelper = new ODataConsumerHelper(aConnectorConfig);
    LOG.info("Calling OData:" + myODataCredentials.getServiceURL() + ":" 
        + myODataCredentials.getRemoteQueryExecutionObjectName());
    ODataFeed myRemoteQueryExecutionsFeed = myODataConsumerHelper.readFeed(
        myODataCredentials.getServiceURL(),
        myODataCredentials.getRemoteQueryExecutionObjectName(), 
        NO_ID, 
        NO_PATH, 
        "$filter=CONNECTOR_NAME eq '" 
            + myODataCredentials.getRemoteExecutionConnectorName() 
            + "' and (STATUS eq 'Queued' or STATUS eq null)", 
        APPLICATION_ATOM_XML);
    LOG.info(myRemoteQueryExecutionsFeed.toString());
    processRemoteQueryExecutions(
        myRemoteQueryExecutions, myRemoteQueryExecutionsFeed, myTimezone, myODataCredentials);

    // retrieve feeds for following pages and process entries
    while (myRemoteQueryExecutionsFeed.getFeedMetadata().getNextLink() != null)
    {
      String myQuery = extractNextLinkQuery(myRemoteQueryExecutionsFeed.getFeedMetadata().getNextLink());
      myRemoteQueryExecutionsFeed = myODataConsumerHelper.readFeed(myODataCredentials.getServiceURL(),
          myODataCredentials.getRemoteQueryExecutionObjectName(), NO_ID, NO_PATH, myQuery, APPLICATION_ATOM_XML);
      processRemoteQueryExecutions(
          myRemoteQueryExecutions, myRemoteQueryExecutionsFeed, myTimezone, myODataCredentials);
    }

    return myRemoteQueryExecutions;
  }

  private void processRemoteQueryExecutions(List<RemoteQueryExecution> aRemoteQueryExecutions,
      ODataFeed aRemoteQueryExecutionsFeed,
      ZoneId aZoneId,
      EsysOdataConnectionCredentials myODataCredentials)
  {
    List<ODataEntry> myEntries = aRemoteQueryExecutionsFeed.getEntries();
    for (ODataEntry myEntry : myEntries)
    {
      //Get values out of xml
      String myGUID = (String) myEntry.getProperties().get("GUID");
      String myQueryName = (String) myEntry.getProperties().get("QUERY_NAME");
      String myParameters = (String) myEntry.getProperties().get("PARAMETERS");
      Calendar myExecutionDateTime = (Calendar) myEntry.getProperties().get("EXECUTION_DATETIME");

      // Convert EnergySys date time to Date
      LocalDateTime myLocalDateTime = 
          LocalDateTime.ofInstant(myExecutionDateTime.toInstant(), ZoneId.of("UTC"));
      ZonedDateTime myZonedDateTime = ZonedDateTime.of(myLocalDateTime, aZoneId);
      Date myDate = Date.from(myZonedDateTime.toInstant());

      // Create Remote Query Execution
      RemoteQueryExecution myRemoteQueryExecution = new RemoteQueryExecution();
      myRemoteQueryExecution.setGUID(myGUID);
      myRemoteQueryExecution.setQueryName(myQueryName);
      myRemoteQueryExecution.setParameters(myParameters);
      myRemoteQueryExecution.setExecutionDateTime(myDate);
      myRemoteQueryExecution.setStatus(RemoteQueryExecution.Status.QUEUED);
      myRemoteQueryExecution.setExecuterType(myODataCredentials.getRemoteExecutionType());
      myRemoteQueryExecution.setConnectorName(myODataCredentials.getRemoteExecutionConnectorName());
      
      // Add to list of Remote Query Executions
      aRemoteQueryExecutions.add(myRemoteQueryExecution);
    }
  }
  private String getTopQuery(EsysOdataConnectionCredentials aConfigItem) 
  {
    return "$top=" + aConfigItem.getMaxRecordsPerPage();
  }

}
