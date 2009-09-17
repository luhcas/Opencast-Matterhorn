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

import org.opencastproject.util.StaticResource;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

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
  private BundleContext bundleContext;
  /**
   * {@inheritDoc}
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException{
    response.setContentType("text/html");
    PrintWriter writer = response.getWriter();
    writeHtmlHeader(writer);
    writeWsdlEndpoints(writer);
    writeJaxRsEndpoints(writer);
    writeUserInterfaces(writer);
    writeSystemConsole(writer);
    writeHtmlFooter(writer);
  }

  private void writeHtmlHeader(PrintWriter writer) {
    writer.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
    writer.println("<html><head>");
    writer.println("<title>Matterhorn Service Endpoints</title>");
    writer.println("</head>");
    writer.println("<body>");
  }
  
  private void writeWsdlEndpoints(PrintWriter writer) {
    ServiceReference[] serviceRefs = null;
    try {
      serviceRefs = bundleContext.getAllServiceReferences(null, "(org.apache.cxf.ws.httpservice.context=*)");
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (serviceRefs == null) {
      writer.println("<div>There are no wsdl endpoints available.</div>");
    } else {
      writer.println("<table border=\"1\">");
      writer.println("<title>WSDL Service Endpoints</title>");
      writer.println("<th>Base URL</th>");
      writer.println("<th>Description</th>");
      writer.println("<th>WSDL</th>");
      for (ServiceReference wsdlRef : serviceRefs) {
        String description = (String)wsdlRef.getProperty("service.description");
        String servletContextPath = (String)wsdlRef.getProperty("org.apache.cxf.ws.httpservice.context");
        writer.println("<tr>");
        writer.println("<td>");
        writer.println(servletContextPath);
        writer.println("</td>");
        writer.println("<td>");
        writer.println(description);
        writer.println("</td>");
        writer.println("<td>");
        writer.println("<a href=\"" + servletContextPath + "?wsdl\">" + servletContextPath + "</a>");
        writer.println("</td>");
        writer.println("</tr>");
      }
      writer.println("</table>");
    }
  }

  private void writeJaxRsEndpoints(PrintWriter writer) {
    ServiceReference[] serviceRefs = null;
    try {
      serviceRefs = bundleContext.getAllServiceReferences(null, "(org.apache.cxf.rs.httpservice.context=*)");
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (serviceRefs == null) {
      writer.println("<div>There are no JAX-RS endpoints available.</div>");
    } else {
      writer.println("<table border=\"1\">");
      writer.println("<title>REST Service Endpoints</title>");
      writer.println("<th>Base URL</th>");
      writer.println("<th>Description</th>");
      writer.println("<th>Documentation</th>");
      writer.println("<th>WADL</th>");
      for (ServiceReference jaxRsRef : serviceRefs) {
        String description = (String)jaxRsRef.getProperty("service.description");
        String servletContextPath = (String)jaxRsRef.getProperty("org.apache.cxf.rs.httpservice.context");
        writer.println("<tr>");
        writer.println("<td>");
        writer.println(servletContextPath);
        writer.println("</td>");
        writer.println("<td>");
        writer.println(description);
        writer.println("</td>");
        writer.println("<td>");
        writer.println("<a href=\"" + servletContextPath + "/docs\">" + servletContextPath + "/docs</a>");
        writer.println("</td>");
        writer.println("<td>");
        writer.println("<a href=\"" + servletContextPath + "/?_wadl&_type=xml\">" + servletContextPath + "/?_wadl&_type=xml</a>");
        writer.println("</td>");
        writer.println("</tr>");
      }
      writer.println("</table>");
    }
  }
  
  private void writeUserInterfaces(PrintWriter writer) {
    ServiceReference[] serviceRefs = null;
    try {
      serviceRefs = bundleContext.getAllServiceReferences(StaticResource.class.getName(), "(&(alias=*)(classpath=*))");
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (serviceRefs == null) {
      writer.println("<div>There are no user interfaces available.</div>");
    } else {
      writer.println("<table border=\"1\">");
      writer.println("<title>User Interfaces</title>");
      writer.println("<th>Base URL</th>");
      writer.println("<th>Description</th>");
      writer.println("<th>Welcome Page</th>");
      for (ServiceReference uiRef : serviceRefs) {
        String description = (String)uiRef.getProperty("service.description");
        String alias = (String)uiRef.getProperty("alias");
        String welcomeFile = (String)uiRef.getProperty("welcome.file");
        String welcomePath = alias + "/" + welcomeFile;
        writer.println("<tr>");
        writer.println("<td>");
        writer.println(alias);
        writer.println("</td>");
        writer.println("<td>");
        writer.println(description);
        writer.println("</td>");
        writer.println("<td>");
        writer.println("<a href=\"" + welcomePath + "\">" + welcomePath + "</a>");
        writer.println("</td>");
        writer.println("</tr>");
      }
      writer.println("</table>");
    }
  }
  
  private void writeSystemConsole(PrintWriter writer) {
    writer.println("<p>");
    writer.println("<a href=\"/system/console\">System Console</a>");
    writer.println("</p>");
  }

  private void writeHtmlFooter(PrintWriter writer) {
    writer.println("</body></html>");
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
          httpService.registerServlet("/", servlet, null, httpContext);
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

