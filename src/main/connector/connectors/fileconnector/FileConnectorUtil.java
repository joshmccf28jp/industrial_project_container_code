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
import com.energysys.connector.EventResult;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.filesource.FileSourceComparison;
import com.energysys.filesource.IFileSource;
import com.energysys.filesource.IFileSourceFile;
import com.energysys.filesource.IFileSourceFileFilter;
import com.energysys.filesource.exception.FileSourceException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

/**
 * Util class for the FileConnector.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public final class FileConnectorUtil
{

  private static final Logger LOG = Logger.getLogger(FileConnectorUtil.class.getName());

  private static final String OWNER_SYSTEM_PROPERTY = "file.connector.owner";

  private FileConnectorUtil()
  {

  }

  /**
   * Copies all files from one file source to another that match the given filename pattern and match the given file
   * filter.
   *
   * @param aSource Source file source
   * @param aDestination Destination file source
   * @param aFileNamePattern the file pattern
   * @param isDirRecursive whether to recurse sub dirs
   * @param aFileFilter the file filter
   * @return a result message.
   * @throws ConnectorException on error
   */
  public static EventResult copyNewFiles(IFileSource aSource, final IFileSource aDestination,
          final String aFileNamePattern, final Boolean isDirRecursive, final IFileSourceFileFilter aFileFilter) throws
          ConnectorException
  {
    Long myStartMillis = CurrentDateTime.getCurrentTimeInMillis();
    List<IFileSourceFile> myFilesToUpload = aSource.findFiles(aFileNamePattern, isDirRecursive,
            new IFileSourceFileFilter()
    {
      @Override
      public boolean accept(IFileSourceFile aFile)
      {
        if (aFileFilter == null || aFileFilter.accept(aFile))
        {
          FileSourceComparison myResult = aDestination.compare(aFile);
          return (myResult.getStatus() == FileSourceComparison.Status.NOT_PRESENT || myResult.getStatus()
                  == FileSourceComparison.Status.NEWER);
        }
        else
        {
          return false;
        }
      }
    });

    LOG.info("Copying Files: " + myFilesToUpload.toString());

    // Upload each file
    for (IFileSourceFile myFile : myFilesToUpload)
    {
      try (InputStream myInputStream = aSource.getInputStream(myFile))
      {
        aDestination.putContent(myInputStream, myFile);
      }
      catch (IOException | FileSourceException ex)
      {
        throw new ConnectorException("Error copying file: " + myFile.getFileId(), ex);
      }
    }
    return new EventResult("FileConnectorUtil:CopyNewFiles", EventResult.Result.SUCCESS,
            myFilesToUpload.size() + " Files Uploaded",
            "Files synced: " + myFilesToUpload.toString(), myStartMillis);
  }

}
