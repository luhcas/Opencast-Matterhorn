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
package org.opencastproject.rest;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;

/**
 * Lists all rest and wsdl services currently registered with the system.
 */
public class InfoServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private BundleContext bundleContext;
  public InfoServlet(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException{
    PrintWriter writer = response.getWriter();
    writeWsdls(writer);
    writeJaxRsResources(writer);
  }
  /**
   * Writes links to the DOSGI services that are exposed via WSDL.  This servlet assumes
   * that the DOSGI reference implementation is in use, and that configuration of the exposed
   * services is via pojo.
   * 
   * @param writer
   */
  private void writeWsdls(PrintWriter writer) {
    ServiceReference[] wsdlRefs = null;
    try {
      wsdlRefs = bundleContext.getAllServiceReferences(null,
          "(osgi.remote.configuration.pojo.httpservice.context=*)");
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (wsdlRefs == null) {
      writer.write("<div>There are no WSDL services available.</div>");
    } else {
      writer.write("<div>WSDL Web Services</div>");
      for (ServiceReference wsdlRef : wsdlRefs) {
        writer.write("<div>");
        Object relativePathObject =
          wsdlRef.getProperty("osgi.remote.configuration.pojo.httpservice.context");
        String relativePath = null;
        if(relativePathObject instanceof String[]) {
          relativePath = ((String[])relativePathObject)[0];
        } else {
          relativePath = (String)relativePathObject;
        }
        writer.write("<a href=\"" + relativePath + "?wsdl\">" + relativePath + "</a>\n");
        writer.write("</div>");
      }
    }
  }
  /**
   * Writes links to the restful web services registered in the OSGI environment.
   * 
   * @param writer
   */
  private void writeJaxRsResources(PrintWriter writer) {
    ServiceReference[] jaxRsRefs = null;
    try {
      jaxRsRefs = bundleContext.getAllServiceReferences(null, OsgiActivator.REST_FILTER);
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (jaxRsRefs == null) {
      writer.write("<div>There are no REST services available.</div>");
    } else {
      writer.write("<div>REST Services</div>");
      for (ServiceReference jaxRsRef : jaxRsRefs) {
        writer.write("<div>");
        Object jaxRsResource = bundleContext.getService(jaxRsRef);
        Path pathAnnotation = jaxRsResource.getClass().getAnnotation(Path.class);
        String relativePath = ResteasyServlet.SERVLET_PATH + pathAnnotation.value();
        writer.write("<a href=\"" + relativePath + "\">" + relativePath + "</a>\n");
        writer.write("</div>");
      }
    }
  }
}
