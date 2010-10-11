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
package org.opencastproject.remote.impl.webconsole;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;

/**
 * Registers a webconsole plugin to monitor jobs on potentially remote servers.
 */
public class Activator implements BundleActivator {

  /** The osgi service registration for the webconsole plugin */
  ServiceRegistration registration = null;
  
  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void start(BundleContext context) throws Exception {
    Dictionary registrationProps = new Hashtable();
    registrationProps.put("felix.webconsole.label", "jobs");
    registrationProps.put("felix.webconsole.title", "Media Processing Jobs");
    Servlet servlet = new ReceiptsWebconsolePlugin();
    registration = context.registerService(Servlet.class.getName(), servlet, registrationProps);
  }
  
  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    registration.unregister();
  }
}
