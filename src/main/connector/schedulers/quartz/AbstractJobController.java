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
package com.energysys.connector.schedulers.quartz;

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.EventResult;
import com.energysys.connector.IJobExecuter;
import com.energysys.connector.config.ConnectorConfig;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.web.beans.EventLogBean;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.JobExecutionException;

/**
 *
 * Abstract class for all Job Controllers implementations.
 *
 * Executes the job and ensures that EventLog entries are created.
 *
 * @author EnergySys Limited
 */
public class AbstractJobController
{

  private static final Logger LOG = Logger.getLogger(AbstractJobController.class.getName());
  private static final String CLOSING_BRACKET = ")";

  /**
   * Enumeration for execution types.
   */
  public enum ExecutionType
  {
    /** Manual. **/
    MANUAL,
    /** Scheduled. **/
    SCHEDULED,
    /** Re-run. **/
    RE_RUN,
    /** Remote Execution. **/
    REMOTE
  };

  /**
   * Default Constructor.
   */
  public AbstractJobController()
  {
  }

  /**
   * Executes a job and adds the Event Log entries.
   *
   * @param aJobName
   * @param aJobTarget
   * @param aJobExecuterType
   * @param aTargetFireTime
   * @param anExecutionType
   * @return te result
   * @throws JobExecutionException
   */
  @SuppressWarnings(
      {
        "checkstyle:multiplestringliterals", "checkstyle:illegalcatch", "UseSpecificCatch"
      })
  protected EventResult execute(String aJobName, String aJobTarget, String aJobExecuterType, Date aTargetFireTime,
                                ExecutionType anExecutionType)
          throws JobExecutionException
  {
    try
    {
      LOG.log(Level.INFO, "Job started: {0}", aJobName);
      if (aJobExecuterType == null)
      {
        throw new JobExecutionException("Invalid Job Executer Class: " + aJobExecuterType);
      }
      IJobExecuter myJobExecuter = (IJobExecuter) Class.forName(aJobExecuterType).newInstance();
      
      Integer myRetryCount = 0;
      EventResult myResult;
      
      // Execute the job
      myResult = executeJob(myJobExecuter, aJobName, aJobTarget, aTargetFireTime, myRetryCount, anExecutionType);

      ConnectorConfig aConnectorConfig = ConnectorConfig.loadFromKeystore();
      // If the job was not successful then retry (so long as the maxRetries isn't zero)
      while (!anExecutionType.equals(ExecutionType.MANUAL) && myRetryCount < aConnectorConfig.getJobMaxRetries()
          && myResult.getResult() != EventResult.Result.SUCCESS && myResult.getResult() != EventResult.Result.WARNINGS)
      {
        try
        {
          myRetryCount++;
          LOG.log(Level.INFO, "Job retrying in {0} seconds: {1} (Retry {2})", new Object[]
          {
            aConnectorConfig.getJobRepeatInterval(),
            aJobName, myRetryCount,
          });
          // Sleep for the retryInterval seconds then run again
          Thread.sleep(aConnectorConfig.getJobRepeatInterval() * 1000);
          myResult = executeJob(myJobExecuter, aJobName, aJobTarget, aTargetFireTime, myRetryCount, anExecutionType);
        }
        catch (InterruptedException ex)
        {
          myResult = new EventResult(aJobName, ex, CurrentDateTime.getCurrentTimeInMillis());
          logResult(myResult, myRetryCount, anExecutionType);
        }
      }
      
      return myResult;
    }
    catch (IllegalAccessException | ClassNotFoundException | InstantiationException ex)
    {
      throw new JobExecutionException("Invalid Job Executer Class: " + aJobExecuterType);
    }
    catch (ConnectorException myEx)
    {
      throw new JobExecutionException("Failed to execute Job", myEx);
    }
  }

  @SuppressWarnings("IllegalCatch")
  private EventResult executeJob(IJobExecuter anExecuter, String aJobName, String aJobTarget, Date aTargetFireTime,
      Integer aRetryCount, ExecutionType anExecutionType)
  {
    EventResult myResult;
    try
    {
      myResult = anExecuter.execute(aJobName, aJobTarget, aTargetFireTime, anExecutionType);

      logResult(myResult, aRetryCount, anExecutionType);
    }
    catch (Exception ex)
    {
      myResult = new EventResult(aJobName, ex, CurrentDateTime.getCurrentTimeInMillis());
      // If this is a retry then amend the result description

      logResult(myResult, aRetryCount, anExecutionType);
    }
    return myResult;

  }

  private void logResult(EventResult aResult, Integer aRetryCount, ExecutionType anExecutionType)
  {
    if (anExecutionType == ExecutionType.MANUAL)
    {
      aResult.appendDescription(" (Manual Execution)");
    }
    else if (anExecutionType == ExecutionType.RE_RUN)
    {
      aResult.appendDescription(" (Re-Run)");
    }
    else if (anExecutionType == ExecutionType.REMOTE)
    {
      aResult.appendDescription(" (Remote Execution)");
    }

    if (aRetryCount > 0)
    {
      aResult.appendDescription(" (Retry " + aRetryCount + CLOSING_BRACKET);
    }

    switch (aResult.getResult())
    {
      case SUCCESS:
        LOG.log(Level.INFO, "Job successful: {0} ({1}"
            + CLOSING_BRACKET, new Object[]
            {
              aResult.getEventDescription(), aResult.getSummaryMessage(),
            });
        break;
      case FAILED:
        LOG.log(Level.WARNING, "Job failed: {0} ({1}"
            + CLOSING_BRACKET, new Object[]
            {
              aResult.getEventDescription(), aResult.getSummaryMessage(),
            });
        break;
      case EXCEPTION:
      default:
        LOG.log(Level.SEVERE, "Job encounted error: {0} ({1}"
            + CLOSING_BRACKET, new Object[]
            {
              aResult.getEventDescription(), aResult.getSummaryMessage(),
            });
        break;
    }
    EventLogBean.addLog(aResult);

  }

}
