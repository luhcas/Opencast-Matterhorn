package org.opencastproject.sampleservice;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.Path;

import org.opencastproject.rest.OpencastRestService;
import org.opencastproject.sampleservice.api.SampleService;
import org.ops4j.pax.web.extender.whiteboard.Resources;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class OsgiActivator implements BundleActivator {

  protected ServiceRegistration sampleServiceRegistration;
  protected ServiceRegistration jaxRsRegistration;
  protected ServiceRegistration staticFilesRegistration;

  /**
   * Starts the {@link SampleWebService} at /samplews and registers the static
   * web resources at /samplejs.
   */
  public void start(BundleContext context) throws Exception {
    // Register the (distributed) OSGI service.
    sampleServiceRegistration = OpencastServiceRegistrationUtil.register(
        context, new SampleServiceImpl(), SampleService.class,
        "/samplews");

    // Register the restful service
    jaxRsRegistration = context.registerService(OpencastRestService.class
        .getName(), new SampleRestService(), null);

    // Register the static web resources
    registerStaticWebResources(context);
  }

  public void stop(BundleContext context) throws Exception {
    sampleServiceRegistration.unregister();
    jaxRsRegistration.unregister();
    staticFilesRegistration.unregister();
  }

  public void registerStaticWebResources(BundleContext context) {
    Dictionary<String, String> staticResourcesProps = new Hashtable<String, String>();
    staticResourcesProps.put("alias", "/samplejs");
    staticFilesRegistration = context.registerService(Resources.class
        .getName(), new Resources("/js"), staticResourcesProps);
  }
}
