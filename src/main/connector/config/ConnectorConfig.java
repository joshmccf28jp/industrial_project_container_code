/*
 * Copyright 2022 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.config;

import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.keystore.CredentialsStoreDAO;
import com.energysys.connector.keystore.StoredCredentials;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.TimeZone;

/**
 * Encapsulates the connector configuration.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public final class ConnectorConfig
{
    private static final String CREDENTIALS_KEYSTORE_ALIAS = "connector-config";

    /**
     * PROPERTY KEY VALUES.
     */
    private static final String PROPERTY_DATA_FILE_DIR = "data_file_dir";
    private static final String PROPERTY_DATA_FILE_RETENTION_PERIOD = "data_file_retention_period";
    private static final String PROPERTY_CONNECTOR_TIMEZONE = "connector_timezone";
    private static final String PROPERTY_JOB_MAX_RETRIES = "job_max_retries";
    private static final String PROPERTY_JOB_REPEAT_INTERVAL = "job_repeat_interval";
    private static final String PROPERTY_HTTPS_PROXY_HOST = "https_proxy_host";
    private static final String PROPERTY_HTTP_PROXY_PORT = "http_proxy_port";
    private static final String PROPERTY_HTTPS_PROXY_PORT = "https_proxy_port";
    private static final String PROPERTY_HTTP_READ_TIMEOUT = "http_read_timeout";
    private static final String PROPERTY_HTTP_CONNECTION_TIMEOUT = "http_connection_timeout";
    private static final String PROPERTY_HTTP_CONNECTION_TRUSTING = "http_connection_trusting";

    /**
     * DEFAULT VALUES FOR PROPERTIES.
     */
    private static final String DEFAULT_CONNECTOR_DATA = "/ConnectorData/";
    private static final String DEFAULT_DATA_FILE_DIR = DEFAULT_CONNECTOR_DATA;
    private static final String DEFAULT_DEFAULT_VALUE = "28";
    private static final String DEFAULT_DATA_FILE_RETENTION_PERIOD = DEFAULT_DEFAULT_VALUE;
    private static final String DEFAULT_CONNECTOR_TIMEZONE = TimeZone.getDefault().getID();
    private static final String DEFAULT_JOB_MAX_RETRIES = "5";
    private static final String DEFAULT_JOB_REPEAT_INTERVAL = "120";
    private static final String DEFAULT_HTTP_READ_TIMEOUT = "300000";
    private static final String DEFAULT_HTTP_CONNECTION_TIMEOUT = "300000";

    private static final String DEFAULT_HTTP_CONNECTION_TRUSTING = "true";

    /**
     * Bean fields.
     */
    private final String dataFileDirectory;

    private final Integer dataFileRetentionPeriod;

    private final TimeZone connectorTimezone;

    private final Integer jobMaxRetries;

    private final Integer jobRepeatInterval;

    private final String httpsProxyHost;

    private final Integer httpsProxyPort;

    private final Integer httpReadTimeout;

    private final Integer httpConnectionTimeout;

    private Boolean isHttpConnectionTrusting;


    /**
     * Default constructor.
     */
    private ConnectorConfig(String aCredentialsString) throws ConnectorException
    {
        Properties myProps = new Properties();
        try
        {
            String theJettyHomePath = System.getProperty("jetty.base");
            myProps.load(new StringReader(aCredentialsString));

            dataFileDirectory =
                    myProps.getProperty(PROPERTY_DATA_FILE_DIR,
                            theJettyHomePath + DEFAULT_DATA_FILE_DIR);

            dataFileRetentionPeriod =
                    Integer.parseInt(myProps.getProperty(PROPERTY_DATA_FILE_RETENTION_PERIOD,
                            DEFAULT_DATA_FILE_RETENTION_PERIOD));

            connectorTimezone =
                    TimeZone.getTimeZone(myProps.getProperty(PROPERTY_CONNECTOR_TIMEZONE,
                            DEFAULT_CONNECTOR_TIMEZONE));

            jobMaxRetries =
                    Integer.parseInt(myProps.getProperty(PROPERTY_JOB_MAX_RETRIES,
                            DEFAULT_JOB_MAX_RETRIES));

            jobRepeatInterval =
                    Integer.parseInt(myProps.getProperty(PROPERTY_JOB_REPEAT_INTERVAL,
                            DEFAULT_JOB_REPEAT_INTERVAL));

            httpsProxyHost =
                    myProps.getProperty(PROPERTY_HTTPS_PROXY_HOST);

            httpsProxyPort = myProps.containsKey(PROPERTY_HTTP_PROXY_PORT)
                    ? Integer.parseInt(myProps.getProperty(PROPERTY_HTTPS_PROXY_PORT)) : null;

            httpReadTimeout =
                    Integer.parseInt(myProps.getProperty(PROPERTY_HTTP_READ_TIMEOUT,
                            DEFAULT_HTTP_READ_TIMEOUT));

            httpConnectionTimeout =
                    Integer.parseInt(myProps.getProperty(PROPERTY_HTTP_CONNECTION_TIMEOUT,
                            DEFAULT_HTTP_CONNECTION_TIMEOUT));

            isHttpConnectionTrusting =
                Boolean.parseBoolean(
                    myProps.getProperty(PROPERTY_HTTP_CONNECTION_TRUSTING, DEFAULT_HTTP_CONNECTION_TRUSTING));
        }
        catch (IOException ex)
        {
            throw new ConnectorException("Error reading AdaptorCredentials", ex);
        }
    }

    public String getDataFileDirectory()
    {
        return dataFileDirectory;
    }

    public Integer getDataFileRetentionPeriod()
    {
        return dataFileRetentionPeriod;
    }

    public TimeZone getConnectorTimezone()
    {
        return connectorTimezone;
    }

    public Integer getJobMaxRetries()
    {
        return jobMaxRetries;
    }

    public Integer getJobRepeatInterval()
    {
        return jobRepeatInterval;
    }

    public String getHttpsProxyHost()
    {
        return httpsProxyHost;
    }

    public Integer getHttpsProxyPort()
    {
        return httpsProxyPort;
    }

    public Integer getHttpReadTimeout()
    {
        return httpReadTimeout;
    }

    public Integer getHttpConnectionTimeout()
    {
        return httpConnectionTimeout;
    }

    public Boolean isHttpConnectionTrusting()
    {
        return isHttpConnectionTrusting;
    }
    /**
     * Load the configuration from the keystore.
     * @return ConnectorConfig
     * @throws ConnectorException
     */
    public static ConnectorConfig loadFromKeystore() throws ConnectorException
    {
        CredentialsStoreDAO myCredDAO = new CredentialsStoreDAO();
        StoredCredentials myStoredCredentials = myCredDAO.getEntry(CREDENTIALS_KEYSTORE_ALIAS);

        if (myStoredCredentials == null)
        {
            return new ConnectorConfig("");
        }
        ConnectorConfig myConfig = new ConnectorConfig(myStoredCredentials.getCredentials());

        return myConfig;
    }
}
