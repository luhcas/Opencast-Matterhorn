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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Lists all service endpoints currently registered with the system.
 */
public class InfoServlet implements BundleActivator {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = LoggerFactory.getLogger(InfoServlet.class);
  private static final String WS_CONTEXT = "org.apache.cxf.ws.httpservice.context";
  private static final String WS_CONTEXT_FILTER = "(" + WS_CONTEXT + "=*)";
  private static final String RS_CONTEXT = "org.apache.cxf.rs.httpservice.context";
  private static final String RS_CONTEXT_FILTER = "(" + RS_CONTEXT + "=*)";
  
  private BundleContext bundleContext;

  /**
   * Gets the server url including protocol and port as a string.  Returns null if the server url has not been
   * explicitly identified in the container.
   *
   * @return A URL or null if the server has not been set   
   */
  protected String getServerUrl() {
    String serverUrl = bundleContext.getProperty("serverUrl");
    return serverUrl;
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

  
  protected boolean isTestMode() {
    return "true".equalsIgnoreCase(bundleContext.getProperty("testMode"));
  }
  
  @SuppressWarnings("unchecked")
  public void start(BundleContext context) throws Exception {
    logger.debug("start()");
    this.bundleContext = context;
    
    // Register the info servlet
    Dictionary servletProps = new Hashtable();
    servletProps.put("alias", "/info.json");
    servletProps.put("servlet-name", "Info Servlet");
    context.registerService(Servlet.class.getName(), new Info(), servletProps);

    // Register the UI test harness
    if(isTestMode()) {
      Dictionary testServletProps = new Hashtable();
      testServletProps.put("alias", "/TestSuites.html");
      testServletProps.put("servlet-name", "Test Suite Servlet");
      context.registerService(Servlet.class.getName(), new TestSuiteServlet(), testServletProps);
    }
  }
  
  public void stop(BundleContext context) throws Exception {
  }
  
  class Info extends HttpServlet {
    private static final long serialVersionUID = 1L;
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
      //check for relative paths
      if (serverUrl == null){
          serverUrl="";
      }
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
      //check for relative paths
      if (serverUrl == null){
          serverUrl="";
      }
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
      //check for relative paths
      if (serverUrl == null){
          serverUrl="";
      }
      for (ServiceReference ref : serviceRefs) {
        String description = (String)ref.getProperty(Constants.SERVICE_DESCRIPTION);
        String alias = (String)ref.getProperty("alias");
        String welcomeFile = (String)ref.getProperty("welcome.file");
        String welcomePath = "/".equals(alias) ? alias + welcomeFile : alias + "/" + welcomeFile;
        String testSuite = (String)ref.getProperty("test.suite");
        JSONObject endpoint = new JSONObject();
        endpoint.put("description", description);
        endpoint.put("welcomepage", serverUrl + welcomePath);
        if(testSuite != null && isTestMode()) {
          String testSuitePath = "/".equals(alias) ? alias + testSuite : alias + "/" + testSuite;
          endpoint.put("testsuite", serverUrl + testSuitePath);
        }
        json.add(endpoint);
      }
      return json;
    }
  }
  
  class TestSuiteServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      String serverUrl = getServerUrl();
      //check for relative paths
      if (serverUrl == null){
        serverUrl="";
      }
      PrintWriter out = resp.getWriter();
      out.println("<html><head><meta content=\"text/html; charset=ISO-8859-1\" http-equiv=\"content-type\"><title>Test Suite</title></head>");
      out.println("<body><h1>Acceptance Test Suites</h1><ul>");
      try {
        for(ServiceReference ref : getUserInterfaceServiceReferences()) {
          String description = (String)ref.getProperty(Constants.SERVICE_DESCRIPTION);
          String alias = (String)ref.getProperty("alias");
          String testSuite = (String)ref.getProperty("test.suite");
          if(testSuite != null) {
            String testSuitePath = "/".equals(alias) ? serverUrl + alias + testSuite : serverUrl + alias + "/" + testSuite;
            testSuitePath = URLEncoder.encode(testSuitePath, "ISO-8859-1");
            out.println("<li><a href=\"core/TestRunner.html?test=" + testSuitePath + "\">" + description + "</a></li>");
          }
        }
      } catch (InvalidSyntaxException e) {
        out.println("<li>Error: unable to generate this test suite</li>");
        logger.error(e.getMessage());
      }
      out.println("</ul></body></html>");
    }
  }
}
