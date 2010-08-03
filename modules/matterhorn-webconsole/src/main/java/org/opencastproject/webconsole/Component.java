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
package org.opencastproject.webconsole;

import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

/**
 * Registers the web management console
 */
public class Component {
  private static final Logger logger = LoggerFactory.getLogger(Component.class);

  protected ComponentContext componentContext;
  protected HttpService httpService;
  protected HttpContext httpContext;
  protected OsgiManager manager;

  @SuppressWarnings("unchecked")
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
    manager = new WebConsole(componentContext.getBundleContext());
    try {
      httpService.registerServlet("/system/console", manager, new Hashtable(), httpContext);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public void deactivate() {
    try {
      httpService.unregister("/system/console");
      manager.destroy();
    } catch (Exception e) {
      logger.warn("Deactivation problem: {}", e.getMessage());
    }

  }

  public void setHttpService(HttpService service) {
    this.httpService = service;
  }

  public void setHttpContext(HttpContext httpContext) {
    this.httpContext = httpContext;
  }

}
