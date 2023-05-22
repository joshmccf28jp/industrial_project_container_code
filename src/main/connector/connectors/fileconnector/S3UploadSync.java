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
import com.energysys.filesource.local.ILocalFileSourceConfig;
import com.energysys.filesource.local.LocalFileSource;
import com.energysys.filesource.s3.S3FileSource;
import java.util.logging.Logger;
import com.energysys.connector.IJobExecuter;
import com.energysys.connector.EventResult;
import com.energysys.connector.database.GenericDAO;
import com.energysys.connector.schedulers.quartz.AbstractJobController;
import com.energysys.connector.schedulers.quartz.RemoteQueryExecution;
import com.energysys.filesource.ConnectionStatus;
import com.energysys.filesource.exception.FileSourceException;
import com.energysys.filesource.s3.S3RemoteCachedCatalog;
import java.util.Date;

/**
 * This is a File Connector class for uploading new and updated objects to a remote S3 file source. Uploads any local
 * files that are not present in the remote S3 file sources. Because the S3 file source uses a catalog to store what is
 * present on S3 it is possible to remove or archive objects stored in S3 and not have them uploaded again.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class S3UploadSync implements IJobExecuter
{

  private static final Logger LOG = Logger.getLogger(S3UploadSync.class.getName());

  private IFileSyncConfig<ILocalFileSourceConfig, CredentialsBackedS3FileSourceConfig> theConfig;

  private String theJobName;

  /**
   * Default Constructor.
   */
  public S3UploadSync()
  {

  }

  @Override
  public EventResult execute(String aJobName, String aTarget, Date aTargetRunTime, 
      AbstractJobController.ExecutionType anexExecutionType)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try (GenericDAO myDAO = new GenericDAO())
    {
      theConfig = myDAO.findById(SyncConfiguration.class, Integer.parseInt(aTarget));
      theConfig.getDestinationConfig().loadCredentials();
      theJobName = aJobName;
    }
    catch (ConnectorException ex)
    {
      return new EventResult(theJobName, ex, myStartTime);
    }
    return execute();
  }

  /**
   * Execute the uploader.
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
      LocalFileSource myLocalFileSource = new LocalFileSource((ILocalFileSourceConfig) theConfig.getSourceConfig());

      try
      {
        EventResult myCopyResult = FileConnectorUtil.copyNewFiles(myLocalFileSource, myS3FileSource,
                theConfig.getFilePattern(), theConfig.getIsDirRecursive(), null);
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
