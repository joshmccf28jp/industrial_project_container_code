/*
 * Copyright 2015 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.connectors.dataconnector.odata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

/**
 * Encapsulates a tag list.
 *
 * @author EnergySys Limited
 * @version $Revision$
 * Last modified by: $Author$
 */
public class TagList 
{
  private static final Logger LOG = Log.getLogger(TagList.class.getName());
  private final String theTagListName;
  private Map<String, String> theTagList;
  
  /**
   * Constructor.
   * @param aTagListName name
   * @param aTagList tag name/ID pairs 
   */
  public TagList(String aTagListName, Map<String, String> aTagList)
  {
    theTagListName = aTagListName;
    theTagList = aTagList;
  }
  
  /**
   * Constructor.
   * @param aTagListName name
   */
  public TagList(String aTagListName)
  {
    theTagListName = aTagListName;
  }
  

  /**
   * Get the tag list name.
   * @return name
   */
  public String getName() 
  {
    return theTagListName;
  }

  /**
   * Get the tag names.
   * @return List of tag names.
   */
  public List<String> getTagNames() 
  {
    List<String> myNames = new ArrayList<>();
    Iterator myIterator = theTagList.keySet().iterator();
    while (myIterator.hasNext())
    {
      myNames.add((String) myIterator.next());
    }
    return myNames;
  }
  
  /**
   * Get tag IDs.
   * @return IDs
   */
  public List<String> getTagIDs() 
  {
    List<String> myIDs = new ArrayList<>(theTagList.values());
    return myIDs;
  }
  
  /**
   * Set the tag id value if the tag already exists in the list. Otherwise do nothing.
   * @param aTagName name
   * @param aTagId id
   */
  public void setTagId(String aTagName, String aTagId)
  {
    if (theTagList.containsKey(aTagName) && aTagId != null && !aTagId.isEmpty())
    {
      StringBuilder myLogString = new StringBuilder();
      myLogString.append("Populating TagListEntry with values ('");
      myLogString.append(aTagName).append("','").append(aTagId).append("')");
      LOG.info(myLogString.toString());
      theTagList.put(aTagName, aTagId);
    }
  }

  /**
   * Gets the tag id given a tag name.
   * @param aTagName
   * @return the tag name
   */
  public String getTagId(String aTagName)
  {
    return theTagList.get(aTagName);
  }
  
  /**
   * Returns a displayable message indicating which tags do not have corresponding IDs.
   * @return message
   */
  public String getTagIDValidationMessage()
  {
    StringBuilder myValidationMessage = new StringBuilder();
    
    Iterator<String> myIterator = theTagList.keySet().iterator();
    while (myIterator.hasNext())
    {
      String myTagName = myIterator.next();
      if (theTagList.get(myTagName) == null || theTagList.get(myTagName).isEmpty())
      {
        myValidationMessage.append("The tag <" + myTagName + "> in the tag list <" 
                + theTagListName + "> does not have a defined ID.\n");
      }
    }
    
    return myValidationMessage.toString();
  }
  
  /**
   * Put the entry into the tag list.
   * @param aTagName the tag name
   */
  public void put(String aTagName)
  {
    if (theTagList == null)
    {
      theTagList = new HashMap<>();
    }
    theTagList.put(aTagName, null);
  }
  
}
