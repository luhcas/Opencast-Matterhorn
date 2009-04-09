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

import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.jboss.resteasy.spi.Registry;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Dictionary;
import java.util.Hashtable;

public class OsgiActivator implements BundleActivator {
  protected Registry registry;

  public void start(BundleContext context) throws Exception {
    // Create a servlet to handle /rest URLs
    ResteasyServlet restServlet = new ResteasyServlet();
    ResteasyBootstrap restServletContextListener = new ResteasyBootstrap();

    // FIXME: This is a hack -- it assumes the existence of a pax web
    // container (and that it's present and active at the time this bundle starts)
    WebContainer pax = (WebContainer) context.getService(context
        .getServiceReference(WebContainer.class.getName()));
    Dictionary<String, String> contextParams = new Hashtable<String, String>();
    HttpContext httpContext = pax.createDefaultHttpContext();
    contextParams.put("resteasy.scan", "false");
    contextParams.put("resteasy.servlet.mapping.prefix", ResteasyServlet.SERVLET_PATH);
    pax.setContextParam(contextParams, httpContext);
    pax.registerEventListener(restServletContextListener, httpContext);
    pax.registerServlet(restServlet, "RestEasy", new String[] { ResteasyServlet.SERVLET_PATH + "/*" },
        new Hashtable<String, String>(), null);

    // Add the existing JAX-RS resources
    registry = (Registry) restServlet.getServletContext().getAttribute(
        Registry.class.getName());
    ServiceReference[] jaxRsRefs = context.getAllServiceReferences(OpencastRestService.class.getName(), null);
    if (jaxRsRefs != null) {
      for (ServiceReference jaxRsRef : jaxRsRefs) {
        registry.addSingletonResource(context.getService(jaxRsRef));
      }
    }

    // Track JAX-RS Resources that are added and removed
    ServiceTracker tracker = new ServiceTracker(context,
        OpencastRestService.class.getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        OpencastRestService jaxRsResource = (OpencastRestService)context.getService(reference);
        registry.addSingletonResource(jaxRsResource);
        return super.addingService(reference);
      }

      @Override
      public void removedService(ServiceReference reference, Object service) {
        registry.removeRegistrations(context.getService(reference).getClass());
        super.removedService(reference, service);
      }
    };
    tracker.open();
  }

  public void stop(BundleContext arg0) throws Exception {
  }

}
