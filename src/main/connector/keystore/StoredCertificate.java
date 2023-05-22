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
import java.security.cert.Certificate;

/**
 * This class represents a certificate stored in a keystore.
 * 
 * @author EnergySys Limited
 * @version $Revision$
 */
public class StoredCertificate implements IIdentifiable
{
  private Integer theId;
  
  private String theName;
  
  private Certificate theCertificate;

  /**
   * Basic constructor.
   * @param anId an id
   * @param aName a name
   * @param aCertificate a certificate 
   */
  public StoredCertificate(Integer anId, String aName, Certificate aCertificate)
  {
    this.theId = anId;
    this.theName = aName;
    this.theCertificate = aCertificate;
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

  public Certificate getCertificate()
  {
    return theCertificate;
  }

  public void setCertificate(Certificate aCertificate)
  {
    this.theCertificate = aCertificate;
  }



}

