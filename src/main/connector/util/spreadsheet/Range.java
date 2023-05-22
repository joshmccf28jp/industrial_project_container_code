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

/**
 * Represents a range of cells in a spreadsheet.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
public class Range
{
  private int theSheet = -1;
  private int theStartRow = -1;
  private int theEndRow = -1;
  private int theStartCol = -1;
  private int theEndCol = -1;

  /**
   * Constructor of a range object.
   * @param aSheet the sheet number
   * @param aStartRow the top-left row number
   * @param aStartCol the top-left column number
   * @param anEndRow the bottom-right row number
   * @param aNumRows the number of rows in the worksheet - the created range becomes
   *  the minimum of this and the end row of the range
   * @param anEndCol the bottom-right column number
   * @param aNumCols the number of columns in the worksheet - the created range becomes
   *  the minimum of this and the end column of the range
   */
  public Range(int aSheet, int aStartRow, int aStartCol,
    int anEndRow, int aNumRows, int anEndCol, int aNumCols) throws SpreadsheetException
  {
    theSheet = aSheet;
    theStartRow = aStartRow;
    theEndRow = Math.min(anEndRow, aNumRows);
    theStartCol = aStartCol;
    theEndCol = Math.min(anEndCol, aNumCols);
  }  
  
  /**
   * Constructor of a range object.
   * @param aSheet the sheet number
   * @param aStartRow the top-left row number
   * @param aStartCol the top-left column number
   * @param anEndRow the bottom-right row number
   * @param anEndCol the bottom-right column number
   */
  public Range(int aSheet, int aStartRow, int aStartCol, int anEndRow, int anEndCol)
  {
    theSheet = aSheet;
    theStartRow = aStartRow;
    theEndRow = anEndRow;
    theStartCol = aStartCol;
    theEndCol = anEndCol;
  }
  
  public int getSheet()
  {
    return theSheet;
  }

  public int getStartRow()
  {
    return theStartRow;
  }

  public int getEndRow()
  {
    return theEndRow;
  }

  public int getStartCol()
  {
    return theStartCol;
  }

  public int getEndCol()
  {
    return theEndCol;
  }
}
