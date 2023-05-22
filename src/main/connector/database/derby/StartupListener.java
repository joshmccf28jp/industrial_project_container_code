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
package com.energysys.connector.database.derby;

import com.energysys.connector.database.GenericDAO;
import com.energysys.connector.web.security.UserRole;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.cfg.Configuration;

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
  @SuppressWarnings({"UseSpecificCatch", "checkstyle:illegalcatch"})
  public void contextInitialized(ServletContextEvent aContextEvent)
  {
    System.out.println("Initialising Derby Database");
    try
    {
      GenericDAO.initSessionFactory(new Configuration()
              .configure() // configures settings from hibernate.cfg.xml
              .buildSessionFactory());
      initialiseDB();
    }
    catch (Throwable ex)
    {
      LOG.log(Level.SEVERE, "Error Initialising Derby Database", ex);
    }

  }

  @Override
  public void contextDestroyed(ServletContextEvent anEvent)
  {
  }

  private void initialiseDB()
  {
    // If there are no users then create the default one
    try (GenericDAO myDAO = new GenericDAO(false))
    {
      if (myDAO.countAll(UserRole.class) == 0)
      {
        UserRole myAdminRole = new UserRole();
        myAdminRole.setName("Admin");
        myAdminRole.setRole("ADMIN_ROLE");
        myDAO.save(myAdminRole);

        UserRole myUserRole = new UserRole();
        myUserRole.setName("User");
        myUserRole.setRole("USER_ROLE");
        myDAO.save(myUserRole);

        UserRole myConfigRole = new UserRole();
        myConfigRole.setName("Config");
        myConfigRole.setRole("CONFIG_ROLE");
        myDAO.save(myConfigRole);
      }
    }
  }
}
