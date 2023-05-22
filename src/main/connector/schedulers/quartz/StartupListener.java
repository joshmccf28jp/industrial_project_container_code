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

import com.energysys.connector.exception.ConnectorSystemException;
import com.energysys.connector.util.TimeZoneUtil;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Startup listener for the servlet context.
 *
 * This class should initialise any deployment dependent services, such as the quartz scheduler and the database
 * connection.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class StartupListener implements ServletContextListener
{
  private static final Logger LOG = Logger.getLogger(StartupListener.class.getName());

  /**
   * Default Constructor.
   */
  public StartupListener()
  {
  }

  @Override
  @SuppressWarnings({"checkstyle:illegalcatch", "UseSpecificCatch"})
  public void contextInitialized(ServletContextEvent aContextEvent)
  {
    LOG.info("Starting Quartz SchedulerManager");
    try
    {
      // Initialise TimeZone
      TimeZoneUtil.loadTimeZoneFromCredentials();
      SchedulerManager mySheduler = new SchedulerManager();
      mySheduler.start();
    }
    catch (Exception ex)
    {
      throw new ConnectorSystemException("Error starting up scheduler", ex);
    }

  }

  @Override
  @SuppressWarnings({"checkstyle:illegalcatch", "UseSpecificCatch"})
  public void contextDestroyed(ServletContextEvent aContextEvent)
  {
    try
    {
      SchedulerManager myScheduler = new SchedulerManager();
      myScheduler.clearJobs();
    }
    catch (Exception ex)
    {
      LOG.log(Level.SEVERE, "Error shutting down Quartz SchudulerManager", ex);
    }
  }


}
