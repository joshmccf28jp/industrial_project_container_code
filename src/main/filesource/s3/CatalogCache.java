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
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author brett
 */
class CatalogCache implements Serializable
{

  private static final long serialVersionUID = 1L;
  private Map<String, IFileSourceFile> theCatalog;
  private Date theLatestObjectUpdateDate;

  protected CatalogCache(Map<String, IFileSourceFile> theCatalog, Date theLatestObjectUpdateDate)
  {
    this.theCatalog = theCatalog;
    this.theLatestObjectUpdateDate = theLatestObjectUpdateDate;
  }

  protected Map<String, IFileSourceFile> getTheCatalog()
  {
    return theCatalog;
  }

  protected Date getTheLatestObjectUpdateDate()
  {
    return theLatestObjectUpdateDate;
  }

}
