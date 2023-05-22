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
import com.energysys.filesource.IFileSourceFileFilter;
import com.energysys.filesource.exception.FileSourceException;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Date;
import java.util.List;

/**
 * Interface representing a Catalog of FileSourceFile's that are held in a FileSource.
 * 
 * @author EnergySys Limited
 * @version $Revision$
 */
public interface IS3Catalog
{
  /**
   * Opens and initialises the catalog.
   * @param anS3Client
   * @param aConfig
   * @throws FileSourceException
   */
  void openCatalog(S3Client anS3Client, IS3FileSourceConfig aConfig) throws FileSourceException;
  /**
   * Gets all the files in the file source.
   *
   * @param aFilter
   * @return all files in the catalog
   */
  List<IFileSourceFile> getAllFiles(IFileSourceFileFilter aFilter);

  /**
   * Gets the file for a specific id.
   *
   * @param aFileId the key
   * @return the file
   */
  IFileSourceFile getFileWithID(String aFileId);
  
  /**
   * Checks whether a file with a given id is present in the file source.
   * @param aFileId the file id
   * @return if present
   */
  Boolean containsFile(String aFileId);
  
  /**
   * Called when catalog is no longer required.
   * Should perform any cleanup of saving of caches etc.
   * @throws FileSourceException
   */
  void closeCatalog() throws FileSourceException;

  /**
   * Updates a value in the catalog.
   * @param aKey
   * @param aFile
   */
  void update(String aKey, S3FileSourceFile aFile);

  /**
   * Gets latest update date of files contained in catalog.
   * @return latest update date
   */
  Date getLatestUpdateDate();
}
