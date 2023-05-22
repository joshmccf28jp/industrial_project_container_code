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
import java.util.Objects;
import java.util.logging.Logger;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

/**
 * User Details class used for user authentication.
 *
 * @author EnergySys Limited
 */
@Entity
@Table(name = "USERS")
@XmlRootElement
public class UserDetails implements IIdentifiable
{

  private static final Logger LOG = Logger.getLogger(UserDetails.class.getName());

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "username", unique = true)
  private String username;

  @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.DETACH)
  @Fetch(FetchMode.SELECT)
  @JoinTable(name = "USER_ROLES", joinColumns =
  {
    @JoinColumn(name = "USER_ID", referencedColumnName = "id")
  }, inverseJoinColumns =
  {
    @JoinColumn(name = "ROLE_ID", referencedColumnName = "id")
  })
  private UserRole role;

  @Column(name = "password")
  private String password;

  /**
   * Default Constructor.
   */
  public UserDetails()
  {
  }

  @Override
  public Integer getId()
  {
    return id;
  }

  @Override
  public void setId(Integer anId)
  {
    this.id = anId;
  }

  public String getUsername()
  {
    return username;
  }

  public void setUsername(String aUsername)
  {
    this.username = aUsername;
  }

  public UserRole getRole()
  {
    return role;
  }

  public void setRole(UserRole aRole)
  {
    this.role = aRole;
  }

  public String getPassword()
  {
    return password;
  }

  public void setPassword(String aPassword)
  {
    this.password = aPassword;
  }

  @Override
  public int hashCode()
  {
    int myHash = getId().hashCode();
    return myHash;
  }

  @Override
  public boolean equals(Object anObj)
  {
    if (this == anObj)
    {
      return true;
    }
    if (anObj == null)
    {
      return false;
    }
    if (getClass() != anObj.getClass())
    {
      return false;
    }
    final UserDetails myOther = (UserDetails) anObj;
    if (!Objects.equals(this.id, myOther.id))
    {
      return false;
    }
    return true;
  }

  @Override
  public String getName()
  {
    return username;
  }
}
