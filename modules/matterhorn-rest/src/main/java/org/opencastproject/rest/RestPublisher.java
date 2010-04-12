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
package org.opencastproject.rest;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.GenericServlet;
import javax.ws.rs.Path;

/**
 * Listens for JAX-RS annotated services and publishes them to the global URL space using a single shared HttpContext.
 */
public class RestPublisher {
  private static final Logger logger = LoggerFactory.getLogger(RestPublisher.class);
  public static final String SERVICE_PROPERTY = "opencast.rest.url";

  protected HttpService httpService;
  protected HttpContext httpContext;
  protected ComponentContext componentContext;
  protected ServiceListener listener = null;

  protected Map<String, GenericServlet> servletMap;
  
  public void activate(ComponentContext componentContext) {
    logger.info("activate()");
    this.componentContext = componentContext;
    this.servletMap = new ConcurrentHashMap<String, GenericServlet>();
    final BundleContext bundleContext = componentContext.getBundleContext();
    listener = new ServiceListener() {
      public void serviceChanged(ServiceEvent event) {
        ServiceReference ref = event.getServiceReference();
        Object service = bundleContext.getService(ref);
        Path pathAnnotation = service.getClass().getAnnotation(Path.class);
        if(pathAnnotation == null) return;
        if(ref.getProperty(SERVICE_PROPERTY) == null) return;
        switch (event.getType()) {
        case ServiceEvent.REGISTERED:
          logger.debug("JAX-RS service registered");
          createEndpoint(ref, service);
          break;
        case ServiceEvent.MODIFIED:
          logger.debug("JAX-RS service modified");
          break;
        case ServiceEvent.UNREGISTERING:
          logger.debug("JAX-RS service unregistered");
          destroyEndpoint(ref.getProperty(SERVICE_PROPERTY).toString());
          break;
        }
      }
    };
    
    // Register any pre-existing JAX-RS services
    for(Bundle bundle : componentContext.getBundleContext().getBundles()) {
      ServiceReference[] refs = bundle.getRegisteredServices();
      if(refs == null) continue;
      for(ServiceReference ref : refs) {
        Object service = bundleContext.getService(ref);
        if(service == null) continue;
        Path pathAnnotation = service.getClass().getAnnotation(Path.class);
        if(pathAnnotation != null) createEndpoint(ref, service);
      }
    }

    // And register any that that appear from now on
    bundleContext.addServiceListener(listener);
  }

  /**
   * Creates a REST endpoint for the JAX-RS annotated service.
   * @param alias The endpoint's root URI
   * @param service The service itself
   */
  protected void createEndpoint(ServiceReference ref, Object service) {
    Object aliasObj = ref.getProperty(SERVICE_PROPERTY);
    if(aliasObj == null) {
      logger.warn("Unable to publish a REST endpoint for {}", service.getClass().getName());
      return;
    } else if ( ! (aliasObj instanceof String)) {
      logger.warn("Property '{}' must be a string, but is a {}", SERVICE_PROPERTY, aliasObj.getClass());
      return;
    }
    String alias = (String)aliasObj;
    CXFNonSpringServlet cxf = new CXFNonSpringServlet();
    try {
      httpService.registerServlet(alias, cxf, new Hashtable<String, String>(), httpContext);
      logger.info("Registered REST endpoint at " + alias);
    } catch (Exception e) {
      logger.info("Problem registering REST endpoint {} : {}", alias, e.getMessage());
      return;
    }
    servletMap.put(alias, cxf);
    Bus bus = cxf.getBus();
    JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
    factory.setBus(bus);
    factory.setServiceClass(service.getClass());
    factory.setResourceProvider(service.getClass(), new SingletonResourceProvider(service));
    factory.setAddress("/");
    ClassLoader bundleClassLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader delegateClassLoader = JAXRSServerFactoryBean.class.getClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(delegateClassLoader);
      factory.create();
    } finally {
      Thread.currentThread().setContextClassLoader(bundleClassLoader);
    }
  }

  /**
   * Removes an endpoint
   * @param alias The URL space to reclaim
   */
  protected void destroyEndpoint(String alias) {
    try {
      httpService.unregister(alias);
      logger.info("Unregistered rest endpoint {}" + alias);
    } catch(Exception e) {
      logger.debug("Unable to unregister " + alias, e);
    }
    // Shut down the CXF servlet for this endpoint
    GenericServlet servlet = servletMap.remove(alias);
    if(servlet != null) servlet.destroy();
  }

  public void deactivate(ComponentContext componentContext) {
    logger.info("deactivate()");
    if (listener != null) {
      componentContext.getBundleContext().removeServiceListener(listener);
    }
    for(String alias : servletMap.keySet()) {
      destroyEndpoint(alias);
    }
    servletMap = null;
  }

  public void setHttpService(HttpService httpService) {
    this.httpService = httpService;
  }

  public void setHttpContext(HttpContext httpContext) {
    this.httpContext = httpContext;
  }
}
