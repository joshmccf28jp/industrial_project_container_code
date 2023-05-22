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
package com.energysys.connector.connectors.dataconnector;

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.EventResult;
import com.energysys.connector.config.ConnectorConfig;
import com.energysys.connector.util.Cache;
import com.energysys.connector.connectors.dataconnector.odata.EsysOdataConnectionCredentials;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.connectors.dataconnector.odata.TagList;
import com.energysys.connector.connectors.dataconnector.odata.ODataProxy;
import com.energysys.connector.web.beans.EventLogBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Manages the available TagLists.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public class TagListManager
{
    private static final Logger LOG = Logger.getLogger(TagListManager.class.getName());
    private static Cache cache;
    private static final String CACHE_KEY = "TagList";
    private static final String TIMEZONE_KEY = "Timezone";
    private static Map<String, TagList> theLatestFetchedTagList;

    private final EsysOdataConnectionCredentials esysOdataCreds;

    private final ConnectorConfig connectorConfig;

    /**
     * Constructor.
     */
    public TagListManager() throws ConnectorException
    {
        esysOdataCreds = EsysOdataConnectionCredentials.loadFromKeystore();

        connectorConfig = ConnectorConfig.loadFromKeystore();
        cache = new Cache(esysOdataCreds.getTagListTimeout() * 1000L,
                esysOdataCreds.getTagCacheCheckTimeout() * 60 * 1000L);
    }

    /**
     * Get the names.
     *
     * @return List
     * @throws com.energysys.connector.exception.ConnectorException
     */
    public List<String> getTagListNames() throws ConnectorException
    {
        List<String> myTagListNames = new ArrayList<>();

        Map<String, TagList> myTagLists = getTagLists();
        if (myTagLists != null)
        {
            myTagListNames = new ArrayList<>(myTagLists.keySet());
            Collections.sort(myTagListNames);
        }
        return myTagListNames;
    }

    /**
     * Gets the asset timezone.
     *
     * @return the timezone id
     * @throws ConnectorException on Exception
     */
    public TimeZone getAssetTimezone() throws ConnectorException
    {
        if (cache.get(TIMEZONE_KEY) == null)
        {
            ODataProxy myProxy = new ODataProxy();
            TimeZone myTimezone = myProxy.getAssetTimezone(connectorConfig, esysOdataCreds);
            cache.put(TIMEZONE_KEY, myTimezone);
        }
        return (TimeZone) cache.get(TIMEZONE_KEY);
    }

    /**
     * Return the matching tagList.
     *
     * @param aTagListName name
     * @return Tag List
     * @throws com.energysys.connector.exception.ConnectorException
     */
    public TagList getTagList(String aTagListName) throws ConnectorException
    {
        TagList myTagList = null;
        Map<String, TagList> myTagLists = getTagLists();

        if (myTagLists != null)
        {
            myTagList = myTagLists.get(aTagListName);
        }
        return myTagList;
    }

    /**
     * Refreshed the tag list stored in the CACHE from OData and returns TRUE.
     * OR... If the fetch fails, puts the previous fetched tag list in the cache instead and returns FALSE.
     * This method can be used when the caller SPECIFICALLY wants to know if the tag list was successfully refreshed.
     * It is also used privately by this class whenever the tag list is required and has been removed from the CACHE
     * due to timeout.
     *
     * @return if the tag list was updated from OData
     * @throws ConnectorException if fetch failed and there was no previous tag list
     */
    public Boolean refreshTagLists() throws ConnectorException
    {
        if (esysOdataCreds.getServiceURL() == null)
        {
            String myMessage =
                "Tag lists could not be retrieved from EnergySys. "
                    + "esys-odata-cred configuration file missing service_url.";
            throw new ConnectorException(myMessage);
        }

        // Try and refesh tag list from OData
        Map<String, TagList> myTagLists = null;
        ConnectorException myTagListFetchException = null;
        try
        {
            //OData feed query
            ODataProxy myProxy = new ODataProxy();
            myTagLists = myProxy.getTagLists(connectorConfig, esysOdataCreds);
        }
        catch (ConnectorException myEx)
        {
            myTagListFetchException = myEx;
        }

        // If the returned tag list is not empty then update stored tags and return true
        if (myTagLists != null && !myTagLists.isEmpty())
        {
            theLatestFetchedTagList = myTagLists;
            cache.put(CACHE_KEY, myTagLists);
            return true;
        }
        // If the returned list was empty or null then refresh cache with previous list and return false
        else if (theLatestFetchedTagList != null && !theLatestFetchedTagList.isEmpty())
        {
            LOG.warning("Tag List was not refreshed. Using previous tag list");
            if (myTagListFetchException != null)
            {
              EventLogBean.addLog(new EventResult("Tag List was not refreshed. Previous tag list used",
                  myTagListFetchException, CurrentDateTime.getCurrentTimeInMillis()));
            }
            cache.put(CACHE_KEY, theLatestFetchedTagList);
            return false;
        }
        // If returned list was empty and no previous list exists then throw an exception.
        else
        {
            throw myTagListFetchException;
        }

    }

    /**
     * Gets the Tag Lists from the CACHE or from OData OR from the previous fetch, if available.
     *
     * @return the Tag Lists
     * @throws ConnectorException on error
     */
    private Map<String, TagList> getTagLists() throws ConnectorException
    {
        Map<String, TagList> myTagList = (Map<String, TagList>) cache.get(CACHE_KEY);

        // If cached version exists, return that
        if (myTagList != null)
        {
            return myTagList;
        }
        // Otherwise refresh cache and return new cached version
        refreshTagLists();
        return (Map<String, TagList>) cache.get(CACHE_KEY);
    }

}
