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

import com.energysys.AbstractBaseException;

/**
 * This class is responsible for supporting exceptions in the spreadsheet engine package.
 *
 * @author EnergySys Limited
 * @version $Revision$
 * Last modified by: $Author$
 */
public class SpreadsheetException extends AbstractBaseException
{

  /**
   * Creates a new SpreadsheetException with the specified error message.
   *
   * @param aReason The error message.
   */
  public SpreadsheetException(String aReason)
  {
    super(aReason);
  }

  /**
   * Creates a new SpreadsheetException with the specified error message and cause.
   * @param aReason The reason
   * @param aCause The cause
   */
  public SpreadsheetException(String aReason, Throwable aCause)
  {
    super(aReason, aCause);
  }

}

