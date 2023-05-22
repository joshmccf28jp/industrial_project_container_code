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
import com.energysys.connector.web.security.UserDetails;
import com.energysys.connector.web.security.UserRole;
import org.eclipse.jetty.util.security.Credential;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import java.util.List;
import java.util.logging.Logger;


/**
 * JSF Backing bean for manage users screen.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@ManagedBean(name = "UserDataBean")
@ViewScoped
public class UserDataBean extends AbstractGenericDAOBackingBean<UserDetails>
{

  private static final Logger LOG = Logger.getLogger(UserDataBean.class.getName());
  private static final Integer PASSWORD_MIN_CHARS = 8;

  private static final String EVENT_TYPE_SAVE = ":Save";

  private List<UserRole> theRoleList;

  /**
   * Default Constructor.
   */
  public UserDataBean()
  {
  }

  @Override
  protected Class getDataClass()
  {
    return UserDetails.class;
  }

  @Override
  protected Boolean preAddProcess(UserDetails aDataObject)
  {
    return true;
  }


  @Override
  protected Boolean preSaveProcess(UserDetails aDataObject)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();

    Boolean isAdminRolePresent = false;
    Boolean isNameDuplicate = false;

    for (UserDetails myCurrUser : getData())
    {
      if (myCurrUser.getRole().getRole().equals("ADMIN_ROLE"))
      {
        isAdminRolePresent = true;
      }
      if (!myCurrUser.getId().equals(aDataObject.getId()) && myCurrUser.getName().equals(aDataObject.getName()))
      {
        isNameDuplicate = true;
      }
    }

    if (!isAdminRolePresent)
    {
      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + EVENT_TYPE_SAVE, EventResult.Result.FAILED,
              "Must have at least one user with Admin role", "", myStartTime));
    }
    if (isNameDuplicate)
    {
      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + EVENT_TYPE_SAVE, EventResult.Result.FAILED,
              "Username must be unique", "", myStartTime));
    }
    // support existing CRYPT credentials, but use MD5 for newly created credentials
    if (!aDataObject.getPassword().startsWith("CRYPT:") && !aDataObject.getPassword().startsWith("MD5:"))
    {
      if (!aDataObject.getPassword().matches("^(?=(.*\\d){1}.*$)(?=.*[a-z]{1}.*$)(?=.*[A-Z]{1}.*$).{8,20}$"))
      {
        addGrowlMessage(new EventResult(getDataClass().getSimpleName() + EVENT_TYPE_SAVE, EventResult.Result.FAILED,
          "Password: Validation Error: Must be between 8 and 20 characters and include at least one number, "
            + "one uppercase and one lowercase letter",
          "", myStartTime));
      }
      else
      {
        aDataObject.setPassword(Credential.MD5.digest(aDataObject.getPassword()));
      }
    }
    return !FacesContext.getCurrentInstance().isValidationFailed();
  }

  @Override
  protected Boolean preDeleteProcess(UserDetails aDataObject)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    if (getData().size() == 1)
    {
      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":Delete", EventResult.Result.FAILED,
              "Must have at least one user", "", myStartTime));
      return false;
    }
    
    Boolean isAdminRolePresent = false;
    for (UserDetails myCurrUser : getData())
    {
      if (!myCurrUser.getId().equals(aDataObject.getId()) && myCurrUser.getRole().getRole().equals("ADMIN_ROLE"))
      {
        isAdminRolePresent = true;
      }
    }

    if (!isAdminRolePresent)
    {
      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":Delete", EventResult.Result.FAILED,
              "Must have at least one user with Admin role", "", myStartTime));
      return false;
    }
    
    return true;
  }

  /**
   * Get the list of roles.
   *
   * @return List of SelectItems for roles.
   */
  public List<UserRole> getRoles()
  {
    if (theRoleList == null)
    {
      try (GenericDAO genericDAO = new GenericDAO())
      {
        theRoleList = genericDAO.list(UserRole.class);
      }
    }
    return theRoleList;
  }

}
