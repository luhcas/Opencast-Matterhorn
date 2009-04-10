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
package org.opencastproject.ingest;

import org.opencastproject.api.OpencastJcrServer;
import org.opencastproject.rest.OpencastRestService;
import org.ops4j.pax.web.extender.whiteboard.Resources;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Dictionary;
import java.util.Hashtable;

public class OsgiActivator implements BundleActivator {
  public void start(BundleContext context) throws Exception {
    // Register the media bundle handler when the JCR service is available.
    // Unregister when the JCR service goes away
    ServiceTracker jcrTracker = new ServiceTracker(context,
        OpencastJcrServer.class.getName(), null) {
      ServiceRegistration reg = null;
      @Override
      public Object addingService(ServiceReference reference) {
        OpencastJcrServer jcrServer = (OpencastJcrServer)context.getService(reference);
        MediaBundleUploadHandler uploadHandler = new MediaBundleUploadHandler(jcrServer);
        reg = context.registerService(OpencastRestService.class.getName(), uploadHandler, null);
        return super.addingService(reference);
      }

      @Override
      public void removedService(ServiceReference reference, Object service) {
        reg.unregister();
        super.removedService(reference, service);
      }
    };
    jcrTracker.open();

    // Register the static html resources when the html service is available
    Dictionary<String, String> staticResourcesProps = new Hashtable<String, String>();
    staticResourcesProps.put("alias", "/up");
    context.registerService(Resources.class.getName(), new Resources("/static"), staticResourcesProps);
   
  }

  public void stop(BundleContext arg0) throws Exception {
  }

}
