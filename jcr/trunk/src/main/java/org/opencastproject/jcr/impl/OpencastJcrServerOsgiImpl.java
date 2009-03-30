package org.opencastproject.jcr.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Repository;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import org.opencastproject.api.OpencastJcrServer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpencastJcrServerOsgiImpl implements OpencastJcrServer,
    BundleActivator {
  private static final Logger logger = LoggerFactory
      .getLogger(OpencastJcrServerOsgiImpl.class);

  String nodeId, repoHome;

  Repository repo;
  ServiceRegistration serviceRegistration;
  OpencastJcrServerOsgiImpl impl;

  public OpencastJcrServerOsgiImpl() {
  }

  public OpencastJcrServerOsgiImpl(String nodeId, String repoHome) {
    this.nodeId = nodeId;
    this.repoHome = repoHome;
  }

  public Repository getRepository() {
    return repo;
  }

  public void start(BundleContext context) throws Exception {
    nodeId = System.getProperty("nodeId");
    repoHome = System.getProperty("repoHome");

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
    impl = new OpencastJcrServerOsgiImpl(nodeId, repoHome);
    serviceRegistration = context.registerService(OpencastJcrServer.class
        .getName(), impl, null);
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
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void stop(BundleContext context) throws Exception {
    serviceRegistration.unregister();
    ((JackrabbitRepository) repo).shutdown();
    repo = null;
  }

}
