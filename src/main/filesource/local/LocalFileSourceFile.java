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

package com.energysys.filesource.local;


import com.energysys.filesource.IFileSourceFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of IFileSourceFile for LocalFileSource's. Wraps a java.io.File and directs methods to appropriate
 * properties on the file.
 * 
 * @author EnergySys Limited
 * @version $Revision$
 */
public class LocalFileSourceFile implements IFileSourceFile
{
  private final File theFile;
  private final String theFileID;
  private final String theOwner;

  /**
   * Basic constructor.
   * @param aFile the local file
   * @param aFileID the File Source file id.
   * @param anOwner the file owner.
   */
  public LocalFileSourceFile(File aFile, String aFileID, String anOwner)
  {
    this.theFile = aFile;
    this.theFileID = aFileID;
    this.theOwner = anOwner;
  }
  
  @Override
  public Date getProducerModifiedDate()
  {
    return Date.from(Instant.ofEpochMilli(theFile.lastModified()));
  }

  @Override
  public String getFileId()
  {
    return theFileID;
  }

  @Override
  public String getOwner()
  {
    return theOwner;
  }

  @Override
  public Long getSize()
  {
    return theFile.length();
  }

  @Override
  public String toString()
  {
    return "LocalFileSourceFile{" + "theFileID=" + theFileID + '}';
  }

  @Override
  public Date getFileSourceModifiedDate()
  {
    return Date.from(Instant.ofEpochMilli(theFile.lastModified()));
  }

  @Override
  public String getMimeType()
  {
    try
    {
      return Files.probeContentType(theFile.toPath());
    }
    catch (IOException ex)
    {
      Logger.getLogger(LocalFileSourceFile.class.getName()).log(Level.SEVERE, null, ex);
      return null;
    }
  }
}

