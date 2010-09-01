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
package org.opencastproject.runtimeinfo;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides selenium test suites
 */
public class TestSuiteServlet extends HttpServlet {
  /** The serialization version */
  private static final long serialVersionUID = 1L;

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(TestSuiteServlet.class);

  /** This server's URL */
  private String serverUrl;

  /** The bundle context */
  private BundleContext bundleContext;

  /** Whether the test mode setting is enabled */
  private boolean testMode;

  public void activate(ComponentContext cc) {
    logger.debug("start()");
    this.bundleContext = cc.getBundleContext();
    this.serverUrl = bundleContext.getProperty("org.opencastproject.server.url");
    this.testMode = "true".equalsIgnoreCase(bundleContext.getProperty("testMode"));
  }

  protected ServiceReference[] getUserInterfaceServiceReferences() throws InvalidSyntaxException {
    return bundleContext.getAllServiceReferences(Servlet.class.getName(), "(&(alias=*)(classpath=*))");
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    if(!testMode) return;
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
