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
import com.energysys.connector.JobConfiguration;
import com.energysys.connector.database.GenericDAO;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.schedulers.quartz.SchedulerManager;
import com.energysys.connector.web.DatabaseExport;
import com.energysys.connector.web.TableDetails;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.Unmarshaller;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 * Abstract backing bean for Import / Export screens. Extending classes need to provide the list of TableDetail classes
 * describing which the Entities can be import/exported.
 *
 * @author EnergySys Limited
 */
public abstract class AbstractImportExportBean extends AbstractDataListBackingBean<TableDetails>
{
  /**
   * Runs checks for pre truncation of objects.
   * @param aDataObject
   * @return if ok to truncate
   */
  protected abstract Boolean preTruncateTable(TableDetails aDataObject);

  @Override
  protected Class getDataClass()
  {
    return TableDetails.class;
  }

  @Override
  protected void loadData()
  {
    ArrayList<TableDetails> myTableList = getTableList();
    setData(myTableList);
    try (GenericDAO myDAO = new GenericDAO())
    {
      for (TableDetails myTableDetails : myTableList)
      {
        myTableDetails.setRowCount(myDAO.countAll(myTableDetails.getType()));
      }
    }
  }

  /**
   * Provides the list of TableDetails that the import bean provides for import/export.
   *
   * @return the list of table details
   */
  protected abstract ArrayList<TableDetails> getTableList();

  /**
   * Imports from an uploaded file.
   *
   * @param aFileUploadEvent the file upload event
   */
  @SuppressWarnings(
          {
            "checkstyle:illegalcatch", "UseSpecificCatch"
          })
  public void importFromFile(FileUploadEvent aFileUploadEvent)
  {
    Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    UploadedFile myFile = aFileUploadEvent.getFile();
    String myFileName = myFile.getFileName();
    try
    {
      try (GenericDAO myDAO = new GenericDAO())
      {
        List<Class> myDataClasses = new ArrayList<>();
        myDataClasses.add(DatabaseExport.class);
        for (TableDetails myTableDetails : getData())
        {
          myDataClasses.add(myTableDetails.getType());
        }
        DatabaseExport myExport = convertFromXML(myFile.getInputStream(), myDataClasses.toArray(new Class[0]));
        Class myCurrClass = null;
        for (Object myObject : myExport.getData())
        {
          // Truncate table if new class
          if (myCurrClass == null || !myCurrClass.equals(myObject.getClass()))
          {
            myDAO.deleteAll(myObject.getClass());
          }
          // If this is a JobConfiuration then override the isActive and set to false.
          if (myObject instanceof JobConfiguration)
          {
            ((JobConfiguration) myObject).setIsActive(false);
          }
          myDAO.merge(myObject);
          myCurrClass = myObject.getClass();
        }
      }
      loadData();
      SchedulerManager myScheduler = new SchedulerManager();
      myScheduler.refreshScheduledJobs();
      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":Import",
              EventResult.Result.SUCCESS, "Data Imported", "", myStartTime));
    }
    catch (Exception ex)
    {
      addGrowlMessage(ex);
    }
  }

  /**
   * Exports all tables.
   *
   * @return a download stream
   */
  @SuppressWarnings(
          {
            "checkstyle:illegalcatch", "UseSpecificCatch"
          })
  public StreamedContent exportAll()
  {
    try (GenericDAO myDAO = new GenericDAO())
    {
      DatabaseExport myExport = new DatabaseExport("ConnectorExport");
      List<Class> myDataClasses = new ArrayList<>();
      myDataClasses.add(DatabaseExport.class);
      for (TableDetails myTableDetails : getData())
      {
        myDataClasses.add(myTableDetails.getType());
        myExport.getData().addAll(myDAO.list(myTableDetails.getType()));
      }
      return convertToXML(myExport, myDataClasses.toArray(new Class[0]));
    }
    catch (Exception ex)
    {
      addGrowlMessage(ex);
      return null;
    }
  }

  /**
   * Exports a single table.
   *
   * @return download stream
   * @throws ConnectorException
   */
  @SuppressWarnings(
          {
            "checkstyle:illegalcatch", "UseSpecificCatch"
          })
  public StreamedContent getTableExport() throws ConnectorException
  {
    try (GenericDAO myDAO = new GenericDAO())
    {
      DatabaseExport myExport = new DatabaseExport(getSelectedRow().getType().getSimpleName());
      myExport.getData().addAll(myDAO.list(getSelectedRow().getType()));
      return convertToXML(myExport, DatabaseExport.class, getSelectedRow().getType());
    }
    catch (Exception ex)
    {
      addGrowlMessage(ex);
      return null;
    }
  }

  /**
   * Truncates a table.
   *
   * @param aDataObject the table to truncate
   * @throws ConnectorException
   */
  public void truncateTable(TableDetails aDataObject) throws ConnectorException
  {
    if (preTruncateTable(aDataObject))
    {
      Long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
      try (GenericDAO myDAO = new GenericDAO())
      {
        myDAO.deleteAll(aDataObject.getType());
      }
      loadData();
      addGrowlMessage(new EventResult(getDataClass().getSimpleName() + ":Truncate",
              EventResult.Result.SUCCESS, "Data Truncated", "", myStartTime));
    }
  }

  @Override
  protected Boolean saveUpdate(TableDetails aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Boolean saveCreate(TableDetails aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Boolean delete(TableDetails aDataObject) throws ConnectorException
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Converts an xml DatabaseExport to a proper object given an input stream.
   *
   * @param aDataStream the input stream
   * @param someContextClasses the list of context classes for the jaxb
   * @return a DatabaseExport object
   * @throws JAXBException
   */
  protected DatabaseExport convertFromXML(InputStream aDataStream, Class... someContextClasses) throws JAXBException
  {
    JAXBContext myContext = JAXBContext.newInstance(someContextClasses);
    final Unmarshaller myUnmarshaller = myContext.createUnmarshaller();
    return (DatabaseExport) myUnmarshaller.unmarshal(aDataStream);
  }

  /**
   * Converts a DatabaseExport to an xml file and streams contents for download.
   *
   * @param anExport the database export
   * @param someContextClasses list of context classes for jaxb
   * @return stream for download
   * @throws PropertyException
   * @throws JAXBException
   */
  protected StreamedContent convertToXML(DatabaseExport anExport, Class... someContextClasses) throws PropertyException,
          JAXBException
  {
    JAXBContext myContext = JAXBContext.newInstance(someContextClasses);
    final Marshaller myMarshaller = myContext.createMarshaller();
    myMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    ByteArrayOutputStream myOut = new ByteArrayOutputStream();
    myMarshaller.marshal(anExport, myOut);
    System.out.println(new String(myOut.toByteArray()));
    ByteArrayInputStream myIn = new ByteArrayInputStream(myOut.toByteArray());

    return DefaultStreamedContent.builder()
            .name(anExport.getExportName() + ".xml")
            .contentType("text/text-file")
            .stream(() -> myIn)
            .build();
  }

}
