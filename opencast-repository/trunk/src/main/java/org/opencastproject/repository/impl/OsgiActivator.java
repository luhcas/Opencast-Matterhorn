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
package org.opencastproject.repository.impl;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import org.opencastproject.repository.api.OpencastRepository;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Repository;

public class OsgiActivator implements BundleActivator {
  private static final Logger logger = LoggerFactory
      .getLogger(OsgiActivator.class);
  private ServiceRegistration serviceRegistration;
  private ServiceRegistration repoRegistration;
  private ServiceRegistration restRegistration;
  private Repository jcrRepository;

  public void start(BundleContext context) throws Exception {
    // Open the configuration template from within this bundle
    InputStream configTemplate = context.getBundle().getEntry(
        "/cluster-repository-template.xml").openStream();

    // Get the configuration settings from the framework or system properties
    String nodeId = context.getProperty("matterhorn.jcr.nodeId");
    String repoHome = context.getProperty("matterhorn.jcr.repoPath");
    String dbUrl = context.getProperty("matterhorn.jcr.db.url");
    String dbDriver = context.getProperty("matterhorn.jcr.db.driver");
    String dbUser = context.getProperty("matterhorn.jcr.db.user");
    String dbPass = context.getProperty("matterhorn.jcr.db.password");
    String pm = context.getProperty("matterhorn.jcr.persistence.manager");
    String dataStorePath = context.getProperty("matterhorn.jcr.datastore.path");

    // load defaults for the values not found
    File currentDir = new File( System.getProperty("user.dir") );
    if (nodeId == null) {
        nodeId = "matterhorn";
        System.out.println("Opencast Repository: Using default nodeId: " + nodeId);
    }
    if (repoHome == null) {
        File rh = new File(currentDir, "matterhorn"+File.pathSeparatorChar+"repo");
        if (! rh.exists()) {
            if (currentDir.canWrite() && rh.mkdirs()) {
                // created the dir
            } else {
                throw new IllegalStateException("No matterhorn.jcr.repoPath specified and the default path ("+rh+") cannot be written to");
            }
        }
        repoHome = rh.getAbsolutePath();
        System.out.println("Opencast Repository: Using default repoPath: " + repoHome);
    }
    if (dbUrl == null || dbDriver == null || pm == null) {
        // force usage of the derby stuff if any of these are null
        System.out.println("Opencast Repository: Using default derby database and jcr persistence manager");
        File mh = new File(currentDir, "matterhorn");
        if (! mh.exists()) {
            if (currentDir.canWrite() && mh.mkdirs()) {
                // created the dir
            } else {
                throw new IllegalStateException("No matterhorn.jcr.db.url specified and the default path ("+mh+") cannot be written to");
            }
        }
        dbDriver = "org.apache.derby.jdbc.EmbeddedDriver";
        dbUrl = "jdbc:derby:matterhorn/db;create=true";
        dbUser = "sa";
        dbPass = "";
        pm = "org.apache.jackrabbit.core.persistence.bundle.DerbyPersistenceManager";
    }
    if (dbUser == null) {
        dbUser = "";
    }
    if (dbPass == null) {
        dbPass = "";
    }
    if (dataStorePath == null) {
        File ds = new File(currentDir, "matterhorn"+File.pathSeparatorChar+"datastore");
        if (! ds.exists()) {
            if (currentDir.canWrite() && ds.mkdirs()) {
                // created the dir
            } else {
                throw new IllegalStateException("No matterhorn.jcr.datastore.path specified and the default path ("+ds+") cannot be written to");
            }
        }
        dataStorePath = ds.getAbsolutePath();
        System.out.println("Opencast Repository: Using default datastore.path: " + dataStorePath);
    }
    
    // This is just a hack... better to use an xml parser
    BufferedReader br = new BufferedReader( new InputStreamReader(configTemplate) );
    StringBuilder sb = new StringBuilder();
    String line = null;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    br.close();
    configTemplate.close();
    String templateString = sb.toString();
    String configString = templateString.replaceAll("PERSISTENCE_MGR", pm)
        .replaceAll("NODE_ID", nodeId).replaceAll("DB_DRIVER", dbDriver)
        .replaceAll("DB_USER", dbUser).replaceAll("DB_PASS", dbPass)
        .replaceAll("DB_URL", dbUrl).replaceAll("DATA_STORE_PATH", dataStorePath);

    InputStream config = new ByteArrayInputStream(configString.getBytes("UTF8"));

    logger.info(configString);
    
    // Build the repository
    RepositoryConfig rc = RepositoryConfig.create(config, repoHome);

    logger.info("Creating a new JCR instance with cluster id="
        + rc.getClusterConfig().getId());

    jcrRepository = RepositoryImpl.create(rc);
    OpencastRepository opencastRepo = new OpencastRepositoryImpl(jcrRepository);
    repoRegistration = context.registerService(Repository.class.getName(), jcrRepository, null);
    serviceRegistration = context.registerService(OpencastRepository.class.getName(),
        opencastRepo, null);
    Dictionary<String, String> props = new Hashtable<String, String>();
    props.put("opencast.rest.service", Boolean.TRUE.toString());
    restRegistration = context.registerService(RepositoryRestService.class.getName(),
        new RepositoryRestService(opencastRepo), props);
  }

  public void stop(BundleContext context) throws Exception {
    repoRegistration.unregister();
    serviceRegistration.unregister();
    restRegistration.unregister();
    if (jcrRepository != null) {
      ((JackrabbitRepository) jcrRepository).shutdown();
    }
  }
}
