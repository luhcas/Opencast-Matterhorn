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
package org.opencastproject.authentication.api;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class Activator implements BundleActivator {
  private static final Logger logger = LoggerFactory.getLogger(Activator.class);
  ServiceRegistration reg = null;
  
  @SuppressWarnings("unchecked")
  @Override
  public void start(BundleContext context) throws Exception {
    String[] urls = new String[] {"/*", "*", "/", "/info.json", "/workflow/rest"};
    logger.info("registering authentication filter for {} urls: {}", urls.length, urls);
    Dictionary props = new Hashtable();
    props.put("urlPatterns", urls);
    props.put("filter-name", "Authentication Filter");
    reg = context.registerService(Filter.class.getName(), new AuthenticationFilter(), props);
  }
  @Override
  public void stop(BundleContext context) throws Exception {
    if(reg != null) {
      logger.info("unregistering authentication filter");
      reg.unregister();
    }
  }
  
  class AuthenticationFilter implements Filter {
    @Override
    public void init(FilterConfig config) throws ServletException {logger.info("init() {}", config.getServletContext().getContextPath());}
    @Override
    public void destroy() {logger.info("destroy()");}
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
      logger.info("Request={}", request);
      chain.doFilter(request, response);
    }
  }
}
