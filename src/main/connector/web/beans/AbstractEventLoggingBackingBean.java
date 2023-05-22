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
package com.energysys.connector.web.beans;

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.EventResult;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import java.util.logging.Logger;

/**
 * Abstract backing bean providing growl and event messaging.
 * @author EnergySys Limited
 */
public abstract class AbstractEventLoggingBackingBean
{

  /**
   * Growl PrimeFaces dialog with no detailed message.
   */
  public static final String GROWL_DIALOG_SIMPLE = "growl-simple";
  /**
   * Growl PrimeFaces dialog with summaray and detailed messages.
   */
  public static final String GROWL_DIALOG_DETAILED = "growl-detailed";

  /**
   * Error title.
   */
  public static final String GROWL_TITLE_ERROR = "Error";

  /**
   * Validation message prefix.
   */
  public static final String GROWL_VALIDATION = "Validation: ";

  /**
   * Logger for all sub classes.
   */
  protected static final Logger LOG = Logger.getLogger("UIEventLogger");

  /**
   * Adds a growl message for an event result with options for logging in the event log and for showing the detailed 
   * message.
   * @param anEvent the result
   * @param isEventLogEntry add entry to log
   * @param isShowDetail show message detail
   */
  protected void addGrowlMessage(EventResult anEvent, Boolean isEventLogEntry, Boolean isShowDetail)
  {
    FacesMessage.Severity mySeverity;
    String mySummaryMessage = anEvent.getSummaryMessage();
    switch (anEvent.getResult())
    {
      case EXCEPTION:
        mySeverity = FacesMessage.SEVERITY_ERROR;
        mySummaryMessage = mySummaryMessage + "\nSee Event Log for details";
        break;
        
      case FAILED:
        mySeverity = FacesMessage.SEVERITY_ERROR;
        break;

      case WARNINGS:
        mySeverity = FacesMessage.SEVERITY_WARN;
        break;

      default:
        mySeverity = FacesMessage.SEVERITY_INFO;
        break;
    }

    String myDialogId;
    if (!isShowDetail)
    {
      myDialogId = GROWL_DIALOG_SIMPLE;
    }
    else
    {
      myDialogId = GROWL_DIALOG_DETAILED;
    }

    if (isEventLogEntry)
    {
      EventLogBean.addLog(anEvent);
    }

    FacesMessage myFacesMessage
            = new FacesMessage(mySeverity, mySummaryMessage, anEvent.getDetailedMessage());
    FacesContext.getCurrentInstance().addMessage(myDialogId, myFacesMessage);
    if (anEvent.getResult() != EventResult.Result.SUCCESS)
    {
      FacesContext.getCurrentInstance().validationFailed();
    }
  }

  /**
   * Adds a growl message based on a ui EventResult.
   * @param anEvent the event
   */
  protected void addGrowlMessage(EventResult anEvent)
  {
    Boolean myLogEvent;
    Boolean myShowDetail;

    if (anEvent.getResult() == EventResult.Result.EXCEPTION)
    {
      myLogEvent = true;
      myShowDetail = false;
    }
    else if (anEvent.getDetailedMessage() == null || anEvent.getDetailedMessage().isEmpty())
    {
      myLogEvent = false;
      myShowDetail = false;
    }
    else
    {
      myLogEvent = false;
      myShowDetail = true;
    }

    addGrowlMessage(anEvent, myLogEvent, myShowDetail);
  }

  /**
   * Adds a growl message based on an exception.
   * @param anEx the exception
   */
  protected void addGrowlMessage(Exception anEx)
  {
    EventResult myEvent = new EventResult("UI Operation Error", anEx, CurrentDateTime.getCurrentTimeInMillis());
    addGrowlMessage(myEvent, Boolean.TRUE, Boolean.FALSE);
  }
}
