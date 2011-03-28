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

import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestService;
import org.opencastproject.util.doc.rest.RestDocData;
import org.opencastproject.util.DocUtil;

import static org.opencastproject.rest.RestConstants.SERVICES_FILTER;
import static org.opencastproject.rest.RestConstants.SERVICE_PATH_PROPERTY;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * A bundle activator that registers the REST documentation servlet.
 */
public class Activator extends HttpServlet implements BundleActivator {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(Activator.class);

  /** The query string parameter used to specify a specific service */
  private static final String PATH_PARAM = "path";

  /** java.io serialization UID */
  private static final long serialVersionUID = 6930336096831297329L;

  /** The OSGI bundle context */
  protected BundleContext bundleContext;

  /** The registration for the documentation servlet. */
  protected ServiceRegistration docServletRegistration;

  @Override
  public void start(BundleContext bundleContext) throws Exception {
    this.bundleContext = bundleContext;
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("alias", "/docs.html");
    bundleContext.registerService(Servlet.class.getName(), this, props);
  }

  @Override
  public void stop(BundleContext bundleContext) throws Exception {
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String docPath = req.getParameter(PATH_PARAM);
    if (StringUtils.isBlank(docPath)) {
      // write a listing of the available services and their descriptions
      writeTableOfContents(req, resp);
    } else {
      // write the details for this service
      writeServiceDocumentation(docPath, req, resp);
    }
  }

  private void writeServiceDocumentation(String docPath, HttpServletRequest req, HttpServletResponse resp)
          throws IOException {
    ServiceReference reference = null;
    for (ServiceReference ref : getRestEndpointServices()) {
      String alias = (String) ref.getProperty(SERVICE_PATH_PROPERTY);
      if (docPath.equalsIgnoreCase(alias)) {
        reference = ref;
        break;
      }
    }

    StringBuilder docs = new StringBuilder();

    if (reference == null) {
      docs.append("REST docs unavailable for ");
      docs.append(docPath);
    } else {
      Object endpointService = bundleContext.getService(reference);

      RestService rs = (RestService) endpointService.getClass().getAnnotation(RestService.class);
      if (rs != null) {
        RestDocData data = new RestDocData(rs.name(), rs.title(), docPath, rs.notes());
        data.setAbstract(rs.abstractText());

        for (Method m : endpointService.getClass().getMethods()) {
          RestQuery rq = (RestQuery) m.getAnnotation(RestQuery.class);
          String httpMethodString = null;
          for (Annotation a : m.getAnnotations()) {
            HttpMethod httpMethod = (HttpMethod) a.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethod != null) {
              httpMethodString = httpMethod.value();
            }
          }
          Produces produces = (Produces) m.getAnnotation(Produces.class);
          Path path = (Path) m.getAnnotation(Path.class);
          if ((rq != null) && (httpMethodString != null) && (path != null)) {
            data.addEndpoint(rq, produces, httpMethodString, path);
          }
        }
        String template = DocUtil.loadTemplate("/ui/restdocs/template2.xhtml");
        docs.append(DocUtil.generate(data, template));
      } else {
        docs.append("No documentation has been found for " + endpointService.getClass().getSimpleName());
      }
    }

    resp.setContentType("text/html");
    resp.getWriter().write(docs.toString());
  }

  private void writeTableOfContents(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    StringBuilder toc = new StringBuilder();
    for (ServiceReference ref : getRestEndpointServices()) {
      // TODO: replace this with proper templating
      Object endpoint = bundleContext.getService(ref);
      String alias = (String) ref.getProperty(SERVICE_PATH_PROPERTY);

      toc.append("<div><a href=\"?");
      toc.append(PATH_PARAM);
      toc.append("=");
      toc.append(alias);
      toc.append("\">");
      toc.append("<li>Alias: ");
      toc.append(alias);
      toc.append("</li><li>@Path annotation: ");
      toc.append(endpoint.getClass().getAnnotation(Path.class).value());
      toc.append("</li></a></div><hr/>");
    }

    resp.getWriter().write(toc.toString());
  }

  private ServiceReference[] getRestEndpointServices() {
    try {
      return bundleContext.getAllServiceReferences(null, SERVICES_FILTER);
    } catch (InvalidSyntaxException e) {
      logger.warn("Unable to query the OSGI service registry for all registered rest endpoints");
      return new ServiceReference[0];
    }
  }

}
