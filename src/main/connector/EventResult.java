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

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.web.IIdentifiable;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.hibernate.annotations.Nationalized;

/**
 * Class representing a Result that has summary and detailed messages and timings data.
 *
 * @author EnergySys Limited
 *
 */
@Entity
@Table(name = "EVENT_RESULT")
public class EventResult implements IIdentifiable, Serializable
{

  private static final Logger LOG = Logger.getLogger(EventResult.class.getName());

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer theId;

  private String theEventDescription;
  private Result theResult;
  private String theSummaryMessage;
  @Lob
  @Nationalized
  private String theDetailedMessage;

  @Temporal(TemporalType.TIMESTAMP)
  private Date theRunTimestamp;

  private Long theRunDuration;

  /**
   * Default Constructor (used by hibernate).
   */
  public EventResult()
  {
    
  }
  /**
   * Basic constructor.
   *
   * @param anEventDescription description
   * @param aResult the result
   * @param aSummaryMessage a summary message
   * @param aDetailedMessage a detailed message
   * @param aStartTimeInMillis time event started
   */
  public EventResult(String anEventDescription, Result aResult, String aSummaryMessage, String aDetailedMessage,
          Long aStartTimeInMillis)
  {
    setDescription(anEventDescription);
    this.theResult = aResult;
    setSummaryMessage(aSummaryMessage);
    setDetailedMessage(aDetailedMessage);
    this.theRunTimestamp = CurrentDateTime.getCurrentDate();
    this.theRunDuration = (CurrentDateTime.getCurrentTimeInMillis() - aStartTimeInMillis);
    logResult();
  }

  /**
   * Constructor taking exception.
   *
   * @param anEventDescription description of event
   * @param anEx an exception
   * @param aStartTimeInMillis when process started
   */
  public EventResult(String anEventDescription, Throwable anEx, Long aStartTimeInMillis)
  {
    setDescription(anEventDescription);
    this.theRunTimestamp = CurrentDateTime.getCurrentDate();
    this.theRunDuration = theRunTimestamp.getTime() - aStartTimeInMillis;
    this.theResult = Result.EXCEPTION;

    if (anEx instanceof ConnectorException)
    {
      setSummaryMessage(anEx.getMessage());
    }
    else
    {
      this.theSummaryMessage = "Unexpected Error";
    }

    StringWriter myMessageWriter = new StringWriter();
    myMessageWriter.append(anEx.getMessage() + "\n\n");
    try (PrintWriter myPrintWriter = new PrintWriter(myMessageWriter))
    {
      anEx.printStackTrace(myPrintWriter);
      setDetailedMessage(myMessageWriter.getBuffer().toString());
    }
    logResult();
  }

  /**
   * Constructor taking other EventResult to copy.
   * @param anEventDescription description of event
   * @param anEventToCopy the EventResult to copy
   * @param aStartTime when process started
   */
  public EventResult(String anEventDescription, EventResult anEventToCopy, Long aStartTime)
  {
    this(anEventDescription, anEventToCopy.theResult, anEventToCopy.getSummaryMessage(), anEventToCopy.
            getDetailedMessage(), aStartTime);
  }

  public String getEventDescription()
  {
    return theEventDescription;
  }

  public Result getResult()
  {
    return theResult;
  }

  public String getSummaryMessage()
  {
    return theSummaryMessage;
  }

  public String getDetailedMessage()
  {
    return theDetailedMessage;
  }

  public Date getRunTimestamp()
  {
    return theRunTimestamp;
  }

  public Long getRunDurationSeconds()
  {
    return theRunDuration / 1000;
  }

  private void logResult()
  {
    Level myLogLevel = getLogLevel();
    LOG.log(myLogLevel, theEventDescription + " Event Complete. " + "Took " + theRunDuration / 1000 + " seconds");
    LOG.log(myLogLevel, getSummaryMessage());
    LOG.log(myLogLevel, getDetailedMessage());
  }

  /**
   * Works out a Logger Level based on the result.
   *
   * @return the level
   */
  protected Level getLogLevel()
  {
    Level myLogLevel;
    switch (theResult)
    {
      case EXCEPTION:
        myLogLevel = Level.SEVERE;
        break;

      case FAILED:
        myLogLevel = Level.WARNING;
        break;

      default:
        myLogLevel = Level.INFO;
        break;

    }
    return myLogLevel;
  }

  @Override
  public Integer getId()
  {
    return theId;
  }

  @Override
  public String getName()
  {
    return theEventDescription;
  }

  @Override
  public void setId(Integer anId)
  {
    this.theId = anId;
  }

  private void setSummaryMessage(String aSummaryMessage)
  {
    if (aSummaryMessage == null)
    {
      theSummaryMessage = "";
    }
    else if (aSummaryMessage.length() > 255)
    {
      theSummaryMessage = aSummaryMessage.substring(0, 255);
    }
    else
    {
      theSummaryMessage = aSummaryMessage;
    }
  }

  private void setDetailedMessage(String aDetailedMessage)
  {
    if (aDetailedMessage == null)
    {
      theDetailedMessage = "";
    }
    else if (aDetailedMessage.length() > 32700)
    {
      theDetailedMessage = aDetailedMessage.substring(0, 32700);
    }
    else
    {
      theDetailedMessage = aDetailedMessage;
    }
  }

  private void setDescription(String anEventDescription)
  {
    if (anEventDescription == null)
    {
      theEventDescription = "";
    }
    else if (anEventDescription.length() > 255)
    {
      theEventDescription = anEventDescription.substring(0, 255);
    }
    else
    {
      theEventDescription = anEventDescription;
    }
  }

  /**
   * Appends a string to the description.
   * @param aAppendage
   */
  public void appendDescription(String aAppendage)
  {
    if (aAppendage != null)
    {
      theEventDescription = theEventDescription.concat(aAppendage);
    }
  }

  /**
   * Result enum.
   */
  public enum Result
  {
    /**
     * SUCCESS.
     */
    SUCCESS,
    /**
     * FAILED.
     */
    FAILED,
    /**
     * EXCEPTION.
     */
    EXCEPTION,
    /**
     * WARNINGS.
     */
    WARNINGS,
  }

}
