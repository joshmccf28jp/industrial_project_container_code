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
package com.energysys.connector;

import com.energysys.connector.web.IIdentifiable;
import java.io.Serializable;
import java.util.Date;
import java.util.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.hibernate.annotations.GenericGenerator;

/**
 * Class representing the configuration of a scheduled quartz Job.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@Entity
@Table(name = "JOB_CONFIGURATION")
@XmlRootElement
public class JobConfiguration implements IIdentifiable, Serializable
{

  private static final Logger LOG = Logger.getLogger(JobConfiguration.class.getName());

  @Id
  @GenericGenerator(name = "JOB_CONFIGURATION_SEQ",
          strategy = "com.energysys.connector.database.AssignedOrIdentityGenerator")
  @GeneratedValue(generator = "JOB_CONFIGURATION_SEQ")
  private Integer theId;

  private String theName;

  private String theJobExecuterType;

  private String theJobExecuterTarget;

  @Temporal(TemporalType.TIMESTAMP)
  private Date theStartDate;

  @Temporal(TemporalType.TIMESTAMP)
  private Date theEndDate;

  private Integer theRepeatValue;

  @Enumerated
  private Repeat theRepeatInterval;

  private Boolean isActive;

  /**
   * Default Constructor.
   */
  public JobConfiguration()
  {
  }

  @Override
  public Integer getId()
  {
    return theId;
  }

  @Override
  public void setId(Integer anId)
  {
    this.theId = anId;
  }

  @Override
  public String getName()
  {
    return theName;
  }

  public void setName(String aName)
  {
    this.theName = aName;
  }

  public String getJobExecuterType()
  {
    return theJobExecuterType;
  }

  public void setJobExecuterType(String aJobExecuterType)
  {
    this.theJobExecuterType = aJobExecuterType;
  }

  public String getJobExecuterTarget()
  {
    return theJobExecuterTarget;
  }

  public void setJobExecuterTarget(String aJobExecuterTarget)
  {
    this.theJobExecuterTarget = aJobExecuterTarget;
  }

  public Date getStartDate()
  {
    return theStartDate;
  }

  public void setStartDate(Date aStartDate)
  {
    this.theStartDate = aStartDate;
  }

  public Date getEndDate()
  {
    return theEndDate;
  }

  public void setEndDate(Date aEndDate)
  {
    this.theEndDate = aEndDate;
  }

  public Boolean getIsActive()
  {
    return isActive;
  }

  public void setIsActive(Boolean isActive)
  {
    this.isActive = isActive;
  }

  public Integer getRepeatValue()
  {
    return theRepeatValue;
  }

  public void setRepeatValue(Integer aRepeatValue)
  {
    this.theRepeatValue = aRepeatValue;
  }

  public Repeat getRepeatInterval()
  {
    return theRepeatInterval;
  }

  public void setRepeatInterval(Repeat aRepeatInterval)
  {
    this.theRepeatInterval = aRepeatInterval;
  }

  /**
   * Gets a copy of this Job Configuration.
   * @return the copy
   */
  public JobConfiguration copy()
  {
    JobConfiguration myNewJob = new JobConfiguration();
    myNewJob.isActive = isActive;
    myNewJob.theEndDate = theEndDate;
    myNewJob.theId = theId;
    myNewJob.theJobExecuterTarget = theJobExecuterTarget;
    myNewJob.theJobExecuterType = theJobExecuterType;
    myNewJob.theName = theName;
    myNewJob.theRepeatInterval = theRepeatInterval;
    myNewJob.theRepeatValue = theRepeatValue;
    myNewJob.theStartDate = theStartDate;
    return myNewJob;
  }

  /**
   * Enum for repeat intervals.
   */
  public enum Repeat
  {
    /**
     * MINUTES.
     */
    MINUTES,
    /**
     * HOURS.
     */
    HOURS,
    /**
     * DAYS.
     */
    DAYS,
    /**
     * WEEKS.
     */
    WEEKS,
    /**
     * MONTHS.
     */
    MONTHS,
    /**
     * NEVER.
     */
    NEVER
  }

  @Override
  public String toString()
  {
    return "JobConfiguration{" + "theName=" + theName + ", theJobExecuterType=" + theJobExecuterType 
        + ", theJobExecuterTarget=" + theJobExecuterTarget + ", theStartDate=" + theStartDate 
        + ", theEndDate=" + theEndDate + ", theRepeatValue=" + theRepeatValue 
        + ", theRepeatInterval=" + theRepeatInterval + ", isActive=" + isActive + '}';
  }
}
