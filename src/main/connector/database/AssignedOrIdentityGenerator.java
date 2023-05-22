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
package com.energysys.connector.database;

import com.energysys.connector.exception.ConnectorSystemException;
import com.energysys.connector.web.IIdentifiable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import java.util.logging.Logger;

/**
 * A special Id Generator for hibernate that only generates Ids if they are not already set. This is used for the
 * Import/Export processes which need to be able to add objects to the database with their original Id's.
 *
 * @author EnergySys Limited
 */
public class AssignedOrIdentityGenerator extends SequenceStyleGenerator
{
  private static final Logger LOG = Logger.getLogger(SequenceStyleGenerator.class.getName());
  /**
   * Default Constructor.
   */
  public AssignedOrIdentityGenerator()
  {
  }

  @Override
  public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException
  {
    if (object instanceof IIdentifiable)
    {
      Integer myId = ((IIdentifiable) object).getId();
      if (myId != null && myId != -1)
      {
        // Don't change ID. This is an import and we need to preserve the id's (so foreign keys match)
        // However, we need to ensure the objects sequence is greater than the supplied id or next time an insert is
        // done on this object it will throw an exception.
        while (((Integer) super.generate(session, object)) < myId)
        {
          LOG.info("Increased sequence");
        }
        return myId;
      }
    }
    else
    {
      throw new ConnectorSystemException(
              "Class " + object.getClass().getSimpleName()
              + " uses AssignedOrIdentityGenerator but does not implement IIdentifiable");
    }

    return super.generate(session, object);
  }

}
