package org.opencastproject.rest;

import java.util.Hashtable;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.jboss.resteasy.spi.Registry;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class OsgiActivator implements BundleActivator {
  public static final String urlPattern = "/rest/*";

  protected Registry registry;

  public void start(BundleContext context) throws Exception {
    // Create a servlet to handle /rest URLs
    HttpServletDispatcher restServlet = new HttpServletDispatcher();
    ResteasyBootstrap restServletContextListener = new ResteasyBootstrap();

    // FIXME: This is a hack -- it assumes the existence of a pax web
    // container at the time the bundle starts
    WebContainer pax = (WebContainer) context.getService(context
        .getServiceReference(WebContainer.class.getName()));
    pax.registerEventListener(restServletContextListener, null);
    pax.registerServlet(restServlet, new String[] { urlPattern },
        new Hashtable<String, String>(), null);

    // Add the existing JAX-RS resources
    registry = (Registry) restServlet.getServletContext().getAttribute(
        Registry.class.getName());
    for (ServiceReference jaxRsRef : context.getAllServiceReferences(
        OpencastRestService.class.getName(), null)) {
      registry.addSingletonResource(context.getService(jaxRsRef));
    }

    // Track JAX-RS Resources that are added and removed
    ServiceTracker tracker = new ServiceTracker(context,
        OpencastRestService.class.getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        registry.addSingletonResource(context.getService(reference));
        return super.addingService(reference);
      }

      @Override
      public void removedService(ServiceReference reference,
          Object service) {
        registry.removeRegistrations(context.getService(reference)
            .getClass());
        super.removedService(reference, service);
      }
    };
    tracker.open();
  }

  public void stop(BundleContext arg0) throws Exception {
  }

}
