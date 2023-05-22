/*
 * Copyright 2020 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.schedulers.quartz;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Date;
import java.util.Objects;

/**
 * Bean class representing a Remote Query Execution that has been created in EnergySys.
 * @author EnergySys Limited
 */
@Entity
@Table(name = "REMOTE_QUERY_EXEC")
@XmlRootElement
public class RemoteQueryExecution
{
  
  @Id
  private String theGUID;
  
  private String theConnectorName;

  private String theQueryName;

  @Temporal(TemporalType.TIMESTAMP)
  private Date theExecutionDateTime;

  private String theParameters;

  private Status theStatus;

  private String theMessage;
  
  private String theExecuterType;

  @Convert(converter = org.hibernate.type.NumericBooleanConverter.class)
  private Boolean isProcessingComplete;

  /**
   * Default Constructor.
   */
  public RemoteQueryExecution()
  { }

  public String getConnectorName()
  {
    return theConnectorName;
  }

  public void setConnectorName(String aConnectorName)
  {
    this.theConnectorName = aConnectorName;
  }

  public Boolean isProcessingComplete()
  {
    return isProcessingComplete == null ? false : isProcessingComplete;
  }

  public void setIsProcessingComplete(Boolean isProcessingComplete)
  {
    this.isProcessingComplete = isProcessingComplete;
  }

  public String getGUID()
  {
    return theGUID;
  }

  public void setGUID(String aGUID)
  {
    this.theGUID = aGUID;
  }

  public String getQueryName()
  {
    return theQueryName;
  }

  public void setQueryName(String aQueryName)
  {
    this.theQueryName = aQueryName;
  }

  public Date getExecutionDateTime()
  {
    return theExecutionDateTime;
  }

  public void setExecutionDateTime(Date aExecutionDateTime)
  {
    this.theExecutionDateTime = aExecutionDateTime;
  }

  public String getParameters()
  {
    return theParameters;
  }

  public void setParameters(String aParameters)
  {
    this.theParameters = aParameters;
  }

  public Status getStatus()
  {
    return theStatus;
  }

  public void setStatus(Status aStatus)
  {
    this.theStatus = aStatus;
  }

  public String getMessage()
  {
    return theMessage;
  }

  public void setMessage(String aMessage)
  {
    this.theMessage = aMessage;
  }

  public String getExecuterType()
  {
    return theExecuterType;
  }

  public void setExecuterType(String aExecuterType)
  {
    this.theExecuterType = aExecuterType;
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    return hash;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    final RemoteQueryExecution other = (RemoteQueryExecution) obj;
    if (!Objects.equals(this.theGUID, other.theGUID))
    {
      return false;
    }
    return true;
  }

  @Override
  public String toString()
  {
    return "RemoteQueryExecution{" + "theGUID=" + theGUID + ", theQueryName=" + theQueryName 
        + ", theExecutionDateTime=" + theExecutionDateTime + ", theParameters=" + theParameters 
        + ", theStatus=" + theStatus + ", theMessage=" + theMessage + ", theExecuterType=" + theExecuterType 
        + ", isProcessingComplete=" + isProcessingComplete + '}';
  }

  /**
   * Enum representing the status of the Remote Query Execution.
   */
  public enum Status 
  {
    /** Queued. **/
    QUEUED("Queued"),
    /** Accepted. **/
    ACCEPTED("Accepted"),
    /** Failed. **/
    FAILED("Failed"),
    /** Success. **/
    SUCCESS("Success");
    
    private String name;
    
    Status(String name)
    {
      this.name = name;
    }
    
    public String getName()
    {
      return name;
    }
  }

}
