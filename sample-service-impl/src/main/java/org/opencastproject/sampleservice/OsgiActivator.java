package org.opencastproject.sampleservice;

import java.util.Dictionary;
import java.util.Hashtable;

import org.opencastproject.sampleservice.api.SampleService;
import org.ops4j.pax.web.extender.whiteboard.Resources;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class OsgiActivator implements BundleActivator {
	
	protected ServiceRegistration sampleServiceRegistration;

	/**
	 * Starts the {@link SampleWebService} at /samplews and registers the
	 * static web resources at /samplejs.
	 */
	public void start(BundleContext context) throws Exception {
		sampleServiceRegistration = OpencastServiceRegistration.register(
				context, new SampleServiceImpl(), SampleService.class,
				"/samplews");
		registerStaticWebResources(context);
	}

    public void stop(BundleContext context) throws Exception {
    	sampleServiceRegistration.unregister();
    }

    public void registerStaticWebResources(BundleContext context) {
		Dictionary<String, String> staticResourcesProps =
			new Hashtable<String, String>();
		staticResourcesProps.put( "alias", "/samplejs" );
		context.registerService( Resources.class.getName(),
				new Resources( "/js" ), staticResourcesProps );
    }
}
