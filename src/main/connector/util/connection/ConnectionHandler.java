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
package com.energysys.connector.util.connection;

import com.energysys.connector.config.ConnectorConfig;
import com.energysys.connector.exception.ConnectorException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.energysys.connector.keystore.CertificateStoreDAO;
import com.energysys.connector.keystore.TrustStoreDAO;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.win.WinHttpClients;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;

/**
 * Handles URL connections.
 *
 * @author EnergySys Limited
 * @version $Revision$
 * Last modified by: $Author$
 */
public class ConnectionHandler 
{
  
  private static final Logger LOG = Logger.getLogger(ConnectionHandler.class.getName());

  private final ConnectorConfig connectorConfig;
  /**
   * Constructor.
   * @param aConnectorConfig
   */
  public ConnectionHandler(ConnectorConfig aConnectorConfig)
  {
    connectorConfig = aConnectorConfig;
  }
  
  /**
   * Connect to the URL and return the response as a string.
   * If a username and password are supplied, basic authentication is assumed.
   * If not, Windows Native (Kerberos) authentication is attempted.
   * @param aURLString the url
   * @param aUsername the username
   * @param aPassword the password
   * @return String response
   * @throws ConnectorException on error 
   */
  public String connect(String aURLString, String aUsername, String aPassword) throws ConnectorException
  {
    String myResponse = null;
    
    try
    {
      if (aUsername != null && aPassword != null)
      {
        myResponse = connectUsingBasicAuthentication(aURLString, aUsername, aPassword);
      }
      else
      {
        myResponse = connectUsingKerberosAuthentication(aURLString);
      }
    }
    catch (IOException myEx)
    {
      throw new ConnectorException(myEx.getMessage());
    }
    
    return myResponse;
  }
  
  private String connectUsingKerberosAuthentication(String aURLString) throws IOException, ConnectorException
  {
    StringBuilder myResponse = new StringBuilder();
    
    if (!WinHttpClients.isWinAuthAvailable())
    {
      LOG.warning("Integrated Win auth is not supported.");
    }
    
    LOG.info("Connecting to URL: " + aURLString);
    HttpGet myHttpGet = new HttpGet(encodeChars(aURLString));
    
    try (CloseableHttpClient myHttpClient = getHttpClientForKerberosAuthentication();
         CloseableHttpResponse myHttpResponse = myHttpClient.execute(myHttpGet))
    {
      LOG.info("Got response line: " + myHttpResponse.getReasonPhrase());
      BufferedReader myReader = new BufferedReader(new InputStreamReader(myHttpResponse.getEntity().getContent()));
      String myLine = null;
      while ((myLine = myReader.readLine()) != null)
      {
        myResponse.append(myLine).append("\n");
      }
      LOG.fine(myResponse.toString());
    }
    
    return myResponse.toString();
  }
  
  private CloseableHttpClient getHttpClientForKerberosAuthentication() throws ConnectorException
  {
    CloseableHttpClient myHttpClient = null;
    TrustStoreDAO myTrustDAO = new TrustStoreDAO();
    CertificateStoreDAO myCertDAO = new CertificateStoreDAO();
    
    try
    {
      //Trust own CA and all self-signed certs
      SSLContext mySSLContext = SSLContexts.custom()
              .loadTrustMaterial(myTrustDAO.getKeyStore(), new TrustSelfSignedStrategy())
              .loadKeyMaterial(myCertDAO.getKeyStore(), myCertDAO.getKSPassword().toCharArray())
              .build();

      SSLConnectionSocketFactory mySSLSocketFactory = SSLConnectionSocketFactoryBuilder.create()
              .setSslContext(mySSLContext)
              .setHostnameVerifier(new NoopHostnameVerifier())
              .build();

      PoolingHttpClientConnectionManager myConnectionManager = PoolingHttpClientConnectionManagerBuilder.create()
              .setSSLSocketFactory(mySSLSocketFactory)
              .build();

      
      RequestConfig myRequestConfig =
              RequestConfig.custom()
                      .setConnectTimeout(Timeout.ofSeconds(connectorConfig.getHttpConnectionTimeout()))
                      .setResponseTimeout(Timeout.ofSeconds(connectorConfig.getHttpReadTimeout()))
                      .build();

      myHttpClient = WinHttpClients
              .custom()
              .setConnectionManager(myConnectionManager)
              .setDefaultRequestConfig(myRequestConfig)
              .build();

    }
    catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException myEx)
    {
      throw new ConnectorException(myEx.getMessage());
    }
    
    return myHttpClient;
  }
  
  private String connectUsingBasicAuthentication(String aURLString, String aUsername, String aPassword)
          throws ConnectorException
  {
    try 
    {
      TrustStoreDAO myTrustDAO = new TrustStoreDAO();
      //Construct an SSLContext to wrap the ReloadableX509TrustManager
      TrustManager[] myTrustManagers = new TrustManager[] 
      {
        new ReloadableX509TrustManager(myTrustDAO, connectorConfig.isHttpConnectionTrusting()),
      };

      SSLContext mySSLContext = SSLContext.getInstance("SSL");
      mySSLContext.init(null, myTrustManagers, null);
      return connectUsingBasicAuthentication(aURLString, aUsername, aPassword,
              mySSLContext);
    } 
    catch (NoSuchAlgorithmException | KeyManagementException myEx) 
    {
      String myMessage = "An error occurred connecting to the URL: " + aURLString;
      throw new ConnectorException(myMessage, myEx);
    }
  }
  
  private String connectUsingBasicAuthentication(String aURLString, String aUsername, String aPassword, 
          SSLContext anSSLContext) throws ConnectorException
  {
    LOG.info("Connecting to URL: " + aURLString);
    try 
    {
      // Create a client builder
      HttpClient.Builder builder = HttpClient.newBuilder()
          .authenticator(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(aUsername, aPassword.toCharArray());
            }
          })
          .sslContext(anSSLContext)
          .connectTimeout(Duration.of(connectorConfig.getHttpConnectionTimeout(), ChronoUnit.MILLIS));

      //Set the SSLContext, if required
      if (anSSLContext != null)
      {
        builder = builder.sslContext(anSSLContext);
      }

      // Set the proxy, if required
      if (connectorConfig.getHttpsProxyHost() != null && connectorConfig.getHttpsProxyPort() != null)
      {
        ProxySelector selector = ProxySelector.of(new InetSocketAddress(
            connectorConfig.getHttpsProxyHost(), connectorConfig.getHttpsProxyPort()));
        builder = builder.proxy(selector);
      }

      // Build the client and request
      HttpClient client = builder.build();
      HttpRequest request = HttpRequest.newBuilder()
          .GET()
          .uri(new URI(encodeChars(aURLString)))
          .timeout(Duration.of(connectorConfig.getHttpConnectionTimeout(), ChronoUnit.MILLIS))
          .build();

      // Send the request
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != HttpStatus.SC_OK)
      {
        LOG.info("Response Status: " + response.statusCode());
        LOG.info("Response Body: " + response.body());
        throw new ConnectorException("Bad response status: " + response.statusCode());
      }
      LOG.fine(response.body());
      return response.body();
    }
    catch (IOException | URISyntaxException myEx)
    {
      String myMessage = "Failed to connect to URL: " + aURLString;
      throw new ConnectorException(myMessage, myEx);
    }
    catch (InterruptedException myEx)
    {
      Thread.currentThread().interrupt();
      throw new ConnectorException("Interrupted Exception when connecting to URL: " + aURLString);
    }

  }
  
  private String encodeChars(String aURLString) throws UnsupportedEncodingException
  {
    String myEncodedString = null;
    //Add replacements here
    myEncodedString = aURLString.replace("+", URLEncoder.encode("+", "UTF-8"));
    return myEncodedString;
  }

}
