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
import com.smartxls.RangeArea;
import com.smartxls.RangeStyle;
import com.smartxls.WorkBook;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Wrapper for SmartXLS spreadsheets.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public final class SmartXLSSpreadsheet implements ISpreadsheet
{

  private static final String IN_SPREADSHEET_AT = "' in spreadsheet at ";
  private static final Logger LOG = Log.getLogger(SmartXLSSpreadsheet.class.getName());
  private static final String MS = "ms";
  private static final String SINGLE_QUOTE = "'";
  private static final String DELETE_ME = "___DELETE_ME___";
  private static final String SHEET1 = "Sheet1";
  private static final int LETTERS_IN_ALPHABET = 26;
  private static final int ASCII_A = 65;
  private static final String UNABLE_TO_COPY_SHEET = "Unable to copy sheet ";
  private static final String FROM_SOURCE_TO_DEST = " from source spreadsheet to destination sheet ";
  private static final String CANNOT_FIND_SOURCE_SHEET = " from source spreadsheet as it cannot be found";
  private static final String CANNOT_FIND_DEST_SHEET = " as the destination sheet cannot be found";
  private static final String ROW = " row ";
  private static final String COLUMN = " column ";

  // see http://support.microsoft.com/kb/214344 for info on iteration
  private static final boolean ITERATION_ENABLED = !Boolean.getBoolean("spreadsheet.iteration.disabled");
  private static final int ITERATION_MAX = Integer.getInteger("spreadsheet.iteration.max", 100);
  private static final String ITERATION_MAX_CHANGE = System.getProperty("spreadsheet.iteration.max.change");

  private WorkBook theWorkBook = null;
  private FORMAT theFormat = null;

  /**
   * Constructor.
   *
   * @throws SpreadsheetException on error
   */
  public SmartXLSSpreadsheet() throws SpreadsheetException
  {
    // constructor
    theWorkBook = new WorkBook();
    theFormat = FORMAT.XLSX;
    setAutoRecalc(false);
    setupIteration();
  }

  @Override
  public long readXLS(InputStream aStream)
          throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      theWorkBook.read(aStream);
      theFormat = FORMAT.XLS;
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to read spreadsheet from XLS stream";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
  }

  @Override
  public long readXLSX(InputStream aStream)
          throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      theWorkBook.readXLSX(aStream);
      theFormat = FORMAT.XLSX;
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to read spreadsheet from XLSX stream";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
  }

  @Override
  public long readXLSB(InputStream aStream)
          throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      theWorkBook.readXLSB(aStream);
      theFormat = FORMAT.XLSB;
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to read spreadsheet from XLSB stream";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
  }

  @Override
  public long readXLSM(InputStream aStream)
          throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      theWorkBook.readXLSX(aStream);
      theFormat = FORMAT.XLSM;
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to read spreadsheet from XLSM stream";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
  }

  @Override
  public long read(InputStream aStream, FORMAT aFormat)
          throws SpreadsheetException
  {
    if (theFormat.equals(FORMAT.XLSX))
    {
      return readXLSX(aStream);
    }
    else if (theFormat.equals(FORMAT.XLS))
    {
      return readXLS(aStream);
    }
    else if (theFormat.equals(FORMAT.XLSM))
    {
      return readXLSM(aStream);
    }
    else if (theFormat.equals(FORMAT.XLSB))
    {
      return readXLSB(aStream);
    }
    else
    {
      String myMessage = "Unable to read spreadsheet (unknown format) : " + aFormat;
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
  }

  @Override
  public long read(InputStream aStream, String aFileName)
          throws SpreadsheetException
  {
    setFormatFromFileName(aFileName);
    return read(aStream, theFormat);
  }

  @Override
  public long read(File aFile)
          throws SpreadsheetException
  {
    long myResult = 0;
    try
    {
      InputStream myStream = new BufferedInputStream(new FileInputStream(aFile));
      myResult = read(myStream, aFile.getName());
      myStream.close();
    }
    catch (IOException myEx)
    {
      String myMessage = "Unable to read file : " + aFile.getAbsolutePath();
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public long recalculate()
          throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      theWorkBook.recalc();
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to recalculate spreadsheet";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
  }

  @Override
  public void setAutoRecalc(boolean isAutoRecalc)
          throws SpreadsheetException
  {
    theWorkBook.setAutoRecalc(isAutoRecalc);
    setupIteration();
  }

  @Override
  public FORMAT getFormat()
  {
    return theFormat;
  }

  @Override
  public long writeXLS(OutputStream aStream)
          throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      theWorkBook.write(aStream);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to write spreadsheet to XLS stream";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
  }

  @Override
  public long writeXLSX(OutputStream aStream)
          throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      // the writeXLSX method closes the stream, so when writing to a ZipOutputStream (like we do with the calc log)
      // the zip file is closed prematurely. This was raised with SmartXLS, and the response was: 'Please use
      // memory cache to store the stream buffer written by this component, don't use it directly.'
      ByteArrayOutputStream myStream = new ByteArrayOutputStream();
      theWorkBook.writeXLSX(myStream);
      aStream.write(myStream.toByteArray());
      myStream.close();
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to write spreadsheet to XLSX stream";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
  }

  @Override
  public long writeXLSB(OutputStream aStream)
          throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      // the writeXLSB method closes the stream, so when writing to a ZipOutputStream (like we do with the calc log)
      // the zip file is closed prematurely. This was raised with SmartXLS, and the response was: 'Please use
      // memory cache to store the stream buffer written by this component, don't use it directly.'
      ByteArrayOutputStream myStream = new ByteArrayOutputStream();
      theWorkBook.writeXLSB(myStream);
      aStream.write(myStream.toByteArray());
      myStream.close();
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to write spreadsheet to XLSB stream";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
  }

  @Override
  public long writeXLSM(OutputStream aStream)
          throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();
    try
    {
      // the writeXLSX method closes the stream, so when writing to a ZipOutputStream (like we do with the calc log)
      // the zip file is closed prematurely. This was raised with SmartXLS, and the response was: 'Please use
      // memory cache to store the stream buffer written by this component, don't use it directly.'
      ByteArrayOutputStream myStream = new ByteArrayOutputStream();
      theWorkBook.writeXLSX(myStream);
      aStream.write(myStream.toByteArray());
      myStream.close();
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to write spreadsheet to XLSM stream";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
  }

  @Override
  public long write(OutputStream aStream)
          throws SpreadsheetException
  {
    if (theFormat.equals(FORMAT.XLSX))
    {
      return writeXLSX(aStream);
    }
    else if (theFormat.equals(FORMAT.XLS))
    {
      return writeXLS(aStream);
    }
    else if (theFormat.equals(FORMAT.XLSM))
    {
      return writeXLSM(aStream);
    }
    else if (theFormat.equals(FORMAT.XLSB))
    {
      return writeXLSB(aStream);
    }
    else
    {
      String myMessage = "Unable to write spreadsheet (unknown format) : " + theFormat.toString();
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
  }

  private void setFormatFromFileName(String aFileName) throws SpreadsheetException
  {
    if (aFileName.toUpperCase().endsWith(FORMAT.XLSX.toString()))
    {
      theFormat = FORMAT.XLSX;
    }
    else if (aFileName.toUpperCase().endsWith(FORMAT.XLS.toString()))
    {
      theFormat = FORMAT.XLS;
    }
    else if (aFileName.toUpperCase().endsWith(FORMAT.XLSM.toString()))
    {
      theFormat = FORMAT.XLSM;
    }
    else if (aFileName.toUpperCase().endsWith(FORMAT.XLSB.toString()))
    {
      theFormat = FORMAT.XLSB;
    }
    else
    {
      String myMessage = "Unable to write spreadsheet (unknown format) : " + aFileName;
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
  }

  @Override
  public long write(OutputStream aStream, String aFileName)
          throws SpreadsheetException
  {
    setFormatFromFileName(aFileName);
    return write(aStream);
  }

  @Override
  public long write(OutputStream aStream, FORMAT aFormat)
          throws SpreadsheetException
  {
    theFormat = aFormat;
    return write(aStream);
  }

  @Override
  public long write(File aFile, FORMAT aFormat)
          throws SpreadsheetException
  {
    long myResult = 0;
    try
    {
      OutputStream myStream = new BufferedOutputStream(new FileOutputStream(aFile));
      myResult = write(myStream, aFormat);
      myStream.close();
    }
    catch (IOException myEx)
    {
      String myMessage = "Unable to write spreadsheet to file : " + aFile.getAbsolutePath();
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public long write(File aFile)
          throws SpreadsheetException
  {
    long myResult = 0;
    try
    {
      OutputStream myStream = new BufferedOutputStream(new FileOutputStream(aFile));
      myResult = write(myStream);
      myStream.close();
    }
    catch (IOException myEx)
    {
      String myMessage = "Unable to write spreadsheet to file : " + aFile.getAbsolutePath();
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public String getNamedRange(String aNamedRange)
  {
    String myResult = null;
    try
    {
      myResult = theWorkBook.getDefinedName(aNamedRange);
    }
    catch (Exception myEx)
    {
      LOG.info("Unable to locate named range : " + aNamedRange, this);
    }
    return myResult;
  }

  @Override
  public Iterator<String> getNamedRangeIterator()
          throws SpreadsheetException
  {
    return getNamedRanges().iterator();
  }

  @Override
  public List<String> getNamedRanges()
          throws SpreadsheetException
  {
    List<String> myNamedRanges = new ArrayList<String>();
    int myNamedRangeCount = theWorkBook.getDefinedNameCount();
    for (int i = 0; i < myNamedRangeCount; i++)
    {
      try
      {
        // defined names start at 1
        myNamedRanges.add(theWorkBook.getDefinedName(i + 1));
      }
      catch (Exception myEx)
      {
        String myMessage = "Unable to retrieve name of defined range number " + i;
        LOG.warn(myMessage, myEx);
        throw new SpreadsheetException(myMessage, myEx);
      }
    }
    return myNamedRanges;
  }

  @Override
  public boolean isNamedRangeValid(String aNamedRange)
  {
    boolean myResult = false;
    try
    {
      theWorkBook.getDefinedName(aNamedRange);
      myResult = true;
    }
    catch (Exception myEx)
    {
      LOG.info("Unable to locate named range : " + aNamedRange, this);
    }

    return myResult;
  }

  @Override
  public int getNamedRangeCount()
  {
    return theWorkBook.getDefinedNameCount();
  }

  @Override
  public void setNamedRange(String aNamedRange, String aFormula) throws SpreadsheetException
  {
    try
    {
      theWorkBook.setDefinedName(aNamedRange, aFormula);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to add or update defined name "
              + aNamedRange + " with formula " + aFormula;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public List<String> getSheets()
          throws SpreadsheetException
  {
    List<String> mySheets = new ArrayList<String>();
    int mySheetCount = theWorkBook.getNumSheets();
    for (int i = 0; i < mySheetCount; i++)
    {
      try
      {
        mySheets.add(theWorkBook.getSheetName(i));
      }
      catch (Exception myEx)
      {
        String myMessage = "Unable to retrieve sheet name for sheet number " + i;
        LOG.warn(myMessage, myEx);
        throw new SpreadsheetException(myMessage, myEx);
      }
    }
    return mySheets;
  }

  @Override
  public void deleteSheet(int aSheet) throws SpreadsheetException
  {
    try
    {
      theWorkBook.deleteSheets(aSheet, 1);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to delete sheet number " + aSheet;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public void deleteSheet(String aSheet) throws SpreadsheetException
  {
    deleteSheet(getSheetNum(aSheet));
  }

  @Override
  public void createSheet(String aSheet) throws SpreadsheetException
  {
    createSheet(aSheet, getSheetCount());
  }

  @Override
  public void createSheet(String aSheet, int aPosition) throws SpreadsheetException
  {
    try
    {
      int myNumberOfSheets = 1;
      theWorkBook.insertSheets(aPosition, myNumberOfSheets);
      renameSheet(aPosition, aSheet);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to insert sheet " + aSheet + " at position " + aPosition;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public Iterator<String> getSheetsIterator()
          throws SpreadsheetException
  {
    return getSheets().iterator();
  }

  @Override
  public int getSheetCount()
  {
    return theWorkBook.getNumSheets();
  }

  @Override
  public String getSheetName(int aSheetNum)
          throws SpreadsheetException
  {
    String myResult = null;
    try
    {
      myResult = theWorkBook.getSheetName(aSheetNum);
    }
    catch (Exception myEx)
    {
      LOG.info("Unable to locate sheet number : " + aSheetNum, this);
    }
    return myResult;
  }

  @Override
  public void renameSheet(String anOldSheetName, String aNewSheetName)
          throws SpreadsheetException
  {
    renameSheet(getSheetNum(anOldSheetName), aNewSheetName);
  }

  @Override
  public void renameSheet(int aSheetNum, String aNewSheetName)
          throws SpreadsheetException
  {
    try
    {
      theWorkBook.setSheetName(aSheetNum, aNewSheetName);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to rename sheet number "
              + aSheetNum + " to '" + aNewSheetName + SINGLE_QUOTE;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public int getSheetNum(String aSheetName)
          throws SpreadsheetException
  {
    return theWorkBook.findSheetByName(aSheetName);
  }

  @Override
  public Range getRange(String aRef) throws SpreadsheetException
  {
    return getRange(aRef, false);
  }

  @Override
  public Range getRange(String aRef, boolean isCanAdjust) throws SpreadsheetException
  {
    Range myResult = null;
    if (aRef != null && !aRef.isEmpty())
    {
      RangeArea myRange = theWorkBook.getFormulaRange(aRef);
      if (myRange == null)
      {
        String myMessage = "Unable to find cell reference '" + aRef + SINGLE_QUOTE;
        LOG.warn(myMessage, this);
        throw new SpreadsheetException(myMessage);
      }
      if (isCanAdjust)
      {
        myResult = new Range(myRange.getSheet(), myRange.getRow1(), myRange.getCol1(),
                myRange.getRow2(), getLastRow(myRange.getSheet()),
                myRange.getCol2(), getLastCol(myRange.getSheet()));
      }
      else
      {
        myResult = new Range(myRange.getSheet(), myRange.getRow1(), myRange.getCol1(),
                myRange.getRow2(), myRange.getCol2());
      }
    }
    return myResult;
  }

  @Override
  public String getCellRef(int aSheet, int aRow, int aCol)
          throws SpreadsheetException
  {
    return getCellRef(getSheetName(aSheet), aRow, aCol);
  }

  @Override
  public String getCellRef(String aSheet, int aRow, int aCol)
          throws SpreadsheetException
  {
    return SINGLE_QUOTE + aSheet + "'!" + getCellRef(aRow, aCol);
  }

  @Override
  public String getCellRef(int aSheet, int aStartRow, int aStartCol, int anEndRow, int anEndCol)
          throws SpreadsheetException
  {
    return getCellRef(getSheetName(aSheet), aStartRow, aStartCol, anEndRow, anEndCol);
  }

  @Override
  public String getCellRef(String aSheet, int aStartRow, int aStartCol, int anEndRow, int anEndCol)
          throws SpreadsheetException
  {
    return SINGLE_QUOTE + aSheet + "'!" + getCellRef(aStartRow, aStartCol) + ":" + getCellRef(anEndRow, anEndCol);
  }

  @Override
  public String getCellRef(int aRow, int aCol)
          throws SpreadsheetException
  {
    return getColumnName(aCol) + (aRow + 1);
  }

  private static String getColumnName(int aColNum) throws SpreadsheetException
  {
    StringBuilder myResult = new StringBuilder();

    // maximum number of columns in Excel 2010 is 16,384
    if (aColNum < 0 || aColNum > 16383)
    {
      String myMessage = "Column number out of range : " + aColNum;
      LOG.warn(myMessage);
      throw new SpreadsheetException(myMessage);
    }

    int myColumn = aColNum;

    myResult.insert(0, getSingleColumnName(myColumn));
    while (myColumn >= LETTERS_IN_ALPHABET)
    {
      myColumn /= LETTERS_IN_ALPHABET;
      myColumn -= 1;
      myResult.insert(0, getSingleColumnName(myColumn));
    }
    return myResult.toString();
  }

  private static String getSingleColumnName(int aColNum) throws SpreadsheetException
  {
    return Character.toString((char) ((aColNum % LETTERS_IN_ALPHABET) + ASCII_A));
  }

  @Override
  public void setNumber(String aSheet, int aRow, int aCol, double aNumericValue)
          throws SpreadsheetException
  {
    setNumber(getSheetNum(aSheet), aRow, aCol, aNumericValue);
  }

  @Override
  public void setNumber(int aSheet, int aRow, int aCol, double aNumericValue)
          throws SpreadsheetException
  {
    try
    {
      theWorkBook.setNumber(aSheet, aRow, aCol, aNumericValue);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to set numeric value '" + aNumericValue
              + IN_SPREADSHEET_AT + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public void setText(String aSheet, int aRow, int aCol, String aTextValue)
          throws SpreadsheetException
  {
    setText(getSheetNum(aSheet), aRow, aCol, aTextValue);
  }

  @Override
  public void setText(int aSheet, int aRow, int aCol, String aTextValue)
          throws SpreadsheetException
  {
    try
    {
      theWorkBook.setText(aSheet, aRow, aCol, aTextValue);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to set text value '" + aTextValue
              + IN_SPREADSHEET_AT + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public void setEntry(String aSheet, int aRow, int aCol, String anEntry)
          throws SpreadsheetException
  {
    setEntry(getSheetNum(aSheet), aRow, aCol, anEntry);
  }

  @Override
  public void setEntry(int aSheet, int aRow, int aCol, String anEntry)
          throws SpreadsheetException
  {
    try
    {
      theWorkBook.setEntry(aSheet, aRow, aCol, anEntry);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to set entry to '" + anEntry
              + IN_SPREADSHEET_AT + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public void setFormula(String aSheet, int aRow, int aCol, String aFormula)
          throws SpreadsheetException
  {
    setText(getSheetNum(aSheet), aRow, aCol, aFormula);
  }

  @Override
  public void setFormula(int aSheet, int aRow, int aCol, String aFormula)
          throws SpreadsheetException
  {
    try
    {
      theWorkBook.setFormula(aSheet, aRow, aCol, aFormula);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to set formula '" + aFormula
              + IN_SPREADSHEET_AT + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public CELLTYPE getType(String aSheetName, int aRow, int aCol) throws SpreadsheetException
  {
    return getType(getSheetNum(aSheetName), aRow, aCol);
  }

  @Override
  public CELLTYPE getType(int aSheet, int aRow, int aCol) throws SpreadsheetException
  {
    CELLTYPE myResult = null;
    try
    {
      int myType = theWorkBook.getType(aSheet, aRow, aCol);
      if (myType < 0)
      {
        myResult = CELLTYPE.Formula;
      }
      else
      {
        switch (myType)
        {
          case (WorkBook.TypeEmpty):
            myResult = CELLTYPE.Empty;
            break;
          case (WorkBook.TypeNumber):
            myResult = CELLTYPE.Number;
            break;
          case (WorkBook.TypeText):
            myResult = CELLTYPE.Text;
            break;
          case (WorkBook.TypeLogical):
            myResult = CELLTYPE.Logical;
            break;
          case (WorkBook.TypeError):
            myResult = CELLTYPE.Error;
            break;
          default:
            myResult = null;
            break;
        }
      }
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get the type from spreadsheet cell "
              + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public void setFormat(String aSheetName, int aRow, int aCol, String aFormat) throws SpreadsheetException
  {
    setFormat(getSheetNum(aSheetName), aRow, aCol, aFormat);
  }

  @Override
  public void setFormat(int aSheet, int aRow, int aCol, String aFormat) throws SpreadsheetException
  {
    try
    {
      theWorkBook.setSheet(aSheet);
      RangeStyle myRangeStyle = theWorkBook.getRangeStyle(aRow, aCol, aRow, aCol);
      myRangeStyle.setCustomFormat(aFormat);
      theWorkBook.setRangeStyle(myRangeStyle, aRow, aCol, aRow, aCol);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to set the format '" + aFormat + "' into spreadsheet cell "
              + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public String getFormat(String aSheetName, int aRow, int aCol) throws SpreadsheetException
  {
    return getFormat(getSheetNum(aSheetName), aRow, aCol);
  }

  @Override
  public String getFormat(int aSheet, int aRow, int aCol) throws SpreadsheetException
  {
    String myResult = null;
    try
    {
      theWorkBook.setSheet(aSheet);
      myResult = theWorkBook.getRangeStyle(aRow, aCol, aRow, aCol).getCustomFormat();
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get the format from spreadsheet cell "
              + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public double getNumber(String aSheetName, int aRow, int aCol) throws SpreadsheetException
  {
    return getNumber(getSheetNum(aSheetName), aRow, aCol);
  }

  @Override
  public double getNumber(int aSheet, int aRow, int aCol) throws SpreadsheetException
  {
    double myResult = -1;
    try
    {
      myResult = theWorkBook.getNumber(aSheet, aRow, aCol);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get the numeric value from spreadsheet cell "
              + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public String getText(String aSheetName, int aRow, int aCol) throws SpreadsheetException
  {
    return getText(getSheetNum(aSheetName), aRow, aCol);
  }

  @Override
  public String getText(int aSheet, int aRow, int aCol) throws SpreadsheetException
  {
    String myResult = null;
    try
    {
      myResult = theWorkBook.getText(aSheet, aRow, aCol);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get the text value from spreadsheet cell "
              + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public String getFormula(String aSheetName, int aRow, int aCol) throws SpreadsheetException
  {
    return getFormula(getSheetNum(aSheetName), aRow, aCol);
  }

  @Override
  public String getFormula(int aSheet, int aRow, int aCol) throws SpreadsheetException
  {
    String myResult = null;
    try
    {
      myResult = theWorkBook.getFormula(aSheet, aRow, aCol);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get the formula from spreadsheet cell "
              + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public String getFormattedText(String aSheetName, int aRow, int aCol) throws SpreadsheetException
  {
    return getFormattedText(getSheetNum(aSheetName), aRow, aCol);
  }

  @Override
  public String getFormattedText(int aSheet, int aRow, int aCol) throws SpreadsheetException
  {
    String myResult = null;
    try
    {
      theWorkBook.setSheet(aSheet);
      myResult = theWorkBook.getFormattedText(aRow, aCol);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get the formatted text value from spreadsheet cell "
              + getCellRef(aSheet, aRow, aCol);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public String getSheetRange(String aSheet) throws SpreadsheetException
  {
    return getSheetRange(getSheetNum(aSheet));
  }

  @Override
  public String getSheetRange(int aSheet) throws SpreadsheetException
  {
    String myResult = null;
    try
    {
      myResult = getCellRef(aSheet, 0, 0, getLastRow(aSheet), getLastCol(aSheet));
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get range for sheet : " + aSheet;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public long writeCSV(OutputStream aStream) throws SpreadsheetException
  {
    long myStartTime = CurrentDateTime.getCurrentTimeInMillis();

    SmartXLSCSVHelper.writeCSVHeader(this, aStream);
    try
    {
      for (int mySheet = 0; mySheet < getSheetCount(); mySheet++)
      {
        writeCSV(aStream, getSheetRange(mySheet));
      }
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to write spreadsheet to CSV";
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }

    long myElapsed = CurrentDateTime.getCurrentTimeInMillis() - myStartTime;
    String myMessage = "Write spreadsheet to CSV stream in " + MS + ": " + myElapsed;
    LOG.info(myMessage, this);
    return myElapsed;
  }

  @Override
  public long writeCSV(OutputStream aStream, String aRef)
          throws SpreadsheetException
  {
    return SmartXLSCSVHelper.writeCSV(this, aStream, aRef);
  }

  @Override
  public long readCSV(File aFile) throws SpreadsheetException
  {
    long myResult = 0;
    try
    {
      InputStream myStream = new BufferedInputStream(new FileInputStream(aFile));
      myResult = readCSV(myStream);
      myStream.close();
    }
    catch (IOException myEx)
    {
      String myMessage = "Unable to read csv file : " + aFile.getAbsolutePath();
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public long readCSV(InputStream aStream) throws SpreadsheetException
  {
    // reset the spreadsheet.
    theWorkBook = new WorkBook();
    renameSheet(SHEET1, DELETE_ME);
    long myTime = SmartXLSCSVHelper.readCSV(this, aStream);
    deleteSheet(DELETE_ME);
    return myTime;
  }

  @Override
  public void copySheet(String aSourceSheet, ISpreadsheet aSourceSpreadsheet, String aDestSheet, COPY aCopyType)
          throws SpreadsheetException
  {
    int myDestSheetIndex = getSheetNum(aDestSheet);
    int mySourceSheetIndex = aSourceSpreadsheet.getSheetNum(aSourceSheet);
    if (mySourceSheetIndex == -1)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + aSourceSheet + CANNOT_FIND_SOURCE_SHEET;
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
    if (myDestSheetIndex == -1)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + aSourceSheet
              + FROM_SOURCE_TO_DEST + aDestSheet + CANNOT_FIND_DEST_SHEET;
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
    copySheet(mySourceSheetIndex, aSourceSpreadsheet, myDestSheetIndex, aCopyType);
  }

  @Override
  public void copySheet(int aSourceSheet, ISpreadsheet aSourceSpreadsheet, int aDestSheet, COPY aCopyType)
          throws SpreadsheetException
  {
    String mySourceSheetName = aSourceSpreadsheet.getSheetName(aSourceSheet);
    String myDestSheetName = getSheetName(aDestSheet);

    if (mySourceSheetName == null)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + aSourceSheet + CANNOT_FIND_SOURCE_SHEET;
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
    if (myDestSheetName == null)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + mySourceSheetName
              + FROM_SOURCE_TO_DEST + aDestSheet + CANNOT_FIND_DEST_SHEET;
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
    try
    {
      Range myRange = aSourceSpreadsheet.getRange(aSourceSpreadsheet.getSheetRange(aSourceSheet));
      copyRange(aSourceSheet, aSourceSpreadsheet, aDestSheet, aCopyType, myRange);
    }
    catch (Exception myEx)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + mySourceSheetName
              + FROM_SOURCE_TO_DEST + myDestSheetName;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public void copyRange(String aSourceSheet, ISpreadsheet aSourceSpreadsheet,
          String aDestSheet, COPY aCopyType, Range aRange) throws SpreadsheetException
  {
    int myDestSheetIndex = getSheetNum(aDestSheet);
    int mySourceSheetIndex = aSourceSpreadsheet.getSheetNum(aSourceSheet);
    if (mySourceSheetIndex == -1)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + aSourceSheet + CANNOT_FIND_SOURCE_SHEET;
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
    if (myDestSheetIndex == -1)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + aSourceSheet
              + FROM_SOURCE_TO_DEST + aDestSheet + CANNOT_FIND_DEST_SHEET;
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
    copyRange(mySourceSheetIndex, aSourceSpreadsheet, myDestSheetIndex, aCopyType, aRange);
  }

  @Override
  public void copyRange(int aSourceSheet, ISpreadsheet aSourceSpreadsheet,
          int aDestSheet, COPY aCopyType, Range aRange) throws SpreadsheetException
  {
    String mySourceSheetName = aSourceSpreadsheet.getSheetName(aSourceSheet);
    String myDestSheetName = getSheetName(aDestSheet);

    if (mySourceSheetName == null)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + aSourceSheet + CANNOT_FIND_SOURCE_SHEET;
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
    if (myDestSheetName == null)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + mySourceSheetName
              + FROM_SOURCE_TO_DEST + aDestSheet + CANNOT_FIND_DEST_SHEET;
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
    try
    {
      copy(aSourceSheet, aSourceSpreadsheet, aDestSheet, aCopyType, aRange);
    }
    catch (Exception myEx)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + mySourceSheetName
              + FROM_SOURCE_TO_DEST + myDestSheetName;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  private void copy(int aSourceSheet, ISpreadsheet aSourceSpreadsheet,
          int aDestSheet, COPY aCopyType, Range aRange) throws SpreadsheetException
  {
    try
    {
      List<Integer> myCopyTypes = new ArrayList<Integer>();
      if (aCopyType.equals(COPY.VALUES))
      {
        // copy values and formats
        myCopyTypes.add(WorkBook.CopyValues);
        myCopyTypes.add(WorkBook.CopyFormats);
      }
      else if (aCopyType.equals(COPY.FORMULAS))
      {
        // copy formulas and formats
        myCopyTypes.add(WorkBook.CopyFormulas);
        myCopyTypes.add(WorkBook.CopyFormats);
      }
      else if (aCopyType.equals(COPY.FORMATS))
      {
        // copy formats
        myCopyTypes.add(WorkBook.CopyFormats);
      }
      else if (aCopyType.equals(COPY.ALL))
      {
        // copy all
        myCopyTypes.add(WorkBook.CopyAll);
      }

      for (Integer myCopyType : myCopyTypes)
      {
        theWorkBook.copyRange(aDestSheet, aRange.getStartRow(), aRange.getStartCol(),
                aRange.getEndRow(), aRange.getEndCol(),
                getWorkbook(aSourceSpreadsheet), aSourceSheet,
                aRange.getStartRow(), aRange.getStartCol(),
                aRange.getEndRow(), aRange.getEndCol(), myCopyType);
      }
      // copy column widths and whether they are hidden
      for (int myColumn = aRange.getStartCol(); myColumn < aRange.getEndCol(); myColumn++)
      {
        setColumnWidth(aDestSheet, myColumn, aSourceSpreadsheet.getColumnWidth(aSourceSheet, myColumn));
        setColumnHidden(aDestSheet, myColumn, aSourceSpreadsheet.isColumnHidden(aSourceSheet, myColumn));
      }
      // copy row heights and whether they are hidden
      for (int myRow = aRange.getStartRow(); myRow < aRange.getEndRow(); myRow++)
      {
        setRowHeight(aDestSheet, myRow, aSourceSpreadsheet.getRowHeight(aSourceSheet, myRow));
        setRowHidden(aDestSheet, myRow, aSourceSpreadsheet.isRowHidden(aSourceSheet, myRow));
      }
    }
    catch (Exception myEx)
    {
      String myMessage = UNABLE_TO_COPY_SHEET + aSourceSpreadsheet.getSheetName(aSourceSheet)
              + FROM_SOURCE_TO_DEST + getSheetName(aDestSheet);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public int getLastRow(String aSheetName) throws SpreadsheetException
  {
    return getLastRow(getSheetNum(aSheetName));
  }

  @Override
  public int getLastRow(int aSheet) throws SpreadsheetException
  {
    try
    {
      theWorkBook.setSheet(aSheet);
      return theWorkBook.getLastRow();
    }
    catch (Exception myEx)
    {
      String myMessage = "Could not get last row for sheet " + getSheetName(aSheet);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public int getLastCol(String aSheetName) throws SpreadsheetException
  {
    return getLastCol(getSheetNum(aSheetName));
  }

  @Override
  public int getLastCol(int aSheet) throws SpreadsheetException
  {
    int myLastCol = 0;
    try
    {
      theWorkBook.setSheet(aSheet);
      int myLastRow = theWorkBook.getLastRow();
      myLastRow = Math.max(0, myLastRow);
      for (int i = 0; i <= myLastRow; i++)
      {
        int myLastColForRow = theWorkBook.getLastColForRow(i);
        myLastCol = Math.max(myLastCol, myLastColForRow);
      }
    }
    catch (Exception myEx)
    {
      String myMessage = "Could not get last column for sheet " + getSheetName(aSheet);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myLastCol;
  }

  private WorkBook getWorkbook(ISpreadsheet aSpreadsheet) throws SpreadsheetException
  {
    if (aSpreadsheet instanceof SmartXLSSpreadsheet)
    {
      SmartXLSSpreadsheet mySpreadsheet = (SmartXLSSpreadsheet) aSpreadsheet;
      return mySpreadsheet.theWorkBook;
    }
    else
    {
      String myMessage = "Spreadsheet is not a " + SmartXLSSpreadsheet.class.getName();
      LOG.warn(myMessage, this);
      throw new SpreadsheetException(myMessage);
    }
  }

  @Override
  public void setNumber(String aRef, double aNumericValue) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    setNumber(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol(), aNumericValue);
  }

  @Override
  public void setEntry(String aRef, String anEntry) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    setEntry(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol(), anEntry);
  }

  @Override
  public void setText(String aRef, String aTextValue) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    setText(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol(), aTextValue);
  }

  @Override
  public void setFormula(String aRef, String aFormula) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    setFormula(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol(), aFormula);
  }

  @Override
  public double getNumber(String aRef) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    return getNumber(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol());
  }

  @Override
  public void setFormat(String aRef, String aFormat) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    setFormat(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol(), aFormat);
  }

  @Override
  public String getFormat(String aRef) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    return getFormat(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol());
  }

  @Override
  public CELLTYPE getType(String aRef) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    return getType(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol());
  }

  @Override
  public String getText(String aRef) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    return getText(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol());
  }

  @Override
  public String getFormula(String aRef) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    return getFormula(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol());
  }

  @Override
  public String getFormattedText(String aRef) throws SpreadsheetException
  {
    Range myRange = getRange(aRef);
    return getFormattedText(myRange.getSheet(), myRange.getStartRow(), myRange.getStartCol());
  }

  @Override
  public int getColumnWidth(String aSheet, int aColumn) throws SpreadsheetException
  {
    return getColumnWidth(getSheetNum(aSheet), aColumn);
  }

  @Override
  public int getColumnWidth(int aSheet, int aColumn) throws SpreadsheetException
  {
    int myResult = -1;
    try
    {
      theWorkBook.setSheet(aSheet);
      myResult = theWorkBook.getColWidth(aColumn);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get the column width from sheet " + getSheetName(aSheet) + COLUMN
              + getColumnName(aColumn);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public void setColumnWidth(String aSheet, int aColumn, int aWidth) throws SpreadsheetException
  {
    setColumnWidth(getSheetNum(aSheet), aColumn, aWidth);
  }

  @Override
  public void setColumnWidth(int aSheet, int aColumn, int aWidth) throws SpreadsheetException
  {
    try
    {
      theWorkBook.setSheet(aSheet);
      theWorkBook.setColWidth(aColumn, aWidth);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to set the column width in sheet " + getSheetName(aSheet) + COLUMN
              + getColumnName(aColumn) + " width " + aWidth;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public boolean isColumnHidden(String aSheet, int aColumn) throws SpreadsheetException
  {
    return isColumnHidden(getSheetNum(aSheet), aColumn);
  }

  @Override
  public boolean isColumnHidden(int aSheet, int aColumn) throws SpreadsheetException
  {
    boolean myResult = false;
    try
    {
      theWorkBook.setSheet(aSheet);
      myResult = theWorkBook.isColHidden(aColumn);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get the column hidden value from sheet " + getSheetName(aSheet) + COLUMN
              + getColumnName(aColumn);
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public void setColumnHidden(String aSheet, int aColumn, boolean isHidden) throws SpreadsheetException
  {
    setColumnHidden(getSheetNum(aSheet), aColumn, isHidden);
  }

  @Override
  public void setColumnHidden(int aSheet, int aColumn, boolean isHidden) throws SpreadsheetException
  {
    try
    {
      theWorkBook.setSheet(aSheet);
      theWorkBook.setColHidden(aColumn, isHidden);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to set the column hidden value in sheet " + getSheetName(aSheet) + COLUMN
              + getColumnName(aColumn) + " hidden " + isHidden;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public int getRowHeight(String aSheet, int aRow) throws SpreadsheetException
  {
    return getRowHeight(getSheetNum(aSheet), aRow);
  }

  @Override
  public int getRowHeight(int aSheet, int aRow) throws SpreadsheetException
  {
    int myResult = -1;
    try
    {
      theWorkBook.setSheet(aSheet);
      myResult = theWorkBook.getRowHeight(aRow);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get the row height from sheet " + getSheetName(aSheet) + ROW + aRow;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public void setRowHeight(String aSheet, int aRow, int aHeight) throws SpreadsheetException
  {
    setRowHeight(getSheetNum(aSheet), aRow, aHeight);
  }

  @Override
  public void setRowHeight(int aSheet, int aRow, int aHeight) throws SpreadsheetException
  {
    try
    {
      theWorkBook.setSheet(aSheet);
      theWorkBook.setRowHeight(aRow, aHeight);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to set the row height in sheet "
              + getSheetName(aSheet) + ROW + aRow + " height " + aHeight;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  @Override
  public boolean isRowHidden(String aSheet, int aRow) throws SpreadsheetException
  {
    return isRowHidden(getSheetNum(aSheet), aRow);
  }

  @Override
  public boolean isRowHidden(int aSheet, int aRow) throws SpreadsheetException
  {
    boolean myResult = false;
    try
    {
      theWorkBook.setSheet(aSheet);
      myResult = theWorkBook.isRowHidden(aRow);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to get the row hidden value from sheet "
              + getSheetName(aSheet) + ROW + aRow;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
    return myResult;
  }

  @Override
  public void setRowHidden(String aSheet, int aRow, boolean isHidden) throws SpreadsheetException
  {
    setRowHidden(getSheetNum(aSheet), aRow, isHidden);
  }

  @Override
  public void setRowHidden(int aSheet, int aRow, boolean isHidden) throws SpreadsheetException
  {
    try
    {
      theWorkBook.setSheet(aSheet);
      theWorkBook.setRowHidden(aRow, isHidden);
    }
    catch (Exception myEx)
    {
      String myMessage = "Unable to set the row hidden value in sheet " + getSheetName(aSheet) + ROW
              + aRow + " hidden " + isHidden;
      LOG.warn(myMessage, myEx);
      throw new SpreadsheetException(myMessage, myEx);
    }
  }

  private void setupIteration()
  {
    theWorkBook.setIterationEnabled(ITERATION_ENABLED);
    theWorkBook.setIterationMax(ITERATION_MAX);

    double myIterationMaxChange = 0.001d;
    if (ITERATION_MAX_CHANGE != null)
    {
      try
      {
        myIterationMaxChange = Double.parseDouble(ITERATION_MAX_CHANGE);
      }
      catch (NumberFormatException myEx)
      {
        // log but otherwise ignore
        String myMessage = "Unable to parse system property spreadsheet.iteration.max.change : " + ITERATION_MAX_CHANGE;
        LOG.info(myMessage, myEx);
      }
    }

    theWorkBook.setIterationMaxChange(myIterationMaxChange);
  }
}
