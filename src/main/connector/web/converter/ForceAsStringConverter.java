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
package com.energysys.connector.web.converter;


import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 * For use by data beans that store dates as String values. This is good for storing "dates" that are not actual
 * instants in time but just dates for use in queries or when the object is not concerned about time zones etc.
 *
 * @author EnergySys Limited
 */
@FacesConverter(value = "ForceAsStringConverter")
public class ForceAsStringConverter implements Converter
{

  /**
   * Default Constructor.
   */
  public ForceAsStringConverter()
  {
  }

  @Override
  public Object getAsObject(FacesContext aContext, UIComponent aComponent, String aVal)
  {
    return aVal;
  }

  @Override
  public String getAsString(FacesContext aContext, UIComponent aComponent, Object aVal)
  {
    if (aVal == null)
    {
      return null;
    }
    else
    {
      return aVal.toString();
    }
  }

}
