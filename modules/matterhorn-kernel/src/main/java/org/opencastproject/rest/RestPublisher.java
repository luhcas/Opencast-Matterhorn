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
import org.apache.cxf.jaxrs.provider.JSONProvider;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
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
  public static final String SERVICE_FILTER = "(" + SERVICE_PROPERTY + "=*)";

  protected HttpService httpService;
  protected HttpContext httpContext;
  protected ComponentContext componentContext;
  protected ServiceTracker tracker = null;

  protected Map<String, GenericServlet> servletMap;

  public void activate(ComponentContext componentContext) {
    logger.info("activate()");
    this.componentContext = componentContext;
    this.servletMap = new ConcurrentHashMap<String, GenericServlet>();
    try {
      tracker = new RestServiceTracker();
    } catch (InvalidSyntaxException e) {
      throw new IllegalStateException(e);
    }
    tracker.open();
  }

  public void deactivate() {
    tracker.close();
  }

  /**
   * Creates a REST endpoint for the JAX-RS annotated service.
   * 
   * @param ref
   *          the osgi service reference
   * @param service
   *          The service itself
   */
  protected void createEndpoint(ServiceReference ref, Object service) {
    Object aliasObj = ref.getProperty(SERVICE_PROPERTY);
    if (aliasObj == null) {
      logger.warn("Unable to publish a REST endpoint for {}", service.getClass().getName());
      return;
    } else if (!(aliasObj instanceof String)) {
      logger.warn("Property '{}' must be a string, but is a {}", SERVICE_PROPERTY, aliasObj.getClass());
      return;
    }
    String alias = (String) aliasObj;
    CXFNonSpringServlet cxf = new CXFNonSpringServlet();
    try {
      httpService.registerServlet(alias, cxf, new Hashtable<String, String>(), httpContext);
      logger.info("Registered REST endpoint at " + alias);
    } catch (Exception e) {
      logger.info("Problem registering REST endpoint {} : {}", alias, e.getMessage());
      return;
    }
    servletMap.put(alias, cxf);

    // Set up cxf
    Bus bus = cxf.getBus();
    JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
    factory.setBus(bus);
    
    // Add custom interceptors
    factory.getInInterceptors().add(new JsonpInInterceptor());
    factory.getOutInterceptors().add(new JsonpPreOutInterceptor());
    factory.getOutInterceptors().add(new JsonpPostOutInterceptor());

    // Remove namespaces from json, since it's not useful for us
    JSONProvider jsonProvider = new JSONProvider();
    jsonProvider.setIgnoreNamespaces(true);
    factory.setProvider(jsonProvider);
    
    // Set the service class
    factory.setServiceClass(service.getClass());
    factory.setResourceProvider(service.getClass(), new SingletonResourceProvider(service));

    // Set the address to '/', which will force the use of the http service
    factory.setAddress("/");
    
    // Use the cxf classloader itself to create the cxf server
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
   * 
   * @param alias
   *          The URL space to reclaim
   */
  protected void destroyEndpoint(String alias) {
    try {
      httpService.unregister(alias);
      logger.info("Unregistered rest endpoint {}", alias);
    } catch (Exception e) {
      logger.info("Unable to unregister {}", alias, e);
    }
    // Shut down the CXF servlet for this endpoint
    GenericServlet servlet = servletMap.remove(alias);
    if (servlet != null)
      servlet.destroy();
  }

  public void deactivate(ComponentContext componentContext) {
    logger.info("deactivate()");
  }

  public void setHttpService(HttpService httpService) {
    this.httpService = httpService;
  }

  public void setHttpContext(HttpContext httpContext) {
    this.httpContext = httpContext;
  }

  /**
   * A custom ServiceTracker that published JAX-RS annotated services with the {@link RestPublisher#SERVICE_PROPERTY}
   * property set to some non-null value.
   */
  class RestServiceTracker extends ServiceTracker {
    
    RestServiceTracker() throws InvalidSyntaxException {
      super(componentContext.getBundleContext(), componentContext.getBundleContext().createFilter(SERVICE_FILTER), null);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
      destroyEndpoint(reference.getProperty(SERVICE_PROPERTY).toString());
      super.removedService(reference, service);
    }

    @Override
    public Object addingService(ServiceReference reference) {
      Object service = componentContext.getBundleContext().getService(reference);
      Path pathAnnotation = service.getClass().getAnnotation(Path.class);
      if (pathAnnotation == null) {
        logger.warn("{} was registered with '{}={}', but the service is not annotated with the JAX-RS "
                + "@Path annotation",
                new Object[] { service, SERVICE_PROPERTY, reference.getProperty(SERVICE_PROPERTY) });
        return null;
      }
      createEndpoint(reference, service);
      return super.addingService(reference);
    }
  }
}
