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

import com.energysys.connector.schedulers.quartz.AbstractJobController;
import com.energysys.connector.schedulers.quartz.RemoteQueryExecution;
import java.util.Date;

/**
 * Interface for JobExecuters.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public interface IJobExecuter
{

  /**
   * Execute based given a job name and a target id.
   *
   * @param aJobName display name of job
   * @param aTargetId job target id
   * @param aTargetRuntTime
   * @param aType
   * @return the result of the sync
   */
  EventResult execute(String aJobName, String aTargetId, Date aTargetRuntTime,
      AbstractJobController.ExecutionType aType);

  /**
   * Returns whether this Job Executor supports re-runs.
   * @return if supported
   */
  Boolean supportsReRuns();

  /**
   * Validate a remote Query Execution.
   * @param aRequestName
   * @param aRemoteQueryExecution
   * @return the result
   */
  EventResult validateRemoteQueryExecution(String aRequestName, RemoteQueryExecution aRemoteQueryExecution);

}
