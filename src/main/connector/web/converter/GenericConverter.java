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

import com.energysys.connector.web.IIdentifiable;

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItems;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Generic converter class for UI lists that uses the IIdentifiable interface.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@FacesConverter(value = "GenericConverter")
public class GenericConverter implements Converter
{
  /**
   * Default Constructor.
   */
  public GenericConverter()
  {
  }

  @Override
  public Object getAsObject(FacesContext aContext, UIComponent aComponent, String aVal)
  {
    if (aVal == null)
    {
      return null;
    }
    else
    {
      for (Object myItem : getSelectItems(aComponent))
      {
        if (aVal.equals(getAsString(aContext, aComponent, myItem)))
        {
          return myItem;
        }
      }
      return null;
    }
  }

  @Override
  public String getAsString(FacesContext aFacesContext, UIComponent aUIComponent, Object anObject)
  {
    return Integer.toString(((IIdentifiable)anObject).getId());
  }

  
  private Collection<Object> getSelectItems(UIComponent aComponent)
  {
    Collection<Object> myCollection = new ArrayList<>();

    for (UIComponent myChild : aComponent.getChildren())
    {
      if (myChild instanceof UISelectItems)
      {
        UISelectItems myItem = (UISelectItems) myChild;
        Object myValue = myItem.getValue();
        myCollection.addAll((Collection<Object>) myValue);
      }
    }

    return myCollection;
  }
}
