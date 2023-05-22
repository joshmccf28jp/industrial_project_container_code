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
package com.energysys.connector.web.validator;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 *
 * Validator for Dates that are actually stored as Strings in their backing bean.
 * Useful when representing a LocalDateTime, i.e. a date with no associated timezone or offset.
 * @author EnergySys Limited
 */
@FacesValidator(value = "DateAsStringValidator")
public class DateAsStringValidator implements Validator
{

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /**
   * Default Constructor.
   */
  public DateAsStringValidator()
  {
  }

  
  @Override
  public void validate(FacesContext aContext, UIComponent aComponent, Object aValue) throws ValidatorException
  {
    try
    {
      LocalDate.parse(aValue.toString(), DATE_FORMAT);
    }
    catch (DateTimeParseException ex)
    {
      throw new ValidatorException(new FacesMessage(
              aComponent.getId() + ": Validation Error: Date must be in format dd/MM/yyyy"));
    }
  }
}
