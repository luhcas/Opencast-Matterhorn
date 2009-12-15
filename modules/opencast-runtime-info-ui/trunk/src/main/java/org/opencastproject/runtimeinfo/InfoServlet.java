/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.runtimeinfo;

import org.opencastproject.http.StaticResource;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Lists all service endpoints currently registered with the system.
 */
public class InfoServlet extends HttpServlet implements BundleActivator {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(InfoServlet.class);
  private static final String WS_CONTEXT = "org.apache.cxf.ws.httpservice.context";
  private static final String WS_CONTEXT_FILTER = "(" + WS_CONTEXT + "=*)";
  private static final String RS_CONTEXT = "org.apache.cxf.rs.httpservice.context";
  private static final String RS_CONTEXT_FILTER = "(" + RS_CONTEXT + "=*)";
  
  private BundleContext bundleContext;
  /**
   * {@inheritDoc}
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  @SuppressWarnings("unchecked")
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException{
    response.setContentType("application/json");
    JSONObject json = new JSONObject();
    json.put("rest", getRestAsJson());
    json.put("soap", getSoapAsJson());
    json.put("ui", getUserInterfacesAsJson());
    json.writeJSONString(response.getWriter());
  }

  protected String getServerUrl() {
    String serverUrl = bundleContext.getProperty("serverUrl");
    return serverUrl == null ? "http://localhost:8080" : serverUrl;
  }
  
  protected ServiceReference[] getSoapServiceReferences() throws InvalidSyntaxException {
    return bundleContext.getAllServiceReferences(null, WS_CONTEXT_FILTER);
  }

  protected ServiceReference[] getRestServiceReferences() throws InvalidSyntaxException {
    return bundleContext.getAllServiceReferences(null, RS_CONTEXT_FILTER);
  }

  protected ServiceReference[] getUserInterfaceServiceReferences() throws InvalidSyntaxException {
    return bundleContext.getAllServiceReferences(StaticResource.class.getName(), "(&(alias=*)(classpath=*))");
  }

  @SuppressWarnings("unchecked")
  protected JSONArray getSoapAsJson() {
    JSONArray json = new JSONArray();
    ServiceReference[] serviceRefs = null;
    try {
      serviceRefs = getSoapServiceReferences();
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (serviceRefs == null) return json;
    String serverUrl = getServerUrl();
    for (ServiceReference ref : serviceRefs) {
      String description = (String)ref.getProperty(Constants.SERVICE_DESCRIPTION);
      String servletContextPath = (String)ref.getProperty(WS_CONTEXT);
      JSONObject endpoint = new JSONObject();
      endpoint.put("description", description);
      endpoint.put("url", serverUrl + servletContextPath);
      endpoint.put("wsdl", serverUrl + servletContextPath + "/?wsdl");
      json.add(endpoint);
    }
    return json;
  }

  @SuppressWarnings("unchecked")
  protected JSONArray getRestAsJson() {
    JSONArray json = new JSONArray();
    ServiceReference[] serviceRefs = null;
    try {
      serviceRefs = getRestServiceReferences();
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (serviceRefs == null) return json;
    String serverUrl = getServerUrl();
    for (ServiceReference jaxRsRef : serviceRefs) {
      String description = (String)jaxRsRef.getProperty(Constants.SERVICE_DESCRIPTION);
      String servletContextPath = (String)jaxRsRef.getProperty(RS_CONTEXT);
      JSONObject endpoint = new JSONObject();
      endpoint.put("description", description);
      endpoint.put("docs", serverUrl + servletContextPath + "/docs");
      endpoint.put("wadl", serverUrl + servletContextPath + "/?_wadl&type=xml");
      json.add(endpoint);
    }
    return json;
  }
  
  @SuppressWarnings("unchecked")
  protected JSONArray getUserInterfacesAsJson() {
    JSONArray json = new JSONArray();
    ServiceReference[] serviceRefs = null;
    try {
      serviceRefs = getUserInterfaceServiceReferences();
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (serviceRefs == null) return json;
    String serverUrl = getServerUrl();
    for (ServiceReference ref : serviceRefs) {
      String description = (String)ref.getProperty(Constants.SERVICE_DESCRIPTION);
      String alias = (String)ref.getProperty("alias");
      String welcomeFile = (String)ref.getProperty("welcome.file");
      String welcomePath = "/".equals(alias) ? alias + welcomeFile : alias + "/" + welcomeFile;
      String testSuite = (String)ref.getProperty("test.suite");
      JSONObject endpoint = new JSONObject();
      endpoint.put("description", description);
      endpoint.put("welcomepage", serverUrl + welcomePath);
      if(testSuite != null && "true".equalsIgnoreCase(bundleContext.getProperty("testMode"))) {
        String testSuitePath = "/".equals(alias) ? alias + testSuite : alias + "/" + testSuite;
        endpoint.put("testsuite", serverUrl + testSuitePath);
      }
      json.add(endpoint);
    }
    return json;
  }
  
  private ServiceTracker httpTracker;
  
  public void start(BundleContext context) throws Exception {
    logger.debug("start()");
    this.bundleContext = context;
    final HttpServlet servlet = this;
    httpTracker = new ServiceTracker(context, HttpService.class
        .getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        HttpService httpService = (HttpService) context.getService(reference);
        try {
          HttpContext httpContext = httpService.createDefaultHttpContext();
          httpService.registerServlet("/info.json", servlet, null, httpContext);
        } catch (ServletException e) {
          e.printStackTrace();
        } catch (NamespaceException e) {
          e.printStackTrace();
        }
        return super.addingService(reference);
      }
    };
    httpTracker.open();
  }
  
  public void stop(BundleContext context) throws Exception {
    logger.debug("stop()");
    if(httpTracker != null) {
      httpTracker.close();
    }
  }
}
