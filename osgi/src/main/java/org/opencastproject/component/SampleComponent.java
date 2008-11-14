package org.opencastproject.component;

import org.opencastproject.api.OpencastJcrServer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class SampleComponent implements BundleActivator, ServiceListener, SampleService, Runnable {

	ServiceReference serviceReference;
	OpencastJcrServer jcrServer;
	
	public void start(BundleContext context) throws Exception {
		context.addServiceListener(this);
		serviceReference = context.getServiceReference(OpencastJcrServer.class.getName());
		jcrServer = (OpencastJcrServer)context.getService(serviceReference);
		System.out.println("using jcr server " + jcrServer);
		new Thread(this).start();
	}

	public void stop(BundleContext context) throws Exception {
		context.removeServiceListener(this);
		context.ungetService(serviceReference);
		jcrServer = null;
	}

	public void serviceChanged(ServiceEvent event) {
		System.out.println("osgi event: " + event);
	}

	public String getContent(String id) {
		return "Here is the content for id=" + id + "... Nothing from " + jcrServer + " yet!";
	}

	public void run() {
		while(true) {
			System.out.print("running...");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
