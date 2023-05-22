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
package com.energysys.connector.web.beans;

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.EventResult;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.keystore.CredentialsStoreDAO;
import com.energysys.connector.keystore.StoredCredentials;
import com.energysys.encryption.EncryptionUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;


/**
 * This is the session scoped backing bean used for configuration.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
@ManagedBean(name = "CredentialsConfigurationBean")
@ViewScoped
public class CredentialsConfigurationBean extends AbstractDataListBackingBean<StoredCredentials>
{
  /**
   * Text pre-pended to credentials files if they were decrypted.
   * (Used to prevent download)
   */
  public static final String WAS_ENCRYPTED = "##WAS_ENCRYPTED##";
  private static final String UPLOAD_TEXT = ":Upload";
  private String thePassword;

  private UploadedFile theFile;

  /**
   * Create an instance of this bean.
   */
  public CredentialsConfigurationBean()
  {
  }

  public String getPassword()
  {
    return thePassword;
  }

  public void setPassword(String aPassword)
  {
    this.thePassword = aPassword;
  }

  @Override
  protected Class getDataClass()
  {
    return StoredCredentials.class;
  }

  @Override
  protected Boolean saveUpdate(StoredCredentials aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected Boolean saveCreate(StoredCredentials aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  protected Boolean delete(StoredCredentials aDataObject) throws ConnectorException
  {
    CredentialsStoreDAO myDAO = new CredentialsStoreDAO();
    myDAO.removeEntry(aDataObject.getName());
    loadData();
    return true;
  }

  @Override
  protected void loadData()
  {
    try
    {
      CredentialsStoreDAO myDAO = new CredentialsStoreDAO();
      List<StoredCredentials> myStoredCerts = myDAO.getEntries();
      setData(myStoredCerts);
    }
    catch (ConnectorException myEx)
    {
      addGrowlMessage(myEx);
    }
  }

  /**
   * Decrypts the currently selected credentials.
   */
  public void decryptCreds()
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      System.out.println("Decrypt and add file: " + getSelectedRow().getName() + "|" + thePassword);

      if (!getSelectedRow().getName().endsWith(".crd"))
      {
        throw new ConnectorException("Invalid credentials. Must end with .crd");
      }

      String myNewAlias = getSelectedRow().getName().substring(0, getSelectedRow().getName().length() - 4);

      // Decrypt and prepend WAS_ENCRYPTED comment to mark as non downloadable.
      String myDecryptedCreds = WAS_ENCRYPTED + "\n"
              + EncryptionUtils.decryptString(getSelectedRow().getCredentials(), thePassword);

      CredentialsStoreDAO myDAO = new CredentialsStoreDAO();
      myDAO.removeEntry(getSelectedRow().getName());

      getSelectedRow().setName(myNewAlias);
      getSelectedRow().setCredentials(myDecryptedCreds);
      myDAO.storeEntry(getSelectedRow());

      loadData();

      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":DecryptCredentials",
              EventResult.Result.SUCCESS,
              "Credentials Decrypted", "", myStartTime));
    }
    catch (ConnectorException myEx)
    {
      addGrowlMessage(myEx);
    }
  }

  /**
   * UI event for uploading credentials.
   *
   * @param anEvent the FileUplaodEvent
   */
  public synchronized void onUpload(FileUploadEvent anEvent)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      theFile = anEvent.getFile();
      String myFileName = theFile.getFileName();

      if (theFile.getSize() == 0)
      {
        addGrowlMessage(new EventResult(getDataClass().getSimpleName() + UPLOAD_TEXT, EventResult.Result.FAILED,
                "Invalid Credentials File", "File has no content", myStartTime));
        return;
      }
      if (myFileName.endsWith(".properties"))
      {
        myFileName = myFileName.substring(0, myFileName.length() - 11);
      }
      else if (!myFileName.endsWith(".crd"))
      {
        addGrowlMessage(new EventResult(getDataClass().getSimpleName() + UPLOAD_TEXT, EventResult.Result.FAILED,
                "Invalid Credentials File", "Only .properties or .crd files may be uploaded", myStartTime));
        return;
      }

      CredentialsStoreDAO myDAO = new CredentialsStoreDAO();
      final StoredCredentials myCreds = new StoredCredentials(getData().size(), myFileName, new String(
              theFile.getContent()));
      myDAO.storeEntry(myCreds);
      loadData();

      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + UPLOAD_TEXT, EventResult.Result.SUCCESS,
              "Credentials Uploaded", "", myStartTime));
    }
    catch (ConnectorException ex)
    {
      addGrowlMessage(ex);
    }
  }

  /**
   * UI event for downloading credentials.
   *
   * @return the streamed file
   */
  @SuppressWarnings(
          {
            "checkstyle:illegalcatch", "UseSpecificCatch"
          })
  public StreamedContent getDownload()
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      String credentials = getSelectedRow().getCredentials();

      // Check file was not originally encrypted
      // (buttons in UI are actually disabled so should not reach this code, but check anyway)
      if (credentials.startsWith(WAS_ENCRYPTED) || getSelectedRow().getName().contains("s3"))
      {
        addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":Download", EventResult.Result.FAILED,
                "Encrypted Credentials File", "Only unencrypted files may be downloaded", myStartTime));
        return null;
      }
      InputStream myStream = new ByteArrayInputStream(credentials.getBytes("UTF-8"));
      return DefaultStreamedContent.builder()
              .name(getSelectedRow().getName() + ".properties")
              .contentType("application/properties-file")
              .stream(() -> myStream)
              .build();
    }
    catch (Exception myEx)
    {
      addGrowlMessage(myEx);
      return null;
    }
  }
}
