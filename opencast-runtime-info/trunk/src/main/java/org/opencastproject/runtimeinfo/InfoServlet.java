/**
 *  Copyright 2009 Opencast Project (http://www.opencastproject.org)
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
    writeWsdlEndpoints(writer);
    writeJaxRsEndpoints(writer);
  }

  private void writeWsdlEndpoints(PrintWriter writer) {
    ServiceReference[] serviceRefs = null;
    try {
      serviceRefs = bundleContext.getAllServiceReferences(null, "(org.apache.cxf.ws.httpservice.context=*)");
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (serviceRefs == null) {
      writer.write("<div>There are no wsdl endpoints available.</div>");
    } else {
      writer.write("<div>WSDL Service Endpoints</div>");
      writer.write("<table>");
      for (ServiceReference wsdlRef : serviceRefs) {
        writer.write("<tr>");
        writer.write("<td>");
        writer.write((String)wsdlRef.getProperty("service.description"));
        writer.write("</td>");
        writer.write("<td>");
        String servletContextPath = (String)wsdlRef.getProperty("org.apache.cxf.ws.httpservice.context");
        writer.write("<a href=\"" + servletContextPath + "?wsdl\">" + servletContextPath + "</a>");
        writer.write("</td>");
        writer.write("</tr>");
      }
      writer.write("</table>");
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
      writer.write("<div>There are no JAX-RS endpoints available.</div>");
    } else {
      writer.write("<div>REST Service Endpoints</div>");
      writer.write("<table>");
      for (ServiceReference jaxRsRef : serviceRefs) {
        writer.write("<tr>");
        writer.write("<td>");
        writer.write((String)jaxRsRef.getProperty("service.description"));
        writer.write("</td>");
        writer.write("<td>");
        String servletContextPath = (String)jaxRsRef.getProperty("org.apache.cxf.rs.httpservice.context");
        writer.write("<a href=\"" + servletContextPath + "/docs\">" + servletContextPath + "</a>");
        writer.write("</td>");
        writer.write("</tr>");
      }
      writer.write("</table>");
    }
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
          httpService.registerServlet("/*", servlet, null, httpContext);
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

