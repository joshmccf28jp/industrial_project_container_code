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
import com.energysys.connector.JobConfiguration;
import com.energysys.connector.database.GenericDAO;

import java.util.logging.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Main Quartz job class for running syncs.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class QuartzJobController extends AbstractJobController implements Job
{

  private static final Logger LOG = Logger.getLogger(QuartzJobController.class.getName());

  /**
   * Default Constructor.
   */
  public QuartzJobController()
  {
  }

  @Override
  @SuppressWarnings("IllegalCatch")
  public void execute(JobExecutionContext aJobExecutionContext) throws JobExecutionException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();

    LOG.info("Job initiated: " + aJobExecutionContext.getJobDetail().getKey().getName());

    EventResult myResult;
    try
    {
      ExecutionType myExecutionType = getExecutionType(aJobExecutionContext);

      switch (myExecutionType)
      {
        case REMOTE:
          myResult = executeRemote(aJobExecutionContext, myExecutionType);
          break;

        default:
          myResult = executeLocal(aJobExecutionContext, myExecutionType);
          break;
      }
    }
    catch (Throwable ex)
    {
      myResult = new EventResult("Job Execution Failed", ex, myStartTime);
    }
    LOG.info(
        "Job complete: " + myResult.getEventDescription() + ". Took: " + myResult.getRunDurationSeconds()
        + " seconds. Next fire time: " + aJobExecutionContext.getNextFireTime());

  }

  private EventResult executeLocal(JobExecutionContext aJobExecutionContext,
      ExecutionType myExecutionType) throws JobExecutionException, NumberFormatException
  {
    EventResult myResult;
    // Load Job from Local Database
    Integer myJobId = Integer.parseInt(aJobExecutionContext.getJobDetail().getKey().getName());
    JobConfiguration myJobConfig;
    try (GenericDAO myDAO = new GenericDAO())
    {
      myJobConfig = myDAO.findById(JobConfiguration.class, myJobId);
    }

    myResult = super.execute(
        myJobConfig.getName(),
        myJobConfig.getJobExecuterTarget(),
        myJobConfig.getJobExecuterType(),
        aJobExecutionContext.getScheduledFireTime(),
        myExecutionType);
    return myResult;
  }

  private EventResult executeRemote(JobExecutionContext aJobExecutionContext,
      ExecutionType myExecutionType) throws JobExecutionException
  {
    // Load RemoteQueryExecution from Local Database
    RemoteQueryExecution myRemoteQueryExecution;
    try (GenericDAO myDAO = new GenericDAO())
    {
      myRemoteQueryExecution = myDAO.findById(RemoteQueryExecution.class,
          aJobExecutionContext.getJobDetail().getKey().getName());
    }

    EventResult myResult = super.execute(
        myRemoteQueryExecution.getQueryName(),
        myRemoteQueryExecution.getGUID(),
        myRemoteQueryExecution.getExecuterType(),
        aJobExecutionContext.getScheduledFireTime(),
        myExecutionType);

    // Update the RemoteQueryExecution status
    switch (myResult.getResult())
    {
      case SUCCESS:
      case WARNINGS:
        myRemoteQueryExecution.setStatus(RemoteQueryExecution.Status.SUCCESS);
        break;
      case EXCEPTION:
      case FAILED:
      default:
        myRemoteQueryExecution.setStatus(RemoteQueryExecution.Status.FAILED);
    }
    myRemoteQueryExecution.setMessage(myResult.getSummaryMessage());
    try (GenericDAO myDAO = new GenericDAO())
    {
      myDAO.save(myRemoteQueryExecution);
    }
    return myResult;
  }

  private ExecutionType getExecutionType(JobExecutionContext aJobExecutionContext) throws JobExecutionException
  {
    final String myGroupName = aJobExecutionContext.getJobDetail().getKey().getGroup();
    if (myGroupName.equals(SchedulerManager.RERUN_GROUP_NAME))
    {
      return ExecutionType.RE_RUN;
    }
    else if (myGroupName.equals(SchedulerManager.SCHEDULED_GROUP_NAME))
    {
      return ExecutionType.SCHEDULED;
    }
    else if (myGroupName.equals(SchedulerManager.REMOTE_EXECUTION_GROUP_NAME))
    {
      return ExecutionType.REMOTE;
    }
    else
    {
      throw new JobExecutionException("Unknown Execution Type: " + myGroupName);
    }
  }
}
