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
package com.energysys.connector.util.spreadsheet;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a spreadsheet.
 * @author EnergySys Limited
 * @version $Revision$
 */
public interface ISpreadsheet
{
  /**
   * Spreadsheet MIME type.
   */
  String SPREADSHEET_MIME_TYPE = "application/vnd.ms-excel";
  
  /**
   * Spreadsheet file formats.
   */
  enum FORMAT
  {
    /**
     * XLS file format.
     */
    XLS,
    /**
     * XLSX file format.
     */
    XLSX,
    /**
     * XLSM file format.
     */
    XLSM,
    /**
     * XLSB file format.
     */
    XLSB,
  };

  /**
   * The type of the spreadsheet cell.
   */
  enum CELLTYPE
  {
    /**
     * Empty type.
     */
    Empty,
    /**
     * Numeric type.
     */
    Number,
    /**
     * Text type.
     */
    Text,
    /**
     * Logical (boolean) type.
     */
    Logical,
    /**
     * Error type.
     */
    Error,
    /**
     * Formula type.
     */
    Formula,
  };

  /**
   * Spreadsheet copy types.
   */
  enum COPY
  {
    /**
     * Copy formulas and formats.
     */
    FORMULAS,
    /**
     * Copy values and formats.
     */
    VALUES,
    /**
     * Copy formats only.
     */
    FORMATS,
    /**
     * Copy all.
     */
    ALL,
  };  
  
  /**
   * Get the format of the spreadsheet.
   * This is the format that the spreadsheet was read in as, or last saved as.
   * @return FORMAT the format of the spreadsheet;
   */
  FORMAT getFormat();
  
  /**
   * Read XLS format spreadsheet.
   * @param aStream an input stream to read
   * @return long time in milliseconds it took to read the spreadsheet
   * @throws SpreadsheetException on error
   */
  long readXLS(InputStream aStream) throws SpreadsheetException;

  /**
   * Read XLSX format spreadsheet.
   * @param aStream an input stream to read
   * @return long time in milliseconds it took to read the spreadsheet
   * @throws SpreadsheetException on error
   */
  long readXLSX(InputStream aStream) throws SpreadsheetException;

  /**
   * Read XLSB format spreadsheet.
   * @param aStream an input stream to read
   * @return long time in milliseconds it took to read the spreadsheet
   * @throws SpreadsheetException on error
   */
  long readXLSB(InputStream aStream) throws SpreadsheetException;

  /**
   * Read XLSM format spreadsheet.
   * @param aStream an input stream to read
   * @return long time in milliseconds it took to read the spreadsheet
   * @throws SpreadsheetException on error
   */
  long readXLSM(InputStream aStream) throws SpreadsheetException;

  /**
   * Read spreadsheet using the extension of the filename to determine the format.
   * @param aStream an input stream to read
   * @param aFileName a file name, the file name of the spreadsheet, used to determine the format.
   * @return long time in milliseconds it took to read the spreadsheet
   * @throws SpreadsheetException on error
   */
  long read(InputStream aStream, String aFileName) throws SpreadsheetException;

  /**
   * Read spreadsheet from a file using the extension of the filename to determine the format.
   * @param aFile a file to read
   * @return long time in milliseconds it took to read the spreadsheet
   * @throws SpreadsheetException on error
   */
  long read(File aFile) throws SpreadsheetException;

  /**
   * Read spreadsheet from stream using the format specified.
   * @param aStream an input stream to read
   * @param aFormat the format to use when reading.
   * @return long time in milliseconds it took to read the spreadsheet
   * @throws SpreadsheetException on error
   */  
  long read(InputStream aStream, FORMAT aFormat) throws SpreadsheetException;  
  
  /**
   * Read spreadsheet from an XML file in the Process Input format. Only the top-left
   * value of the range is required, the matrix will be completely read even if it overflows the range.
   * If the XML stream contains cell references, these will be ignored in preference of the reference supplied.
   * @param aFile the file.
   * @return long time in milliseconds it took to read the spreadsheet
   * @throws SpreadsheetException on error
   */
  long readCSV(File aFile) throws SpreadsheetException;
  /**
   * Read spreadsheet from an CVS file. The spreadsheet will be reset before reading from the CSV file, so
   * once complete, the spreadsheet will only contain the data from the CSV file - this is not a merge.
   * @param aStream an input stream to read
   * @return long time in milliseconds it took to read the spreadsheet
   * @throws SpreadsheetException on error
   */
  long readCSV(InputStream aStream) throws SpreadsheetException;

  /**
   * Write XLS format spreadsheet to stream.
   * @param aStream an output stream to write to
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */
  long writeXLS(OutputStream aStream) throws SpreadsheetException;

  /**
   * Write XLSX format spreadsheet to stream.
   * @param aStream an output stream to write to
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */
  long writeXLSX(OutputStream aStream) throws SpreadsheetException;

  /**
   * Write XLSB format spreadsheet to stream.
   * @param aStream an output stream to write to
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */
  long writeXLSB(OutputStream aStream) throws SpreadsheetException;

  /**
   * Write XLSM format spreadsheet to stream.
   * @param aStream an output stream to write to
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */
  long writeXLSM(OutputStream aStream) throws SpreadsheetException;

  /**
   * Write spreadsheet using the format that was used when reading.
   * @param aStream an output stream to write to
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */
  long write(OutputStream aStream) throws SpreadsheetException;

  /**
   * Write spreadsheet to the file using the format specified.
   * @param aFile the file to write to.
   * @param aFormat the format to use when writing.
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */
  long write(File aFile, FORMAT aFormat) throws SpreadsheetException;

  /**
   * Write spreadsheet to the file using the format that was used when reading.
   * @param aFile the file to write to.
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */
  long write(File aFile) throws SpreadsheetException;

  /**
   * Write spreadsheet to the stream using the format of the filename specified.
   * @param aStream an output stream to write to.
   * @param aFileName the file name of the file being written to - only used to determine the format to use.
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */  
  long write(OutputStream aStream, String aFileName) throws SpreadsheetException;

  /**
   * Write spreadsheet to the stream using the format specified.
   * @param aStream an output stream to write to.
   * @param aFormat the format to use when writing.
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */  
  long write(OutputStream aStream, FORMAT aFormat) throws SpreadsheetException;
  
  /**
   * Write spreadsheet to CSV format.
   * @param aStream an output stream to write to
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */
  long writeCSV(OutputStream aStream) throws SpreadsheetException;

  /**
   * Write spreadsheet range to CSV format.
   * @param aStream an output stream to write to
   * @param aRef a range reference to write to XML.
   * @return long time in milliseconds it took to write the spreadsheet
   * @throws SpreadsheetException on error
   */
  long writeCSV(OutputStream aStream, String aRef) throws SpreadsheetException;

  /**
   * Recalculate this workbook.
   * @return long time in milliseconds it took to recalculate the spreadsheet
   * @throws SpreadsheetException on error.
   */
  long recalculate() throws SpreadsheetException;

  /**
   * Specify whether to auto recalculate the spreadsheet or not.
   * @param isAutoRecalc true if auto recalculate should be switched on, false otherwise
   * @throws SpreadsheetException on error.
   */
  void setAutoRecalc(boolean isAutoRecalc) throws SpreadsheetException;

  /**
   * Copies a sheet from the source spreadsheet to a sheet in the current spreadsheet.
   * @param aSourceSheet the number of the sheet to copy
   * @param aSourceSpreadsheet the source spreadsheet
   * @param aDestSheet the number of the sheet to copy to
   * @param aCopyType the type of copy to make (values, formulas, etc)
   * @throws SpreadsheetException on error
   */
   void copySheet(int aSourceSheet, ISpreadsheet aSourceSpreadsheet, int aDestSheet, COPY aCopyType)
     throws SpreadsheetException;

  /**
   * Copies a sheet from the source spreadsheet to a sheet in the current spreadsheet.
   * @param aSourceSheet the name of the sheet to copy
   * @param aSourceSpreadsheet the source spreadsheet
   * @param aDestSheet the name of the sheet to copy to
   * @param aCopyType the type of copy to make (values, formulas, etc)
   * @throws SpreadsheetException on error
   */
   void copySheet(String aSourceSheet, ISpreadsheet aSourceSpreadsheet, String aDestSheet, COPY aCopyType)
     throws SpreadsheetException;

  /**
   * Copies a range from a sheet in the source spreadsheet to a sheet in the current spreadsheet.
   * @param aSourceSheet the name of the sheet to copy
   * @param aSourceSpreadsheet the source spreadsheet
   * @param aDestSheet the name of the sheet to copy to
   * @param aCopyType the type of copy to make (values, formulas, etc)
   * @param aRange the range of cells to copy
   * @throws SpreadsheetException on error
   */
   void copyRange(String aSourceSheet, ISpreadsheet aSourceSpreadsheet,
     String aDestSheet, COPY aCopyType, Range aRange) throws SpreadsheetException;

  /**
   * Copies a range from a sheet in the source spreadsheet to a sheet in the current spreadsheet.
   * @param aSourceSheet the name of the sheet to copy
   * @param aSourceSpreadsheet the source spreadsheet
   * @param aDestSheet the name of the sheet to copy to
   * @param aCopyType the type of copy to make (values, formulas, etc)
   * @param aRange the range of cells to copy
   * @throws SpreadsheetException on error
   */
   void copyRange(int aSourceSheet, ISpreadsheet aSourceSpreadsheet,
     int aDestSheet, COPY aCopyType, Range aRange) throws SpreadsheetException;
   
  /**
   * Get the list of named ranges from this spreadsheet.
   * @return List of named ranges
   * @throws SpreadsheetException on error.
   */
  List<String> getNamedRanges() throws SpreadsheetException;

  /**
   * Get the list of named ranges from this spreadsheet as an iterator.
   * @return Iterator over the list of named ranges
   * @throws SpreadsheetException on error.
   */
  Iterator<String> getNamedRangeIterator() throws SpreadsheetException;

  /**
   * Check whether a named range is present in a spreadsheet.
   * @param aNamedRange a name to check for in the spreadsheet.
   * @return true if the named range is present, false otherwise.
   */
  boolean isNamedRangeValid(String aNamedRange);

  /**
   * Retrieve the value of a named range.
   * @param aNamedRange a name to retrieve the value for.
   * @return the value of the named range, or null if the name does not exist.
   */
  String getNamedRange(String aNamedRange);

  /**
   * Set the formula associated with a named range.
   * @param aNamedRange an existing named range if the formula is being updated, or a new name if a new named
   * range is being defined
   * @param aFormula the formula to set. Do not include a leading equal sign in the formula.
   * @throws SpreadsheetException on error
   */
  void setNamedRange(String aNamedRange, String aFormula) throws SpreadsheetException;

  /**
   * Retrieve the number of named ranges.
   * @return the number of named ranges.
   */
  int getNamedRangeCount();

  /**
   * Get the list of sheets from this spreadsheet.
   * @return List of sheets
   * @throws SpreadsheetException on error.
   */
  List<String> getSheets() throws SpreadsheetException;

  /**
   * Get the list of sheets from this spreadsheet as an iterator.
   * @return Iterator over the list of sheets
   * @throws SpreadsheetException on error.
   */
  Iterator<String> getSheetsIterator() throws SpreadsheetException;

  /**
   * Get the number of a sheet given its name.
   * @param aSheetName the name of a worksheet
   * @return the number of the sheet, or -1 if a sheet with that name cannot be found
   * @throws SpreadsheetException on error.
   */
  int getSheetNum(String aSheetName) throws SpreadsheetException;

  /**
   * Rename a sheet in the spreadsheet.
   * @param aSheet the number of a worksheet
   * @param aNewSheetName the new name of a worksheet
   * @throws SpreadsheetException on error.
   */
  void renameSheet(int aSheet, String aNewSheetName) throws SpreadsheetException;

  /**
   * Rename a sheet in the spreadsheet.
   * @param anOldSheetName the name of the sheet to rename.
   * @param aNewSheetName the new name of the sheet
   * @throws SpreadsheetException on error.
   */
  void renameSheet(String anOldSheetName, String aNewSheetName) throws SpreadsheetException;

  /**
   * Delete a sheet from the spreadsheet.
   * @param aSheet the number of a worksheet to delete
   * @throws SpreadsheetException on error.
   */
  void deleteSheet(int aSheet) throws SpreadsheetException;

  /**
   * Delete a sheet from the spreadsheet.
   * @param aSheet the name of a worksheet to delete
   * @throws SpreadsheetException on error.
   */
  void deleteSheet(String aSheet) throws SpreadsheetException;

  /**
   * Create a sheet in the spreadsheet at the end of the list of sheets.
   * @param aSheet the name of the new worksheet
   * @throws SpreadsheetException on error.
   */
  void createSheet(String aSheet) throws SpreadsheetException;

  /**
   * Create a sheet in the spreadsheet at the specified position.
   * @param aSheet the name of the worksheet to create
   * @param aPosition the position in the spreadsheet to create the new sheet.
   * @throws SpreadsheetException on error.
   */
  void createSheet(String aSheet, int aPosition) throws SpreadsheetException;

  /**
   * Get the name of a sheet given its number.
   * @param aSheet the number of a worksheet
   * @return the name of the sheet, or null if a sheet with that number cannot be found
   * @throws SpreadsheetException on error.
   */
  String getSheetName(int aSheet) throws SpreadsheetException;

  /**
   * Retrieve the number of sheets.
   * @return the number of sheets.
   */
  int getSheetCount();

  /**
   * Get a sheet/cell reference to all the cells in the sheet.
   * @param aSheet the name of a sheet.
   * @return a reference to all the cells in the sheet.
   * @throws SpreadsheetException on error.
   */
  String getSheetRange(String aSheet) throws SpreadsheetException;

  /**
   * Get a sheet/cell reference to all the cells in the sheet.
   * @param aSheet the number of a sheet.
   * @return a reference to all the cells in the sheet.
   * @throws SpreadsheetException on error.
   */
  String getSheetRange(int aSheet) throws SpreadsheetException;

  /**
   * Convert a sheet number, row number and cell number into a cell reference.
   * @param aSheet a sheet number starting at zero. The sheet must exist in the current spreadsheet.
   * @param aRow a row number starting at zero.
   * @param aCol a column number starting at zero (A).
   * @return the cell reference as a string.
   * @throws SpreadsheetException on error.
   */
  String getCellRef(int aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Convert a sheet name, row number and cell number into a cell reference. The sheet and cell do not need to
   * exist in the current spreadsheet.
   * @param aSheet a sheet name.
   * @param aRow a row number starting at zero.
   * @param aCol a column number starting at zero (A).
   * @return the cell reference as a string.
   * @throws SpreadsheetException on error.
   */
  String getCellRef(String aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Convert a sheet number and range into a cell reference.
   * @param aSheet a sheet number starting at zero. The sheet must exist in the current spreadsheet.
   * @param aRow1 a row number starting at zero, representing the top-left starting row of the range.
   * @param aCol1 a column number starting at zero (A), representing the top-left starting column of the range.
   * @param aRow2 a row number starting at zero, representing the bottom-right ending row of the range.
   * @param aCol2 a column number starting at zero (A), representing the bottom-right ending column of the range.
   * @return the cell reference as a string.
   * @throws SpreadsheetException on error.
   */
  String getCellRef(int aSheet, int aRow1, int aCol1, int aRow2, int aCol2) throws SpreadsheetException;

  /**
   * Convert a sheet name and range into a cell reference. The sheet and cells do not need to
   * exist in the current spreadsheet.
   * @param aSheet a sheet name.
   * @param aRow1 a row number starting at zero, representing the top-left starting row of the range.
   * @param aCol1 a column number starting at zero (A), representing the top-left starting column of the range.
   * @param aRow2 a row number starting at zero, representing the bottom-right ending row of the range.
   * @param aCol2 a column number starting at zero (A), representing the bottom-right ending column of the range.
   * @return the cell reference as a string.
   * @throws SpreadsheetException on error.
   */
  String getCellRef(String aSheet, int aRow1, int aCol1, int aRow2, int aCol2) throws SpreadsheetException;

  /**
   * Convert a row number and cell number into a cell reference. The cell does not need to exist in the current
   * spreadsheet.
   * @param aRow a row number starting at zero.
   * @param aCol a column number starting at zero (A).
   * @return the cell reference as a string.
   * @throws SpreadsheetException on error.
   */
  String getCellRef(int aRow, int aCol) throws SpreadsheetException;

  /**
   * Set the numeric value of the cell to the specified number.
   * @param aRef a sheet/cell reference.
   * @param aNumericValue the value to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setNumber(String aRef, double aNumericValue) throws SpreadsheetException;  
  
  /**
   * Set the numeric value of the cell to the specified number.
   * @param aSheet the name of the sheet to set the value in.
   * @param aRow the row of the cell to set the value in.
   * @param aCol the column of the cell to set the value in.
   * @param aNumericValue the value to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setNumber(String aSheet, int aRow, int aCol, double aNumericValue) throws SpreadsheetException;

  /**
   * Set the numeric value of the cell to the specified number.
   * @param aSheet the number of the sheet to set the value in.
   * @param aRow the row of the cell to set the value in.
   * @param aCol the column of the cell to set the value in.
   * @param aNumericValue the value to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setNumber(int aSheet, int aRow, int aCol, double aNumericValue) throws SpreadsheetException;

  /**
   * Set the text value of the cell to the specified string.
   * @param aRef a sheet/cell reference.
   * @param anEntry the entry to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setEntry(String aRef, String anEntry) throws SpreadsheetException;  
  
  /**
   * Set the text value of the cell to the specified string.
   * @param aSheet the name of the sheet to set the value in.
   * @param aRow the row of the cell to set the value in.
   * @param aCol the column of the cell to set the value in.
   * @param anEntry the entry to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setEntry(String aSheet, int aRow, int aCol, String anEntry) throws SpreadsheetException;

  /**
   * Set the text value of the cell to the specified string.
   * @param aSheet the number of the sheet to set the value in.
   * @param aRow the row of the cell to set the value in.
   * @param aCol the column of the cell to set the value in.
   * @param anEntry the entry to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setEntry(int aSheet, int aRow, int aCol, String anEntry) throws SpreadsheetException;

  /**
   * Set the text value of the cell to the specified string.
   * @param aRef a sheet/cell reference.
   * @param aTextValue the value to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setText(String aRef, String aTextValue) throws SpreadsheetException;  
  
  /**
   * Set the text value of the cell to the specified string.
   * @param aSheet the name of the sheet to set the value in.
   * @param aRow the row of the cell to set the value in.
   * @param aCol the column of the cell to set the value in.
   * @param aTextValue the value to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setText(String aSheet, int aRow, int aCol, String aTextValue) throws SpreadsheetException;

  /**
   * Set the text value of the cell to the specified string.
   * @param aSheet the number of the sheet to set the value in.
   * @param aRow the row of the cell to set the value in.
   * @param aCol the column of the cell to set the value in.
   * @param aTextValue the value to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setText(int aSheet, int aRow, int aCol, String aTextValue) throws SpreadsheetException;

  /**
   * Set the formula of the cell to the specified formula.
   * @param aRef a sheet/cell reference.
   * @param aFormula the formula to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setFormula(String aRef, String aFormula) throws SpreadsheetException;  
  
  /**
   * Set the formula of the cell to the specified formula.
   * @param aSheet the name of the sheet to set the value in.
   * @param aRow the row of the cell to set the value in.
   * @param aCol the column of the cell to set the value in.
   * @param aFormula the formula to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setFormula(String aSheet, int aRow, int aCol, String aFormula) throws SpreadsheetException;

  /**
   * Set the formula of the cell to the specified formula.
   * @param aSheet the number of the sheet to set the value in.
   * @param aRow the row of the cell to set the value in.
   * @param aCol the column of the cell to set the value in.
   * @param aFormula the formula to set in the cell.
   * @throws SpreadsheetException on error.
   */
  void setFormula(int aSheet, int aRow, int aCol, String aFormula) throws SpreadsheetException;

  /**
   * Get the numeric value of the cell.
   * @param aRef a sheet/cell reference.
   * @return the numeric value of the cell.
   * @throws SpreadsheetException on error.
   */
  double getNumber(String aRef) throws SpreadsheetException;  
  
  /**
   * Get the numeric value of the cell.
   * @param aSheet the name of the sheet to get the value from.
   * @param aRow the row of the cell to get the value from.
   * @param aCol the column of the cell to get the value from.
   * @return the numeric value of the cell.
   * @throws SpreadsheetException on error.
   */
  double getNumber(String aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get the numeric value of the cell.
   * @param aSheet the name of the sheet to get the value from.
   * @param aRow the row of the cell to get the value from.
   * @param aCol the column of the cell to get the value from.
   * @return the numeric value of the cell.
   * @throws SpreadsheetException on error.
   */
  double getNumber(int aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Set the format of the cell.
   * @param aRef a sheet/cell reference.
   * @param aFormat the new format of the cell.
   * @throws SpreadsheetException on error.
   */
  void setFormat(String aRef, String aFormat) throws SpreadsheetException;  
  
  /**
   * Set the format of the cell.
   * @param aSheet the name of the sheet to get the format from.
   * @param aRow the row of the cell to get the format from.
   * @param aCol the column of the cell to get the format from.
   * @param aFormat the new format of the cell.
   * @throws SpreadsheetException on error.
   */
  void setFormat(String aSheet, int aRow, int aCol, String aFormat) throws SpreadsheetException;

  /**
   * Set the format of the cell.
   * @param aSheet the name of the sheet to get the format from.
   * @param aRow the row of the cell to get the format from.
   * @param aCol the column of the cell to get the format from.
   * @param aFormat the new format of the cell.
   * @throws SpreadsheetException on error.
   */
  void setFormat(int aSheet, int aRow, int aCol, String aFormat) throws SpreadsheetException;

  /**
   * Get the format of the cell.
   * @param aRef a sheet/cell reference.
   * @return the format of the cell.
   * @throws SpreadsheetException on error.
   */
  String getFormat(String aRef) throws SpreadsheetException;  
  
  /**
   * Get the format of the cell.
   * @param aSheet the name of the sheet to get the format from.
   * @param aRow the row of the cell to get the format from.
   * @param aCol the column of the cell to get the format from.
   * @return the format of the cell.
   * @throws SpreadsheetException on error.
   */
  String getFormat(String aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get the format of the cell.
   * @param aSheet the name of the sheet to get the format from.
   * @param aRow the row of the cell to get the format from.
   * @param aCol the column of the cell to get the format from.
   * @return the format of the cell.
   * @throws SpreadsheetException on error.
   */
  String getFormat(int aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get the type of the cell.
   * @param aRef a sheet/cell reference.
   * @return the type of the cell.
   * @throws SpreadsheetException on error.
   */
  CELLTYPE getType(String aRef) throws SpreadsheetException;
  
  /**
   * Get the type of the cell.
   * @param aSheet the name of the sheet to get the type from.
   * @param aRow the row of the cell to get the type from.
   * @param aCol the column of the cell to get the type from.
   * @return the type of the cell.
   * @throws SpreadsheetException on error.
   */
  CELLTYPE getType(String aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get the type of the cell.
   * @param aSheet the name of the sheet to get the type from.
   * @param aRow the row of the cell to get the type from.
   * @param aCol the column of the cell to get the type from.
   * @return the type of the cell.
   * @throws SpreadsheetException on error.
   */
  CELLTYPE getType(int aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get the text value from the cell.
   * @param aRef a sheet/cell reference.
   * @return the text value of the cell.
   * @throws SpreadsheetException on error.
   */
  String getText(String aRef) throws SpreadsheetException;  
  
  /**
   * Get the text value from the cell.
   * @param aSheet the name of the sheet to get the value from.
   * @param aRow the row of the cell to get the value from.
   * @param aCol the column of the cell to get the value from.
   * @return the text value of the cell.
   * @throws SpreadsheetException on error.
   */
  String getText(String aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get the text value from the cell.
   * @param aSheet the name of the sheet to get the value from.
   * @param aRow the row of the cell to get the value from.
   * @param aCol the column of the cell to get the value from.
   * @return the text value of the cell.
   * @throws SpreadsheetException on error.
   */
  String getText(int aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get the formula from the cell.
   * @param aRef a sheet/cell reference.
   * @return the formula of the cell.
   * @throws SpreadsheetException on error.
   */
  String getFormula(String aRef) throws SpreadsheetException;  
  
  /**
   * Get the formula from the cell.
   * @param aSheet the name of the sheet to get the value from.
   * @param aRow the row of the cell to get the value from.
   * @param aCol the column of the cell to get the value from.
   * @return the formula of the cell.
   * @throws SpreadsheetException on error.
   */
  String getFormula(String aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get the formula from the cell.
   * @param aSheet the name of the sheet to get the value from.
   * @param aRow the row of the cell to get the value from.
   * @param aCol the column of the cell to get the value from.
   * @return the formula of the cell.
   * @throws SpreadsheetException on error.
   */
  String getFormula(int aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get the formatted text value from the cell.
   * @param aRef a sheet/cell reference.
   * @return the formatted text value of the cell.
   * @throws SpreadsheetException on error.
   */
  String getFormattedText(String aRef) throws SpreadsheetException;  
  
  /**
   * Get the formatted text value from the cell.
   * @param aSheet the name of the sheet to get the value from.
   * @param aRow the row of the cell to get the value from.
   * @param aCol the column of the cell to get the value from.
   * @return the formatted text value of the cell.
   * @throws SpreadsheetException on error.
   */
  String getFormattedText(String aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get the formatted text value from the cell.
   * @param aSheet the name of the sheet to get the value from.
   * @param aRow the row of the cell to get the value from.
   * @param aCol the column of the cell to get the value from.
   * @return the formatted text value of the cell.
   * @throws SpreadsheetException on error.
   */
  String getFormattedText(int aSheet, int aRow, int aCol) throws SpreadsheetException;

  /**
   * Get a range object from a string reference.
   * @param aRef a string reference.
   * @return Range object representing the cell areas
   * @throws SpreadsheetException on error.
   */
  Range getRange(String aRef) throws SpreadsheetException;
  
  /**
   * Get a range object from a string reference.
   * @param aRef a string reference.
   * @param isCanAdjust true if the returned range can be adjusted to fit in the bounds of the worksheet,
   * false if the returned range should just represent the reference supplied.
   * @return Range object representing the cell areas
   * @throws SpreadsheetException on error.
   */
  Range getRange(String aRef, boolean isCanAdjust) throws SpreadsheetException;

  /**
   * Get the last row in the sheet.
   * @param aSheet the sheet name.
   * @return the last row in the sheet.
   * @throws SpreadsheetException on error.
   */
  int getLastRow(String aSheet) throws SpreadsheetException;
  
  /**
   * Get the last row in the sheet.
   * @param aSheet the sheet number.
   * @return the last row in the sheet.
   * @throws SpreadsheetException on error.
   */
  int getLastRow(int aSheet) throws SpreadsheetException;

  /**
   * Get the last column in the sheet.
   * @param aSheet the sheet name.
   * @return the last column in the sheet.
   * @throws SpreadsheetException on error.
   */
  int getLastCol(String aSheet) throws SpreadsheetException;  
  
  /**
   * Get the last column in the sheet.
   * @param aSheet the sheet number.
   * @return the last column in the sheet.
   * @throws SpreadsheetException on error.
   */
  int getLastCol(int aSheet) throws SpreadsheetException;
  
  /**
   * Get the column width.
   * @param aSheet the sheet name.
   * @param aColumn the column number.
   * @return the column width.
   * @throws SpreadsheetException on error. 
   */
  int getColumnWidth(String aSheet, int aColumn) throws SpreadsheetException;

  /**
   * Get the column width.
   * @param aSheet the sheet number.
   * @param aColumn the column number.
   * @return the column width.
   * @throws SpreadsheetException on error. 
   */
  int getColumnWidth(int aSheet, int aColumn) throws SpreadsheetException;

  /**
   * Set the column width.
   * @param aSheet the sheet name.
   * @param aColumn the column number.
   * @param aWidth the new width to set.
   * @throws SpreadsheetException on error. 
   */
  void setColumnWidth(String aSheet, int aColumn, int aWidth) throws SpreadsheetException;

  /**
   * Set the column width.
   * @param aSheet the sheet number.
   * @param aColumn the column number.
   * @param aWidth the new width to set.
   * @throws SpreadsheetException on error. 
   */
  void setColumnWidth(int aSheet, int aColumn, int aWidth) throws SpreadsheetException;

  /**
   * Determine if the column is hidden.
   * @param aSheet the sheet name.
   * @param aColumn the column number.
   * @return true if the column is hidden.
   * @throws SpreadsheetException on error.
   */
  boolean isColumnHidden(String aSheet, int aColumn) throws SpreadsheetException;

  /**
   * Determine if the column is hidden.
   * @param aSheet the sheet number.
   * @param aColumn the column number.
   * @return true if the column is hidden.
   * @throws SpreadsheetException on error.
   */
  boolean isColumnHidden(int aSheet, int aColumn) throws SpreadsheetException;

  /**
   * Set whether the column is hidden or not.
   * @param aSheet the sheet name.
   * @param aColumn the column number.
   * @param isHidden true if the column should be hidden.
   * @throws SpreadsheetException on error.
   */
  void setColumnHidden(String aSheet, int aColumn, boolean isHidden) throws SpreadsheetException;
  
  /**
   * Set whether the column is hidden or not.
   * @param aSheet the sheet number.
   * @param aColumn the column number.
   * @param isHidden true if the column should be hidden.
   * @throws SpreadsheetException on error.
   */
  void setColumnHidden(int aSheet, int aColumn, boolean isHidden) throws SpreadsheetException;

 /**
   * Get the row height.
   * @param aSheet the sheet name.
   * @param aRow the row number.
   * @return the row height.
   * @throws SpreadsheetException on error. 
   */
  int getRowHeight(String aSheet, int aRow) throws SpreadsheetException;

  /**
   * Get the row height.
   * @param aSheet the sheet number.
   * @param aRow the row number.
   * @return the row height.
   * @throws SpreadsheetException on error. 
   */
  int getRowHeight(int aSheet, int aRow) throws SpreadsheetException;

  /**
   * Set the row height.
   * @param aSheet the sheet name.
   * @param aRow the row number.
   * @param aHeight the new height to set.
   * @throws SpreadsheetException on error. 
   */
  void setRowHeight(String aSheet, int aRow, int aHeight) throws SpreadsheetException;

  /**
   * Set the row height.
   * @param aSheet the sheet number.
   * @param aRow the row number.
   * @param aHeight the new height to set.
   * @throws SpreadsheetException on error. 
   */
  void setRowHeight(int aSheet, int aRow, int aHeight) throws SpreadsheetException;

  /**
   * Determine if the row is hidden.
   * @param aSheet the sheet name.
   * @param aRow the row number.
   * @return true if the row is hidden.
   * @throws SpreadsheetException on error.
   */
  boolean isRowHidden(String aSheet, int aRow) throws SpreadsheetException;

  /**
   * Determine if the row is hidden.
   * @param aSheet the sheet number.
   * @param aRow the row number.
   * @return true if the row is hidden.
   * @throws SpreadsheetException on error.
   */
  boolean isRowHidden(int aSheet, int aRow) throws SpreadsheetException;

  /**
   * Set whether the row is hidden or not.
   * @param aSheet the sheet name.
   * @param aRow the row number.
   * @param isHidden true if the row should be hidden.
   * @throws SpreadsheetException on error.
   */
  void setRowHidden(String aSheet, int aRow, boolean isHidden) throws SpreadsheetException;
  
  /**
   * Set whether the row is hidden or not.
   * @param aSheet the sheet number.
   * @param aRow the row number.
   * @param isHidden true if the row should be hidden.
   * @throws SpreadsheetException on error.
   */
  void setRowHidden(int aSheet, int aRow, boolean isHidden) throws SpreadsheetException;
}
