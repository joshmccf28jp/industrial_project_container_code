/*
 * Copyright 2013 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.util.spreadsheet.smartxls;

import com.energysys.calendar.CurrentDateTime;
import com.energysys.connector.util.spreadsheet.ISpreadsheet;
import com.energysys.connector.util.spreadsheet.Range;
import com.energysys.connector.util.spreadsheet.SpreadsheetException;
import com.energysys.connector.util.spreadsheet.ISpreadsheet.CELLTYPE;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Smart XLS CSV helper.
 * @author EnergySys Limited
 * @version $Revision$
 */
public final class SmartXLSCSVHelper
{
  private static final String COLON = " : ";
  private static final String ENCODED_COMMA = "\u060C";
  private static final String ENCODED_QUOTE = "\u201C";
  private static final String HASH = "#";
  private static final String QUOTE = "\"";
  private static final String COMMA = ",";
  private static final String NL = "\n";
  private static final String HASH_SPACE = HASH + " ";
  private static final Logger LOG = Log.getLogger(SmartXLSCSVHelper.class.getName());
  private static final String MS = "ms";
  private static final String SHEET_NAME = "Sheet Name";
  private static final Pattern CSV_LINE_PATTERN = Pattern.compile("^\\d+,\\d+,.*$");
  private static final Pattern CSV_SHEET_PATTERN = Pattern.compile("^\\# Sheet (\\d+) \\: (.*)$");
  private static final Pattern CSV_NAME_PATTERN = Pattern.compile("^\\# Name (\\w+) \\: (.*)$");
  private static final String ENCODING = "UTF-8";

  /**
   * Private constructor.
   */
  private SmartXLSCSVHelper()
  {
    super();
  }
  
  /**
   * Write CSV header to output stream.
   * @param aSpreadsheet the spreadsheet being written to CSV.
   * @param aStream an output stream to write to
   * @throws SpreadsheetException on error
   */  
  public static void writeCSVHeader(ISpreadsheet aSpreadsheet, OutputStream aStream) throws SpreadsheetException
  {
    write(aStream, HASH, true, 0);
    write(aStream, HASH_SPACE + "Sheets", true, 0);
    for (String mySheet : aSpreadsheet.getSheets())
    {
      write(aStream, HASH_SPACE + "Sheet " + aSpreadsheet.getSheetNum(mySheet) + COLON + mySheet, true, 0);
    }
    write(aStream, HASH, true, 0);
    write(aStream, HASH_SPACE + "Named Ranges", true, 0);
    for (String myNamedRange : aSpreadsheet.getNamedRanges())
    {
      write(aStream, HASH_SPACE + "Name " + myNamedRange + COLON + aSpreadsheet.getNamedRange(myNamedRange), true, 0);
    }
    write(aStream, HASH, true, 0);
  }  
  
  /**
   * Write spreadsheet to output stream in CSV format.
   * @param aSpreadsheet the spreadsheet being written to CSV.
   * @param aStream an output stream to write to
   * @param aRef the sheet and cell reference to write to the stream.
   * @return long time in milliseconds it took to write the spreadsheet range.
   * @throws SpreadsheetException on error
   */    
  public static long writeCSV(ISpreadsheet aSpreadsheet, OutputStream aStream, String aRef)
    throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    Range myRange = aSpreadsheet.getRange(aRef, true);
    try
    {
      write(aStream, HASH, true, 0);
      write(aStream, HASH_SPACE + "Sheet Number" + COLON + myRange.getSheet(), true, 0);
      write(aStream, HASH_SPACE + SHEET_NAME + COLON + aSpreadsheet.getSheetName(myRange.getSheet()), true, 0);
      write(aStream, HASH_SPACE + "Cell reference" + COLON + aRef, true, 0);
      write(aStream, HASH, true, 0);
      write(aStream, "# row,column,ref,text/number/formula,formatted-text,formula,type,format", true, 0);

      for (int myRow = myRange.getStartRow(); myRow <= myRange.getEndRow(); myRow++)
      {
        for (int myCol = myRange.getStartCol(); myCol <= myRange.getEndCol(); myCol++)
        {
          StringBuilder myLine = new StringBuilder();
          myLine.append(myRow).append(COMMA).append(myCol).append(COMMA);
          myLine.append(encodeCSV(aSpreadsheet.getCellRef(myRange.getSheet(), myRow, myCol))).append(COMMA);
          writeCSVValue(aSpreadsheet, myRange.getSheet(), myRow, myCol, myLine);
          myLine.append(encodeCSV(aSpreadsheet.getFormattedText(myRange.getSheet(), myRow, myCol))).append(COMMA);
          myLine.append(encodeCSV(aSpreadsheet.getFormula(myRange.getSheet(), myRow, myCol))).append(COMMA);
          myLine.append(encodeCSV(aSpreadsheet.getType(myRange.getSheet(), myRow, myCol).toString())).append(COMMA);
          myLine.append(encodeCSV(aSpreadsheet.getFormat(myRange.getSheet(), myRow, myCol)));
          write(aStream, myLine.toString(), true, 0);
        }
      }
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to write spreadsheet range " + aRef + " to CSV";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }

    long myElapsed = CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
    String myMessage = "Write spreadsheet range to CSV stream in " + myElapsed + MS;
    LOG.info(myMessage);
    return myElapsed;
  }
  
  private static void writeCSVValue(ISpreadsheet aSpreadsheet, int aSheet, int aRow, int aCol, StringBuilder aLine)
    throws SpreadsheetException, Exception
  {
    CELLTYPE myType = aSpreadsheet.getType(aSheet, aRow, aCol);

    if (myType.equals(CELLTYPE.Number)
      || myType.equals(CELLTYPE.Logical)
      || myType.equals(CELLTYPE.Error))
    {
      aLine.append(aSpreadsheet.getNumber(aSheet, aRow, aCol)).append(COMMA);
    }
    else if (myType.equals(CELLTYPE.Text))
    {
      aLine.append(encodeCSV(aSpreadsheet.getText(aSheet, aRow, aCol))).append(COMMA);
    }
    else
    {
      aLine.append(COMMA);
    }
  }  
  
  private static String encodeCSV(String someText)
  {
    StringBuilder myBuilder = new StringBuilder();
    if (someText != null && !someText.isEmpty())
    {
      myBuilder.append(QUOTE);
      // replace embedded double-quotes with encoded double-quotes as per
      // http://en.wikipedia.org/wiki/Comma-separated_values 
      // encode embedded commas with a coded comma
      myBuilder.append(someText.replace(QUOTE, ENCODED_QUOTE).replace(COMMA, ENCODED_COMMA));
      myBuilder.append(QUOTE);
    }
    return myBuilder.toString();
  }  
  
  private static void write(OutputStream aStream, String aLine, boolean isPrettyPrint, int anIndent)
    throws SpreadsheetException
  {
    try
    {
      if (isPrettyPrint)
      {
        for (int myCount = 0; myCount < anIndent; myCount++)
        {
          aStream.write(" ".getBytes(ENCODING));
        }
      }
      aStream.write(aLine.getBytes(ENCODING));
      if (isPrettyPrint)
      {
        aStream.write(NL.getBytes(ENCODING));
      }
    }
    catch (IOException myEx)
    {
      String myMessage = "Unable to write line to output stream : " + aLine;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }  

  /**
   * Read CSV data from the stream into the spreadsheet.
   * @param aSpreadsheet the spreadsheet being read from CSV.
   * @param aStream an input stream being read from
   * @return long time in milliseconds it took to read the spreadsheet.
   * @throws SpreadsheetException on error
   */      
  public static long readCSV(ISpreadsheet aSpreadsheet, InputStream aStream) throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      BufferedReader myReader = new BufferedReader(new InputStreamReader(aStream, ENCODING));

      String myLine;
      StringBuilder myLineToProcess = new StringBuilder();

      while ((myLine = myReader.readLine()) != null)
      {
        if (myLine.trim().startsWith(HASH))
        {
          // the first character is a hash (comment) so just process the line
          readCSV(aSpreadsheet, myLine);
        }
        else if (CSV_LINE_PATTERN.matcher(myLine).find())
        {
          // it matches, so it's the start of what we want but the next line may be a continuation. Process what we
          // have so far, and start the next line
          readCSV(aSpreadsheet, myLineToProcess.toString());
          myLineToProcess.setLength(0);
          myLineToProcess.append(myLine);
        }
        else
        {
          // it doesn't match, therefore add the current line to what we've got so far
          myLineToProcess.append(myLine);
        }
      }
      readCSV(aSpreadsheet, myLineToProcess.toString());
    }
    catch (IOException myEx)
    {
      String myMessage = "Unable to read spreadsheet from CVS";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
  }
  
  private static void readCSV(ISpreadsheet aSpreadsheet, String aLine) throws SpreadsheetException
  {
    if (aLine.trim().startsWith(HASH))
    {
      Matcher mySheetMatcher = CSV_SHEET_PATTERN.matcher(aLine);
      Matcher myNameMatcher = CSV_NAME_PATTERN.matcher(aLine);
      if (mySheetMatcher.find())
      {
        int mySheetNum = Integer.parseInt(mySheetMatcher.group(1));
        String mySheetName = mySheetMatcher.group(2);
        ensureSheetExists(aSpreadsheet, mySheetName, mySheetNum);
      }
      else if (myNameMatcher.find())
      {
        String myName = myNameMatcher.group(1);
        String myValue = myNameMatcher.group(2);
        try
        {
          aSpreadsheet.setNamedRange(myName, myValue);
        }
        catch (SpreadsheetException myEx)
        {
          String myMessage = "Unable to set named range from CSV : " + myName + " : " + myValue;
          LOG.info(myMessage);
        }
      }
    }
    else if (!aLine.trim().isEmpty())
    {
      try
      {
        addCSVLine(aSpreadsheet, aLine);
      }
      catch (SpreadsheetException myEx)
      {
        String myMessage = "Unable to set CSV line in spreadsheet : " + aLine;
        LOG.info(myMessage);
      }
    }
  }

  private static void addCSVLine(ISpreadsheet aSpreadsheet, String aLine)
    throws SpreadsheetException
  {
    int myRow = 0;
    int myColumn = 0;
    String myRef = null;
    String myValue = null;
    String myFormula = null;
    String myType = null;
    String myFormat = null;
    String[] myValues = aLine.split(COMMA);
    try
    {
      if (myValues.length == 8)
      {
        myRow = Integer.parseInt(myValues[0]);
        myColumn = Integer.parseInt(myValues[1]);
        myRef = decodeCSVText(myValues[2]);
        myValue = decodeCSVText(myValues[3]);
        myFormula = decodeCSVText(myValues[5]);
        myType = decodeCSVText(myValues[6]);
        myFormat = decodeCSVText(myValues[7]);
      }
    }
    catch (NumberFormatException myEx)
    {
      String myMessage = "Unable to process CSV line " + aLine;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    addCSVData(aSpreadsheet, myRef, myRow, myColumn, myValue, myFormula, myType);
    if (myFormat != null && !myFormat.isEmpty())
    {
      aSpreadsheet.setFormat(aSpreadsheet.getRange(myRef).getSheet(), myRow, myColumn, myFormat);
    }
  }

  private static String decodeCSVText(String someText)
  {
    String myResult = someText;
    if (myResult != null && !myResult.isEmpty())
    {
      // strip leading and trailing quotes, if they're there
      if (myResult.startsWith(QUOTE) && myResult.endsWith(QUOTE))
      {
        myResult = myResult.substring(1, myResult.length() - 1);
      }
      // convert encoded double-quotes to double-quotes and encoded commas to commas
      myResult = myResult.replace(ENCODED_QUOTE, QUOTE).replace(ENCODED_COMMA, COMMA);
    }
    return myResult;
  }  
  
  private static void addCSVData(ISpreadsheet aSpreadsheet, String aRef, int aRow, int aCol,
    String aValue, String aFormula, String aType) throws SpreadsheetException
  {
    int mySheet = aSpreadsheet.getRange(aRef).getSheet();

    if (aFormula != null && !aFormula.isEmpty())
    {
      aSpreadsheet.setFormula(mySheet, aRow, aCol, aFormula);
    }
    else if (aType.equals(CELLTYPE.Number.toString())
      || aType.equals(CELLTYPE.Logical.toString())
      || aType.equals(CELLTYPE.Error.toString()))
    {
      aSpreadsheet.setNumber(mySheet, aRow, aCol, Double.parseDouble(aValue));
    }
    else if (aType.equals(CELLTYPE.Text.toString()))
    {
      aSpreadsheet.setText(mySheet, aRow, aCol, aValue);
    }
    else if (aType.equals(CELLTYPE.Formula.toString()))
    {
      aSpreadsheet.setFormula(mySheet, aRow, aCol, aValue);
    }
    else if (!aType.equals(CELLTYPE.Empty.toString()))
    {
      aSpreadsheet.setEntry(mySheet, aRow, aCol, aValue);
    }
  }
  
  private static void ensureSheetExists(ISpreadsheet aSpreadsheet, String aSheetName, int aPosition)
    throws SpreadsheetException
  {
    String myCurrentSheetName = aSpreadsheet.getSheetName(aPosition);
    if (myCurrentSheetName == null)
    {
      aSpreadsheet.createSheet(aSheetName, aPosition);
    }
    else if (!myCurrentSheetName.equals(aSheetName))
    {
      aSpreadsheet.renameSheet(aPosition, aSheetName);
    }
  } 
}
