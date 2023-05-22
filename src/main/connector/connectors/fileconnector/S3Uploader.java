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
import com.energysys.filesource.IFileSource;
import com.energysys.filesource.IFileSourceFile;
import com.energysys.filesource.IFileSourceFileFilter;
import com.energysys.filesource.exception.FileSourceException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * This is a File Connector class for uploading files to a remote S3 file source and then moving them to a processed
 * directory. Uploads any local files that match the configured filter and then moves them into a processed folder so
 * they are not uploaded again.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class S3Uploader implements IJobExecuter
{
  private static final Logger LOG = Logger.getLogger(S3Uploader.class.getName());
  private static final String PROCESSED_DIR = "processed/";

  private IFileSyncConfig<ILocalFileSourceConfig, CredentialsBackedS3FileSourceConfig> theConfig;

  private String theJobName;

  /**
   * Default Constructor.
   */
  public S3Uploader()
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
    try (S3FileSource myS3FileSource = new S3FileSource(theConfig.getDestinationConfig()))
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
        EventResult myCopyResult = copyAllFiles(myLocalFileSource, myS3FileSource,
                theConfig.getFilePattern(), theConfig.getIsDirRecursive());
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

  /**
   * Copies all files from one file source to another that match the given filename pattern and match the given file
   * filter.
   *
   * @param aSource Source file source
   * @param aDestination Destination file source
   * @param aFileNamePattern the file pattern
   * @param isDirRecursive whether to recurse sub dirs
   * @return a result message.
   * @throws ConnectorException on error
   */
  public EventResult copyAllFiles(IFileSource aSource, final IFileSource aDestination,
          final String aFileNamePattern, final Boolean isDirRecursive) throws
          ConnectorException
  {
    Long myStartMillis = CurrentDateTime.getCurrentTimeInMillis();
    List<IFileSourceFile> myFilesToUpload = aSource.findFiles(aFileNamePattern, isDirRecursive,
            new IFileSourceFileFilter()
    {
      @Override
      public boolean accept(IFileSourceFile aFile)
      {
        return !aFile.getFileId().startsWith(PROCESSED_DIR);
      }
    });

    LOG.info("Copying Files: " + myFilesToUpload.toString());
    // Create formatted timestamp for appending to processed filenames
    final String myFileTimestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(myStartMillis), ZoneId.systemDefault()).
            format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS"));
    // Upload each file
    for (IFileSourceFile myFile : myFilesToUpload)
    {
      try (InputStream myInputStream = aSource.getInputStream(myFile))
      {
        // Copy file to destination 
        aDestination.putContent(myInputStream, myFile);
      }
      catch (IOException | FileSourceException ex)
      {
        throw new ConnectorException("Error uploading file: " + myFile.getFileId(), ex);
      }

      try
      {
        // Compose path of original file when moving to processed directory
        String myFilename = myFile.getFileId();
        String myMovedFileName = PROCESSED_DIR + myFilename;
        // if the processed directory already contains a file with the same name, append a timestamp to the file name
        if (aSource.findFile(myMovedFileName) != null)
        {
          if (myFilename.matches(".*\\.([^\\.\\/]*$)"))
          {
            int myExtensionIndex = myFilename.lastIndexOf(".");
            myMovedFileName = PROCESSED_DIR + myFilename.substring(0, myExtensionIndex)
                + "(" + myFileTimestamp + ")" + myFilename.substring(myExtensionIndex);
          }
          else
          {
            myMovedFileName = PROCESSED_DIR + myFilename + "(" + myFileTimestamp + ")";
          }
        }
        // Move original to processed directory
        aSource.moveFile(myFile, myMovedFileName);
      }
      catch (FileSourceException ex)
      {
        throw new ConnectorException("Error moving file to processed: " + myFile.getFileId(), ex);
      }
    }
    return new EventResult("FileConnectorUtil:CopyNewFiles", EventResult.Result.SUCCESS,
            myFilesToUpload.size() + " Files Uploaded",
            "Files synced: " + myFilesToUpload.toString(), myStartMillis);
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
