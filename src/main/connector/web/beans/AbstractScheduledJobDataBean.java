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
import com.energysys.connector.IJobExecuter;
import com.energysys.connector.JobConfiguration;
import com.energysys.connector.database.GenericDAO;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.schedulers.quartz.SchedulerManager;
import com.energysys.connector.web.IIdentifiable;

import javax.faces.context.FacesContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract class performing most of the basic functionality required for a JSF Backing Bean for Job Scheduling. The
 * only behaviour that this class doesn't dictate is the list of available JobExecuters that are available in the UI and
 * the mapping of JobExecuter classes to their corresponding JobTarget types.
 *
 * @author EnergySys Limited
 */
public abstract class AbstractScheduledJobDataBean
        extends AbstractGenericDAOBackingBean<JobConfiguration>
{

    private static final Logger LOG = Logger.getLogger(AbstractScheduledJobDataBean.class.getName());
    private static final String RUN_JOB_TEXT = ":Run Job";
    private static final String TOOK_TEXT = "Took ";
    private static final String NEW_LINE = "\n";
    private static final String SECONDS_TEXT = " seconds.";
    private static final String SEE_EVENT_LOG_FOR_DETAILS_TEXT = "See Event Log for details";

    private List<Executer> theExecuters;
    private Map<String, Executer> theExecuterMap;

    private Date theReRunFromDate = Date.from(LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault()).
            toInstant());
    private Date theReRunToDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());

    public Date getReRunFromDate()
    {
        return theReRunFromDate;
    }

    public void setReRunFromDate(Date theReRunFromDate)
    {
        this.theReRunFromDate = theReRunFromDate;
    }

    public Date getReRunToDate()
    {
        return theReRunToDate;
    }

    public void setReRunToDate(Date theReRunToDate)
    {
        this.theReRunToDate = theReRunToDate;
    }

    /**
     * Returns a list of the available JobExecuters.
     *
     * @return the list of job executers
     */
    protected abstract List<Class> getJobExecuterClasses();

    /**
     * Returns the target type for the job executer.
     *
     * @param aClass the executer
     * @return the target type
     */
    protected abstract Class getJobTargetClassFor(Class aClass);

    private Map<String, Executer> getExecuterMap()
    {
        if (theExecuterMap == null)
        {
            getJobExecuters();
        }
        return theExecuterMap;
    }

    /**
     * Lists the Executers (a ui wrapper for a JobExecuter class).
     *
     * @return the list of executers
     */
    public List<Executer> getJobExecuters()
    {
        if (theExecuters == null)
        {
            try (GenericDAO myDAO = new GenericDAO())
            {
                theExecuters = new ArrayList<>();
                theExecuterMap = new HashMap();

                for (Class myClass : getJobExecuterClasses())
                {
                    Class myTargetClass = getJobTargetClassFor(myClass);
                    List myJobExectuterTargets = myDAO.list(myTargetClass);

                    List<IIdentifiable> myTargets = new ArrayList<>();
                    myTargets.addAll(myJobExectuterTargets);
                    Executer myExecuter = new Executer(myClass, myTargets);
                    theExecuters.add(myExecuter);
                    theExecuterMap.put(myClass.getName(), myExecuter);
                }
            }
            catch (InstantiationException | IllegalAccessException ex)
            {
                addGrowlMessage(
                        new EventResult("Failed to initialise screen",
                                ex,
                                CurrentDateTime.getCurrentTimeInMillis()));
            }
        }

        return theExecuters;
    }

    /**
     * Gets the Executer for a given JobConfiguration.
     *
     * @param aJob a job
     * @return the Executer
     */
    public Executer getExecuterForJob(JobConfiguration aJob)
    {
        Executer myExecuter = getExecuterMap().get(aJob.getJobExecuterType());
        if (myExecuter != null)
        {
            return myExecuter;
        }
        else
        {
            return getJobExecuters().get(0);
        }
    }

    public List<JobConfiguration.Repeat> getRepeatIntervals()
    {
        return Arrays.asList(JobConfiguration.Repeat.values());
    }

    @Override
    protected Class getDataClass()
    {
        return JobConfiguration.class;
    }

    @Override
    protected void postSaveUpdateProcess(JobConfiguration aDataObject) throws ConnectorException
    {
        SchedulerManager myScheduler = new SchedulerManager();
        myScheduler.updateTrigger(aDataObject);
    }

    @Override
    protected void postSaveCreateProcess(JobConfiguration aDataObject) throws ConnectorException
    {
        SchedulerManager myScheduler = new SchedulerManager();
        myScheduler.createJob(aDataObject);
    }

    @Override
    protected void postDeleteProcess(JobConfiguration aDataObject) throws ConnectorException
    {
        SchedulerManager myScheduler = new SchedulerManager();
        myScheduler.removeJob(aDataObject);
    }

    @Override
    protected Boolean preAddProcess(JobConfiguration aDataObject)
    {
        aDataObject.setJobExecuterType(getJobExecuters().get(0).theClass.getName());
        aDataObject.setJobExecuterTarget(getJobExecuters().get(0).theTargets.get(0).getId().toString());
        aDataObject.setRepeatValue(1);
        return true;
    }

    @Override
    protected Boolean preSaveProcess(JobConfiguration aDataObject)
    {
        if (aDataObject.getStartDate() == null)
        {
            aDataObject.setStartDate(Date.from(ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES).toInstant()));
        }
        if (aDataObject.getRepeatInterval() == JobConfiguration.Repeat.NEVER)
        {
            aDataObject.setRepeatValue(null);
        }
        else if (aDataObject.getRepeatValue() == null)
        {
            FacesContext.getCurrentInstance().validationFailed();
            addGrowlMessage(new EventResult("", EventResult.Result.FAILED,
                            "Repeat Value: Validation Error", "Value is required if Repeat Interval is not NEVER",
                            CurrentDateTime.getCurrentTimeInMillis()),
                    false, true);
            return false;
        }
        return super.preSaveProcess(aDataObject);
    }

    /**
     * Runs the S3 Uploader for the selected row.
     *
     * @param aDataObject a data object
     */
    @SuppressWarnings("IllegalCatch")
    public void runJob(JobConfiguration aDataObject)
    {
        Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
        String myMessage;
        try
        {
            SchedulerManager myScheduler = new SchedulerManager();
            EventResult myResult = myScheduler.runJobNow(aDataObject);

            switch (myResult.getResult())
            {
                case SUCCESS:
                    addGrowlMessage(new EventResult(
                                    getDataClass().getSimpleName() + RUN_JOB_TEXT,
                                    EventResult.Result.SUCCESS,
                                    "Job Ran",
                                    TOOK_TEXT + myResult.getRunDurationSeconds() + SECONDS_TEXT
                                            + NEW_LINE + SEE_EVENT_LOG_FOR_DETAILS_TEXT,
                                    myStartTime),
                            false, true);
                    break;

                case WARNINGS:
                    addGrowlMessage(new EventResult(
                            getDataClass().getSimpleName() + RUN_JOB_TEXT,
                                    EventResult.Result.WARNINGS,
                                    "Job Ran with Warnings",
                                    TOOK_TEXT + myResult.getRunDurationSeconds()
                                            + SECONDS_TEXT + NEW_LINE + SEE_EVENT_LOG_FOR_DETAILS_TEXT,
                                    myStartTime),
                            false, true);
                    break;

                default:
                    addGrowlMessage(new EventResult(
                            getDataClass().getSimpleName() + RUN_JOB_TEXT,
                                    myResult.getResult(),
                                    "Job Failed",
                                    TOOK_TEXT + myResult.getRunDurationSeconds()
                                            + SECONDS_TEXT + NEW_LINE + SEE_EVENT_LOG_FOR_DETAILS_TEXT,
                                    myStartTime),
                            false, true);
            }
        }
        catch (Exception myEx)
        {
            addGrowlMessage(myEx);
        }
    }

    /**
     * Re-runs a job.
     *
     * @param aDataObject
     */
    @SuppressWarnings("IllegalCatch")
    public void rerunJob(JobConfiguration aDataObject)
    {
        LOG.info("RUNNING RE-RUN JOB (From: " + theReRunFromDate.toString() + ", To: " + theReRunToDate.toString());
        Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();

        String myMessage;
        try
        {
            SchedulerManager myScheduler = new SchedulerManager();
            EventResult myResult = myScheduler.rerunJob(aDataObject, theReRunFromDate, theReRunToDate);

            addGrowlMessage(myResult,
                    false, true);

        }
        catch (Exception myEx)
        {
            addGrowlMessage(myEx);
        }
    }

    /**
     * Whether this data bean supports re-runs.
     *
     * @return if supported
     */
    public Boolean supportsReRuns()
    {
        for (Executer theExecuter : getJobExecuters())
        {
            if (theExecuter.supportsReRuns())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this bean supports re-runs given a job configuration.
     *
     * @param aDataObject
     * @return if re-runs are supported
     */
    public Boolean supportsReRuns(JobConfiguration aDataObject)
    {
        return getExecuterForJob(aDataObject).supportsReRuns();
    }

    /**
     * UI wrapper for displaying and managing JobExecuters and the available list of targets for that Executer type.
     * This is used by the UI to populate the list of Job Targets when a JobExecter is chosen.
     */
    public class Executer
    {

        private Class<IJobExecuter> theClass;
        private List<IIdentifiable> theTargets;
        private Boolean theSupportsReRuns;

        /**
         * Basic constructor.
         *
         * @param aClass      the executer class
         * @param someTargets the available targets
         */
        public Executer(Class<IJobExecuter> aClass, List<IIdentifiable> someTargets) throws InstantiationException,
                IllegalAccessException
        {
            this.theClass = aClass;
            this.theTargets = someTargets;
            IJobExecuter myJobExecuter = aClass.newInstance();
            this.theSupportsReRuns = myJobExecuter.supportsReRuns();
        }

        public Class getClazz()
        {
            return theClass;
        }

        /**
         * Lists the valid set of targets.
         *
         * @return list of targets
         */
        public List<IIdentifiable> getTargets()
        {
            return theTargets;
        }

        /**
         * Gets the target object given it's id.
         *
         * @param anId the id
         * @return the target object
         */
        public IIdentifiable getTargetWithID(String anId)
        {
            for (IIdentifiable myTarget : theTargets)
            {
                if (myTarget.getId().toString().equals(anId))
                {
                    return myTarget;
                }
            }

            return null;
        }

        /**
         * Returns whether this bean supports re-runs.
         *
         * @return if re-runs are supported
         */
        public Boolean supportsReRuns()
        {
            return theSupportsReRuns;
        }
    }

}
