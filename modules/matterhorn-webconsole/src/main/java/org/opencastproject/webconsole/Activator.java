/**
 *  Copyright 2009, 2010 The Regents of the University of California
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
package org.opencastproject.webconsole;

import org.opencastproject.http.StaticResource;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Activates the Felix web console, which has been customized for use in matterhorn
 */
public class Activator implements BundleActivator {
  /** The static resource registration */
  ServiceRegistration staticResourceRegistration = null;

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext bundleContext) throws Exception {
    StaticResource staticResource = new StaticResource(bundleContext, "/res", "/system/console/res", null);
    staticResourceRegistration = bundleContext.registerService(StaticResource.class.getName(), staticResource, null);
  }
  
  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    staticResourceRegistration.unregister();
  }
}