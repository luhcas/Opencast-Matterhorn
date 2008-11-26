package org.opencastproject.samplecomponent;

import org.opencastproject.api.OpencastJcrServer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class OsgiActivator implements BundleActivator {
	ServiceReference jcrServiceReference;
	ServiceReference httpServiceReference;

	public void start(BundleContext context) throws Exception {
		// Attach to the JCR server
		jcrServiceReference = context.getServiceReference(OpencastJcrServer.class.getName());
//		OpencastJcrServer jcrServer = (OpencastJcrServer)context.getService(jcrServiceReference);

//		httpServiceReference = context.getServiceReference("org.osgi.service.http.HttpService");
//		HttpService httpServer = (HttpService)context.getService(httpServiceReference);
//		HttpContext httpContext = httpServer.createDefaultHttpContext();
//		httpServer.registerServlet("/sample", new SampleServlet(), null, httpContext);



	}

    public void stop(BundleContext context) throws Exception {
		context.ungetService(jcrServiceReference);
		context.ungetService(httpServiceReference);
    }

}
