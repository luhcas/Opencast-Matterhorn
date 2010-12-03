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

import org.opencastproject.http.SharedHttpContext;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.JSONProvider;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;
import javax.ws.rs.Path;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Listens for JAX-RS annotated services and publishes them to the global URL space using a single shared HttpContext.
 */
public class RestPublisher {
  /** The logger **/
  private static final Logger logger = LoggerFactory.getLogger(RestPublisher.class);

  /** The service property indicating the type of service. This is an arbitrary ID, not necessarily a java interface. */
  public static final String SERVICE_TYPE_PROPERTY = "opencast.service.type";

  /** The service property indicating the URL path that the service is attempting to claim */
  public static final String SERVICE_PATH_PROPERTY = "opencast.service.path";

  /** The service property indicating that this service should be registered in the remote service registry */
  public static final String SERVICE_JOBPRODUCER_PROPERTY = "opencast.service.jobproducer";

  /** The rest publisher looks for any non-servlet with the 'opencast.service.path' property */
  public static final String SERVICE_FILTER = "(&(!(objectClass=javax.servlet.Servlet))("
          + RestPublisher.SERVICE_PATH_PROPERTY + "=*))";

  /** A map that sets default xml namespaces in {@link XMLStreamWriter}s */
  protected static final ConcurrentHashMap<String, String> NAMESPACE_MAP;
  
  static {
    NAMESPACE_MAP = new ConcurrentHashMap<String, String>();
    NAMESPACE_MAP.put("http://www.w3.org/2001/XMLSchema-instance", "");
  }

  /** The rest publisher's OSGI declarative services component context */
  protected ComponentContext componentContext;

  /** A service tracker that monitors JAX-RS annotated services, (un)publishing servlets as they (dis)appear */
  protected ServiceTracker tracker = null;

  /** The base URL for this server */
  protected String baseServerUri;

  /** Holds references to servlets that this class publishes, so they can be unpublished later  */
  protected Map<String, ServiceRegistration> servletRegistrationMap;

  /**  Activates this rest publisher */
  protected void activate(ComponentContext componentContext) {
    logger.debug("activate()");
    this.baseServerUri = componentContext.getBundleContext().getProperty("org.opencastproject.server.url");
    this.componentContext = componentContext;
    this.servletRegistrationMap = new ConcurrentHashMap<String, ServiceRegistration>();
    try {
      tracker = new JaxRsServiceTracker();
    } catch (InvalidSyntaxException e) {
      throw new IllegalStateException(e);
    }
    tracker.open();
  }

  /**
   * Deactivates the rest publisher
   */
  protected void deactivate() {
    logger.debug("deactivate()");
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
    CXFNonSpringServlet cxf = new CXFNonSpringServlet();
    ServiceRegistration reg = null;
    String serviceType = (String) ref.getProperty(SERVICE_TYPE_PROPERTY);
    String servicePath = (String) ref.getProperty(SERVICE_PATH_PROPERTY);
    boolean jobProducer = Boolean.parseBoolean((String) ref.getProperty(SERVICE_JOBPRODUCER_PROPERTY));
    try {
      Dictionary<String, Object> props = new Hashtable<String, Object>();
      props.put("contextId", SharedHttpContext.HTTP_CONTEXT_ID);
      props.put("alias", servicePath);
      props.put(SERVICE_TYPE_PROPERTY, serviceType);
      props.put(SERVICE_PATH_PROPERTY, servicePath);
      props.put(SERVICE_JOBPRODUCER_PROPERTY, jobProducer);
      reg = componentContext.getBundleContext().registerService(Servlet.class.getName(), cxf, props);
    } catch (Exception e) {
      logger.info("Problem registering REST endpoint {} : {}", servicePath, e.getMessage());
      return;
    }
    servletRegistrationMap.put(servicePath, reg);

    // Set up cxf
    Bus bus = cxf.getBus();
    JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
    factory.setBus(bus);

    JSONProvider jsonProvider = new MatterhornJSONProvider();
    jsonProvider.setIgnoreNamespaces(true);
    jsonProvider.setNamespaceMap(NAMESPACE_MAP);
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
    logger.info("Registered REST endpoint at " + servicePath);
  }

  /**
   * Removes an endpoint
   * 
   * @param alias
   *          The URL space to reclaim
   */
  protected void destroyEndpoint(String alias) {
    ServiceRegistration reg = servletRegistrationMap.remove(alias);
    if (reg != null) {
      reg.unregister();
    }
  }

  /**
   * Extends the CXF JSONProvider for the grand purpose of removing '@' symbols from json and padded jsonp.
   */
  private static class MatterhornJSONProvider extends JSONProvider {
    private final Charset UTF8 = Charset.forName("utf-8");

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.cxf.jaxrs.provider.JSONProvider#createWriter(java.lang.Object, java.lang.Class,
     *      java.lang.reflect.Type, java.lang.String, java.io.OutputStream, boolean)
     */
    protected XMLStreamWriter createWriter(Object actualObject, Class<?> actualClass, Type genericType, String enc,
            OutputStream os, boolean isCollection) throws Exception {
      Configuration c = new Configuration(NAMESPACE_MAP);
      c.setSupressAtAttributes(true);
      MappedNamespaceConvention convention = new MappedNamespaceConvention(c);
      return new MappedXMLStreamWriter(convention, new OutputStreamWriter(os, UTF8)) {
        @Override
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }
        @Override
        public void writeStartElement(String uri, String local) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }
        @Override
        public void setPrefix(String pfx, String uri) throws XMLStreamException {}
        @Override
        public void setDefaultNamespace(String uri) throws XMLStreamException {}
      };
    }
  }

  /**
   * A custom ServiceTracker that published JAX-RS annotated services with the {@link RestPublisher#SERVICE_PROPERTY}
   * property set to some non-null value.
   */
  public class JaxRsServiceTracker extends ServiceTracker {

    JaxRsServiceTracker() throws InvalidSyntaxException {
      super(componentContext.getBundleContext(), componentContext.getBundleContext().createFilter(SERVICE_FILTER), null);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
      String servicePath = (String) reference.getProperty(SERVICE_PATH_PROPERTY);
      destroyEndpoint(servicePath);
      super.removedService(reference, service);
    }

    @Override
    public Object addingService(ServiceReference reference) {
      Object service = componentContext.getBundleContext().getService(reference);
      Path pathAnnotation = service.getClass().getAnnotation(Path.class);
      if (pathAnnotation == null) {
        logger.warn("{} was registered with '{}={}', but the service is not annotated with the JAX-RS "
                + "@Path annotation",
                new Object[] { service, SERVICE_PATH_PROPERTY, reference.getProperty(SERVICE_PATH_PROPERTY) });
        return null;
      }
      createEndpoint(reference, service);
      return super.addingService(reference);
    }
  }
}
