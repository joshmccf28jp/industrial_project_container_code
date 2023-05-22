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
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.web.IIdentifiable;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;

import javax.faces.context.FacesContext;
import java.util.List;
import java.util.logging.Level;


/**
 * This class is an abstract jsf backing bean with basic add, remove, edit functionality for siaplaying a data table of
 * objects.
 *
 * @author EnergySys Limited
 * @version $Revision$
 * @param <T> the type of object the bean is displaying
 */
public abstract class AbstractDataListBackingBean<T extends IIdentifiable> extends AbstractEventLoggingBackingBean
{

  private T theSelectedRow;
  private Boolean isInEditMode = false;

  private List<T> theDataList;

  /**
   * Default constructor.
   */
  public AbstractDataListBackingBean()
  {
    loadData();
  }

  /**
   * Returns a Class object of the sub classes data.
   *
   * @return the class
   */
  protected abstract Class getDataClass();

  /**
   * Loads the data into theDataList.
   */
  protected abstract void loadData();

  /**
   * Saves changes to an already existing data object.
   *
   * @param aDataObject the data object
   * @return If successful
   * @throws ConnectorException on error
   */
  protected abstract Boolean saveUpdate(T aDataObject) throws ConnectorException;

  /**
   * Saves a new data object.
   *
   * @param aDataObject the data object
   * @return If successful
   * @throws ConnectorException on error
   */
  protected abstract Boolean saveCreate(T aDataObject) throws ConnectorException;

  /**
   * Deletes a data object.
   *
   * @param aDataObject the data object
   * @return If successful
   * @throws ConnectorException on error
   */
  protected abstract Boolean delete(T aDataObject) throws ConnectorException;

  /**
   * Called just before a save operation is made (either update or create). Should perform any tasks that are required
   * prior to saving to a data source such as default data population and extra validation not performed by the client
   * ui. Returns true if the object is ok to save of false if it should be aborted.
   *
   * @param aDataObject the data object
   * @return if ok to save
   */
  protected Boolean preSaveProcess(T aDataObject)
  {
    return true;
  }

  /**
   * Called after a update operation is performed.
   *
   * @param aDataObject the data object
   * @throws ConnectorException on error
   */
  protected void postSaveUpdateProcess(T aDataObject) throws ConnectorException
  {
  }

  /**
   * Called after a create operation is performed.
   *
   * @param aDataObject the data object
   * @throws ConnectorException on error
   */
  protected void postSaveCreateProcess(T aDataObject) throws ConnectorException
  {
  }

  /**
   * Called before a delete operation is performed. Should perform any tasks that are required prior to deleting from
   * the data source such as extra validation not performed by the client ui. Returns true if the object is ok to delete
   * of false if it should be aborted.
   *
   * @param aDataObject the data object
   * @return if ok to delete
   */
  protected Boolean preDeleteProcess(T aDataObject)
  {
    return true;
  }

  /**
   * Called after a delete operation is performed.
   *
   * @param aDataObject the data object
   * @throws ConnectorException on error
   */
  protected void postDeleteProcess(T aDataObject) throws ConnectorException
  {
  }

  /**
   * Called just before an add operation is made (when a new dataobject is added to the data list). Should perform any
   * tasks that are required prior to adding a data object such as default data population. Returns true if the object
   * is ok to be added of false if it should be aborted.
   *
   * @param aDataObject the data object
   * @return if ok to add
   */
  protected Boolean preAddProcess(T aDataObject)
  {
    return true;
  }

  /**
   * Called after the add operation is performed.
   *
   * @param aDataObject the data object
   * @throws ConnectorException on error
   */
  protected void postAddProcess(T aDataObject) throws ConnectorException
  {
  }

  /**
   * UI event fired on add row. Adds new data object to the list.
   */
  @SuppressWarnings(
          {
            "checkstyle:illegalcatch", "UseSpecificCatch"
          })
  public void onAddRow()
  {
    try
    {
      T myObject = (T) getDataClass().newInstance();
      if (preAddProcess(myObject))
      {
        myObject.setId(-1);
        postAddProcess(myObject);
        theDataList.add(myObject);
        setSelectedRow(myObject);
        isInEditMode = true;
      }
    }
    catch (Exception ex)
    {
      addGrowlMessage(ex);
    }

    LOG.log(Level.FINE, getDataClass().getSimpleName() + ".onAddRow()");
  }

  /**
   * UI event fired on row delete. Removes the given object.
   *
   * @param anObject to remove
   */
  @SuppressWarnings(
          {
            "checkstyle:illegalcatch", "UseSpecificCatch"
          })
  public void onRowDelete(T anObject)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    if (preDeleteProcess(anObject))
    {
      try
      {
        if (delete(anObject))
        {
          postDeleteProcess(anObject);
          theDataList.remove(anObject);
        }
      }
      catch (Exception ex)
      {
        addGrowlMessage(ex);
      }
    }
    if (!FacesContext.getCurrentInstance().isValidationFailed())
    {
      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":Delete",
              EventResult.Result.SUCCESS,
              "Item Deleted", "", myStartTime));
    }
  }

  /**
   * UI event fired when row editing is initiated.
   *
   * @param anEvent row edit event
   */
  public void onRowEditInit(RowEditEvent anEvent)
  {
    setSelectedRow((T) anEvent.getObject());
    isInEditMode = true;

    LOG.log(Level.FINE, getDataClass().getSimpleName() + ".onRowEditInit() : " + theSelectedRow.getName());
  }

  /**
   * UI event fired when row editing has been confirmed. Saves or updates the rows data.
   *
   * @param anEvent row edit event
   */
  @SuppressWarnings(
          {
            "checkstyle:illegalcatch", "UseSpecificCatch"
          })
  public void onRowEdit(RowEditEvent anEvent)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    T myDatObject = (T) anEvent.getObject();
    if (preSaveProcess(myDatObject))
    {
      try
      {
        if (myDatObject.getId() == -1)
        {
          if (saveCreate(myDatObject))
          {
            postSaveCreateProcess(myDatObject);
          }
        }
        else
        {
          if (saveUpdate(myDatObject))
          {
            postSaveUpdateProcess(myDatObject);
          }
        }
      }
      catch (Exception ex)
      {
        addGrowlMessage(ex);
      }
    }
    if (!FacesContext.getCurrentInstance().isValidationFailed())
    {
      isInEditMode = false;
      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":Saved",
              EventResult.Result.SUCCESS,
              "Item Saved", "", myStartTime));

      LOG.log(Level.FINE, getDataClass().getSimpleName() + ".onRowEdit() : " + myDatObject.getName());
    }

  }

  /**
   * UI event fired when row editing is cancelled.
   *
   * @param anEvent ui event
   */
  public void onRowCancel(RowEditEvent anEvent)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    isInEditMode = false;
    // If this is a new record then we need to remove it from the data list or the row stays on screen
    T myDatObject = (T) anEvent.getObject();
    if (myDatObject.getId() == -1)
    {
      theDataList.remove(myDatObject);
    }
    addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":Edit Cancelled",
            EventResult.Result.SUCCESS,
            "Edit Cancelled", "", myStartTime));

    LOG.log(Level.FINE, getDataClass().getSimpleName() + ".onRowCancel()");
  }

  /**
   * Fires on row selection.
   *
   * @param anEvent the event
   */
  public void onRowSelect(SelectEvent anEvent)
  {
    LOG.log(Level.FINE, getDataClass().getSimpleName() + ".onRowSelect() : " + getSelectedRow().getName());
  }

  /**
   * Fires on row un-selection.
   *
   * @param anEvent the event
   */
  public void onRowUnselect(UnselectEvent anEvent)
  {
    LOG.log(Level.FINE, getDataClass().getSimpleName() + ".onRowUnselect()");
  }

  /**
   * Returns the data list.
   *
   * @return list
   */
  public List<T> getData()
  {
    return theDataList;
  }

  /**
   * Sets the data list.
   *
   * @param aDataList the data list
   */
  protected void setData(List<T> aDataList)
  {
    this.theDataList = aDataList;
  }

  /**
   * Gets the data object currently selected in the UI.
   *
   * @return the data object
   */
  public T getSelectedRow()
  {
    return theSelectedRow;
  }

  /**
   * Sets the Data Object currently selected in the UI.
   *
   * @param aSelectedRow selected data object
   */
  public void setSelectedRow(T aSelectedRow)
  {
    if (!isInEditMode)
    {
      this.theSelectedRow = aSelectedRow;
      LOG.log(Level.FINE, getDataClass().getSimpleName() + ".setSelectedRow() = " + theSelectedRow);
    }
    else
    {
      LOG.log(Level.FINE, getDataClass().getSimpleName() + ".setSelectedRow() = " + theSelectedRow);
    }
  }

  public Boolean isRowSelected()
  {
    return theSelectedRow != null;
  }

  public Boolean isInEditMode()
  {
    return isInEditMode;
  }

}
