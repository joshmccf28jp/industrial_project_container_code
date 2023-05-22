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
package com.energysys.connector.util;

import com.energysys.calendar.CurrentDateTime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Expiring cache which removes items after a timeout. Also scans the cache
 * periodically for expired items that have not been accessed for a while.
 * 
 * @author EnergySys Limited
 * @version $Revision$
 * Last modified by: $Author$
 */
public class Cache
{
  private Map<String, CacheItem> theCache = new ConcurrentHashMap<String, CacheItem>();
  private long theItemTimeout = 0;
  private long theCacheCheckTimeout = 0;
  private long theCacheCheckExpiry = 0;

  /**
   * Constructor.
   * @param anItemTimeout the amount of time an item will
   * remain active in the cache before it is removed
   * @param aCacheCheckTimeout the amount of time before
   * all items in the cache are checked for expiry.
   */
  public Cache(long anItemTimeout, long aCacheCheckTimeout)
  {
    theItemTimeout = anItemTimeout;
    theCacheCheckTimeout = aCacheCheckTimeout;
  }  
  
  /**
   * Get an item from the cache.
   * @param aKey the key of the item.
   * @return the item.
   */
  public Object get(String aKey)
  {
    CacheItem myCacheItem = theCache.get(aKey);
    
    if (myCacheItem != null)
    {
      if (CurrentDateTime.getCurrentTimeInMillis() > myCacheItem.getExpiry())
      {
        theCache.remove(aKey);
        myCacheItem = null;
      }
    }

    checkCache();

    if (myCacheItem == null)
    {
      return null;
    }
    else
    {
      return myCacheItem.getItem();
    }
  }

  /**
   * Add an item to the cache.
   * @param aKey the key of the item.
   * @param anItem the object to add.
   * @return the object that was added.
   */
  public Object put(String aKey, Object anItem)
  {
    return theCache.put(aKey, new CacheItem(anItem, theItemTimeout));
  }

  private void checkCache()
  {
    if (CurrentDateTime.getCurrentTimeInMillis() > theCacheCheckExpiry)
    {
      theCacheCheckExpiry = CurrentDateTime.getCurrentTimeInMillis() + theCacheCheckTimeout;
      long myCurrentTime = CurrentDateTime.getCurrentTimeInMillis();
      for (String myKey : theCache.keySet())
      {
        if (myCurrentTime > theCache.get(myKey).getExpiry())
        {
          theCache.remove(myKey);
        }
      }
    }
  }

  /**
   * Clear the cache.
   */
  public void clear()
  {
    this.theCache.clear();
  }
}

class CacheItem
{

  private Object theItem = null;
  private long theExpiry = 0;

  CacheItem(Object anItem, long anItemTimeout)
  {
    theItem = anItem;
    theExpiry = CurrentDateTime.getCurrentTimeInMillis() + anItemTimeout;
  }

  public long getExpiry()
  {
    return theExpiry;
  }

  public Object getItem()
  {
    return theItem;
  }
}
