/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.db;

import org.h2.jdbcx.JdbcConnectionPool;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.sql.DataSource;

/**
 * Starts an embedded database and publishes a javax.sql.DataSource as an OSGi service
 */
public class Activator implements BundleActivator {
  protected String rootDir;
  protected ServiceRegistration datasourceRegistration;
  protected JdbcConnectionPool cp;

  public Activator() {
    this(System.getProperty("java.io.tmpdir") + File.separator + "opencast" + File.separator + "db");
  }
  
  public Activator(String rootDir) {
    this.rootDir = rootDir;
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    cp = JdbcConnectionPool.create("jdbc:h2:" + rootDir, "sa", "sa");
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("vendor", "h2");
    datasourceRegistration = context.registerService(DataSource.class.getName(), cp, props);
   }

  /**
   * {@inheritDoc}
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    datasourceRegistration.unregister();
    cp.dispose();
  }

}
