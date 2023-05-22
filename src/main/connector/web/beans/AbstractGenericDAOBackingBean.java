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

import com.energysys.connector.database.GenericDAO;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.web.IIdentifiable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract JSF backing bean that contains most of the functionality to perform basic add, edit, delete functionality.
 *
 * @author EnergySys Limited
 * @version $Revision$
 * @param <T> Type of row items
 */
public abstract class AbstractGenericDAOBackingBean<T extends IIdentifiable> extends AbstractDataListBackingBean<T>
{
  private static final String NAME_ALIAS = "theName";

  private static final String USERNAME_ALIAS = "username";

  @Override
  @SuppressWarnings(
  {
    "checkstyle:illegalcatch", "UseSpecificCatch"
  })
  protected Boolean delete(T aDataObject) throws ConnectorException
  {
    try (GenericDAO myGenericDAO = new GenericDAO())
    {
      myGenericDAO.delete(aDataObject);
      return true;
    }
    catch (Exception ex)
    {
      addGrowlMessage(ex);
      return false;
    }
  }

  @Override
  @SuppressWarnings(
  {
    "checkstyle:illegalcatch", "UseSpecificCatch"
  })
  protected Boolean saveCreate(T aDataObject) throws ConnectorException
  {
    try (GenericDAO myGenericDAO = new GenericDAO())
    {
      Integer myId = (Integer) myGenericDAO.save(aDataObject);
      aDataObject.setId(myId);
      return true;
    }
    catch (Exception ex)
    {
      addGrowlMessage(ex);
      return false;
    }
  }

  @Override
  @SuppressWarnings(
  {
    "checkstyle:illegalcatch", "UseSpecificCatch"
  })
  protected Boolean saveUpdate(T aDataObject) throws ConnectorException
  {
    try (GenericDAO myGenericDAO = new GenericDAO())
    {
      myGenericDAO.update(aDataObject);
      return true;
    }
    catch (Exception ex)
    {
      addGrowlMessage(ex);
      return false;
    }
  }

  @Override
  protected void loadData()
  {
    try (GenericDAO genericDAO = new GenericDAO())
    {
      List<T> myDataList;
      // check whether the data class has a name or username field
      String orderByCol = "";
      Field[] myFields = getDataClass().getDeclaredFields();
      for (Field myField : myFields)
      {
        if (myField.getName().equals(NAME_ALIAS))
        {
          orderByCol = NAME_ALIAS;
          break;
        }
        else if (myField.getName().equals(USERNAME_ALIAS))
        {
          orderByCol = USERNAME_ALIAS;
          break;
        }
      }
      // if name or username is a valid field, use it for ordering, otherwise use standard ordering
      if (orderByCol.equals(NAME_ALIAS))
      {
        myDataList = genericDAO.list(getDataClass(), NAME_ALIAS, GenericDAO.ORDERBY.ASC);
      }
      else if (orderByCol.equals(USERNAME_ALIAS))
      {
        myDataList = genericDAO.list(getDataClass(), USERNAME_ALIAS, GenericDAO.ORDERBY.ASC);
      }
      else
      {
        myDataList = genericDAO.list(getDataClass());
      }
      if (myDataList.isEmpty())
      {
        myDataList = new ArrayList<>();
      }
      setData(myDataList);
    }
  }

 
}
