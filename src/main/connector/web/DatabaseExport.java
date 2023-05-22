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
package com.energysys.connector.web;

import com.energysys.calendar.CurrentDateTime;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Wrapper class for exporting database contents as XML.
 * @author EnergySys Limited
 */
@XmlRootElement
public class DatabaseExport
{
  private String theExportName;
  
  private Date theExportDate;
  
  @XmlAnyElement(lax = true)
  private List theData;

  /**
   * Default Constructor.
   */
  public DatabaseExport()
  {
    theData = new ArrayList();
  }
  
  /**
   * Constructor taking name of export.
   * @param anExportName the name
   */
  public DatabaseExport(String anExportName)
  {
    this();
    theExportDate = CurrentDateTime.getCurrentDate();
    theExportName = anExportName;
    
  }

  @XmlTransient
  public List getData()
  {
    return theData;
  }

  public void setData(List someData)
  {
    this.theData = someData;
  }

  public String getExportName()
  {
    return theExportName;
  }

  public void setExportName(String anExportName)
  {
    this.theExportName = anExportName;
  }

  public Date getExportDate()
  {
    return theExportDate;
  }

  public void setExportDate(Date anExportDate)
  {
    this.theExportDate = anExportDate;
  }
  
  
}
