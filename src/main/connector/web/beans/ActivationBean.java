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
import com.energysys.connector.database.GenericDAO;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.web.security.UserDetails;
import com.energysys.connector.web.security.UserRole;
import org.eclipse.jetty.util.security.Credential;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import java.util.Map;

/**
 * JSF bean for the activation process. Activation is the process which has to happen when there are no users in the
 * database.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@ManagedBean(name = "ActivationBean")
@RequestScoped
public class ActivationBean extends AbstractEventLoggingBackingBean
{
  
  private static final String EVENT_DESCRIPTION = "SystemActivation";

  private String username;
  private String newPassword;
  private String confirmPassword;

  /**
   * Default Constructor.
   */
  public ActivationBean()
  {
  }

  /**
   * UI submit process.
   *
   * @return redirect url
   * @throws com.energysys.connector.exception.ConnectorException
   */
  @SuppressWarnings(
  {
    "checkstyle:illegalcatch", "UseSpecificCatch", "checkstyle:returncount"
  })
  public String submit() throws ConnectorException
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    if (isSystemActivated())
    {
      return "dashboard.xhtml?faces-redirect=true";
    }
    
    if (!newPassword.equals(confirmPassword))
    {
      addGrowlMessage(new EventResult(EVENT_DESCRIPTION, EventResult.Result.FAILED,
                      "Passwords don't match", "", myStartTime));
      return null;
    }
    
    try (GenericDAO myDAO = new GenericDAO())
    {
      if (!myDAO.list(UserDetails.class, Map.of("username", username)).isEmpty())
      {
        addGrowlMessage(new EventResult(EVENT_DESCRIPTION, EventResult.Result.FAILED,
                        "Username already exists", "", myStartTime));
        return null;
      }

      UserRole myAdminRole = (UserRole) myDAO.list(UserRole.class, Map.of("theName", "Admin")).get(0);

      UserDetails myActivateUser = new UserDetails();
      myActivateUser.setUsername(username);
      myActivateUser.setRole(myAdminRole);
      myActivateUser.setPassword(Credential.MD5.digest(newPassword));
      myDAO.save(myActivateUser);

      addGrowlMessage(new EventResult(EVENT_DESCRIPTION, EventResult.Result.SUCCESS,
                      "Connector has been activated", "Now please log in", myStartTime));
    }
    catch (Exception myEx)
    {
      addGrowlMessage(myEx);
      return null;
    }

    return "dashboard.xhtml?faces-redirect=true";
  }

  /**
   * Returns whether the system is activated or not.
   *
   * @return if activated.
   * @throws com.energysys.connector.exception.ConnectorException
   */
  @SuppressWarnings("checkstyle:IllegalCatch")
  public Boolean isSystemActivated() throws ConnectorException
  {
    // Updating activation check from activation code (will no longer exist) to a check for existing users.
    try (GenericDAO userDAO = new GenericDAO()) {
      return !userDAO.list(UserDetails.class).isEmpty();
    }
    catch (Exception ex) {
      addGrowlMessage(ex);
      return null;
    }
  }

  public String getUsername()
  {
    return username;
  }

  public void setUsername(String aUsername)
  {
    this.username = aUsername;
  }


  public String getNewPassword()
  {
    return newPassword;
  }

  public void setNewPassword(String aNewPassword)
  {
    this.newPassword = aNewPassword;
  }

  public String getConfirmPassword()
  {
    return confirmPassword;
  }

  public void setConfirmPassword(String aConfirmPassword)
  {
    this.confirmPassword = aConfirmPassword;
  }

}
