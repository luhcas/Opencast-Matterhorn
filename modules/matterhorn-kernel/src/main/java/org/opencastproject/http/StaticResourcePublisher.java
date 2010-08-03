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
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes {@link StaticResource}s with the http service.
 */
public class StaticResourcePublisher {
  private static final Logger logger = LoggerFactory.getLogger(StaticResourcePublisher.class);
  
  protected HttpService httpService;
  protected HttpContext httpContext;
  protected ComponentContext componentContext;
  protected ServiceTracker staticResourceTracker = null;

  public void activate(ComponentContext componentContext) {
    logger.info("activate()");
    this.componentContext = componentContext;
    
    // Register static resources with the http service
    staticResourceTracker = new StaticResourceTracker();
    staticResourceTracker.open();
  }

  public void deactivate(ComponentContext componentContext) {
    staticResourceTracker.close();
  }

  public void setHttpService(HttpService httpService) {
    this.httpService = httpService;
  }

  public void setHttpContext(HttpContext httpContext) {
    this.httpContext = httpContext;
  }

  class StaticResourceTracker extends ServiceTracker {
    BundleContext bundleContext;
    
    StaticResourceTracker() {
      super(componentContext.getBundleContext(), StaticResource.class.getName(), null);
      bundleContext = componentContext.getBundleContext();
    }
    
    @Override
    public Object addingService(ServiceReference reference) {
      StaticResource staticResource = (StaticResource)bundleContext.getService(reference);
      try {
        httpService.registerServlet(staticResource.alias, staticResource, null, httpContext);
        logger.info("Registered static resource  at {}", staticResource.alias);
      } catch(Exception e) {
        logger.warn("Can not register resources at alias {}, {}", staticResource.alias, e);
      }
      return super.addingService(reference);
    }
    @Override
    public void removedService(ServiceReference reference, Object service) {
      StaticResource staticResource = (StaticResource)bundleContext.getService(reference);
      try {
        httpService.unregister(staticResource.alias);
        logger.info("Unregistered static resource  at {}", staticResource.alias);
      } catch(Exception e) {
        logger.warn("Can not unregister resources at alias {}, {}", staticResource.alias, e);
      }
      super.removedService(reference, service);
    }
  }
}
