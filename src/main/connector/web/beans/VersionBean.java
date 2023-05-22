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
package com.energysys.connector.web.beans;

import com.energysys.connector.web.utils.Version;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import java.util.logging.Logger;

/**
 * JSF Backing bean for manage users screen.
 *
 * @author EnergySys Limited
 * @version $Revision$
 */
@ManagedBean(name = "VersionBean")
@ApplicationScoped
public class VersionBean
{

  private static final Logger LOG = Logger.getLogger(VersionBean.class.getName());

  /**
   * Default Constructor.
   */
  public VersionBean()
  {
  }

  public String getVersionLabel()
  {
    return Version.getVersionDescription();
  }

}
