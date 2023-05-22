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
import com.energysys.connector.connectors.dataconnector.adaptor.Notification;
import com.energysys.connector.connectors.dataconnector.odata.TagList;
import com.energysys.connector.connectors.dataconnector.adaptor.TagListQueryResult;
import com.energysys.connector.connectors.dataconnector.adaptor.IAdaptorInterface;
import com.energysys.connector.connectors.dataconnector.adaptor.AdaptorFactory;
import com.energysys.connector.IJobExecuter;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.connectors.dataconnector.config.TagListQueryConfiguration;
import com.energysys.connector.connectors.fileconnector.CredentialsBackedS3FileSourceConfig;
import com.energysys.connector.database.GenericDAO;
import com.energysys.connector.schedulers.quartz.AbstractJobController;
import com.energysys.connector.schedulers.quartz.RemoteQueryExecution;
import com.energysys.connector.util.spreadsheet.smartxls.SmartXLSSpreadsheet;
import com.energysys.filesource.ConnectionStatus;
import com.energysys.filesource.exception.FileSourceException;
import com.energysys.filesource.s3.S3FileSource;
import com.energysys.filesource.s3.S3FileSourceFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Main class for Tag List Query execution.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public class TagListQueryExecutor implements IJobExecuter
{

    private static final Logger LOG = Logger.getLogger(TagListQueryExecutor.class.getName());

    private static final String ERROR_MESSAGE_NO_SPREADSHEET = "No spreadsheet could be found for dispatch.";

    private static final long S3_CONNETION_TIMEOUT = 5 * 1000 * 60;
    private static final String NEW_LINE = "\n";

    private final TagListManager tagListManager;

    /**
     * Constructor.
     */
    public TagListQueryExecutor() throws ConnectorException
    {
        tagListManager = new TagListManager();
    }

    @Override
    @SuppressWarnings("IllegalCatch")
    public EventResult execute(String aJobName, String aTarget, Date aTargetRunTime,
                               AbstractJobController.ExecutionType anexExecutionType)
    {

        Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
        try (GenericDAO myDAO = new GenericDAO())
        {
            //Retrieve Tag List Query
            TagListQueryConfiguration myTagListQuery =
                    myDAO.findById(TagListQueryConfiguration.class, Integer.parseInt(aTarget));
            if (myTagListQuery == null)
            {
                throw new ConnectorException("Invalid Tag Query");
            }
            //Execute
            return execute(aJobName, myTagListQuery, false, aTargetRunTime);
        }
        catch (Exception myEx)
        {
            return new EventResult(aJobName, myEx, myStartTime);
        }
    }

    /**
     * Preview execution of the tag list query - extract data, do not write.
     *
     * @param aTagListQuery the query.
     * @return notification for display.
     * @throws ConnectorException on error
     */
    public EventResult preview(TagListQueryConfiguration aTagListQuery) throws ConnectorException
    {
        return execute("Preview", aTagListQuery, true, CurrentDateTime.getCurrentDate());
    }

    @Override
    public Boolean supportsReRuns()
    {
        try
        {
            IAdaptorInterface myAdaptor = AdaptorFactory.getAdaptor();
            return myAdaptor.supportsReRuns();
        }
        catch (ConnectorException myEx)
        {
            return false;
        }
    }

    private EventResult execute(String aJobName, TagListQueryConfiguration
            aTagListQuery, boolean isPreview, Date aTargetRunTime) throws ConnectorException
    {
        Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();

        TagListManager myManager = new TagListManager();

        // Call refreshTagLists() manually as we want to know whether the list was retrieved from
        // EnergySys or not.
        Boolean myTagListWasRefreshed = myManager.refreshTagLists();

        TagListQueryResult myTagListQueryResult =
                executeAndDispatch(aJobName, aTagListQuery, isPreview, aTargetRunTime);

        Notification myNotification = myTagListQueryResult.getNotification();
        StringBuilder myMessageBuff = new StringBuilder("Process Completed");
        StringBuilder myDetailedMessageBuff = new StringBuilder("Process Completed\n");
        myDetailedMessageBuff.append("Target Run Time: " + aTargetRunTime.toString() + NEW_LINE);
        if (myTagListQueryResult.getResultsFile() != null)
        {
            myDetailedMessageBuff.append("Results File: ");
            myDetailedMessageBuff.append(myTagListQueryResult.getResultsFile().getAbsoluteFile());
            myDetailedMessageBuff.append(NEW_LINE);
        }
        EventResult.Result myEventResult;
        if (myNotification.hasWarningMessage() || !myTagListWasRefreshed)
        {
            myMessageBuff.append(" With Warnings");
            myDetailedMessageBuff.append("Had Warnings: ");
            if (!myTagListWasRefreshed)
            {
                myDetailedMessageBuff.append("Tag List was not refreshed. Previous tag list used.\n");
            }
            if (myNotification.hasWarningMessage())
            {
                myDetailedMessageBuff.append(myNotification.getWarningMessage());
            }
            myEventResult = EventResult.Result.WARNINGS;
        }
        else
        {
            myEventResult = EventResult.Result.SUCCESS;
        }
        if (isPreview)
        {
            myDetailedMessageBuff.append(NEW_LINE);
            myDetailedMessageBuff.append(myNotification.getRetrievedDataMessage());
        }

        return new EventResult(aJobName, myEventResult,
                myMessageBuff.toString(), myDetailedMessageBuff.toString(), myStartTime);
    }

    private TagListQueryResult executeAndDispatch(String aJobName,
                                                  TagListQueryConfiguration aTagListQuery,
                                                  boolean isPreview,
                                                  Date aTargetRunTime) throws ConnectorException
    {
        TagListQueryResult myTagListQueryResult;
        //Retrieve the tag list
        TagList myTagList = tagListManager.getTagList(aTagListQuery.getTagListName());
        TimeZone myTimezone = tagListManager.getAssetTimezone();
        if (myTagList == null)
        {
            String myMessage = "The tag list could not be found with the name: " + aTagListQuery.getTagListName();
            throw new ConnectorException(myMessage);
        }
        //Execute
        IAdaptorInterface myAdaptor = AdaptorFactory.getAdaptor();
        myTagListQueryResult = myAdaptor.execute(
                aTagListQuery,
                myTagList,
                ConnectorConfig.loadFromKeystore(),
                myTimezone,
                aTargetRunTime);
        if (!isPreview)
        {
            //Dispatch data to Esys process
            dispatchToS3(aJobName, myTagListQueryResult);
        }
        return myTagListQueryResult;
    }

    private void dispatchToS3(String aJobName, TagListQueryResult aTagListQueryResult) throws ConnectorException
    {
        Date myStartTime = CurrentDateTime.getCurrentDate();
        File myFile = aTagListQueryResult.getResultsFile();

        // Load the S3 config (so process will fail if not present)
        List<CredentialsBackedS3FileSourceConfig> myS3Configs = CredentialsBackedS3FileSourceConfig.loadFromKeystore();
        if (myS3Configs.size() == 0)
        {
            throw new ConnectorException(
                "Could not upload tag query result - No S3 credentials installed in connector");
        }
        if (myS3Configs.size() > 1)
        {
            throw new ConnectorException(
                "Could not upload tag query result - Multiple S3 credentials installed in connector");
        }

        CredentialsBackedS3FileSourceConfig myS3Config = myS3Configs.get(0);
        // Send file to S3
        S3FileSource myS3FileSource = new S3FileSource(myS3Config);

        S3FileSourceFile myFileMetadata = new S3FileSourceFile(
                generateFileName(aJobName, myStartTime),
                myStartTime,
                myS3Config.getOwner(),
                myFile.length(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        try
        {
            if (openS3Connection(myS3FileSource))
            {
                myS3FileSource.putContent(new FileInputStream(myFile), myFileMetadata);
            }
            else
            {
                throw new ConnectorException(
                        "Could not upload tag query result - Failed to acquire lock on S3 File Source");
            }
        }
        catch (FileSourceException | FileNotFoundException ex)
        {
            throw new ConnectorException("Could not upload tag query result - " + ex.getMessage());
        }
    }

    private String generateFileName(String aJobName, Date aStartTime)
    {
        StringBuilder myFilePath = new StringBuilder();
        SimpleDateFormat mySDF = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS");
        myFilePath.append(aJobName);
        myFilePath.append("-");
        myFilePath.append(mySDF.format(aStartTime));
        myFilePath.append(".").append(SmartXLSSpreadsheet.FORMAT.XLSX.toString().toLowerCase());
        return myFilePath.toString();
    }

    @SuppressWarnings({"SleepWhileInLoop", "IllegalCatch"})
    private Boolean openS3Connection(S3FileSource aS3FileSource) throws FileSourceException, ConnectorException
    {
        long myTimeoutPoint = CurrentDateTime.getCurrentTimeInMillis() + S3_CONNETION_TIMEOUT;

        try
        {
            do
            {
                ConnectionStatus myConnStatus = aS3FileSource.openConnection();
                if (myConnStatus == ConnectionStatus.OPEN)
                {
                    return true;
                }
                Thread.sleep(1 * 1000 * 20);
            }
            while (CurrentDateTime.getCurrentTimeInMillis() < myTimeoutPoint);
        }
        catch (Exception ex)
        {
            throw new ConnectorException("Error connecting to S3", ex);
        }
        return false;
    }

    @Override
    public EventResult validateRemoteQueryExecution(String aRequestName, RemoteQueryExecution aRemoteQueryExecution)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
