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
package org.opencastproject.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.sql.DataSource;

/**
 * Registers shared persistence properties as a map. This allows bundles using JPA to obtain persistence properties
 * without hard coding values in each bundle.
 */
public class Activator implements BundleActivator {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(Activator.class);

  /**
   * The persistence properties service registration
   */
  protected ServiceRegistration propertiesRegistration = null;

  protected String rootDir;
  protected ServiceRegistration datasourceRegistration;
  protected ComboPooledDataSource pooledDataSource;

  public Activator() {
  }

  public Activator(String rootDir) {
    this.rootDir = rootDir;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void start(BundleContext bundleContext) throws Exception {
    // Use the configured storage directory
    rootDir = bundleContext.getProperty("org.opencastproject.storage.dir") + File.separator + "db";

    // Register the Datasource, defaulting to an embedded H2 database if DB configurations are not specified
    String vendor = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.vendor"), "HSQL");
    String jdbcDriver = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.driver"),
            "org.h2.Driver");
    String jdbcUrl = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.url"), "jdbc:h2:"
            + rootDir);
    String jdbcUser = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.user"), "sa");
    String jdbcPass = getConfigProperty(bundleContext.getProperty("org.opencastproject.db.jdbc.pass"), "sa");
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass(jdbcDriver);
    pooledDataSource.setJdbcUrl(jdbcUrl);
    pooledDataSource.setUser(jdbcUser);
    pooledDataSource.setPassword(jdbcPass);

    Connection connection = null;
    try {
      connection = pooledDataSource.getConnection();
      datasourceRegistration = bundleContext.registerService(DataSource.class.getName(), pooledDataSource, null);
    } catch (SQLException e) {
      logger.error("Connection attempt to {} failed", jdbcUrl);
      logger.error("Exception: ", e);
      throw e;
    } finally {
      if (connection != null)
        connection.close();
    }

    // Register the persistence properties
    @SuppressWarnings("rawtypes")
    Dictionary props = new Hashtable();
    props.put("type", "persistence");
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.target-database", vendor);
    props.put("eclipselink.logging.logger", "JavaLogger");
    if ("true".equalsIgnoreCase(bundleContext.getProperty("org.opencastproject.db.ddl.generation"))) {
      props.put("eclipselink.ddl-generation", "create-tables");
      props.put("eclipselink.ddl-generation.output-mode", "database");
    }
    propertiesRegistration = bundleContext.registerService(Map.class.getName(), props, props);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    if (propertiesRegistration != null)
      propertiesRegistration.unregister();
    if (datasourceRegistration != null)
      datasourceRegistration.unregister();
    DataSources.destroy(pooledDataSource);
  }

  private String getConfigProperty(String config, String defaultValue) {
    return config == null ? defaultValue : config;
  }

}
