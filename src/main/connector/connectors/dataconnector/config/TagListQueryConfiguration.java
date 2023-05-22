/*
 * Copyright 2013 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.connectors.dataconnector.config;

import com.energysys.connector.web.IIdentifiable;
import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.hibernate.annotations.GenericGenerator;

/**
 * Encapsulates a tag group query.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
@Entity
@Table(name = "TAG_LIST_QUERY_CONFIGURATION")
@XmlRootElement
public class TagListQueryConfiguration implements IIdentifiable, Serializable
{

  @Id
  @GenericGenerator(name = "TAG_LIST_QUERY_CONFIGURATION_SEQ",
          strategy = "com.energysys.connector.database.AssignedOrIdentityGenerator")
  @GeneratedValue(generator = "TAG_LIST_QUERY_CONFIGURATION_SEQ")
  private Integer theID;
  private String theName;
  private String theAdaptorQuery;
  private String theSummaryType;
  private String theTagList;
  private Integer theSamplesInterval;

  private String theDateFrom;
  private String theTimeFrom;
  private String theDateTo;
  private String theTimeTo;
  
  

  /**
   * Constructor.
   */
  public TagListQueryConfiguration()
  {

  }

  @Override
  public String getName()
  {
    return theName;
  }

  public String getAdaptorQuery()
  {
    return theAdaptorQuery;
  }

  public String getTagListName()
  {
    return theTagList;
  }

  public String getDateFrom()
  {
    return theDateFrom;
  }

  public String getTimeFrom()
  {
    return theTimeFrom;
  }

  public String getDateTo()
  {
    return theDateTo;
  }

  public String getTimeTo()
  {
    return theTimeTo;
  }

  public void setName(String anName)
  {
    this.theName = anName;
  }

  public void setAdaptorQuery(String anAdaptorQuery)
  {
    this.theAdaptorQuery = anAdaptorQuery;
  }

  public void setTagListName(String aTagList)
  {
    this.theTagList = aTagList;
  }

  public void setDateFrom(String aDateFrom)
  {
    this.theDateFrom = aDateFrom;
  }

  public void setTimeFrom(String aTimeFrom)
  {
    this.theTimeFrom = aTimeFrom;
  }

  public void setDateTo(String aDateTo)
  {
    this.theDateTo = aDateTo;
  }

  public void setTimeTo(String aTimeTo)
  {
    this.theTimeTo = aTimeTo;
  }

  public String getSummaryType()
  {
    return theSummaryType;
  }

  public void setSummaryType(String aSummaryType)
  {
    theSummaryType = aSummaryType;
  }

  @Override
  public Integer getId()
  {
    return theID;
  }

  /**
   * hasDateFrom.
   *
   * @return true if date from populated
   */
  public boolean hasDateFrom()
  {
    return (theDateFrom != null && !theDateFrom.isEmpty());
  }

  /**
   * hasTimeFrom.
   *
   * @return true if time from populated
   */
  public boolean hasTimeFrom()
  {
    return (theTimeFrom != null && !theTimeFrom.isEmpty());
  }

  /**
   * hasDateTo.
   *
   * @return true if date to populated
   */
  public boolean hasDateTo()
  {
    return (theDateTo != null && !theDateTo.isEmpty());
  }

  /**
   * hasTimeTo.
   *
   * @return true if time to populated
   */
  public boolean hasTimeTo()
  {
    return (theTimeTo != null && !theTimeTo.isEmpty());
  }

  @Override
  public void setId(Integer anId)
  {
    theID = anId;
  }

  public Integer getSamplesInterval()
  {
    return theSamplesInterval;
  }

  public void setSamplesInterval(Integer aSamplesInterval)
  {
    theSamplesInterval = aSamplesInterval;
  }
  
  
}
