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

import java.util.Date;

/**
 * Holds the status of a Downloader. Keeps the IgnoreBeforeDate which is used to speed up the file scanning. Meant to be
 * set after each download operation so the downloader doesn't download the same thing twice even if the item has been
 * removed locally.
 *
 * @author EnergySys Limited
 */
public class S3DownloaderStatus
{
  private Date theIgnoreBeforeDate;

  /**
   * Default Constructor.
   */
  public S3DownloaderStatus()
  {
  }

  public Date getIgnoreBeforeDate()
  {
    return theIgnoreBeforeDate;
  }

  public void setIgnoreBeforeDate(Date anIgnoreBeforeDate)
  {
    this.theIgnoreBeforeDate = anIgnoreBeforeDate;
  }

}
