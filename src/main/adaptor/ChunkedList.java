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

import java.util.ArrayList;
import java.util.List;

/**
 * Class that wraps a list of strings and "chunks" it up into limited sized lists.
 *
 * @author EnergySys Limited
 */
public class ChunkedList
{

  private final List<String> theList;
  private final int theMaxChunkSize;

  private int theCurrentIndex;

  /**
   * Constructor taking list of strings and max chunk size.
   *
   * @param aList list of strings
   * @param aMaxChunkSize max chunk size
   */
  public ChunkedList(List<String> aList, int aMaxChunkSize)
  {
    theList = aList;
    theMaxChunkSize = aMaxChunkSize;
    theCurrentIndex = 0;
  }

  /**
   * Returns if there are more chunks to get or not.
   *
   * @return if more chunks
   */
  public boolean hasMore()
  {
    return theList.size() > theCurrentIndex;
  }

  /**
   * Gets the next chunk of strings.
   *
   * @return list
   */
  public List<String> nextChunk()
  {
    // Get next chunk of tag list
    int myStartIndex = theCurrentIndex;
    int myEndIndex = myStartIndex + theMaxChunkSize;
    List<String> myTagNamesSublist = safeSubList(theList, myStartIndex, myEndIndex);
    theCurrentIndex = myEndIndex;

    return myTagNamesSublist;
  }

  private <T> List<T> safeSubList(List<T> aList, int aFromIndex, int aToIndex)
  {
    List<T> myReturnList = new ArrayList<>();

    //Fail fast if any of the following conditions are met
    if (aList == null || aFromIndex >= aList.size() || aToIndex <= 0 || aFromIndex >= aToIndex)
    {
      return myReturnList;
    }

    int mySize = aList.size();
    aFromIndex = Math.max(0, aFromIndex);
    aToIndex = Math.min(mySize, aToIndex);
    myReturnList = aList.subList(aFromIndex, aToIndex);

    return myReturnList;
  }

}
