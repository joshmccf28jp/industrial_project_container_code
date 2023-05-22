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
package com.energysys.connector.connectors.fileconnector;

import com.energysys.connector.exception.ConnectorException;
import com.energysys.filesource.s3.IS3FileSourceConfig;
import com.energysys.connector.keystore.CredentialsStoreDAO;
import com.energysys.connector.keystore.StoredCredentials;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.services.s3.model.Tag;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Config class for an S3 File Source that is based on a set of credentials stored in the CredentialsStoreDAO.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@Embeddable
public class CredentialsBackedS3FileSourceConfig implements IS3FileSourceConfig, AwsCredentials, Serializable
{
  /**
   * PROPERTY KEY VALUES.
   */
  private static final String PROPERTY_LOCATION = "location";
  private static final String PROPERTY_OWNER = "owner";
  private static final String PROPERTY_BUCKET_NAME = "bucket-name";
  private static final String PROPERTY_IS_LOCK_REQUIRED = "is-lock-required";
  private static final String PROPERTY_AWS_SECRET_KEY = "aws-secret-key";
  private static final String PROPERTY_AWS_ACCESS_KEY_ID = "aws-access-key-id";
  private static final String PROPERTY_AWS_REGION = "aws-region";
  private static final String PROPERTY_TAG_PREFIX = "tag.";

  /**
   * DEFAULT VALUES FOR PROPERTIES.
   */
  private static final String DEFAULT_IS_LOCK_REQUIRED = "false";
  private String theCredentials;

  @Transient
  private String theLocation;

  @Transient
  private String theOwner;

  @Transient
  private String theAWSAccessKeyId;

  @Transient
  private String theAWSSecretKey;

  @Transient
  private Boolean isLockRequired = Boolean.FALSE;

  @Transient
  private String theBucketName;

  @Transient
  private String theAWSRegion;

  @Transient
  private List<Tag> theTags = new ArrayList<>();

  /**
   * Default Constructor.
   */
  public CredentialsBackedS3FileSourceConfig()
  {
  }

  /**
   * Loads the credentials from the keystore.
   *
   * @throws ConnectorException on error
   */
  @SuppressWarnings("checkstyle:booleanexpressioncomplexity")
  public void loadCredentials() throws ConnectorException
  {
    CredentialsStoreDAO myDAO = new CredentialsStoreDAO();
    StoredCredentials myCredentials = myDAO.getEntry(theCredentials);
    if (myCredentials == null)
    {
      throw new ConnectorException("S3 credentials not found: " + theCredentials);
    }

    loadFromProperties(myCredentials.getCredentials());
  }

  private void loadFromProperties(String myCredentialsString) throws ConnectorException
  {
    Properties myProps = new Properties();
    try
    {
      myProps.load(new StringReader(myCredentialsString));
    }
    catch (IOException ex)
    {
      throw new ConnectorException("S3 credentials invalid: " + theCredentials, ex);
    }

    for (String myCurrKey : myProps.stringPropertyNames())
    {
      switch (myCurrKey)
      {
        case PROPERTY_LOCATION:
          theLocation = myProps.getProperty(PROPERTY_LOCATION);
          break;
        case PROPERTY_OWNER:
          theOwner = myProps.getProperty(PROPERTY_OWNER);
          break;
        case PROPERTY_BUCKET_NAME:
          theBucketName = myProps.getProperty(PROPERTY_BUCKET_NAME);
          break;
        case PROPERTY_IS_LOCK_REQUIRED:
          isLockRequired = Boolean.valueOf(myProps.getProperty(PROPERTY_IS_LOCK_REQUIRED, DEFAULT_IS_LOCK_REQUIRED));
          break;
        case PROPERTY_AWS_SECRET_KEY:
          theAWSSecretKey = myProps.getProperty(PROPERTY_AWS_SECRET_KEY);
          break;
        case PROPERTY_AWS_ACCESS_KEY_ID:
          theAWSAccessKeyId = myProps.getProperty(PROPERTY_AWS_ACCESS_KEY_ID);
          break;
        case PROPERTY_AWS_REGION:
          theAWSRegion = myProps.getProperty(PROPERTY_AWS_REGION);
          break;
        default:
          if (myCurrKey.startsWith(PROPERTY_TAG_PREFIX))
          {
            theTags.add(Tag.builder()
                    .key(myCurrKey.substring(4))
                    .value(myProps.getProperty(myCurrKey))
                    .build());
          }
      }
    }

    validate();
  }

  @SuppressWarnings("BooleanExpressionComplexity")
  private void validate() throws ConnectorException
  {
    if (theLocation == null || theOwner == null || theBucketName == null
            || theAWSSecretKey == null || theAWSAccessKeyId == null || theAWSRegion == null)
    {
      throw new ConnectorException("S3 credentials incomplete: " + theCredentials);
    }
  }

  /**
   * Sets the credentials String value.
   * @param aCredentials
   */
  public void setCredentials(String aCredentials)
  {
    this.theCredentials = aCredentials;
  }

  /**
   * Gets the credentials as a string.
   * @return the credentials
   */
  public String getCredentials()
  {
    return theCredentials;
  }

  @Override
  public String getLocation()
  {
    return theLocation;
  }

  @Override
  public String getOwner()
  {
    return theOwner;
  }

  @Override
  public String accessKeyId()
  {
    return theAWSAccessKeyId;
  }

  @Override
  public String secretAccessKey()
  {
    return theAWSSecretKey;
  }

  /**
   * Gets the AWS Region.
   * @return the aws region
   */
  public String getAWSRegion()
  {
    return theAWSRegion;
  }

  @Override
  public String getBucketName()
  {
    return theBucketName;
  }

  @Override
  public Boolean isLockRequired()
  {
    return isLockRequired;
  }

  @Override
  public List<Tag> getTags()
  {
    return theTags;
  }

  /**
   * Loads s3 credentials from keystore.
   *
   * @throws ConnectorException
   * @return CredentialsBackedS3FileSourceConfig
   * **/
  public static List<CredentialsBackedS3FileSourceConfig> loadFromKeystore() throws ConnectorException
  {
    // Get all credentials...
    CredentialsStoreDAO myCredDAO = new CredentialsStoreDAO();
    ArrayList<StoredCredentials> myCreds = (ArrayList<StoredCredentials>) myCredDAO.getEntries();

    // Get those starting with "s3" and convert to configs
    List<CredentialsBackedS3FileSourceConfig> myS3Configs = new ArrayList<>();
    for (StoredCredentials cred : myCreds)
    {
      if (cred.getName().startsWith("s3"))
      {
        CredentialsBackedS3FileSourceConfig myS3Config = new CredentialsBackedS3FileSourceConfig();
        myS3Config.setCredentials(cred.getName());
        myS3Config.loadFromProperties(cred.getCredentials());
        myS3Configs.add(myS3Config);
      }
    }

    return myS3Configs;
  }

}
