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

import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.keystore.CredentialsStoreDAO;
import com.energysys.connector.keystore.StoredCredentials;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Properties;

/**
 * Encapsulates an EnergySys OData configuration.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public final class EsysOdataConnectionCredentials implements Serializable
{
    private static final String ALIAS_ESYS_ODATA_CRED = "esys-odata-cred";

    /**
     * PROPERTY KEY VALUES.
     */
    private static final String PROPERTY_SERVICE_URL = "service_url";
    private static final String PROPERTY_TAG_LIST_OBJECT_NAME = "tag_list_object_name";
    private static final String PROPERTY_ASSET_TIMEZONE_OVERRIDE = "asset_timezone_override";
    private static final String PROPERTY_MAX_RECORDS_PER_PAGE = "max_records_per_page";
    private static final String PROPERTY_REMOTE_QUERY_EXECUTION_OBJECT_NAME = "remote_query_execution_object_name";
    private static final String PROPERTY_REMOTE_QUERY_EXECUTION_ENABLED = "remote_query_execution_enabled";
    private static final String PROPERTY_REMOTE_QUERY_EXECUTION_CHECK_INTERVAL_MINS =
            "remote_query_execution_check_interval_mins";
    private static final String PROPERTY_REMOTE_QUERY_EXECUTION_CONNECTOR_NAME =
            "remote_query_execution_connector_name";
    private static final String PROPERTY_TAG_LIST_TIMEOUT = "tag_list_timeout";
    private static final String PROPERTY_TAG_CACHE_CHECK_TIMEOUT = "tag_cache_check_timeout";

    /**
     * DEFAULT VALUES FOR PROPERTIES.
     */
    private static final String DEFAULT_REMOTE_QUERY_EXECUTION_OBJECT_NAME = "EU_REMOTE_QUERY_EXEC";
    private static final String DEFAULT_MAX_RECORDS_PER_PAGE = "5000";
    private static final String DEFAULT_REMOTE_QUERY_EXECUTION_ENABLED = "false";
    private static final String DEFAULT_REMOTE_QUERY_EXECUTION_CHECK_INTERVAL_MINS = "1";
    private static final String DEFAULT_REMOTE_QUERY_EXECUTION_CONNECTOR_NAME = "connector";
    private static final String DEFAULT_TAG_LIST_TIMEOUT = "30";
    private static final String DEFAULT_TAG_LIST_CHECK_TIMEOUT = "30";
    private static final String STATIC_REMOTE_EXECUTION_EXECUTER =
            "com.energysys.measurementsconnector.MeasurementsJobExecuter";
    private static final String DEFAULT_TAG_LIST_OBJECT_NAME = "PR_METER_LIST_MEMBER";

    /**
     * Bean Fields.
     */
    private final String theServiceURL;
    private final String theTagListObjectName;
    private final String theAssetTimeZoneOverride;
    private final Integer tagListTimeout;
    private final Integer tagCacheCheckTimeout;
    // Properties for Remote Query Executions
    private final String theRemoteQueryExecutionObjectName;
    private final Boolean theRemoteExeuctionEnabled;
    private final Integer theRemoteExecutionCheckIntervalMins;
    private final String theRemoteExecutionConnectorName;
    private final String theMaxRecordsPerPage;
    /**
     * STATIC! No need to have this configurable for now. Will need to be changeable if we ever implement Remote
     * Executions for connector types other than Measurements ones.
     */
    private final String theRemoteExecutionType =  STATIC_REMOTE_EXECUTION_EXECUTER;

    /**
     * Constructor.
     *
     * @param aCredentialsString the credentials in properties form.
     */
    private EsysOdataConnectionCredentials(String aCredentialsString) throws ConnectorException
    {
        Properties myProps = new Properties();
        try
        {
            myProps.load(new StringReader(aCredentialsString));
            theServiceURL = myProps.getProperty(PROPERTY_SERVICE_URL);
            theTagListObjectName = myProps.getProperty(PROPERTY_TAG_LIST_OBJECT_NAME, DEFAULT_TAG_LIST_OBJECT_NAME);
            theAssetTimeZoneOverride = myProps.getProperty(PROPERTY_ASSET_TIMEZONE_OVERRIDE);
            theMaxRecordsPerPage = myProps.getProperty(PROPERTY_MAX_RECORDS_PER_PAGE, DEFAULT_MAX_RECORDS_PER_PAGE);

            theRemoteQueryExecutionObjectName =
                    myProps.getProperty(PROPERTY_REMOTE_QUERY_EXECUTION_OBJECT_NAME,
                            DEFAULT_REMOTE_QUERY_EXECUTION_OBJECT_NAME);

            theRemoteExeuctionEnabled =
                    Boolean.parseBoolean(
                            myProps.getProperty(PROPERTY_REMOTE_QUERY_EXECUTION_ENABLED,
                                    DEFAULT_REMOTE_QUERY_EXECUTION_ENABLED));

            theRemoteExecutionCheckIntervalMins =
                    Integer.parseInt(
                            myProps.getProperty(PROPERTY_REMOTE_QUERY_EXECUTION_CHECK_INTERVAL_MINS,
                                    DEFAULT_REMOTE_QUERY_EXECUTION_CHECK_INTERVAL_MINS));

            theRemoteExecutionConnectorName =
                    myProps.getProperty(PROPERTY_REMOTE_QUERY_EXECUTION_CONNECTOR_NAME,
                            DEFAULT_REMOTE_QUERY_EXECUTION_CONNECTOR_NAME);

            tagListTimeout = Integer.parseInt(myProps.getProperty(PROPERTY_TAG_LIST_TIMEOUT,
                            DEFAULT_TAG_LIST_TIMEOUT));

            tagCacheCheckTimeout =
                    Integer.parseInt(myProps.getProperty(PROPERTY_TAG_CACHE_CHECK_TIMEOUT,
                            DEFAULT_TAG_LIST_CHECK_TIMEOUT));

        }
        catch (IOException ex)
        {
            throw new ConnectorException("Error reading EsysOdataConnectionCredentials", ex);
        }
    }


    public String getRemoteQueryExecutionObjectName()
    {
        return theRemoteQueryExecutionObjectName;
    }

    public Boolean getRemoteExeuctionEnabled()
    {
        return theRemoteExeuctionEnabled;
    }

    public Integer getRemoteExecutionCheckIntervalMins()
    {
        return theRemoteExecutionCheckIntervalMins;
    }

    public String getRemoteExecutionConnectorName()
    {
        return theRemoteExecutionConnectorName;
    }

    public String getServiceURL()
    {
        return theServiceURL;
    }

    public String getTagListObjectName()
    {
        return theTagListObjectName;
    }

    public String getAssetTimeZoneOverride()
    {
        return theAssetTimeZoneOverride;
    }

    public String getRemoteExecutionType()
    {
        return theRemoteExecutionType;
    }

    public String getMaxRecordsPerPage()
    {
        return theMaxRecordsPerPage;
    }

    public Integer getTagListTimeout()
    {
        return tagListTimeout;
    }

    public Integer getTagCacheCheckTimeout()
    {
        return tagCacheCheckTimeout;
    }

    /**
     * Gets the EsysOdataConnectionCredentials from the keystore.
     *
     * @return the credentials
     * @throws ConnectorException
     */
    public static EsysOdataConnectionCredentials loadFromKeystore() throws ConnectorException
    {
        CredentialsStoreDAO myCredDAO = new CredentialsStoreDAO();
        StoredCredentials myStoredCredentials = myCredDAO.getEntry(ALIAS_ESYS_ODATA_CRED);

        if (myStoredCredentials == null)
        {
            return new EsysOdataConnectionCredentials("");
        }

        EsysOdataConnectionCredentials myEsysOdataCreds = new EsysOdataConnectionCredentials(myStoredCredentials.
                getCredentials());

        return myEsysOdataCreds;
    }
}
