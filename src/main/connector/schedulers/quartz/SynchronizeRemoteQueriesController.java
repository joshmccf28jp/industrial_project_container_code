/*
 * Copyright 2020 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.schedulers.quartz;

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.EventResult;
import com.energysys.connector.IJobExecuter;
import com.energysys.connector.config.ConnectorConfig;
import com.energysys.connector.connectors.dataconnector.odata.EsysOdataConnectionCredentials;
import com.energysys.connector.connectors.dataconnector.odata.ODataProxy;
import com.energysys.connector.connectors.fileconnector.CredentialsBackedS3FileSourceConfig;
import com.energysys.connector.database.GenericDAO;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.util.spreadsheet.RemoteQueryExecutionSpreadsheet;
import com.energysys.connector.util.spreadsheet.smartxls.SmartXLSSpreadsheet;
import com.energysys.connector.web.beans.EventLogBean;
import com.energysys.filesource.ConnectionStatus;
import com.energysys.filesource.exception.FileSourceException;
import com.energysys.filesource.s3.S3FileSource;
import com.energysys.filesource.s3.S3FileSourceFile;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * This class runs as a quartz just every minute (if configured) and sychronises the RemoteQueryExecutions
 * between EnergySys and the connector.
 *
 * @author EnergySys Limited
 */
public class SynchronizeRemoteQueriesController implements Job
{

  private static final long S3_CONNETION_TIMEOUT = 5 * 1000 * 60;
  private static final String ESYS_ODATA_CRED_ALIAS = "esys-odata-cred";
  private static final String NEW_EXECUTION_RECEIVED_EVENT_DESCRIPTION = "Remote Query Execution Received";

  private static final String SYNCHRONISE_REMOTE_QUERY_EXECUTIONS_EVENT_DESCRIPTION
      = "Synchronise RemoteQueryExecutions";

  private static final Logger LOG = Logger.getLogger(SynchronizeRemoteQueriesController.class.getName());

  private SchedulerManager theSchedulerManager;

  /**
   * Constructor.
   * @throws ConnectorException
   */
  public SynchronizeRemoteQueriesController() throws ConnectorException
  {
    theSchedulerManager = new SchedulerManager();
  }

  @Override
  @SuppressWarnings("IllegalCatch")
  public void execute(JobExecutionContext jec) throws JobExecutionException
  {
    Date myStartTime = CurrentDateTime.getCurrentDate();
    try
    {
      // Load the EnergySys OData credentials
      EsysOdataConnectionCredentials myODataCredentials = EsysOdataConnectionCredentials.loadFromKeystore();

      // If there are none present or remote query is disabled then quit quietly
      if (!myODataCredentials.getRemoteExeuctionEnabled())
      {
        LOG.fine("esys-odata-cred configuration file is missing or Remote Query Execution is disabled.");
        return;
      }

      // Check the odata credentials to confirm remote execution is enabled
      LOG.info("Synchronizing Remote Query Executions");
      ConnectorConfig myConnectorConfig = ConnectorConfig.loadFromKeystore();
      EventResult myResult = refreshRemoteJobs(myStartTime, myConnectorConfig, myODataCredentials);

    }
    catch (Throwable ex)
    {
      LOG.severe("Error Synchronizing Remote Query Executions");
      EventResult myResult = new EventResult(
          SYNCHRONISE_REMOTE_QUERY_EXECUTIONS_EVENT_DESCRIPTION,
          ex,
          myStartTime.getTime());
      EventLogBean.addLog(myResult);
    }
    LOG.info(MessageFormat.format("Synchronizing Remote Query Executions: completed in {0} milliseconds",
        CurrentDateTime.getCurrentTimeInMillis() - myStartTime.getTime()));
  }

  /**
   * Refreshes the current scheduled jobs with the current list of "queued" ones in EnergySys. Bear in mind there may
   * be a delay in the status in EnergySys being updated to "complete" after the job has been run on the connector. As
   * we don't want to run these job twice, they are stored locally and remain in the database even after being run to
   * ensure that this method will only add them to the schedule once. However, this does mean that we have to manually
   * delete them from the list once it is deemed that processing has been completed for the record. This is determined
   * as any local record that is NOT in the list of "Queued" queries from EnergySys and has a status of
   * PROCESSING_COMPLETE (which means the final result has been determined and that EnergySys has been informed of
   * this result.
   *
   * @param myStartTime
   * @param aConnectorConfig
   * @param myODataCredentials
   * @return the result
   * @throws ConnectorException
   */
  public EventResult refreshRemoteJobs(Date myStartTime, ConnectorConfig aConnectorConfig,
                                       EsysOdataConnectionCredentials myODataCredentials)
      throws ConnectorException, FileSourceException
  {
    // Create an OData Connection
    ODataProxy myProxy = new ODataProxy();

    // Get the Assets TimeZone
    TimeZone myAssetTimezone = myProxy.getAssetTimezone(aConnectorConfig, myODataCredentials);

    // Get the Remote Executions
    LOG.info("Getting OData Remote Executions");
    List<RemoteQueryExecution> myRemoteQueryExecutions = myProxy.getRemoteQueryExecutions(aConnectorConfig,
            myODataCredentials, myAssetTimezone);
    LOG.info(MessageFormat.format("Downloaded {0} Remote Query Executions from EnergySys", myRemoteQueryExecutions.
        size()));

    // We will need a list of jobs that require their status updating in EnergySys 
    // so we can produce a spreadsheet for upload to EnergySys
    List<RemoteQueryExecution> myUpdatedRemoteQueryExecutions = new ArrayList<>();

    try (GenericDAO myDAO = new GenericDAO())
    {
      // Get all remote queries stored in local db
      List<RemoteQueryExecution> myLocalRemoteQueryExecutions = myDAO.list(RemoteQueryExecution.class);
      LOG.info(MessageFormat.format("Comparing to {0} Remote Query Executions stored in local db",
          myLocalRemoteQueryExecutions.size()));

      // Check local records for status updates and deletions
      checkForUpdatesAndDeletions(myLocalRemoteQueryExecutions, myRemoteQueryExecutions, myDAO,
          myUpdatedRemoteQueryExecutions);

      // Check the EnergySys list for NEW queries
      checkForAdditions(myRemoteQueryExecutions, myLocalRemoteQueryExecutions, myODataCredentials,
          myUpdatedRemoteQueryExecutions, myDAO);

      // Create spreadsheet of status updates and upload to EnergySys
      EventResult myResult = createAndUploadSpreadsheet(
              myUpdatedRemoteQueryExecutions,
              myDAO,
              myAssetTimezone,
              myODataCredentials.getRemoteQueryExecutionObjectName(),
              myStartTime);

      // Add EventLog entry if there was a fule uploaded or there was a failure
      if (myResult.getResult() != EventResult.Result.SUCCESS || myUpdatedRemoteQueryExecutions.size() > 0)
      {
        EventLogBean.addLog(myResult);
      }
      return myResult;
    }
    
  }

  private void checkForAdditions(List<RemoteQueryExecution> myRemoteQueryExecutions,
      List<RemoteQueryExecution> myLocalRemoteQueryExecutions, EsysOdataConnectionCredentials myODataCredentials,
      List<RemoteQueryExecution> myUpdatedRemoteQueryExecutions, final GenericDAO myDAO) throws ConnectorException
  {
    for (RemoteQueryExecution myRemoteQueryExecution : myRemoteQueryExecutions)
    {
      // If the Query does not exist in the list of current jobs then add it
      if (!myLocalRemoteQueryExecutions.contains(myRemoteQueryExecution))
      {
        LOG.info(MessageFormat.format("New Remote Query Executions found: {0}", myRemoteQueryExecution));
        EventResult myAddResult = addRemoteQueryExecution(myRemoteQueryExecution, myODataCredentials);

        // Add to list of records to update in EnergySys
        myUpdatedRemoteQueryExecutions.add(myRemoteQueryExecution);

        // Save any status changes
        // (Even Failed ones are saved as we don't want to keep trying to run them each sync.
        // Once the status is updated in EnergySys the previous for loop will remove the record.)
        myDAO.save(myRemoteQueryExecution);
      }
      else
      {
        // Query was already in list of current jobs
        LOG.info(MessageFormat.format("Remote Query Executions already exists: {0}", myRemoteQueryExecution));
      }
    }
  }

  private void checkForUpdatesAndDeletions(List<RemoteQueryExecution> myLocalRemoteQueryExecutions,
      List<RemoteQueryExecution> myRemoteQueryExecutions, final GenericDAO myDAO,
      List<RemoteQueryExecution> myUpdatedRemoteQueryExecutions)
  {
    for (RemoteQueryExecution myLocalQueryExecution : myLocalRemoteQueryExecutions)
    {
      // Check for Jobs in local db that we can remove.
      // We can remove queries that are marked as Processing Complete which are no longer
      // in the list of Queued records from EnergySys.
      // PROCESSING COMPLETE means that the result of the Query has been sent to EnergySys already
      // and no further action is required by the connector.
      // If the list of Queued from energySys doesn't contain this record then the status must have been
      // updated and the local record is no longer required.
      if (myLocalQueryExecution.isProcessingComplete()
          && !myRemoteQueryExecutions.contains(myLocalQueryExecution))
      {
        LOG.info(MessageFormat.format("Deleting completed Remote Query Execution from local db: {0}",
            myLocalQueryExecution));
        myDAO.delete(myLocalQueryExecution);
      }

      // If the status is FAILED or SUCCESS and processing is not complete
      // then we want to update the EnergySys status (once this has been done the 
      // record will end up being marked as processsing complete and will be removed
      // once the EnergySys record has been updated and the sychroniser runs again)
      if (!myLocalQueryExecution.isProcessingComplete()
          && (myLocalQueryExecution.getStatus().equals(RemoteQueryExecution.Status.FAILED)
          || myLocalQueryExecution.getStatus().equals(RemoteQueryExecution.Status.SUCCESS)))
      {
        myUpdatedRemoteQueryExecutions.add(myLocalQueryExecution);
      }
    }
  }

  private EventResult addRemoteQueryExecution(RemoteQueryExecution myRemoteQueryExecution,
      EsysOdataConnectionCredentials aConnectionCredentials) throws ConnectorException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    LOG.info(MessageFormat.format("Validating new Remote Query Execution: {0}", myRemoteQueryExecution));

    EventResult myResult = validateRemoteQueryExecution(myRemoteQueryExecution, aConnectionCredentials, myStartTime);

    if (myResult.getResult() == EventResult.Result.SUCCESS)
    {
      LOG.info(MessageFormat.format("Scheduling Job for new Remote Query Execution: {0}", myRemoteQueryExecution));
      theSchedulerManager.createRemoteJob(myRemoteQueryExecution);
      
      DateFormat myFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

      myResult = new EventResult(
          NEW_EXECUTION_RECEIVED_EVENT_DESCRIPTION,
          EventResult.Result.SUCCESS,
          MessageFormat.format(
              "Job Scheduled to run {0} with Parameters ''{1}'' at ''{2}''",
              myRemoteQueryExecution.getQueryName(),
              myRemoteQueryExecution.getParameters(),
              myRemoteQueryExecution.getExecutionDateTime()),
          MessageFormat.format("Remote Query Execution: {0}",
              myRemoteQueryExecution),
          myStartTime);
    }

    // Update the status of the RemoteQueryExecution in the local db
    switch (myResult.getResult())
    {
      case SUCCESS:
      case WARNINGS:
        myRemoteQueryExecution.setStatus(RemoteQueryExecution.Status.ACCEPTED);
        break;
      case FAILED:
      case EXCEPTION:
      default:
        myRemoteQueryExecution.setStatus(RemoteQueryExecution.Status.FAILED);
        break;
    }
    myRemoteQueryExecution.setMessage(myResult.getSummaryMessage());

    // Create log event
    EventLogBean.addLog(myResult);

    return myResult;
  }

  private EventResult validateRemoteQueryExecution(
      RemoteQueryExecution myRemoteQueryExecution,
      EsysOdataConnectionCredentials aConnectionCredentials,
      long myStartTime) throws ConnectorException
  {
    // Check that the RemoteQueryExecution has a GUID and a Query Name
    if (myRemoteQueryExecution.getGUID() == null
        || myRemoteQueryExecution.getQueryName() == null)
    {
      return new EventResult(
          NEW_EXECUTION_RECEIVED_EVENT_DESCRIPTION,
          EventResult.Result.FAILED,
          "Remote Query must have a Query Name",
          myRemoteQueryExecution.toString(), myStartTime);
    }

    try
    {
      // Pass the RemoteQueryExecution to the connectors "Executer" (different for the connector type) for validation.
      // The "Executer" clasee is set in the "esys-odata-cred" credentials file.
      IJobExecuter myJobExecuter = (IJobExecuter) Class.forName(aConnectionCredentials.
              getRemoteExecutionType()).newInstance();

      return myJobExecuter.validateRemoteQueryExecution(NEW_EXECUTION_RECEIVED_EVENT_DESCRIPTION,
          myRemoteQueryExecution);
    }
    catch (IllegalAccessException | ClassNotFoundException | InstantiationException ex)
    {
      throw new ConnectorException("Invalid Job Executer Class: " + aConnectionCredentials.
          getRemoteQueryExecutionObjectName());
    }
  }

  private EventResult createAndUploadSpreadsheet(
      List<RemoteQueryExecution> someRemoteQueryExecutions,
      GenericDAO aGenericDAO,
      TimeZone anAssetTimeZone,
      String aNamedRangeName,
      Date aStartTime) throws ConnectorException, FileSourceException
  {
    if (someRemoteQueryExecutions.size() == 0)
    {
      // Create Spreadsheet
      LOG.info("No status updates to upload to EnergySys");
      return new EventResult(
          SYNCHRONISE_REMOTE_QUERY_EXECUTIONS_EVENT_DESCRIPTION,
          EventResult.Result.SUCCESS,
          "Success",
          "Nothing to upload",
          aStartTime.getTime());
    }

    // Load the S3 config (so process will fail if not present)
    List<CredentialsBackedS3FileSourceConfig> myS3Configs = CredentialsBackedS3FileSourceConfig.loadFromKeystore();
    if (myS3Configs.size() == 0)
    {
      throw new ConnectorException(
          "Could not upload remote query result - No S3 credentials installed in connector");
    }
    if (myS3Configs.size() > 1)
    {
      throw new ConnectorException(
          "Could not upload remote query result - Multiple S3 credentials installed in connector");
    }
    CredentialsBackedS3FileSourceConfig myS3Config = myS3Configs.get(0);

    // Create a spreadsheet from the data
    LOG.info("Creating spreadsheet for Remote Query Execution statuses to upload to EnergySys");
    RemoteQueryExecutionSpreadsheet mySpreadsheet = new RemoteQueryExecutionSpreadsheet(someRemoteQueryExecutions,
        anAssetTimeZone, aNamedRangeName);

    // Send spreadsheet to S3
    S3FileSource myS3FileSource = new S3FileSource(myS3Config);
    
    String fileName = generateFileName(aStartTime);
    S3FileSourceFile myFileMetadata = new S3FileSourceFile(
        fileName,
        aStartTime,
        myS3Config.getOwner(),
        mySpreadsheet.getSize().longValue(),
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    if (openS3Connection(myS3FileSource))
    {
      myS3FileSource.putContent(mySpreadsheet.getInputStream(), myFileMetadata);
    }
    else
    {
      return new EventResult(
          SYNCHRONISE_REMOTE_QUERY_EXECUTIONS_EVENT_DESCRIPTION,
          EventResult.Result.FAILED,
          "Could not upload spreadsheet",
          "Failed to acquire lock on S3 File Source",
          aStartTime.getTime());
    }

    LOG.info("Marking SUCCESS and FAILED RemoteQueryExecutions as ProcessingComplete");
    for (RemoteQueryExecution myRemoteQueryExecution : someRemoteQueryExecutions)
    {
      // If status is not ACCEPTED then mark as PROCESSING complete
      if (myRemoteQueryExecution.getStatus() != RemoteQueryExecution.Status.ACCEPTED)
      {
        LOG.info("Marking record as processing complete: " + myRemoteQueryExecution);
        myRemoteQueryExecution.setIsProcessingComplete(Boolean.TRUE);
        aGenericDAO.save(myRemoteQueryExecution);
      }
    }

    return new EventResult(
        SYNCHRONISE_REMOTE_QUERY_EXECUTIONS_EVENT_DESCRIPTION,
        EventResult.Result.SUCCESS,
        "Success",
        "Spreadsheet " + fileName + " uploaded with " + someRemoteQueryExecutions.size() + " rows",
        aStartTime.getTime());

  }

  private String generateFileName(Date aStartTime)
  {
    StringBuilder myFilePath = new StringBuilder();
    SimpleDateFormat mySDF = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS");
    myFilePath.append("Connector-RemoteQueryExecution");
    myFilePath.append("-");
    myFilePath.append(mySDF.format(aStartTime));
    myFilePath.append(".").append(SmartXLSSpreadsheet.FORMAT.XLSX.toString().toLowerCase());
    return myFilePath.toString();
  }

  @SuppressWarnings(
      "SleepWhileInLoop")
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
    catch (InterruptedException ex)
    {
      throw new ConnectorException("Error connecting to S3", ex);
    }
    return false;
  }
}
