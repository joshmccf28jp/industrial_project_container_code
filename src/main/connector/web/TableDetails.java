/*
 * Copyright 2017 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.web;

/**
 * UI bean for showing tables available for import/export and their current row count.
 * @author EnergySys Limited
 */
public class TableDetails implements IIdentifiable
{
  private Integer theId;
  private final String theName;
  private final Class theType;
  private Long theRowCount;

  /**
   * Constructor taking all fields.
   * @param anId the (fake) id
   * @param aName table name
   * @param aType table class type
   * @param aRowCount current row count
   */
  public TableDetails(Integer anId, String aName, Class aType, Long aRowCount)
  {
    this.theId = anId;
    this.theName = aName;
    this.theType = aType;
    this.theRowCount = aRowCount;
  }

  public Class getType()
  {
    return theType;
  }

  public Long getRowCount()
  {
    return theRowCount;
  }
  
  public void setRowCount(Long aRowCount)
  {
    this.theRowCount = aRowCount;
  }
  
  @Override
  public Integer getId()
  {
    return theId;
  }

  @Override
  public String getName()
  {
    return theName;
  }

  @Override
  public void setId(Integer anId)
  {
    this.theId = anId;
  }
  
}
