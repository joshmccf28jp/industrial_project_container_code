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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.security.Password;

/**
 * This is the DAO used for configuration data access.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public class CredentialsStoreDAO extends AbstractKeystoreDAO
{

  private static final Logger LOG = Log.getLogger(CredentialsStoreDAO.class.getName());

  private static final String KS_LOCATION = System.getProperty("jetty.base") + "/etc/credentials.keystore";
  private static final String KEY_STORE_TYPE = "JCEKS";
  private static final String DEFAULT_PRIVATE_KEY =
      "1q381hnl1g821w1a16yn1uv41ky61aip1qon16yn1u9t1pjr1pif1u9p16yn1qqv1ail1l0w1uv216yn1w281g9o1hlp1q5m";

  /**
   * Create an instance of this bean.
   * @throws ConnectorException on error
   */
  public CredentialsStoreDAO() throws ConnectorException
  {
    super(KS_LOCATION, KEY_STORE_TYPE);
    File myKeystoreFile = new File(KS_LOCATION);

    if (!myKeystoreFile.exists())
    {
      createKeyStore();
    }
  }

  /**
   * Gets list of current credentials.
   * @return list
   * @throws ConnectorException on error
   */
  public List<StoredCredentials> getEntries() throws ConnectorException
  {
    KeyStore myKeyStore = getKeyStore();

    List<StoredCredentials> myCredentials = new ArrayList<>();

    Enumeration<String> myAliases;
    try
    {
      myAliases = myKeyStore.aliases();
    }
    catch (KeyStoreException myEx)
    {
      throw new ConnectorException("Error accessing credentials keystore", myEx);
    }

    while (myAliases.hasMoreElements())
    {
      String myAlias = myAliases.nextElement();
      myCredentials.add(extractCredentials(myKeyStore, myAlias, myCredentials.size()));
    }
    return myCredentials;
  }

  /**
   * Return the configuration entry.
   *
   * @param anAlias the keystore alias
   * @return the entry if found
   * @throws ConnectorException on error
   */
  public StoredCredentials getEntry(String anAlias) throws ConnectorException
  {
    String myEntry = null;
    KeyStore myKeyStore = getKeyStore();

    return extractCredentials(myKeyStore, anAlias, 0);
  }

  private StoredCredentials extractCredentials(KeyStore aKeyStore, String anAlias, Integer anId) throws
          ConnectorException
  {
    try
    {
      if (aKeyStore.containsAlias(anAlias))
      {
        String myEntry;
        SecretKeyFactory myFactory = SecretKeyFactory.getInstance("PBE");
        KeyStore.PasswordProtection myKSPP = new KeyStore.PasswordProtection(
            Password.deobfuscate("OBF:" + DEFAULT_PRIVATE_KEY).toCharArray());
        KeyStore.SecretKeyEntry mySecretKeyEntry = (KeyStore.SecretKeyEntry) aKeyStore.getEntry(anAlias, myKSPP);

        if (mySecretKeyEntry != null)
        {
          PBEKeySpec myKeySpec = (PBEKeySpec) myFactory.getKeySpec(mySecretKeyEntry.getSecretKey(), PBEKeySpec.class);
          return new StoredCredentials(anId, anAlias, new String(Base64.getDecoder().decode(new String(myKeySpec.
                  getPassword()))));
        }
      }
      return null;
    }
    catch (InvalidKeySpecException | NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException myEx)
    {
      throw new ConnectorException("Error accessing credentials keystore", myEx);
    }
  }

  /**
   * Stores a set of credentials.
   *
   * @param aStoredCredentials the stored credentials
   * @throws ConnectorException on error
   */
  public void storeEntry(StoredCredentials aStoredCredentials) throws ConnectorException
  {
    storePassword(aStoredCredentials.getName(), aStoredCredentials.getCredentials());
  }
  
  /**
   * Stores a password in the keystore.
   * @param anAlias alias for keystore entry
   * @param aPassword a password
   * @throws ConnectorException on error
   */
  public void storePassword(String anAlias, String aPassword) throws ConnectorException
  {
    KeyStore myKeyStore = getKeyStore();

    try
    {
      final byte[] myBytes = aPassword.getBytes();
      SecretKeyFactory myFactory = SecretKeyFactory.getInstance("PBE");
      SecretKey myGeneratedSecret = myFactory.generateSecret(new PBEKeySpec(Base64.getEncoder().encodeToString(myBytes).
              toCharArray()));
      KeyStore.PasswordProtection myKSPP = new KeyStore.PasswordProtection(
          Password.deobfuscate("OBF:" + DEFAULT_PRIVATE_KEY).toCharArray());
      myKeyStore.setEntry(anAlias, new KeyStore.SecretKeyEntry(myGeneratedSecret), myKSPP);
      storeKeyStore(myKeyStore);
    }
    catch (NoSuchAlgorithmException | InvalidKeySpecException | KeyStoreException myEx)
    {
      throw new ConnectorException("Error storing password", myEx);
    }
  }
}
