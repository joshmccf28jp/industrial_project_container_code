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
package com.energysys.connector.web.security;

import com.energysys.connector.web.IIdentifiable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Class representing a user role.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@Entity(name = "USER_ROLE")
@XmlRootElement
public class UserRole implements IIdentifiable
{

  private static final Logger LOG = Logger.getLogger(UserRole.class.getName());

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer theId;

  @Column(name = "name")
  private String theName;

  @Column(name = "role")
  private String theRole;

  /**
   * Default Constructor.
   */
  public UserRole()
  {
  }

  @Override
  public Integer getId()
  {
    return theId;
  }

  @Override
  public void setId(Integer anId)
  {
    this.theId = anId;
  }

  @Override
  public String getName()
  {
    return theName;
  }

  public void setName(String aName)
  {
    this.theName = aName;
  }

  public String getRole()
  {
    return theRole;
  }

  /**
   * Sets the role.
   *
   * @param aRole role
   */
  public void setRole(String aRole)
  {
    this.theRole = aRole;
  }

  @Override
  public int hashCode()
  {
    int myHash = getId().hashCode();
    return myHash;
  }

  @Override
  public boolean equals(Object anObject)
  {
    if (this == anObject)
    {
      return true;
    }
    if (anObject == null)
    {
      return false;
    }
    if (getClass() != anObject.getClass())
    {
      return false;
    }
    final UserRole myOther = (UserRole) anObject;
    if (!Objects.equals(this.theId, myOther.theId))
    {
      return false;
    }
    return true;
  }

}
