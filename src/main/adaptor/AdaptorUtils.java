/*
 * Copyright 2018 EnergySys Limited. All Rights Reserved.
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
package com.energysys.adaptor;

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.config.ConnectorConfig;
import com.energysys.connector.connectors.dataconnector.odata.TagList;
import com.energysys.connector.exception.ConnectorException;
import com.energysys.connector.util.spreadsheet.ISpreadsheet;
import com.energysys.connector.util.spreadsheet.SpreadsheetException;
import com.energysys.connector.util.spreadsheet.smartxls.SmartXLSSpreadsheet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Utils class for common procedures that occur across the different Adaptor classes. Such as Spreadsheet creation and
 * conversion of dates for Energysys.
 *
 * @author EnergySys Limited
 */
public final class AdaptorUtils
{
  /**
   * Default datasheet name used when creating spreadsheets
   */
  private static final String DATA_SHEET_NAME = "Data";

  private static final String NEW_LINE = "\n";

  private AdaptorUtils()
  {
  }
  
  /**
   * Returns a displayable message indicating which tags did not have any results in the query results.
   *
   * @param aTagList the original tag list
   * @param someReturnedValues the results
   * @return the message
   */
  public static String getMissingTagResulstWarningMessages(TagList aTagList,
          Map<String, List<Map<String, String>>> someReturnedValues)
  {
    StringBuilder myValidationMessage = new StringBuilder();

    Iterator<String> myIterator = aTagList.getTagNames().iterator();
    while (myIterator.hasNext())
    {
      String myTagName = myIterator.next();
      if (someReturnedValues.get(myTagName) == null || someReturnedValues.get(myTagName).isEmpty())
      {
        myValidationMessage.append("The tag <" + myTagName + "> in the tag list <"
                + aTagList.getName() + "> did not have any readings.\n");
      }
    }

    return myValidationMessage.toString();
  }

  /**
   * Returns a displayable message detailing the query results.
   *
   * @param someReturnedValues the Query Results
   * @return message
   */
  public static String generateRetrievedDataMessage(Map<String, List<Map<String, String>>> someReturnedValues)
  {
    StringBuilder myMessage = new StringBuilder();
    if (someReturnedValues != null)
    {
      Iterator<String> myTagsIterator = someReturnedValues.keySet().iterator();
      while (myTagsIterator.hasNext())
      {
        String myTagName = myTagsIterator.next();
        myMessage.append("Tag name: ").append(myTagName).append(NEW_LINE).append("Raw Readings:").append(NEW_LINE);
        List<Map<String, String>> myAttributeSets = someReturnedValues.get(myTagName);
        for (Map<String, String> myAtts : myAttributeSets)
        {
          myMessage.append(myAtts.toString()).append(NEW_LINE);
        }
        myMessage.append(NEW_LINE);
      }
    }
    return myMessage.toString();
  }

  /**
   * Converts am ISO formatted Local Date Time to another timezone.
   *
   * @param aTimestamp the ISO formateed date time
   * @param aTimeZone the timezone to convert to
   * @return ISO LOCAL DATE TIME result
   */
  public static String convertToLocalDateTime(String aTimestamp, TimeZone aTimeZone)
  {
    ZonedDateTime myPiZonedDateTime = ZonedDateTime.parse(aTimestamp, DateTimeFormatter.ISO_DATE_TIME);
    Instant myTimestampInstant = myPiZonedDateTime.toInstant();
    ZonedDateTime myAssetTime = ZonedDateTime.ofInstant(myTimestampInstant, aTimeZone.toZoneId());

    return myAssetTime.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
  }

  /**
   * Creates a spreadsheet based on the results of a tag list query.
   *
   * @param someReturnedValues the results of the tag list query
   * @param someHeaders the headers of each column in the data
   * @param someDataValueNames the names of the data elements in the returned values to output
   * @param aDataFileBaseName the base file name for the spreadsheet
   * @param aConnectorConfig the Connector config
   * @return the spreadsheet file name
   * @throws ConnectorException
   */
  public static String createDataSpreadsheet(Map<String, List<Map<String, String>>> someReturnedValues,
                                             List<String> someHeaders, List<String> someDataValueNames,
                                             String aDataFileBaseName,
                                             ConnectorConfig aConnectorConfig)
          throws ConnectorException
  {
    String myCreatedFileName = null;

    try
    {
      int myCurrRow = 1;
      int myCurrCol = 1;
      //Prior to file creation, ensure any old files are removed
      cleanUpFiles(aConnectorConfig.getDataFileDirectory(), aConnectorConfig.getDataFileRetentionPeriod());

      ISpreadsheet mySpreadsheet = new SmartXLSSpreadsheet();
      mySpreadsheet.createSheet(DATA_SHEET_NAME);
      //Write header
      mySpreadsheet = createSpreadsheetRow(mySpreadsheet, DATA_SHEET_NAME, myCurrRow, myCurrCol, someHeaders);
      //Write values
      writeValues(mySpreadsheet, DATA_SHEET_NAME, myCurrRow + 1,
              myCurrCol, someReturnedValues, someDataValueNames);
      //Write spreadsheet to file system
      myCreatedFileName =
              AdaptorUtils.writeSpreadsheet(mySpreadsheet, aDataFileBaseName, aConnectorConfig.getDataFileDirectory());
    }
    catch (SpreadsheetException | IOException myEx)
    {
      String myMessage = "Error converting writing values to spreadsheet output.";
      throw new ConnectorException(myMessage, myEx);
    }

    return myCreatedFileName;
  }

  private static String writeSpreadsheet(ISpreadsheet aSpreadsheet, String aDataFileNameBase, String aDataDir) throws
          IOException, SpreadsheetException
  {
    StringBuilder myFilePath = new StringBuilder(aDataDir);
    SimpleDateFormat mySDF = new SimpleDateFormat("yyyyMMddHHmmss");
    myFilePath.append(aDataFileNameBase);
    myFilePath.append(mySDF.format(CurrentDateTime.getCurrentDate()));
    myFilePath.append(".").append(SmartXLSSpreadsheet.FORMAT.XLSX);

    File myOutputFile = new File(myFilePath.toString());
    myOutputFile.getParentFile().mkdirs();

    try (FileOutputStream myFileOut = new FileOutputStream(myOutputFile))
    {
      aSpreadsheet.write(myFileOut);
    }

    return myFilePath.toString();
  }

  private static ISpreadsheet writeValues(ISpreadsheet aSpreadsheet, String aSheet, int anInitRow, int anInitCol,
          Map<String, List<Map<String, String>>> someReturnedValues, List<String> someDataValueNames)
          throws ConnectorException
  {
    Iterator<String> myTagIterator = someReturnedValues.keySet().iterator();
    int myInitRow = anInitRow;

    while (myTagIterator.hasNext())
    {
      String myTagName = myTagIterator.next();
      List<Map<String, String>> myAttributeSets = someReturnedValues.get(myTagName);
      for (Map<String, String> myAttributeSet : myAttributeSets)
      {
        List<String> mySpreadSheetRowValues = new ArrayList();
        for (String myDataValueName : someDataValueNames)
        {
          mySpreadSheetRowValues.add(myAttributeSet.get(myDataValueName));
        }
        createSpreadsheetRow(aSpreadsheet, aSheet, myInitRow++, anInitCol, mySpreadSheetRowValues);
      }
    }
    return aSpreadsheet;
  }

  private static ISpreadsheet createSpreadsheetRow(ISpreadsheet aSpreadsheet, String aSheet, int anInitRow,
          int anInitCol,
          List<String> someTextValues) throws ConnectorException
  {
    for (String myTextString : someTextValues)
    {
      try
      {
        aSpreadsheet.setText(aSheet, anInitRow, anInitCol++, myTextString);
      }
      catch (SpreadsheetException myEx)
      {
        String myMessage = "Failed to add row when generating spreadsheet output.";
        throw new ConnectorException(aSheet, myEx);
      }
    }
    return aSpreadsheet;
  }

  /**
   * Delete files older than the specified period.
   */
  private static void cleanUpFiles(String aDataDir, Integer fileRetentionPeriod)
  {
    File myFileDir = new File(aDataDir);
    if (myFileDir.isDirectory())
    {
      for (File myFile : myFileDir.listFiles())
      {
        long myLastMod = CurrentDateTime.getCurrentTimeInMillis() - myFile.lastModified();
        if (myLastMod > fileRetentionPeriod * 24 * 60 * 60 * 1000L)
        {
          myFile.delete();
        }
      }
    }
  }
}
