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
package com.energysys.encryption;

import com.energysys.connector.exception.ConnectorException;

/**
 * Utils Class that exposes the protected decrypt method on the EnergySysLibrary Encryption class.
 * @author EnergySys Limited
 */
public final class EncryptionUtils
{

  /**
   * Default Constructor.
   */
  private EncryptionUtils()
  {
    
  }
  
  /**
   * Decrypts a string using a given password.
   * @param anEncryptedString the encrypted string
   * @param aPassword the password
   * @return the decrypted string
   * @throws ConnectorException on error
   */
  public static String decryptString(String anEncryptedString, String aPassword) throws ConnectorException
  {
    try
    {
      String myTransform = "AES/CBC/PKCS5Padding";
      String myDecryptedString = Encryption.decrypt(anEncryptedString, aPassword, myTransform, 16);
      return myDecryptedString;
    }
    catch (EncryptionException ex)
    {
      throw new ConnectorException("Failed decrypt String", ex);
    }
  }
}
