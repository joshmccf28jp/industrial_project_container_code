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
package com.energysys.connector.keystore;

import com.energysys.connector.exception.ConnectorException;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is the DAO used for certificate storage.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public class TrustStoreDAO extends AbstractKeystoreDAO
{
  /**
   * Trust store file location.
   */
  public static final String KS_LOCATION = System.getProperty("jetty.base") + "/etc/trust.keystore";

  private static final Logger LOG = Logger.getLogger(TrustStoreDAO.class.getName());

  /**
   * Create an instance of this bean.
   *
   * @throws com.energysys.connector.exception.ConnectorException
   */
  public TrustStoreDAO() throws ConnectorException
  {
    super(KS_LOCATION, "JKS");
    File myKeystoreFile = new File(KS_LOCATION);

    if (!myKeystoreFile.exists())
    {
      createKeyStore();
    }
  }

  /**
   * Get the map of aliases and certificates.
   *
   * @return map of aliases and certificates
   * @throws com.energysys.connector.exception.ConnectorException on error
   */
  public List<StoredCertificate> getCertificates() throws ConnectorException
  {
    List<StoredCertificate> myCertificates = new ArrayList<>();
    KeyStore myKeyStore = getKeyStore();

    try
    {
      Enumeration<String> myAliases = myKeyStore.aliases();
      while (myAliases.hasMoreElements())
      {
        String myAlias = myAliases.nextElement();
        Certificate myCert = myKeyStore.getCertificate(myAlias);
        myCertificates.add(new StoredCertificate(myCertificates.size(), myAlias, myCert));
      }
    }
    catch (KeyStoreException myEx)
    {
      throw new ConnectorException("Impossible to get map of certificates", myEx);
    }
    return myCertificates;
  }

  /**
   * Return the certificate.
   *
   * @param anAlias the keystore alias
   * @return the entry if found
   * @throws com.energysys.connector.exception.ConnectorException
   */
  public StoredCertificate getCertificate(String anAlias) throws ConnectorException
  {
    KeyStore myKeyStore = getKeyStore();

    try
    {
      if (myKeyStore.containsAlias(anAlias))
      {
        return new StoredCertificate(0, anAlias, myKeyStore.getCertificate(anAlias));
      }
      else
      {
        return null;
      }
    }
    catch (KeyStoreException myEx)
    {
      throw new ConnectorException("Impossible to get certificate for alias " + anAlias, myEx);
    }
  }

  /**
   * Store the certificate.
   *
   * @param anAlias the alias
   * @param aCertInputStream certificate
   * @throws com.energysys.connector.exception.ConnectorException
   */
  public void storeCertificate(String anAlias, InputStream aCertInputStream) throws ConnectorException
  {
    storeCertificate(anAlias, aCertInputStream, "X.509");
  }

  /**
   * Store the certificate.
   *
   * @param anAlias the alias
   * @param aCertInputStream certificate
   * @param aType the type
   * @throws com.energysys.connector.exception.ConnectorException
   */
  public void storeCertificate(String anAlias, InputStream aCertInputStream, String aType) throws ConnectorException
  {
    try
    {
      CertificateFactory myCertFactory = CertificateFactory.getInstance(aType);
      Certificate myCert = myCertFactory.generateCertificate(aCertInputStream);
      storeCertificate(anAlias, myCert);
    }
    catch (CertificateException myEx)
    {
      String myMessage = "Failed to store certificate.";
      throw new ConnectorException(myMessage, myEx);
    }
  }

  /**
   * Store the certificate.
   *
   * @param anAlias the alias
   * @param aCert the cert to be stored
   * @throws com.energysys.connector.exception.ConnectorException
   */
  public void storeCertificate(String anAlias, Certificate aCert) throws ConnectorException
  {
    KeyStore myKeyStore = getKeyStore();

    try
    {
      myKeyStore.setCertificateEntry(anAlias, aCert);
      storeKeyStore(myKeyStore);
    }
    catch (KeyStoreException myEx)
    {
      String myMessage = "Failed to store certificate.";
      throw new ConnectorException(myMessage, myEx);
    }
  }

}
