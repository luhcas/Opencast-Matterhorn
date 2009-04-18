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
package org.opencastproject.sampleservice;

import org.opencastproject.repository.api.OpencastRepository;
import org.opencastproject.rest.OpencastRestService;
import org.opencastproject.sampleservice.api.SampleService;
import org.ops4j.pax.web.extender.whiteboard.Resources;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Dictionary;
import java.util.Hashtable;

public class OsgiActivator implements BundleActivator {

  protected ServiceRegistration sampleServiceRegistration;
  protected ServiceRegistration jaxRsRegistration;
  protected ServiceRegistration staticJsRegistration;
  protected ServiceRegistration staticHtmlRegistration;
  protected ServiceTracker jcrTracker;
  
  /**
   * Starts the {@link SampleWebService} at /samplews and registers the static
   * web resources at /samplejs.
   */
  public void start(BundleContext context) throws Exception {
    jcrTracker = new ServiceTracker(context,
        OpencastRepository.class.getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        OpencastRepository repo = (OpencastRepository)context.getService(reference);

        // Register the DOSGI sample service.
        sampleServiceRegistration = OpencastServiceRegistrationUtil.register(
            context, new SampleServiceImpl(repo), SampleService.class, "/samplews");

        // Register the restful service
        jaxRsRegistration = context.registerService(OpencastRestService.class
            .getName(), new SampleRestService(repo), null);
        return super.addingService(reference);
      }

      @Override
      public void removedService(ServiceReference reference, Object service) {
        sampleServiceRegistration.unregister();
        jaxRsRegistration.unregister();
        super.removedService(reference, service);
      }
    };
    jcrTracker.open();

    // Register the static web resources.  This handles the http service tracking automatically
    Dictionary<String, String> staticJsProps = new Hashtable<String, String>();
    staticJsProps.put("alias", "/samplejs");
    staticJsRegistration = context.registerService(
        Resources.class.getName(), new Resources("/js"), staticJsProps);
    Dictionary<String, String> staticHtmlProps = new Hashtable<String, String>();
    staticHtmlProps.put("alias", "/samplehtml");
    staticHtmlRegistration = context.registerService(
        Resources.class.getName(), new Resources("/html"), staticHtmlProps);
    
  }

  public void stop(BundleContext context) throws Exception {
    sampleServiceRegistration.unregister();
    jaxRsRegistration.unregister();
    staticJsRegistration.unregister();
    staticHtmlRegistration.unregister();
    jcrTracker.close();
  }
}
