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

import com.energysys.connector.exception.ConnectorException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.energysys.connector.keystore.TrustStoreDAO;

/**
 * Makes possible runtime addition/removal of SSL certificates from the truststore.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public class ReloadableX509TrustManager implements X509TrustManager
{

  private static final Logger LOG = Logger.getLogger(ReloadableX509TrustManager.class.getName());

  private final TrustStoreDAO theTrustStoreDAO;
  private X509TrustManager theTrustManager;
  private final Boolean isTrusting;

  /**
   * Constructor.
   *
   * @param trustStoreDAO the trust store
   * @param isTrusting if set to true, any unknown certificate will be automatically loaded into the
   *    *                     keystore and trusted thereon.
   * @throws ConnectorException on error
   */
  public ReloadableX509TrustManager(TrustStoreDAO trustStoreDAO, Boolean isTrusting) throws ConnectorException
  {
    theTrustStoreDAO = trustStoreDAO;
    this.isTrusting = isTrusting;
    reloadTrustManager();
  }

  @Override
  public void checkClientTrusted(X509Certificate[] aChain, String anAuthType) throws CertificateException
  {
    theTrustManager.checkClientTrusted(aChain, anAuthType);
  }

  @Override
  @SuppressWarnings("IllegalCatch")
  public void checkServerTrusted(X509Certificate[] aChain, String anAuthType) throws CertificateException
  {
    try
    {
      // If the truststore is empty this causes an error, so add certificates
      if (theTrustStoreDAO.getKeyStore().size() == 0)
      {
        if (isTrusting)
        {
          addCertsAndReload(aChain);
        }
        else
        {
          throw new CertificateException("Certificate Trust Store is Empty and Http Connections are non trusting");
        }
      }
    }
    catch (ConnectorException | KeyStoreException ex)
    {
      throw new CertificateException(ex);
    }

    try
    {
      theTrustManager.checkServerTrusted(aChain, anAuthType);
    }
    catch (CertificateException myCertEx)
    {
      LOG.log(Level.INFO, "Certificate not trusted", myCertEx);
      if (isTrusting)
      {
        addCertsAndReload(aChain);
      }
      else
      {
        throw myCertEx;
      }
    }
  }

  private void addCertsAndReload(X509Certificate[] aChain) throws CertificateException
  {
    try
    {
      LOG.info("Adding Certificates to trust store");
      addServerCerts(aChain);
      reloadTrustManager();

      //theTrustManager.checkServerTrusted(aChain, anAuthType);
    }
    catch (ConnectorException ex)
    {
      throw new CertificateException(ex);
    }
  }

  @Override
  public X509Certificate[] getAcceptedIssuers()
  {
    return theTrustManager.getAcceptedIssuers();
  }

  private void reloadTrustManager() throws ConnectorException
  {
    try (InputStream myInputStream = new FileInputStream(theTrustStoreDAO.getKSFileLocation()))
    {
      // load keystore from specified cert store (or default)
      KeyStore myKeyStore = KeyStore.getInstance(theTrustStoreDAO.getKSType());
      myKeyStore.load(myInputStream, null);
      // initialize a new TMF with the ts we just loaded
      TrustManagerFactory myTrustSToreFactory = TrustManagerFactory.getInstance(
          TrustManagerFactory.getDefaultAlgorithm());
      myTrustSToreFactory.init(myKeyStore);
      // acquire X509 trust manager from factory
      TrustManager[] myTrustManagers = myTrustSToreFactory.getTrustManagers();
      for (int i = 0; i < myTrustManagers.length; i++)
      {
        if (myTrustManagers[i] instanceof X509TrustManager)
        {
          theTrustManager = (X509TrustManager) myTrustManagers[i];
          break;
        }
      }
      // raise exception if no trust manager found
      if (theTrustManager == null)
      {
        String myMessage = "No X509TrustManager found in TrustManagerFactory.";
        throw new ConnectorException(myMessage);
      }
    }
    catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException myEx)
    {
      String myMessage = "An error occurred loading the trust store.";
      throw new ConnectorException(myMessage, myEx);
    }
  }

  private void addServerCerts(X509Certificate[] aChain) throws ConnectorException
  {
    if (aChain != null)
    {
      TrustStoreDAO myTrustStoreDAO = new TrustStoreDAO();
      for (int i = 0; i < aChain.length; i++)
      {
        X509Certificate myCert = aChain[i];
        String myAlias = myCert.getSubjectDN().getName();
        myTrustStoreDAO.storeCertificate(myAlias, myCert);
      }
    }
  }
}
