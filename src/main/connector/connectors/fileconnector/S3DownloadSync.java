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
package com.energysys.connector.connectors.fileconnector;

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.filesource.IFileSourceFile;
import com.energysys.filesource.IFileSourceFileFilter;
import com.energysys.filesource.local.ILocalFileSourceConfig;
import com.energysys.filesource.local.LocalFileSource;
import com.energysys.filesource.s3.S3FileSource;
import java.util.Date;
import java.util.logging.Logger;
import com.energysys.connector.IJobExecuter;
import com.energysys.connector.EventResult;
import com.energysys.connector.database.GenericDAO;
import com.energysys.connector.schedulers.quartz.AbstractJobController;
import com.energysys.connector.schedulers.quartz.RemoteQueryExecution;
import com.energysys.filesource.ConnectionStatus;
import com.energysys.filesource.exception.FileSourceException;
import com.energysys.filesource.s3.S3RemoteCachedCatalog;

/**
 * This is a File Connector class for downloading new and updated objects from a remote S3 file source.
 *
 * If anIgnoreBeforeDate is specified then it only downloads objects that have been remotely modified after this date.
 * This allows for local files to be deleted without triggering a re-download next time the process is run.
 *
 * BE AWARE THAT THE REMOTE DATE AND LOCAL DATE MAY DIFFER SLIGHTLY SO ENSURE THAT IF AN IGNORE BEFORE DATE IS SPECIFIED
 * THEN THE DATE TAKES THIS INTO ACCOUNT. e.g. using the previous downloads newest objects last modified date as opposed
 * to the last time the download was run. For this purpose the getTheNewestFileDate() method can be used.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class S3DownloadSync implements IJobExecuter
{

  private static final Logger LOG = Logger.getLogger(S3DownloadSync.class.getName());
  private String theJobName;
  private IFileSyncConfig<ILocalFileSourceConfig, CredentialsBackedS3FileSourceConfig> theConfig;
  private S3DownloaderStatus theStatus;

  /**
   * Default constructor.
   */
  public S3DownloadSync()
  {
  }

  /**
   * Basic Constructor.
   *
   * @param aStatus the status
   */
  public S3DownloadSync(S3DownloaderStatus aStatus)
  {
    this.theStatus = aStatus;
  }

  @Override
  @SuppressWarnings("checkstyle:all")
  public EventResult execute(String aJobName, String aTarget, Date aTargetRunTime, 
      AbstractJobController.ExecutionType anexExecutionType)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    EventResult myResult;
    try (GenericDAO myDAO = new GenericDAO())
    {
      theJobName = aJobName;
      theConfig = myDAO.findById(SyncConfiguration.class, Integer.parseInt(aTarget));
      theConfig.getDestinationConfig().loadCredentials();
      if (theStatus == null)
      {
        theStatus = new S3DownloaderStatus();
        // Set date to zero
        theStatus.setIgnoreBeforeDate(new Date(0));
      }
      myResult = execute();

    }
    catch (ConnectorException ex)
    {
      return new EventResult(theJobName, ex, myStartTime);
    }
    return myResult;
  }

  /**
   * Execute the donwload.
   *
   * @return the result
   */
  public EventResult execute()
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    // Open connection to S3
    try (S3FileSource myS3FileSource = new S3FileSource(theConfig.getDestinationConfig(), new S3RemoteCachedCatalog()))
    {
      ConnectionStatus myOpenResult = myS3FileSource.openConnection();

      // Check the open request was successful
      switch (myOpenResult)
      {
        case CLOSED:
          return new EventResult(theJobName, EventResult.Result.EXCEPTION, "Could not open S3 Connection", "",
                  myStartTime);

        case LOCKED_OUT:
          return new EventResult(theJobName, EventResult.Result.FAILED, "Connection locked by other user", "",
                  myStartTime);

        default:
      }

      // Scan local directory for files
      LocalFileSource myLocalFileSource = new LocalFileSource(theConfig.getSourceConfig());

      try
      {
        EventResult myCopyResult = FileConnectorUtil.copyNewFiles(myS3FileSource, myLocalFileSource,
                theConfig.getFilePattern(), theConfig.getIsDirRecursive(), new IFileSourceFileFilter()
        {
          @Override
          public boolean accept(IFileSourceFile aFile)
          {
            return aFile.getFileSourceModifiedDate().after(theStatus.getIgnoreBeforeDate());
          }
        });

        // If successful then update the last update date
        if (myCopyResult.getResult() == EventResult.Result.SUCCESS)
        {
          theStatus.setIgnoreBeforeDate(myS3FileSource.getLatestUpdateDate());
        }
        return new EventResult(theJobName, myCopyResult, myStartTime);
      }
      catch (ConnectorException ex)
      {
        return new EventResult(theJobName, ex, myStartTime);
      }
    }
    catch (FileSourceException ex)
    {
      return new EventResult(theJobName, ex, myStartTime);
    }
  }

  public void setConfig(IFileSyncConfig aConfig)
  {
    this.theConfig = aConfig;
  }

  public S3DownloaderStatus getTheStatus()
  {
    return theStatus;
  }

  @Override
  public Boolean supportsReRuns()
  {
    return false;
  }

  @Override
  public EventResult validateRemoteQueryExecution(String aRequestName, RemoteQueryExecution aRemoteQueryExecution)
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

}
