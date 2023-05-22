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

package com.energysys.filesource;

import java.util.Date;

/**
 * Common Interface for File Source File objects.
 * 
 * @author EnergySys Limited
 * @version $Revision$
 */
public interface IFileSourceFile
{
  /**
   * Gets the id of the file.
   * @return the id
   */
  String getFileId();
  
  /**
   * Gets the original modified date of the file. 
   * For example the original OS modified date.
   * @return the date
   */
  Date getProducerModifiedDate();
  
  /**
   * Gets the date that the file was last written to this File Source.
   * For example the S3 object created date.
   * @return the date
   */
  Date getFileSourceModifiedDate();

  /**
   * Gets the owner of the file on this file source.
   * This should be whatever was set in the file source config when the file was written to the file source.
   * @return the owner
   */
  String getOwner();

  /**
   * Gets the size of the content of the file.
   * @return the content size
   */
  Long getSize();
  
  /**
   * Gets the contents mime type.
   * @return the mime type
   */
  String getMimeType();

}
