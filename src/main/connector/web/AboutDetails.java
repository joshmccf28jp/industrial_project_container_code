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
 * UI bean for showing about details.
 * @author EnergySys Limited
 */
public class AboutDetails implements IIdentifiable
{
  private Integer id;

  private final String name;

  private String aboutProperty;

  /**
   * Constructor taking all fields.
   * @param anId the (fake) id
   * @param aName table name
   * @param anAboutProperty the about data property
   */
  public AboutDetails(Integer anId, String aName, String anAboutProperty) {
    this.id = anId;
    this.name = aName;
    this.aboutProperty = anAboutProperty;
  }

  @Override
  public Integer getId()
  {
    return this.id;
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public void setId(Integer anId)
  {
    this.id = anId;
  }

  public String getAboutProperty()
  {
    return aboutProperty;
  }

  public void setAboutProperty(String anAboutProperty) {
    this.aboutProperty = anAboutProperty;
  }
}
