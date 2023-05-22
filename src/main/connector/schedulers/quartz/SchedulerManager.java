/*
 * Copyright 2015 EnergySys Limited. All Rights Reserved.
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
import com.energysys.connector.connectors.dataconnector.odata.EsysOdataConnectionCredentials;
import com.energysys.connector.database.GenericDAO;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.web.beans.EventLogBean;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.CalendarIntervalScheduleBuilder;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

/**
 * This class is responsible for scheduler management.
 *
 * @author EnergySys Limited
 * @version $Revision$ Last modified by: $Author$
 */
public class SchedulerManager
{

  /** Scheduled jobs Quartz group. **/
  public static final String SCHEDULED_GROUP_NAME = "scheduled.group";
  /** Re-run jobs Quartz group. **/
  public static final String RERUN_GROUP_NAME = "rerun.group";
  /** Remote Execution jobs Quartz group. **/
  public static final String REMOTE_EXECUTION_GROUP_NAME = "remote.execution.group";

  private static final Logger LOG = Logger.getLogger(SchedulerManager.class.getName());
  private Scheduler theScheduler;

  /**
   * Constructor.
   *
   * @throws ConnectorException if scheduler cannot be located
   */
  public SchedulerManager() throws ConnectorException
  {
    try
    {
      StdSchedulerFactory myFactory = new StdSchedulerFactory();
      theScheduler = myFactory.getScheduler();
      LOG.fine("Quartz Scheduler loaded");
    }
    catch (SchedulerException myEx)
    {
      throw new ConnectorException("Scheduler instance could not be created", myEx);
    }
  }

  /**
   * Starts the scheduler.
   *
   * @throws ConnectorException
   */
  public void start() throws ConnectorException
  {
    try
    {
      // Schedule all jobs in the database (JobConfigurations and RemoteQueryExecutions)
      refreshScheduledJobs();
      // Start the scheduler
      theScheduler.start();
      LOG.info("Quartz Scheduler started: " + theScheduler.getSchedulerName());

      // Schedule the RemoteQueryExecution sync job
      scheduleRemoteQuerySyncJob();
    }
    catch (SchedulerException myEx)
    {
      throw new ConnectorException("Error starting scheduler", myEx);
    }
  }

  /**
   * Checks that a job exists.
   * @param aJobId
   * @param aGroupName
   * @return if exists
   * @throws ConnectorException
   */
  public Boolean checkJobExists(String aJobId, String aGroupName) throws ConnectorException
  {
    try
    {
      JobKey myJobKey = new JobKey(aJobId, aGroupName);
      return theScheduler.checkExists(myJobKey);
    }
    catch (SchedulerException ex)
    {
      throw new ConnectorException(
          MessageFormat.format(
              "Error Checking Job exists: {0} in group {1}",
              aJobId,
              aGroupName), ex);
    }
  }
  
  /**
   * Creates a Job for a RemoteQueryExecution.
   * @param myRemoteQueryExecution
   * @throws ConnectorException 
   */
  public void createRemoteJob(RemoteQueryExecution myRemoteQueryExecution) throws ConnectorException
  {
    TriggerKey myKey = new TriggerKey(myRemoteQueryExecution.getGUID(), REMOTE_EXECUTION_GROUP_NAME);

    try
    {
        // Build one off trigger
      TriggerBuilder myTriggerBuilder = TriggerBuilder.newTrigger()
          .withIdentity(myKey)
          .startAt(myRemoteQueryExecution.getExecutionDateTime())
          .withSchedule(SimpleScheduleBuilder.simpleSchedule()
            .withMisfireHandlingInstructionFireNow());

      // Build Job
      JobKey myJobKey = new JobKey(myRemoteQueryExecution.getGUID(), REMOTE_EXECUTION_GROUP_NAME);
      JobDetail myJob = JobBuilder.newJob(QuartzJobController.class)
          .withIdentity(myJobKey)
          .build();

      // Schedule the Job
      theScheduler.scheduleJob(myJob, myTriggerBuilder.build());
      LOG.info(
          MessageFormat.format(
              "Remote Query Execution scheduled: {0} ", myRemoteQueryExecution));

    }
    catch (SchedulerException myEx)
    {
      throw new ConnectorException(
          MessageFormat.format(
              "Error Scheduling Remote Query Execution to run {0} with Parameters ''{1}'' at ''{2}''",
              myRemoteQueryExecution.getQueryName(),
              myRemoteQueryExecution.getParameters(),
              myRemoteQueryExecution.getExecutionDateTime()), myEx);
    }
  }


  /**
   * Ensures that the scheduler is up to date with the currently configured jobs.
   *
   * @throws ConnectorException on error
   */
  public void refreshScheduledJobs() throws ConnectorException
  {
    try
    {
      LOG.info("Refreshing Scheduled Jobs");
      //Clear state
      //theScheduler.pauseAll();
      theScheduler.clear();
      createLocalAndRemoteJobs();
     // theScheduler.resumeAll();
    }
    catch (SchedulerException myEx)
    {
      throw new ConnectorException("Error adding jobs to scheduler", myEx);
    }
  }

  private void createLocalAndRemoteJobs()
  {
    try (GenericDAO genericDAO = new GenericDAO(false))
    {
      // Get the persisted JobConfigurations
      List<JobConfiguration> myJobs = genericDAO.list(JobConfiguration.class);
      //Create the Scheduled jobs
      for (JobConfiguration myJob : myJobs)
      {
        try
        {
          LOG.info("Sheduling local Job: " + myJob.toString());
          createJob(myJob);
        }
        catch (ConnectorException myEx)
        {
          LOG.log(Level.SEVERE, "Failed to schedule job: " + myJob.getName(), myEx);
        }
      }
      
      // Get the persisted RemoteQueryExecutions
      List<RemoteQueryExecution> myRemoteQueryExecutions = genericDAO.list(RemoteQueryExecution.class);
      //Schedule the jobs
      for (RemoteQueryExecution myRemoteQueryExecution : myRemoteQueryExecutions)
      {
        try
        {
          if (myRemoteQueryExecution.getStatus() == RemoteQueryExecution.Status.ACCEPTED
              || myRemoteQueryExecution.getStatus() == RemoteQueryExecution.Status.QUEUED)
          {
            LOG.info("Sheduling remote Job: " + myRemoteQueryExecution.toString());
            createRemoteJob(myRemoteQueryExecution);
          }
        }
        catch (ConnectorException myEx)
        {
          LOG.log(Level.SEVERE, "Failed to schedule job for Remote Query Execution: " + myRemoteQueryExecution, myEx);
        }
      }
    }
  }

  /**
   * Clear the scheduled jobs state.
   *
   * @throws ConnectorException on error.
   */
  public void clearJobs() throws ConnectorException
  {
    try
    {
      theScheduler.clear();
    }
    catch (SchedulerException myEx)
    {
      throw new ConnectorException("Error clearing jobs from scheduler", myEx);
    }
  }

  /**
   * Schedules a new job.
   *
   * @param aJobConfig to be scheduled.
   * @throws ConnectorException on error
   */
  public void createJob(JobConfiguration aJobConfig) throws ConnectorException
  {
    // Only add if the job is active, has an end date in the future, and isn't a NEVER Repeat
    // that has a start date in the past.
    if (aJobConfig.getIsActive()
        && isJobEndDateNullOrInFuture(aJobConfig)
        && isJobRepeatingAndStartDateInPast(aJobConfig))

    {
      Trigger myTrigger = generateTrigger(aJobConfig, false, SCHEDULED_GROUP_NAME);

      LOG.info("Job schduled: " + aJobConfig.getName());
      try
      {
        JobDetail myJob = JobBuilder.newJob(QuartzJobController.class).withIdentity(aJobConfig.getId().toString(),
            SCHEDULED_GROUP_NAME)
            .build();
        theScheduler.scheduleJob(myJob, myTrigger);
      }
      catch (SchedulerException myEx)
      {
        throw new ConnectorException("Error scheduling job: " + aJobConfig.getName(), myEx);
      }
    }
  }

  private static boolean isJobRepeatingAndStartDateInPast(JobConfiguration aJobConfig)
  {
    return aJobConfig.getRepeatInterval() != JobConfiguration.Repeat.NEVER || aJobConfig.getStartDate().after(
            CurrentDateTime.getCurrentDate());
  }

  private static boolean isJobEndDateNullOrInFuture(JobConfiguration aJobConfig)
  {
    return aJobConfig.getEndDate() == null || aJobConfig.getEndDate().after(CurrentDateTime.getCurrentDate());
  }

  /**
   * Updates the jobs trigger.
   *
   * @param aJobConfig config
   * @throws ConnectorException on error
   */
  public void updateTrigger(JobConfiguration aJobConfig) throws ConnectorException
  {
    removeJob(aJobConfig);
    createJob(aJobConfig);
  }

  /**
   * Remove the job from the scheduler.
   *
   * @param aJobConfig the job
   * @throws ConnectorException on error
   */
  public void removeJob(JobConfiguration aJobConfig) throws ConnectorException
  {
    try
    {
      JobKey myKey = JobKey.jobKey(aJobConfig.getId().toString(), SCHEDULED_GROUP_NAME);

      theScheduler.deleteJob(myKey);
      LOG.info("Job removed from scheduler: " + aJobConfig.getName());
    }
    catch (SchedulerException myEx)
    {
      throw new ConnectorException("Error removing job from scheduler:" + aJobConfig.getName(), myEx);
    }
  }

  private Trigger generateTrigger(JobConfiguration aJobConfig, Boolean isRerun, String aGroupName) throws
      ConnectorException
  {
    if (aJobConfig.getRepeatInterval() != JobConfiguration.Repeat.NEVER && aJobConfig.getRepeatValue() == null)
    {
      throw new ConnectorException(aJobConfig.getName() + " Job Config has null Repeat Value and non NEVER interval");
    }

    //Get the necessary values for trigger creation
    ScheduleBuilder mySchedule = generateSchedule(aJobConfig);

    TriggerBuilder<Trigger> myTriggerBuilder = TriggerBuilder.newTrigger().withIdentity(aJobConfig.getId().toString(),
        aGroupName).startAt(aJobConfig.getStartDate());

    // If the job has a schedule, add end date and schedule
    if (mySchedule != null)
    {
      myTriggerBuilder = myTriggerBuilder.endAt(aJobConfig.getEndDate()).withSchedule(mySchedule);
    }

    if (isRerun)
    {
      myTriggerBuilder.startAt(aJobConfig.getStartDate());
    }
    else
    {
      // If job is not in future then we have to roll forward the start time to the first calculated run
      // that would happen after NOW.
      // This is because the Quartz Scheduler always runs the job straight away if the start time in in the past
      Date myNow = CurrentDateTime.getCurrentDate();
      if (aJobConfig.getStartDate().before(myNow) && mySchedule != null)
      {
        Trigger myTrigger = myTriggerBuilder.build();
        myTriggerBuilder.startAt(myTrigger.getFireTimeAfter(myNow));
      }
    }

    return myTriggerBuilder.build();
  }

  private ScheduleBuilder generateSchedule(JobConfiguration aJobConfig)
  {
    ScheduleBuilder mySchedule;
    switch (aJobConfig.getRepeatInterval())
    {
      case NEVER:
        mySchedule = null;
        break;

      case MINUTES:
        mySchedule = CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInMinutes(aJobConfig.
            getRepeatValue()).withMisfireHandlingInstructionIgnoreMisfires();
        break;

      case DAYS:
        mySchedule = CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInDays(aJobConfig.
            getRepeatValue()).withMisfireHandlingInstructionIgnoreMisfires().preserveHourOfDayAcrossDaylightSavings(
                true);
        break;

      case WEEKS:
        mySchedule = CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInWeeks(aJobConfig.
            getRepeatValue()).withMisfireHandlingInstructionIgnoreMisfires().preserveHourOfDayAcrossDaylightSavings(
                true);
        break;

      case MONTHS:
        mySchedule = CalendarIntervalScheduleBuilder.calendarIntervalSchedule().withIntervalInMonths(aJobConfig.
            getRepeatValue()).withMisfireHandlingInstructionIgnoreMisfires().preserveHourOfDayAcrossDaylightSavings(
                true);
        break;

      case HOURS:
      default:

        // If it's for 1 hour then use the Quartz scheduler as this ensures job is run even on daylight saving
        // switchovers (cron missed an hour not work when time "goes back").
        if (aJobConfig.getRepeatValue() == 1)
        {
          mySchedule = CalendarIntervalScheduleBuilder.calendarIntervalSchedule()
            .withIntervalInHours(aJobConfig.getRepeatValue())
            .withMisfireHandlingInstructionIgnoreMisfires()
            .preserveHourOfDayAcrossDaylightSavings(true);
        }
        // Otherwise use Quartz, as this preserves the "hour" of the run time across daylight saving switchovers
        // (quartz ends up changing the hour after a clock change, for a random amount of time!!)
        else
        {
          mySchedule = createCronSchedule(aJobConfig);
        }

        break;
    }
    return mySchedule;
  }

  private ScheduleBuilder createCronSchedule(JobConfiguration aJobConfig)
  {
    ScheduleBuilder mySchedule;
    // Work out cron start time based on job start time and repeat interval
    // i.e. if start time is 05:00 and repeat is 4 hours then actual cron start time will be
    // 01:00
    ZonedDateTime myStartTime = ZonedDateTime.ofInstant(aJobConfig.getStartDate().toInstant(),
        ZoneId.systemDefault());
    // Roll back start time as far as possible for day based on hourly interval
    while (myStartTime.getHour() - aJobConfig.getRepeatValue() >= 0)
    {
      myStartTime = myStartTime.minus(aJobConfig.getRepeatValue(), ChronoUnit.HOURS);
    }
    // Build cron to run every x hours starting at first instance for day
    StringBuilder myCronString = new StringBuilder();
    myCronString.append("0 ");
    myCronString.append(myStartTime.getMinute());
    myCronString.append(" ");
    myCronString.append(myStartTime.getHour());
    myCronString.append("/");
    myCronString.append(aJobConfig.getRepeatValue());
    myCronString.append(" ? * *");
    mySchedule = CronScheduleBuilder.cronSchedule(myCronString.toString()).
        withMisfireHandlingInstructionIgnoreMisfires();
    return mySchedule;
  }

  /**
   * Runs the job now.
   *
   * @param aDataObject a data object
   * @return the result
   */
  public EventResult runJobNow(JobConfiguration aDataObject) throws JobExecutionException
  {
    QuartzJobController myJobController = new QuartzJobController();
    return myJobController.execute(
        aDataObject.getName(), 
        aDataObject.getJobExecuterTarget(), 
        aDataObject.getJobExecuterType(),
        CurrentDateTime.getCurrentDate(),
        AbstractJobController.ExecutionType.MANUAL);
  }

  /**
   * Schedules a job to "re-run" firing as many times as needed to catch up from a given start date to a given end
   * date.
   *
   * Uses quartz in-built ability to catch up trigger misfires to run the process as many times as needed to run from
   * the given start to end date.
   *
   * @param anOrigJobConfig
   * @param aStartDate
   * @param anEndDate
   * @return the result
   */
  public EventResult rerunJob(JobConfiguration anOrigJobConfig, Date aStartDate, Date anEndDate)
  {
    // Record start time for EventResult
    Long myStartMethodTimestamp = CurrentDateTime.getCurrentTimeInMillis();
    DateFormat myFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    final String myEventResultDescription
        = MessageFormat.format("Request to Re-Run {0} for period {1} to {2}.", anOrigJobConfig.getName(), myFormat.
            format(aStartDate), myFormat.
            format(anEndDate));

    EventResult myResult;
    try
    {
      // Check that the End Date is not in the future
      if (anEndDate.after(CurrentDateTime.getCurrentDate()))
      {
        myResult = new EventResult(myEventResultDescription, EventResult.Result.FAILED, "Validation Error",
            "To date cannot be in future",
            myStartMethodTimestamp);
        return myResult;
      }
      // Check that the Start Date is not before the End Date
      if (aStartDate.after(anEndDate))
      {
        myResult = new EventResult(myEventResultDescription, EventResult.Result.FAILED, "Validation Error",
            "To Date must be after From Date",
            myStartMethodTimestamp);
        return myResult;
      }

      // Check that re-run for same job not running already.
      JobKey myJobKey = new JobKey(anOrigJobConfig.getId().toString(), RERUN_GROUP_NAME);
      if (theScheduler.checkExists(myJobKey))
      {
        myResult = new EventResult(myEventResultDescription, EventResult.Result.FAILED, "Re-Run Job Failed",
            MessageFormat.format("Previous re-run for {0} still running.\n Cannot re-run same job sumultaneously.",
                anOrigJobConfig.getName()),
            myStartMethodTimestamp);
        return myResult;
      }

      // The start time of the re-run job needs to be at a time that is synced with the original jobs
      // start time so the "FireTime"s are the same.
      // Generate a trigger based on the orginal job so we can work out the corrected start time of the re-run.
      // Use parameter "isRerun" = true so the start date is not updated to first future fire time.
      Trigger myOrigTrigger = generateTrigger(anOrigJobConfig, true, SCHEDULED_GROUP_NAME);

      Date myNewStartDate;
      if (aStartDate.equals(myOrigTrigger.getStartTime()))
      {
        myNewStartDate = anOrigJobConfig.getStartDate();
      }
      else
      {
        // This may return null if there is no fire time before the end date
        myNewStartDate = myOrigTrigger.getFireTimeAfter(aStartDate);
      }

      // Check that the End Date is not after the original End Date
      if (anOrigJobConfig.getEndDate() != null && anEndDate.after(anOrigJobConfig.getEndDate()))
      {
        anEndDate = anOrigJobConfig.getEndDate();
      }

      // Check that the job was actually due to run between these times (start time may be null)
      if (myNewStartDate == null || myNewStartDate.after(anEndDate))
      {
        myResult = new EventResult(myEventResultDescription, EventResult.Result.FAILED, "Re-Run Job Failed",
            "Job was not scheduled to run between the selected dates",
            myStartMethodTimestamp);
        return myResult;
      }

      // Generate a Trigger based on the original job, but with the adjusted start and end times.
      // Copy the original job (so changes to original arn't refleced in the UI) and set
      // the start and end time 
      JobConfiguration myJobConfig = anOrigJobConfig.copy();
      myJobConfig.setStartDate(myNewStartDate);
      myJobConfig.setEndDate(anEndDate);

      // Generate the new job and trigger, then schedule it
      JobDetail myJob = JobBuilder.newJob(QuartzJobController.class).withIdentity(myJobConfig.getId().toString(),
          RERUN_GROUP_NAME)
          .build();
      Trigger myTrigger = generateTrigger(myJobConfig, true, RERUN_GROUP_NAME);
      theScheduler.scheduleJob(myJob, myTrigger);

      myResult = new EventResult(myEventResultDescription, EventResult.Result.SUCCESS,
          MessageFormat.format("Re-Run Job Scheduled Successfully", myFormat.format(aStartDate), myFormat.format(
              anEndDate)),
          "See event log for indivdual results", myStartMethodTimestamp);

      EventLogBean.addLog(myResult);
      LOG.info("Job Re-Run scheduled: " + myJobConfig.getName());

    }
    catch (ConnectorException myEx)
    {
      myResult = new EventResult("Re-Run " + anOrigJobConfig.getName(), myEx, myStartMethodTimestamp);
      EventLogBean.addLog(myResult);
    }
    catch (SchedulerException myEx)
    {
      myResult = new EventResult("Re-Run " + anOrigJobConfig.getName(), myEx, myStartMethodTimestamp);
      EventLogBean.addLog(myResult);
    }
    return myResult;
  }

  private void scheduleRemoteQuerySyncJob() throws SchedulerException
  {
    try
    {
      EsysOdataConnectionCredentials myODataCredentials = EsysOdataConnectionCredentials.loadFromKeystore();
      Integer interval = myODataCredentials.getRemoteExecutionCheckIntervalMins();
      // Build trigger that runs every x mins
      TriggerBuilder myTriggerBuilder = TriggerBuilder.newTrigger()
              .withIdentity(new TriggerKey("RefreshRemoteQueries"))
              .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(interval).repeatForever());

      // Build Job
      JobKey myJobKey = new JobKey("RefreshRemoteQueries");

      JobDetail myJob = JobBuilder.newJob(SynchronizeRemoteQueriesController.class)
              .withIdentity(myJobKey)
              .build();

      // Schedule the Job
      theScheduler.scheduleJob(myJob, myTriggerBuilder.build());
    }
    catch (ConnectorException ex)
    {
      throw new SchedulerException(ex);
    }
  }
}
