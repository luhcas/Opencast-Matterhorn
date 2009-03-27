package org.opencastproject.samplecomponent;

import java.util.Dictionary;
import java.util.Hashtable;

import org.ops4j.pax.web.extender.whiteboard.Resources;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class OsgiActivator implements BundleActivator {
	
	protected ServiceRegistration webServiceRegistration;
	protected ServiceRegistration restServiceRegistration;
	
	public void start(BundleContext context) throws Exception {
		// Register web resources
		Dictionary<String, String> staticResourcesProps =
			new Hashtable<String, String>();
		staticResourcesProps.put( "alias", "/static/js" );
		context.registerService( Resources.class.getName(),
				new Resources( "/js" ), staticResourcesProps );


		// Publish the webservice endpoint
		Dictionary<String, String> wsProps = new Hashtable<String, String>();
		wsProps.put("osgi.remote.interfaces", "*");
		wsProps.put("osgi.remote.configuration.type", "pojo");
		wsProps.put("osgi.remote.configuration.pojo.httpservice.context",
	      "/samplews");
		webServiceRegistration = context.registerService(SampleWebService.class.getName(),
	      new SampleWebServiceImpl(), wsProps);			
			
		// TODO Publish the rest endpoint
	}

    public void stop(BundleContext context) throws Exception {
//    	if(e != null) {
//    		e.stop();
//    	}
    }

}
