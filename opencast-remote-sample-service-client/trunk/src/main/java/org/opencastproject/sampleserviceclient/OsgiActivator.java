package org.opencastproject.sampleserviceclient;

import org.opencastproject.sampleservice.api.SampleService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class OsgiActivator implements BundleActivator {
  ServiceTracker tracker;
  public void start(BundleContext context) throws Exception {
    tracker = new ServiceTracker(context, SampleService.class.getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
          Object result = super.addingService(reference);
          useService(context, reference);
          return result;
      }
  };
  tracker.open();
  }
  
  protected void useService(BundleContext context, ServiceReference reference) {
    final SampleService sampleService = (SampleService)context.getService(reference);

    Thread t = new Thread(new Runnable() {
        public void run() {
          int counter = 1;
          while(tracker != null) {
            try {
              // Use the sample service
              sampleService.getSomething("getSomething() call #" + counter++);

              // Wait a bit
              try {
                Thread.sleep(1000);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
    });
    System.out.println("Starting a thread to call the sample service every second");
    t.start();
  }
  public void stop(BundleContext context) throws Exception {
    tracker.close();
    tracker = null;
  }
}
