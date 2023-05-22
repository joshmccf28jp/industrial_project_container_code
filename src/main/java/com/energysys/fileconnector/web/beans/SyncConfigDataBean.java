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
package com.energysys.fileconnector.web.beans;

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.EventResult;
import com.energysys.connector.web.beans.AbstractGenericDAOBackingBean;
import com.energysys.filesource.s3.S3FileSource;
import com.energysys.connector.connectors.fileconnector.SyncConfiguration;
import com.energysys.filesource.ConnectionStatus;
import com.energysys.filesource.exception.InvalidCredentialsException;
import com.energysys.connector.keystore.CredentialsStoreDAO;
import com.energysys.connector.keystore.StoredCredentials;
import com.energysys.connector.connectors.fileconnector.FileConnectorUtil;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

/**
 * JSF Backing Bean for S3UploaderConfigs screen.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@ManagedBean(name = "SyncConfigDataBean")
@ViewScoped
public class SyncConfigDataBean extends AbstractGenericDAOBackingBean<SyncConfiguration>
{

  private List<String> theCredentials = null;

  /**
   * Default Constructor.
   */
  public SyncConfigDataBean()
  {

  }

  @Override
  protected Class getDataClass()
  {
    return SyncConfiguration.class;
  }

  @Override
  protected Boolean preAddProcess(SyncConfiguration aDataObject)
  {
    aDataObject.setIsDirRecursive(Boolean.TRUE);
    return true;
  }

  @Override
  @SuppressWarnings(
          {
            "checkstyle:multiplestringliterals"
          })
  protected Boolean preSaveProcess(SyncConfiguration aDataObject)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    String myLocalDir = aDataObject.getSourceConfig().getLocalDir();
    if (myLocalDir.endsWith(File.separator))
    {
      aDataObject.getSourceConfig().setLocalDir(myLocalDir.substring(0, myLocalDir.length() - 1));
    }
    // Check that local dir is valid
    File myFile = new File(myLocalDir);
    if (!myFile.isDirectory())
    {
      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":Save", EventResult.Result.FAILED,
              "Local Dir: Validation Error: Value must be an existing directory", "", myStartTime));
      return false;
    }
    else if (!myFile.canWrite())
    {
      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":Save", EventResult.Result.FAILED,
              "Local Dir: Validation Error: Must have write access to directory", "", myStartTime));
      return false;
    }

    return true;
  }

  /**
   * Auto complete directory listing.
   *
   * @param aQuery the current value
   * @return list of values
   */
  public List<String> completeLocalDir(String aQuery)
  {
    List<String> myResults = new ArrayList<>();

    if (aQuery == null || aQuery.isEmpty())
    {
      aQuery = File.separator;
    }
    if (!aQuery.startsWith(File.separator))
    {
      aQuery = File.separator + aQuery;
    }

    String myDir = aQuery.substring(0, aQuery.lastIndexOf(File.separator) + 1);
    final String myFilename = aQuery.substring(aQuery.lastIndexOf(File.separator) + 1);

    FileFilter myFileFilter = new FileFilter()
    {
      @Override
      public boolean accept(File aFile)
      {
        return !aFile.isHidden() && aFile.isDirectory() && aFile.getName().toLowerCase().startsWith(myFilename.
                toLowerCase());
      }
    };

    File myCurrDir = new File(myDir);

    if (myCurrDir.isDirectory())
    {
      File[] myFilesFound = myCurrDir.listFiles(myFileFilter);

      for (File myFileFound : myFilesFound)
      {
        try
        {
          myResults.add(myFileFound.getCanonicalPath() + File.separator);
        }
        catch (IOException ex)
        {
          Logger.getLogger(FileConnectorUtil.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }

    return myResults;
  }

  /**
   * Tests the S3 connection details.
   *
   * @param aDataObject the config to test
   */
  @SuppressWarnings(
          {
            "checkstyle:illegalcatch", "UseSpecificCatch", "checkstyle:multiplestringliterals",
            "checkstyle:missingswitchdefault"
          })
  public void testConnection(SyncConfiguration aDataObject)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      aDataObject.getDestinationConfig().loadCredentials();

      try (S3FileSource myS3FileSource = new S3FileSource(aDataObject.getDestinationConfig()))
      {
        ConnectionStatus myOpenResult = myS3FileSource.openConnection();
        switch (myOpenResult)
        {
          case OPEN:
            addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":TestConnection",
                    EventResult.Result.SUCCESS, "S3 Connection Successful", "", myStartTime));
            break;

          case CLOSED:
            addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":TestConnection",
                    EventResult.Result.EXCEPTION, "S3 Connection Failed", "", myStartTime));
            break;

          case LOCKED_OUT:
            addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":TestConnection",
                    EventResult.Result.FAILED, "S3 Connection Locked", "", myStartTime));
            break;
        }
      }
      catch (InvalidCredentialsException myEx)
      {
        addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":TestConnection",
                EventResult.Result.FAILED, "Invlaid Credentials", "", myStartTime));
      }
    }
    catch (Exception ex)
    {
      addGrowlMessage(ex);
    }
  }

  /**
   * Gets the list of available S3 credentials. Searches the credentials keystore for any aliases beginning with s3-.
   *
   * These are assumed to be correct s3 credentials.
   *
   * @return the list
   */
  @SuppressWarnings(
          {
            "checkstyle:illegalcatch", "UseSpecificCatch"
          })
  public List<String> getS3Credentials()
  {
    try
    {
      if (theCredentials == null)
      {
        theCredentials = new ArrayList<>();
        CredentialsStoreDAO myCredDAO = new CredentialsStoreDAO();

        for (StoredCredentials myCreds : myCredDAO.getEntries())
        {
          if (myCreds.getName().startsWith("s3-"))
          {
            theCredentials.add(myCreds.getName());
          }
        }
      }
      return theCredentials;
    }
    catch (Exception ex)
    {
      addGrowlMessage(ex);
      return new ArrayList<>();
    }
  }
}
