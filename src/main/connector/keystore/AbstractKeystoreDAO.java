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
package com.energysys.connector.keystore;

import com.energysys.connector.exception.ConnectorException;
import org.eclipse.jetty.util.security.Password;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

/**
 * Abstract class performing basic keystore operations.
 *
 * @author EnergySys Limited
 */
public class AbstractKeystoreDAO
{
  private final String theKSFileLocation;
  private final String theKSType;
  private final String theKSPassword;

  /**
   * Basic constructor.
   * @param aKSFileLocation the keystore file location
   * @param aKSType the keystore type
   */
  public AbstractKeystoreDAO(String aKSFileLocation, String aKSType) throws ConnectorException
  {
    this.theKSFileLocation = aKSFileLocation;
    this.theKSType = aKSType;
    try (FileInputStream myJettySSLInputStream = new FileInputStream("start.d/ssl.ini"))
    {
      Properties myJettySSLConfig = new Properties();
      myJettySSLConfig.load(myJettySSLInputStream);
      myJettySSLConfig.getProperty("jetty.sslContext.keyStorePassword");
      this.theKSPassword = Password.deobfuscate(myJettySSLConfig.getProperty("jetty.sslContext.keyStorePassword"));
    }
    catch (IOException myEx)
    {
      throw new ConnectorException("Error accessing credentials keystore", myEx);
    }
  }

  /**
   * Remove the certificate.
   *
   * @param anAlias the alias
   * @throws ConnectorException
   */
  public final void removeEntry(String anAlias) throws ConnectorException
  {
    KeyStore myKeyStore = getKeyStore();
    try
    {
      myKeyStore.deleteEntry(anAlias);
      storeKeyStore(myKeyStore);
    }
    catch (KeyStoreException myEx)
    {
      String myMessage = "Failed to remove certificate.";
      throw new ConnectorException(myMessage, myEx);
    }
  }

  /**
   * Gets the keystore.
   * @return the keystore
   * @throws ConnectorException on error
   */
  public final KeyStore getKeyStore() throws ConnectorException
  {
    try (FileInputStream myFileIS = new FileInputStream(theKSFileLocation))
    {
      KeyStore myKeyStore = KeyStore.getInstance(theKSType);
      myKeyStore.load(myFileIS, theKSPassword.toCharArray());
      return myKeyStore;
    }
    catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException myEx)
    {
      throw new ConnectorException("Could not load the keystore", myEx);
    }
  }

  /**
   * Create a new keystore.
   * @throws ConnectorException 
   */
  protected void createKeyStore() throws ConnectorException
  {
    try
    {
      KeyStore myKeyStore = KeyStore.getInstance(theKSType);
      myKeyStore.load(null, theKSPassword.toCharArray());
      storeKeyStore(myKeyStore);
    }
    catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException myEx)
    {
      throw new ConnectorException("Error creating Credentials Keystore", myEx);
    }
  }

  /**
   * Saves the keystore.
   * @param aKeyStore a keystore
   * @throws ConnectorException on error
   */
  protected final void storeKeyStore(KeyStore aKeyStore) throws ConnectorException
  {
    try (FileOutputStream myFileOS = new FileOutputStream(theKSFileLocation))
    {
      aKeyStore.store(myFileOS, theKSPassword.toCharArray());
    }
    catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException myEx)
    {
      String myMessage = "Could not store certificate into the keystore.";
      throw new ConnectorException(myMessage, myEx);
    }
  }

  public String getKSPassword()
  {
    return theKSPassword;
  }

  public String getKSFileLocation()
  {
    return theKSFileLocation;
  }

  public String getKSType()
  {
    return theKSType;
  }
}
