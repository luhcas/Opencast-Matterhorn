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
package org.opencastproject.jcr.impl;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.opencastproject.api.OpencastJcrServer;
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

import javax.jcr.Repository;

public class OpencastJcrServerOsgiImpl implements OpencastJcrServer, BundleActivator {
  private static final Logger logger = LoggerFactory
      .getLogger(OpencastJcrServerOsgiImpl.class);

  Repository repo;
  ServiceRegistration serviceRegistration;

  public OpencastJcrServerOsgiImpl() {
  }

  public OpencastJcrServerOsgiImpl(Repository repo) {
    this.repo = repo;
  }

  public Repository getRepository() {
    return repo;
  }

  public void start(BundleContext context) throws Exception {
    String nodeId = System.getProperty("nodeId");
    String repoHome = System.getProperty("repoHome");

    // Just in case
    if (nodeId == null) {
      nodeId = "node1";
    }

    if (repoHome == null) {
      repoHome = System.getProperty("java.io.tmpdir") + "/"
          + System.currentTimeMillis() + "/";
      File dir = new File(repoHome);
      dir.mkdirs();
    }
    try {
      // Find the configuration template
      InputStream configTemplate = context.getBundle().getEntry(
          "/cluster-repository-template.xml").openStream();

      // This is just a hack to replace the node id... better to use an
      // xml parser
      BufferedReader br = new BufferedReader(new InputStreamReader(
          configTemplate));
      StringBuilder sb = new StringBuilder();
      String line = null;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      br.close();
      String templateString = sb.toString();
      String configString = templateString.replaceAll("NODE_ID", nodeId);
      InputStream config = new ByteArrayInputStream(configString
          .getBytes("UTF8"));

      // Build the repository
      RepositoryConfig rc = RepositoryConfig.create(config, repoHome);

      logger.info("Creating a new JCR instance with cluster id="
          + rc.getClusterConfig().getId());

      repo = RepositoryImpl.create(rc);
      serviceRegistration = context.registerService(OpencastJcrServer.class
          .getName(), this, null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void stop(BundleContext context) throws Exception {
    serviceRegistration.unregister();
    ((JackrabbitRepository) repo).shutdown();
  }

}
