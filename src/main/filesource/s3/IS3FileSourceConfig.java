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

import com.energysys.filesource.IFileSourceConfig;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.services.s3.model.Tag;

import java.util.List;

/**
 * Interface for configuration for S3FileSource objects.
 * 
 * @author EnergySys Limited
 * @version $Revision$
 */
public interface IS3FileSourceConfig extends IFileSourceConfig, AwsCredentials
{
  /**
   * Gets the name of the S3 Bucket.
   *
   * @return the bucket name
   */
  String getBucketName();

  /**
   * Returns whether a lock is required on the Remote Catalog.
   *
   * @return if lock required
   */
  Boolean isLockRequired();

  /**
   * Returns the AWS Region.
   *
   * @return the region
   */
  String getAWSRegion();

  /**
   * Gets the tags.
   * @return the tags
   */
  List<Tag> getTags();

}