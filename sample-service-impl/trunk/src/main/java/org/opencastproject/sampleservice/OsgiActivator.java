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

import org.opencastproject.api.OpencastJcrServer;
import org.opencastproject.rest.OpencastRestService;
import org.opencastproject.sampleservice.api.SampleService;
import org.ops4j.pax.web.extender.whiteboard.Resources;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

public class OsgiActivator implements BundleActivator {

  protected ServiceRegistration sampleServiceRegistration;
  protected ServiceRegistration jaxRsRegistration;
  protected ServiceRegistration staticFilesRegistration;

  /**
   * Starts the {@link SampleWebService} at /samplews and registers the static
   * web resources at /samplejs.
   */
  public void start(BundleContext context) throws Exception {
    // Look up the repository service. This should be changed to use a service
    // tracker
    ServiceReference jcrServerRef = context
        .getServiceReference(OpencastJcrServer.class.getName());
    OpencastJcrServer jcrServer = (OpencastJcrServer) context
        .getService(jcrServerRef);

    // Construct the sample service impl
    SampleServiceImpl sampleService = new SampleServiceImpl();
    sampleService.setJcrServer(jcrServer);

    // Register the DOSGI sample service.
    sampleServiceRegistration = OpencastServiceRegistrationUtil.register(
        context, sampleService, SampleService.class, "/samplews");

    // Register the restful service (not working yet)
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
    staticFilesRegistration = context.registerService(
        Resources.class.getName(), new Resources("/js"), staticResourcesProps);
  }
}
