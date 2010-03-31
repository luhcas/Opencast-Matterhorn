/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.http;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class SecureContext implements HttpContext {
  private static final Logger logger = LoggerFactory.getLogger(SecureContext.class);
  private HttpContext delegate;
  private BundleContext bundleContext;
  
  public SecureContext(HttpContext delegate, BundleContext bundleContext) {
    this.delegate = delegate;
    this.bundleContext = bundleContext;
  }
  
  /**
   * {@inheritDoc}
   * @see org.osgi.service.http.HttpContext#getMimeType(java.lang.String)
   */
  @Override
  public String getMimeType(String name) {
    return delegate.getMimeType(name);
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.service.http.HttpContext#getResource(java.lang.String)
   */
  @Override
  public URL getResource(String name) {
    return delegate.getResource(name);
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  @Override
  public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ServiceReference[] refs;
    try {
      refs = bundleContext.getServiceReferences(Filter.class.getName(), "(org.opencastproject.filter=true)");
    } catch (InvalidSyntaxException e) {
      logger.warn(e.getMessage(), e);
      return false;
    }
    if (refs == null || refs.length == 0) {
//      logger.warn("Requests are not permitted without a registered matterhorn security filter.");
//      return false;
      logger.warn("Authentication is currently disabled");
      return true;
    }
    Filter[] filters = new Filter[refs.length];
    for (int i = 0; i < refs.length; i++)
      filters[i] = (Filter) bundleContext.getService(refs[i]);
    try {
      new Chain(filters).doFilter(request, response);
      return !response.isCommitted();
    } catch (ServletException e) {
      logger.warn(e.getMessage(), e);
      return false;
    }
  }

  /**
   * A {@link FilterChain} composed of {@link Filter}s with the
   */
  class Chain implements FilterChain {
    int current = 0;
    Filter[] filters;
  
    Chain(Filter[] filters) {
      this.filters = filters;
    }
  
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
      if (current < filters.length && !response.isCommitted()) {
        Filter filter = filters[current++];
        logger.debug("doFilter() on " + filter);
        filter.doFilter(request, response, this);
      }
    }
  }

}
