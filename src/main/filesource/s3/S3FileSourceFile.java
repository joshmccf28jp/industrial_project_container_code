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
package com.energysys.filesource.s3;

import com.energysys.filesource.IFileSourceFile;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Class representing a file and it's metadata.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class S3FileSourceFile implements Serializable, IFileSourceFile
{

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = Logger.getLogger(S3FileSourceFile.class.getName());

  private final String theFileId;
  private final Date theLocalModifiedDate;
  private final Date theRemoteModifiedDate;
  private final String theOwner;
  private final Long theSize;
  private final String theMimeType;

  /**
   * Basic Constructor from another FileSourceFile.
   *
   * @param aFile the file
   */
  public S3FileSourceFile(IFileSourceFile aFile)
  {
    this.theFileId = aFile.getFileId();
    this.theLocalModifiedDate = aFile.getProducerModifiedDate();
    this.theRemoteModifiedDate = aFile.getProducerModifiedDate();
    this.theOwner = aFile.getOwner();
    this.theSize = aFile.getSize();
    this.theMimeType = aFile.getMimeType();
  }

  /**
   * Constructor taking a head object response.
   * @param someS3Metadata
   */
  public S3FileSourceFile(HeadObjectResponse someS3Metadata)
  {
    this(someS3Metadata.metadata().get(S3FileSource.LOCAL_KEY),
            Date.from(Instant.ofEpochMilli(
                    Long.valueOf(someS3Metadata.metadata().get(S3FileSource.LOCAL_MODIFIED_DATE)))),
            Date.from(someS3Metadata.lastModified()),
            someS3Metadata.metadata().get(S3FileSource.OWNER),
            someS3Metadata.contentLength(),
            someS3Metadata.contentType());

  }

  /**
   * Basic Constructor, with no remote modified date (for use by local services).
   *
   * @param anObjectKey the key
   * @param aLocalModifiedDate the local modified date
   * @param anOwner the owner
   * @param aSize the object content size
   * @param aMimeType the mime Type
   */
  public S3FileSourceFile(String anObjectKey, Date aLocalModifiedDate, String anOwner,
          Long aSize, String aMimeType)
  {
    this.theFileId = anObjectKey;
    this.theLocalModifiedDate = aLocalModifiedDate;
    this.theRemoteModifiedDate = aLocalModifiedDate;
    this.theOwner = anOwner;
    this.theSize = aSize;
    this.theMimeType = aMimeType;
  }

  /**
   * Basic Constructor with all fields.
   *
   * @param anObjectKey the key
   * @param aLocalModifiedDate the local modified date
   * @param aRemoteModifiedDate the remote modified date
   * @param anOwner the owner
   * @param aSize the object content size
   * @param aMimeType the mime Type
   */
  public S3FileSourceFile(String anObjectKey, Date aLocalModifiedDate, Date aRemoteModifiedDate, String anOwner,
          Long aSize, String aMimeType)
  {
    this.theFileId = anObjectKey;
    this.theLocalModifiedDate = aLocalModifiedDate;
    this.theRemoteModifiedDate = aRemoteModifiedDate;
    this.theOwner = anOwner;
    this.theSize = aSize;
    this.theMimeType = aMimeType;
  }

  @Override
  public String getFileId()
  {
    return theFileId;
  }

  @Override
  public Date getProducerModifiedDate()
  {
    return theLocalModifiedDate;
  }

  public Date getTheRemoteModifiedDate()
  {
    return theRemoteModifiedDate;
  }

  @Override
  public String getOwner()
  {
    return theOwner;
  }

  @Override
  public Long getSize()
  {
    return theSize;
  }

  @Override
  public String toString()
  {
    return "S3FileSourceFile{" + "theFildID=" + theFileId + '}';
  }

  @Override
  public Date getFileSourceModifiedDate()
  {
    return theRemoteModifiedDate;
  }

  @Override
  public String getMimeType()
  {
    return theMimeType;
  }

}
