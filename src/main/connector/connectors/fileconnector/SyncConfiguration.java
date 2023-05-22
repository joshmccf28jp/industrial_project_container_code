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

import com.energysys.connector.web.IIdentifiable;
import com.energysys.filesource.local.ILocalFileSourceConfig;

import java.io.Serializable;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.hibernate.annotations.GenericGenerator;

/**
 * Class for representing the configuration of an S3Uploader File Connector.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@Entity
@Table(name = "SYNC_CONFIGURATION")
@XmlRootElement
public class SyncConfiguration implements IIdentifiable, Serializable,
    IFileSyncConfig<ILocalFileSourceConfig, CredentialsBackedS3FileSourceConfig>
{

  private static final Logger LOG = Logger.getLogger(SyncConfiguration.class.getName());

  @Id
  @GenericGenerator(name = "SYNC_CONFIGURATION_SEQ",
      strategy = "com.energysys.connector.database.AssignedOrIdentityGenerator")
  @GeneratedValue(generator = "SYNC_CONFIGURATION_SEQ")
  private Integer theId;

  private String theName;

  @Embedded
  private LocalFileSourceConfig theLocalFSConfig;

  @Embedded
  private CredentialsBackedS3FileSourceConfig theS3FSConfig;

  private Boolean isDirRecursive;

  @Convert(converter = FileExtensionConverter.class)
  private List<String> theFileExtensions;

  /**
   * Default Constructor.
   */
  public SyncConfiguration()
  {
    theLocalFSConfig = new LocalFileSourceConfig();
    theS3FSConfig = new CredentialsBackedS3FileSourceConfig();
  }

  public List<String> getFileExtensions()
  {
    return theFileExtensions;
  }

  public void setFileExtensions(List<String> fileExtensions)
  {
    this.theFileExtensions = fileExtensions;
  }


  public String getOwner()
  {
    return getSourceConfig().getOwner();
  }

  /**
   * Sets the owner.
   *
   * @param anOwner owner
   */
  public void setOwner(String anOwner)
  {
    getSourceConfig().setOwner(anOwner);
  }

  @Override
  public Integer getId()
  {
    return theId;
  }

  @Override
  public void setId(Integer aId)
  {
    this.theId = aId;
  }

  @Override
  public String getName()
  {
    return theName;
  }

  public void setName(String aName)
  {
    this.theName = aName;
  }

  @Override
  public LocalFileSourceConfig getSourceConfig()
  {
    return theLocalFSConfig;
  }

  public void setSourceConfig(LocalFileSourceConfig aSourceConfig)
  {
    this.theLocalFSConfig = aSourceConfig;
  }

  @Override
  public CredentialsBackedS3FileSourceConfig getDestinationConfig()
  {
    return theS3FSConfig;
  }

  public void setDestinationConfig(CredentialsBackedS3FileSourceConfig aDestinationConfig)
  {
    this.theS3FSConfig = aDestinationConfig;
  }

  @Override
  public Boolean getIsDirRecursive()
  {
    return isDirRecursive;
  }

  public void setIsDirRecursive(Boolean isDirRecursive)
  {
    this.isDirRecursive = isDirRecursive;
  }

  @Override
  public String getFilePattern()
  {
    StringBuilder myBuff = new StringBuilder("(?i)");

    final List<String> myFileExtensions = getFileExtensions();
    if (myFileExtensions.isEmpty())
    {
      myBuff.append("^(?!.*[.]zip$).*$");
    }
    for (Iterator<String> iter = myFileExtensions.iterator(); iter.hasNext();)
    {
      String myExtension = iter.next();
      myBuff.append(".*[.]");
      myBuff.append(myExtension);
      myBuff.append("$");
      if (iter.hasNext())
      {
        myBuff.append("|");
      }
    }

    return myBuff.toString();
  }

  @Override
  public int hashCode()
  {
    int myHash = theId.hashCode();
    return myHash;
  }

  @Override
  public boolean equals(Object anObject)
  {
    if (this == anObject)
    {
      return true;
    }
    if (anObject == null)
    {
      return false;
    }
    if (getClass() != anObject.getClass())
    {
      return false;
    }
    final SyncConfiguration myOther = (SyncConfiguration) anObject;
    return Objects.equals(this.theId, myOther.theId);
  }
}
