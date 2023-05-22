/*
 * Copyright 2019 EnergySys Limited. All Rights Reserved.
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
package com.energysys.connector.web.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

/**
 * Security Filter implements required HTTP headers being passed back to connector interface.
 *
 * @author 'EnergySys Limited'
 */
public class SecurityFilter implements Filter
{

  private static final boolean DEBUG = true;
  private static final Logger LOG = Logger.getLogger(SecurityFilter.class.getName());
  private FilterConfig theFilterConfig = null;

  /**
   * Default Constructor.
   */
  public SecurityFilter()
  {
  }

  /**
   * Sets filter that appends required HTTP Headers.
   *
   * @param aRequest The servlet request we are processing
   * @param aResponse The servlet response we are creating
   * @param aChain The filter chain we are processing
   *
   * @exception IOException if an input/output error occurs
   * @exception ServletException if a servlet error occurs
   *
   *
   *
   */
  @Override
  public void doFilter(ServletRequest aRequest, ServletResponse aResponse, FilterChain aChain)
          throws IOException, ServletException
  {

    if (DEBUG)
    {
      LOG.finer("SecurityFilter:doFilter()");
    }

    //Add security headers
    HttpServletResponse myHttpResponse = (HttpServletResponse) aResponse;
    myHttpResponse.setHeader("X-Content-Type-Options", "nosniff");
    myHttpResponse.setHeader("X-Frame-Options", "sameorigin");
    //Strict Security time set to 1 day
    myHttpResponse.setHeader("Strict-Transport-Security", "max-age=86400; includeSubDomains;");
    myHttpResponse.setHeader("Cache-Control", "no-cache");
    myHttpResponse.setHeader("Pragma", "no-cache");
    myHttpResponse.setHeader("Server", "EnergySys Connector");

    aChain.doFilter(aRequest, myHttpResponse);
  }

  /**
   * Return the filter configuration object for this filter.
   * @return FilterConfig
   */
  public FilterConfig getFilterConfig()
  {
    return (this.theFilterConfig);
  }

  /**
   * Set the filter configuration object for this filter.
   *
   * @param aFilterConfig The filter configuration object
   */
  public void setFilterConfig(FilterConfig aFilterConfig)
  {
    this.theFilterConfig = aFilterConfig;
  }

  /**
   * Destroy method for this filter.
   */
  @Override
  public void destroy()
  {
  }

  /**
   * Init method for this filter.
   * @param aFilterConfig the initialising filter config
   */
  @Override
  public void init(FilterConfig aFilterConfig)
  {
    this.theFilterConfig = aFilterConfig;
    if (aFilterConfig != null)
    {
      if (DEBUG)
      {
        LOG.finer("SecurityFilter:Initializing filter");
      }
    }
  }

}
