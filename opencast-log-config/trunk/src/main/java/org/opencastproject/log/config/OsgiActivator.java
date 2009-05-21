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
package org.opencastproject.log.config;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class OsgiActivator implements BundleActivator {

  public static final String PAX_LOGGING_SERVICE_PID = "org.ops4j.pax.logging";
  
  public void start(final BundleContext ctx) throws Exception {
    InputStream inStream = getClass().getResourceAsStream("/log4j.properties");
    if (inStream == null) {
        throw new RuntimeException("Failed to find log4j.properties file in bundle!");
    }
    final Properties log4jProperites = new Properties();
    try {
        log4jProperites.load(inStream);
    } catch (IOException e) {
        throw new RuntimeException("Failed to load log4j.properties", e);
    }
   
    // Use a Servicetracker to wait for ConfigurationAdmin service
    ServiceTracker tracker = new ServiceTracker(ctx, ConfigurationAdmin.class.getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        ConfigurationAdmin configAdmin = (ConfigurationAdmin)ctx.getService(reference);
        Configuration configuration = null;
        try {
            configuration = configAdmin.getConfiguration(PAX_LOGGING_SERVICE_PID, null);
        } catch (IOException e) {
            throw new RuntimeException("Failed to get configuration", e);
        }
        try {
            configuration.update(log4jProperites);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return super.addingService(reference);
      }
      @Override
      public void removedService(ServiceReference reference, Object service) {
        super.removedService(reference, service);
      }
    };
    tracker.open();
  }

  public void stop(BundleContext ctx) throws Exception {
  }
}
