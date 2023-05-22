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
package com.energysys.connector.util.spreadsheet;

import com.energysys.connector.util.spreadsheet.smartxls.SmartXLSSpreadsheet;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.schedulers.quartz.RemoteQueryExecution;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Class that creates a spreadsheet from a list of RemoteQueryExecution and provides an 
 * InputStream of the resulting xlsx file.
 *
 * @author EnergySys Limited
 */
public class RemoteQueryExecutionSpreadsheet
{

  private static final String SHEET_NAME = "Sheet1";
  private static final String DATETIME_FORMAT = "dd/MMM/yyyy HH:mm:ss";
  private static final String SPREADSHEET_DATETIME_FORMAT = "dd/mmm/yyyy HH:mm:ss";
  
  private static final Logger LOG = Log.getLogger(RemoteQueryExecutionSpreadsheet.class.getName());
  
  private byte[] theSpreadsheetBytes;
  private String[] theSpreadsheetHeaders = new String[]
  {
    "ConnectorName", "QueryName", "ExecutionDateTime", "Parameters", "Status", "Message",
  };

  /**
   * Constructor.
   *
   * @param someRemoteQueryExecutions The RemoteQueryExecutions
   * @param anAssetZoneId The EnergySys timezone
   * @param aNamedRangeName name to assign to the data cells Named Range
   * @throws ConnectorException
   */
  public RemoteQueryExecutionSpreadsheet(List<RemoteQueryExecution> someRemoteQueryExecutions, TimeZone anAssetZoneId,
      String aNamedRangeName)
      throws ConnectorException
  {
    try
    {
      SmartXLSSpreadsheet mySpreadsheet = new SmartXLSSpreadsheet();
      //Reuse sheet if already existent
      try
      {
        if (!mySpreadsheet.getSheets().contains(SHEET_NAME))
        {
          mySpreadsheet.createSheet(SHEET_NAME);
        }
      }
      catch (SpreadsheetException myEx)
      {
        LOG.info("Was not possible to create sheet with name: " + SHEET_NAME + " due to " + myEx.getMessage());
      }
      createSpreadsheetHeaders(mySpreadsheet, SHEET_NAME, 0);

      int myCurrRow = 1;
      for (RemoteQueryExecution myRemoteQueryExecution : someRemoteQueryExecutions)
      {
        createSpreadsheetRow(mySpreadsheet, SHEET_NAME, myCurrRow++, myRemoteQueryExecution, anAssetZoneId);
      }

      mySpreadsheet.setNamedRange(aNamedRangeName, "Sheet1!A2:F" + myCurrRow);
      ByteArrayOutputStream myOut = new ByteArrayOutputStream();
      mySpreadsheet.writeXLSX(myOut);
      theSpreadsheetBytes = myOut.toByteArray();

    }
    catch (SpreadsheetException myEx)
    {
      String myMessage = "Error converting to spreadsheet output.";
      throw new ConnectorException(myMessage, myEx);
    }
  }

  private ISpreadsheet createSpreadsheetRow(ISpreadsheet aSpreadsheet, String aSheet, int aRow,
      RemoteQueryExecution aRemoteQueryExecutions, TimeZone anAssetTimeZone) throws ConnectorException
  {
    int myCurrCol = 0;
    try
    {
      aSpreadsheet.setText(aSheet, aRow, myCurrCol++, aRemoteQueryExecutions.getConnectorName());
      aSpreadsheet.setText(aSheet, aRow, myCurrCol++, aRemoteQueryExecutions.getQueryName());

      ZonedDateTime myZonedDateTime = ZonedDateTime.ofInstant(
          aRemoteQueryExecutions.getExecutionDateTime().toInstant(), anAssetTimeZone.toZoneId());

      LocalDateTime myDateValue = myZonedDateTime.toLocalDateTime();
      DateTimeFormatter myDateTimeFormatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT);
      aSpreadsheet.setText(aSheet, aRow, myCurrCol, myDateTimeFormatter.format(myDateValue));
      aSpreadsheet.setFormat(aSheet, aRow, myCurrCol++, SPREADSHEET_DATETIME_FORMAT);

      aSpreadsheet.setText(aSheet, aRow, myCurrCol++, aRemoteQueryExecutions.getParameters());
      aSpreadsheet.setText(aSheet, aRow, myCurrCol++, aRemoteQueryExecutions.getStatus().getName());
      aSpreadsheet.setText(aSheet, aRow, myCurrCol++, aRemoteQueryExecutions.getMessage());
    }
    catch (SpreadsheetException myEx)
    {
      throw new ConnectorException(aSheet, myEx);
    }
    return aSpreadsheet;
  }

  private ISpreadsheet createSpreadsheetHeaders(ISpreadsheet aSpreadsheet, String aSheet, int aRow)
      throws ConnectorException
  {
    int myCurrCol = 0;
    try
    {
      for (int i = 0; i < theSpreadsheetHeaders.length; i++)
      {
        String theHeader = theSpreadsheetHeaders[i];
        aSpreadsheet.setText(aSheet, aRow, i, theHeader);
      }
    }
    catch (SpreadsheetException myEx)
    {
      throw new ConnectorException(aSheet, myEx);
    }
    return aSpreadsheet;
  }

  /**
   * Gets an InputStream of the xlsx file.
   *
   * @return the input stream
   * @throws ConnectorException
   */
  public InputStream getInputStream() throws ConnectorException
  {
    ByteArrayInputStream myIn = new ByteArrayInputStream(theSpreadsheetBytes);
    return myIn;
  }

  /**
   * Gets the size of the xlsx file contents.
   *
   * @return the size
   */
  public Integer getSize()
  {
    return theSpreadsheetBytes.length;
  }
}
