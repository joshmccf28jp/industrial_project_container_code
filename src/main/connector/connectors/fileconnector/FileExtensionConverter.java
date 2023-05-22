/*
 * Copyright 2023 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.connectors.fileconnector;

import jakarta.persistence.AttributeConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Jakarta database attribute Converter class for converting between list of string and "|"
 * separated string.
 * Used by the SyncConfiguration class to store the list of file extensions.
 * @author EnergySys Limited
 */
public class FileExtensionConverter implements AttributeConverter<List<String>, String>
{
  /**
   * Default Constructor.
   */
  public FileExtensionConverter()
  {
  }

  @Override
  public String convertToDatabaseColumn(List<String> someFileExtensions)
  {
    if (someFileExtensions == null || someFileExtensions.isEmpty())
    {
      return "";
    }
    else
    {
      StringBuilder myBuff = new StringBuilder();
      for (Iterator<String> iter = someFileExtensions.iterator(); iter.hasNext();)
      {
        String myExtension = iter.next();
        myBuff.append(myExtension);
        if (iter.hasNext())
        {
          myBuff.append("|");
        }
      }
      return myBuff.toString();
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String theFileExtensions)
  {
    if (theFileExtensions == null || theFileExtensions.isEmpty())
    {
      return new ArrayList<>();
    }
    return Arrays.asList(theFileExtensions.split("[\\|]"));
  }
}
