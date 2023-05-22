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

import com.energysys.filesource.IFileSourceConfig;

/**
 * Common interface required for FileConnectors.
 *
 * @author EnergySys Limited
 * @version $Revision$
 * @param <SOURCE_CONFIG> Config type of source file source
 * @param <DESTINATION_CONFIG> Config type of destination file source
 */
public interface IFileSyncConfig
        <SOURCE_CONFIG extends IFileSourceConfig, DESTINATION_CONFIG extends IFileSourceConfig>
{
  /**
   * If the sync recurses directories.
   * @return if recursive
   */
  Boolean getIsDirRecursive();

  /**
   * Gets the destination config.
   * @return the config
   */
  DESTINATION_CONFIG getDestinationConfig();

  /**
   * File pattern for matching file names.
   * @return the file pattern
   */
  String getFilePattern();

  /**
   * Gets the id of the file connector.
   * @return the id
   */
  Integer getId();

  /**
   * Gets the name of the file connector.
   * @return the name
   */
  String getName();

  /**
   * Gets the source config.
   * @return the config
   */
  SOURCE_CONFIG getSourceConfig();

}
