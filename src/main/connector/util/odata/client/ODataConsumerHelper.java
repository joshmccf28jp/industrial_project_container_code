/*
 *  Copyright 2014 EnergySys Limited. All Rights Reserved.
 * 
 *  This software is the proprietary information of EnergySys Limited.
 *  Use is subject to licence terms.
 *  This software is not designed or supplied to be used in circumstances where
 *  personal injury, death or loss of or damage to property could result from any
 *  defect in the software.
 *  In no event shall the developers or owners be liable for personal injury,
 *  death or loss or damage to property, loss of business, revenue, profits, use,
 *  data or other economic advantage or for any indirect, punitive, special,
 *  incidental, consequential or exemplary loss or damage resulting from the use
 *  of the software or documentation.
 *  Developer and owner make no warranties, representations or undertakings of
 *  any nature in relation to the software and documentation.
 */
package com.energysys.connector.util.odata.client;

import com.energysys.connector.config.ConnectorConfig;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.keystore.CertificateStoreDAO;
import com.energysys.connector.keystore.TrustStoreDAO;
import com.energysys.connector.util.connection.ReloadableX509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.edm.EdmEntityContainer;
import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.ep.EntityProviderReadProperties;
import org.apache.olingo.odata2.api.ep.EntityProviderWriteProperties;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.apache.olingo.odata2.api.processor.ODataResponse;

/**
 * This class is responsible for providing utility methods to connect to an OData service.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public final class ODataConsumerHelper
{

  private static final Logger LOG = Logger.getLogger(ODataConsumerHelper.class.getName());
  private static final String SSL = "SSL";
  private static final String SUN_X509 = "SunX509";
  private static final String HTTP_METHOD_PUT = "PUT";
  private static final String HTTP_METHOD_POST = "POST";
  private static final String HTTP_METHOD_GET = "GET";
  private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
  private static final String HTTP_HEADER_ACCEPT = "Accept";
  private static final String SEPARATOR = "/";
  private static final String SPACE = " ";
  private static final String SPACE_URL = "%20";
  private static final String QUOTE = "'";
  private static final String QUOTE_URL = "%27";
  private static final String COLON = ":";
  private static final String COLON_URL = "%3A";
  private static final String METADATA = "$metadata";
  private static final String APPLICATION_XML = "application/xml";

  private HttpsURLConnection theHttpsURLConnection;

  private ConnectorConfig connectorConfig;

  /**
   * Constructor.
   * @param aConnectorConfig
   */
  public ODataConsumerHelper(ConnectorConfig aConnectorConfig)
  {
    connectorConfig = aConnectorConfig;
  }

  /**
   * Read feed document in the requested content type for the provided service uri, entity set name, entity id, path and
   * query string.
   *
   * @param aServiceUri a service uri
   * @param anEntitySetName an entity name
   * @param anId an entity id
   * @param aPath a path
   * @param aQueryString a query string
   * @param aContentType a content type
   * @return an ODataFeed object
   * @throws ConnectorException on error
   */
  public ODataFeed readFeed(String aServiceUri, String anEntitySetName, String anId, String aPath,
          String aQueryString, String aContentType)
          throws ConnectorException
  {
    ODataFeed myODataFeed = null;

    try
    {
      // get Metadata
      Edm myEdm = readEdm(aServiceUri);
      // create URL
      String myURL = createUri(aServiceUri, anEntitySetName, anId, aPath, aQueryString);
      LOG.info("Requesting OData URL: " + myURL);
      // get content
      InputStream myContent = execute(myURL, aContentType, HTTP_METHOD_GET);
      // get feed
      EdmEntityContainer myEntityContainer = myEdm.getDefaultEntityContainer();
      myODataFeed = EntityProvider.readFeed(aContentType,
              myEntityContainer.getEntitySet(anEntitySetName),
              myContent, EntityProviderReadProperties.init().build());
      LOG.fine("Fine Message Test");
    }
    catch (EdmException | EntityProviderException ex)
    {
      throw new ConnectorException("Failed to read feed.", ex);
    }
    finally
    {
      if (theHttpsURLConnection != null)
      {
        theHttpsURLConnection.disconnect();
      }
    }

    return myODataFeed;
  }
/**
   * Read feed document in the requested content type for the provided service uri, entity set name, entity id, path and
   * query string.
   *
   * @param aServiceUri a service uri
   * @param anEntitySetName an entity name
   * @param anId an entity id
   * @param aPath a path
   * @param aQueryString a query string
   * @param aContentType a content type
   * @return an ODataFeed object
   * @throws ConnectorException on error
   */
  public ODataEntry readEntry(String aServiceUri, String anEntitySetName, String anId, String aPath,
          String aQueryString, String aContentType)
          throws ConnectorException
  {
    ODataEntry myODataEntry = null;

    try
    {
      // get Metadata
      Edm myEdm = readEdm(aServiceUri);
      // create URL
      String myURL = createUri(aServiceUri, anEntitySetName, anId, aPath, aQueryString);
      // get content
      InputStream myContent = execute(myURL, aContentType, HTTP_METHOD_GET);
      // get feed
      EdmEntityContainer myEntityContainer = myEdm.getDefaultEntityContainer();
      myODataEntry = EntityProvider.readEntry(aContentType,
              myEntityContainer.getEntitySet(anEntitySetName),
              myContent, EntityProviderReadProperties.init().build());
    }
    catch (EdmException | EntityProviderException ex)
    {
      throw new ConnectorException("Failed to read entry.", ex);
    }
    finally
    {
      if (theHttpsURLConnection != null)
      {
        theHttpsURLConnection.disconnect();
      }
    }

    return myODataEntry;
  }

  private String createUri(String aServiceUri, String anEntitySetName, String anId, String aPath,
          String aQueryString)
  {
    final StringBuilder myAbsoluteUri = new StringBuilder(aServiceUri).append(SEPARATOR).append(anEntitySetName);
    if (anId != null)
    {
      myAbsoluteUri.append("(").append(encodeURL(anId)).append(")");
    }
    if (aPath != null)
    {
      myAbsoluteUri.append(SEPARATOR).append(encodeURL(aPath));
    }
    if (aQueryString != null)
    {
      myAbsoluteUri.append("?").append(encodeURL(aQueryString));
    }
    return myAbsoluteUri.toString();
  }

  private InputStream execute(String aURL, String aContentType, String anHttpMethod) throws ConnectorException
  {
    InputStream myContent = null;
    try
    {
      LOG.info("Requesting URL: " + aURL);
      // initialise connection
      theHttpsURLConnection = initialiseConnection(aURL, aContentType, anHttpMethod);
      // connect
      theHttpsURLConnection.connect();
      LOG.finest(theHttpsURLConnection.getSSLSocketFactory().toString());
      // check status code
      checkStatus(theHttpsURLConnection);
      // retrieve content
      myContent = theHttpsURLConnection.getInputStream();
      // log document
      myContent = logRawContent("Retrieved data: ", myContent, "\n");
    }
    catch (IOException myEx)
    {
      throw new ConnectorException("OData request execution failed.", myEx);
    }
    return myContent;
  }

  /**
   * Initialise connection and set authentication properties.
   */
  @SuppressWarnings({"checkstyle:illegalcatch", "UseSpecificCatch"})
  private HttpsURLConnection initialiseConnection(String anAbsoluteUri, String aContentType, String anHttpMethod)
          throws ConnectorException
  {
    try
    {
      // open connection
      URL myURL = new URL(anAbsoluteUri);
      if (connectorConfig.getHttpsProxyHost() != null && connectorConfig.getHttpsProxyPort() != null)
      {
        Proxy webProxy
                = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(connectorConfig.getHttpsProxyHost(), connectorConfig.getHttpsProxyPort()));
        theHttpsURLConnection = (HttpsURLConnection) myURL.openConnection(webProxy);

      }
      else
      {
        theHttpsURLConnection = (HttpsURLConnection) myURL.openConnection();
      }
      theHttpsURLConnection.setConnectTimeout(connectorConfig.getHttpConnectionTimeout());
      theHttpsURLConnection.setReadTimeout(connectorConfig.getHttpReadTimeout());

      // construct an Trust Manager for the SSLContext to wrap the ReloadableX509TrustManager
      TrustStoreDAO myTrustDAO = new TrustStoreDAO();
      CertificateStoreDAO myCertDAO = new CertificateStoreDAO();
      TrustManager[] myTrustManagers = new TrustManager[]
      {
        new ReloadableX509TrustManager(myTrustDAO, connectorConfig.isHttpConnectionTrusting()),
      };

      // construct a KeyManagerFactory for the certificate keystore
      KeyManagerFactory myKeyManagerFactory = KeyManagerFactory.getInstance(SUN_X509);
      KeyStore myKeyStore = myCertDAO.getKeyStore();
      myKeyManagerFactory.init(myKeyStore, myCertDAO.getKSPassword().toCharArray());

      // initialise SSL context
      SSLContext mySSLContext = SSLContext.getInstance(SSL);
      mySSLContext.init(myKeyManagerFactory.getKeyManagers(), myTrustManagers, new SecureRandom());
      SSLContext.setDefault(mySSLContext);
      // set SSL context socket factory on http connection
      theHttpsURLConnection.setSSLSocketFactory(mySSLContext.getSocketFactory());
      // set request method
      theHttpsURLConnection.setRequestMethod(anHttpMethod);
      // set request property
      theHttpsURLConnection.setRequestProperty(HTTP_HEADER_ACCEPT, aContentType);
      if (HTTP_METHOD_POST.equals(anHttpMethod) || HTTP_METHOD_PUT.equals(anHttpMethod))
      {
        theHttpsURLConnection.setDoOutput(true);
        theHttpsURLConnection.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, aContentType);
      }
    }
    catch (Exception myEx)
    {
      String myMessage = "Failed to initialise the OData connection.";
      throw new ConnectorException(myMessage, myEx);
    }
    return theHttpsURLConnection;
  }

  /**
   * Check connection status codes and throw exception if error code is found.
   */
  private void checkStatus(HttpsURLConnection aConnection) throws ConnectorException
  {
    try
    {
      HttpStatusCodes myHttpStatusCode = HttpStatusCodes.fromStatusCode(aConnection.getResponseCode());
      if (400 <= myHttpStatusCode.getStatusCode() && myHttpStatusCode.getStatusCode() <= 599)
      {
        throw new ConnectorException("Http Connection failed with status " + myHttpStatusCode.getStatusCode()
                + " " + myHttpStatusCode.toString());
      }
    }
    catch (IOException ex)
    {
      throw new ConnectorException("Failed to check connection status", ex);
    }
  }

  /**
   * Simple URL encoding replacing spaces, quotes and colon characters.
   */
  private String encodeURL(String aURL)
  {
    String myEncodedURL = aURL.replaceAll(SPACE, SPACE_URL).replaceAll(QUOTE, QUOTE_URL).replaceAll(COLON, COLON_URL);
    return myEncodedURL;
  }

  /**
   * Method to convert an input stream to a byte array.
   *
   * @param aStream an input stream
   * @return a byte array
   * @throws ConnectorException on error
   */
  public static byte[] streamToArray(InputStream aStream) throws ConnectorException
  {
    byte[] myResult = new byte[0];
    try
    {
      int myCount = 0;
      ByteArrayOutputStream myBytesToEncode = new ByteArrayOutputStream();
      // Set the buffer size the same as the default for java.io.BufferedInputStream
      byte[] myBuffer = new byte[8 * 1024];
      while ((myCount = aStream.read(myBuffer)) >= 0)
      {
        myBytesToEncode.write(myBuffer, 0, myCount);
      }
      myResult = myBytesToEncode.toByteArray();
      aStream.close();
    }
    catch (IOException myEx)
    {
      throw new ConnectorException("Failed to convert stream to array.", myEx);
    }
    return myResult;
  }

  /**
   * Read the metadata document for the provided service URL.
   *
   * @param aServiceURL a service URL
   * @return an Edm instance for the metadata
   * @throws ConnectorException on error
   */
  public Edm readEdm(String aServiceURL) throws ConnectorException
  {
    Edm myMetadata = null;
    try
    {
      InputStream myContent = execute(aServiceURL + SEPARATOR + METADATA, APPLICATION_XML, HTTP_METHOD_GET);
      myMetadata = EntityProvider.readMetadata(myContent, false);
    }
    catch (EntityProviderException myEx)
    {
      throw new ConnectorException("Failed to read metadata for service " + aServiceURL, myEx);
    }
    return myMetadata;
  }

  /**
   * Create an entry using the provided metadata, service uri, entity set name and properties data.
   *
   * @param anEdm a metadata
   * @param aServiceUri a service uri
   * @param aContentType a content type
   * @param anEntitySetName an entity name
   * @param someData the data to use to construct the entry
   * @return a DOM Document containing the updated entry
   * @throws ConnectorException on error
   */
  public ODataEntry createEntry(Edm anEdm, String aServiceUri, String aContentType,
          String anEntitySetName, Map<String, Object> someData) throws ConnectorException
  {
    String myAbsolutUri = createUri(aServiceUri, anEntitySetName, null, null, null);
    return writeEntity(anEdm, myAbsolutUri, anEntitySetName, someData, aContentType, HTTP_METHOD_POST);
  }

  private ODataEntry writeEntity(Edm anEdm, String anAbsoluteUri, String anEntitySetName,
          Map<String, Object> someData, String aContentType, String anHttpMethod)
          throws ConnectorException
  {
    ODataEntry myODataEntry = null;
    try
    {
      theHttpsURLConnection = initialiseConnection(anAbsoluteUri, aContentType, anHttpMethod);

      EdmEntityContainer myEntityContainer = anEdm.getDefaultEntityContainer();
      EdmEntitySet myEntitySet = myEntityContainer.getEntitySet(anEntitySetName);
      URI myRootUri = new URI(anEntitySetName);

      EntityProviderWriteProperties myProperties = EntityProviderWriteProperties.serviceRoot(myRootUri).build();
      // serialize data into ODataResponse object
      ODataResponse myResponse = EntityProvider.writeEntry(aContentType, myEntitySet, someData, myProperties);
      // get (http) entity which is for default Olingo implementation an InputStream
      Object myEntity = myResponse.getEntity();
      if (myEntity instanceof InputStream)
      {
        byte[] myBuffer = streamToArray((InputStream) myEntity);
        theHttpsURLConnection.getOutputStream().write(myBuffer);
      }

      // if a entity is created (via POST request) the response body contains the new created entity
      HttpStatusCodes myStatusCode = HttpStatusCodes.fromStatusCode(theHttpsURLConnection.getResponseCode());
      if (myStatusCode == HttpStatusCodes.CREATED)
      {
        // get the content as InputStream and de-serialize it into an ODataEntry object
        InputStream myContent = theHttpsURLConnection.getInputStream();
        // get entry
        myODataEntry = EntityProvider.readEntry(aContentType,
                myEntityContainer.getEntitySet(anEntitySetName),
                myContent, EntityProviderReadProperties.init().build());
        theHttpsURLConnection.disconnect();
      }
      else
      {
        theHttpsURLConnection.disconnect();
        throw new ConnectorException("Unexpected Http Response from submit: " + myStatusCode.getInfo());
      }
    }
    catch (IOException | EdmException | URISyntaxException | EntityProviderException myEx)
    {
      throw new ConnectorException("Failed to write entity", myEx);
    }
    return myODataEntry;
  }

  private InputStream logRawContent(String aPrefix, InputStream aContent, String aPostfix)
          throws ConnectorException
  {
    InputStream myContent = aContent;
    try
    {
      if (LOG.isLoggable(Level.FINEST))
      {
        byte[] myBuffer = streamToArray(aContent);
        aContent.close();

        LOG.finest(aPrefix + new String(myBuffer) + aPostfix);
        myContent = new ByteArrayInputStream(myBuffer);
      }
    }
    catch (IOException ex)
    {
      throw new ConnectorException("Failed to log raw content.", ex);
    }
    return myContent;
  }
}
