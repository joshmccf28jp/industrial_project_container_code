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

import com.energysys.connector.connectors.dataconnector.odata.EsysOdataConnectionCredentials;
import com.energysys.connector.connectors.fileconnector.CredentialsBackedS3FileSourceConfig;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.keystore.CredentialsStoreDAO;
import com.energysys.connector.keystore.StoredCredentials;
import com.energysys.connector.web.AboutDetails;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.Manifest;

/**
 * Bean Class to access local ini file for about details page.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@ManagedBean(name = "AboutDataBean")
@ViewScoped
public class AboutDataBean extends AbstractDataListBackingBean<AboutDetails>
{
  private static final String CONNECTOR_CONFIG_FILE_ID = "ConnectorConfig.ini";

  private static final String HTTPS_PORT_KEY = "httpsport";

  private static final String CONNECTOR_NAME_KEY = "connectorname";

  private static final String CERTIFICATE_NAME_KEY = "certificatename";
  private static final String CERTIFICATE_DISTINGUISHED_NAME_KEY = "distinguishedname";

  private static final String DRIVER_NAME_KEY = "drivername";

  private static final String INSTALL_DIR_KEY = "installdir";

  private static final String URL_ALIAS = "url";
  private static final String ESYS_DATABASE_CREDENTIALS_ALIAS = "database-jdbc-cred";

  private static final String IMPLEMENTATION_VERSION = "Implementation-Version";
  private static final String META_INF_MANIFEST_MF = "/META-INF/MANIFEST.MF";


  /**
   * Default Constructor.
   **/
  @SuppressWarnings("checkstyle:OperatorWrap")
  public AboutDataBean()
  {

  }

  @Override
  protected Class getDataClass()
  {
    return AboutDetails.class;
  }

  @SuppressWarnings("checkstyle:CyclomaticComplexity")
  @Override
  protected void loadData()
  {
    ArrayList<AboutDetails> myAboutDetails = new ArrayList<>();
    try
    {
      int currId = 0;

      // Load the details from the WAR's MANIFEST file
      currId = addManifestDetails(currId, myAboutDetails);

      // Load the details from the ConnectorConfig.ini file
      currId = addConnectorConfigDetails(currId, myAboutDetails);

      // Load the details from the S3 credentials
      currId = addS3ConfigDetails(currId, myAboutDetails);

      // Load the details from the OData credentials
      currId = addODataDetails(currId, myAboutDetails);

      // Load the details from the JDBC credentials (Measurements Connector Only)
      addJDBCDetails(currId, myAboutDetails);
    }
    catch (ConnectorException e)
    {
      addGrowlMessage(e);
    }

    // Set the Data
    setData(myAboutDetails);
  }


  @Override
  protected Boolean saveUpdate(AboutDetails aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Boolean saveCreate(AboutDetails aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Boolean delete(AboutDetails aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException();
  }

  private int addJDBCDetails(int currId, ArrayList<AboutDetails> myAboutDetails) throws ConnectorException
  {
    try
    {
      CredentialsStoreDAO myDAO = new CredentialsStoreDAO();
      StoredCredentials myDbCred = myDAO.getEntry(ESYS_DATABASE_CREDENTIALS_ALIAS);
      if (myDbCred != null)
      {
        Properties myDbProperties = new Properties();
        myDbProperties.load(new StringReader(myDbCred.getCredentials()));

        myAboutDetails.add(new AboutDetails(currId++, "Database Source URL", myDbProperties.getProperty(URL_ALIAS)));
      }
    }
    catch (IOException | ConnectorException e)
    {
      throw new ConnectorException("Failed to read ConnectorConfig.ini", e);
    }
    return currId;
  }

  private int addODataDetails(int currId, ArrayList<AboutDetails> myAboutDetails) throws ConnectorException
  {
    EsysOdataConnectionCredentials myODataCred = EsysOdataConnectionCredentials.loadFromKeystore();
    if (myODataCred.getServiceURL() != null && myODataCred.getTagListObjectName() != null)
    {
      myAboutDetails.add(new AboutDetails(currId++, "OData Connection URL", myODataCred.getServiceURL()));
      myAboutDetails.add(new AboutDetails(currId++, "Tag List Object Name", myODataCred.getTagListObjectName()));
    }
    if (myODataCred.getRemoteExeuctionEnabled() && myODataCred.getRemoteExecutionConnectorName() != null)
    {
      myAboutDetails.add(
          new AboutDetails(
              currId++,
              "Remote Execution Connector Name",
              myODataCred.getRemoteExecutionConnectorName()));
    }
    return currId;
  }

  private int addS3ConfigDetails(int currId, ArrayList<AboutDetails> myAboutDetails) throws ConnectorException
  {
    List<CredentialsBackedS3FileSourceConfig> myS3CredList = CredentialsBackedS3FileSourceConfig.loadFromKeystore();
    // If there is only then just get it (and default name to Target Upload Instance)
    // Otherwise iterate over all (and identify each specifically in name)
    if (myS3CredList.size() == 1)
    {
      myAboutDetails.add(
          new AboutDetails(currId++, "Upload Target Instance", myS3CredList.get(0).getLocation()));
    }
    else
    {
      for (CredentialsBackedS3FileSourceConfig myS3Cred : myS3CredList)
      {
        myAboutDetails.add(new AboutDetails(
            currId++,
            "Upload Target: " + myS3Cred.getCredentials().substring(3),
            myS3Cred.getLocation()));
      }
    }
    return currId;
  }

  private int addManifestDetails(int currId, ArrayList<AboutDetails> myAboutDetails) throws ConnectorException
  {
    ServletContext servletContext = (ServletContext) FacesContext
        .getCurrentInstance().getExternalContext().getContext();

    try (InputStream myIn = servletContext.getResourceAsStream(META_INF_MANIFEST_MF))
    {
      if (myIn != null)
      {
        Manifest manifest = new Manifest(myIn);
        String version = manifest.getMainAttributes().getValue(IMPLEMENTATION_VERSION);

        if (version != null)
        {
          myAboutDetails.add(new AboutDetails(currId++, "Version", version));
        }
      }
    }
    catch (IOException e)
    {
      throw new ConnectorException("Failed to read MANIFEST.MF");
    }

    return currId;
  }

  private int addConnectorConfigDetails(int currId, ArrayList<AboutDetails> myAboutDetails) throws ConnectorException
  {
    try (FileInputStream myStream = new FileInputStream(CONNECTOR_CONFIG_FILE_ID))
    {
      Properties myConfigProperties = new Properties();
      myConfigProperties.load(myStream);
      myAboutDetails.add(
          new AboutDetails(currId++,
              "Connector Service Name",
              myConfigProperties.getProperty(CONNECTOR_NAME_KEY)));
      myAboutDetails.add(
          new AboutDetails(currId++,
              "HTTPS Port",
              myConfigProperties.getProperty(HTTPS_PORT_KEY)));
      myAboutDetails.add(
          new AboutDetails(currId++,
              "Certificate Name",
              myConfigProperties.getProperty(CERTIFICATE_NAME_KEY)));
      myAboutDetails.add(
          new AboutDetails(currId++,
              "Certificate Distinguished Name",
              myConfigProperties.getProperty(CERTIFICATE_DISTINGUISHED_NAME_KEY)));
      myAboutDetails.add(
          new AboutDetails(currId++,
              "Install Directory",
              myConfigProperties.getProperty(INSTALL_DIR_KEY)));
      myAboutDetails.add(
          new AboutDetails(currId++,
              "Driver Name",
              myConfigProperties.getProperty(DRIVER_NAME_KEY)));
    }
    catch (IOException e)
    {
      throw new ConnectorException("Failed to read ConnectorConfig.ini", e);
    }
    return currId;
  }

}
