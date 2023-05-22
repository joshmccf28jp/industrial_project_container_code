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

package com.energysys.connector.keystore;

import com.energysys.connector.web.IIdentifiable;
import com.energysys.connector.web.beans.CredentialsConfigurationBean;

/**
 * Class representing a set credentials stored in a keystore.
 * 
 * @author EnergySys Limited
 */
public class StoredCredentials implements IIdentifiable
{
  private Integer theId;
  private String theName;
  private String theCredentials;

  /**
   * Basic constructor.
   * @param anId an id
   * @param anAlias an alias
   * @param aCredentials some credentials
   */
  public StoredCredentials(Integer anId, String anAlias, String aCredentials)
  {
    this.theId = anId;
    this.theName = anAlias;
    this.theCredentials = aCredentials;
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

  public String getCredentials()
  {
    return theCredentials;
  }

  public void setCredentials(String aCredentials)
  {
    this.theCredentials = aCredentials;
  }

  public Boolean isEncrypted()
  {
    return theName.endsWith(".crd");
  }

  /**
   * Returns if these credentials were originally uploaded in encrypted format.
   * @return if true
   */
  public Boolean wasEncrypted()
  {
    return theCredentials.startsWith(CredentialsConfigurationBean.WAS_ENCRYPTED) || theName.contains("s3");
  }



}

