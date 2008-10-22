package org.opencastproject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Repository;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;

/**
 * Manages the lifecycle of a JCR Repository instance, based on the constructor-provided
 * node id and home directory.
 */
public class OpencastJcrServer {
	private static final Logger logger = LoggerFactory.getLogger(OpencastJcrServer.class);
	
	Repository repo;
	
	public OpencastJcrServer(String nodeId, String repoHome) {
		try {
		    // Find the configuration template
			InputStream configTemplate = new DefaultResourceLoader().getResource(
	          "classpath:cluster-repository-template.xml").getInputStream();

		    // This is just a hack to replace the node id... better to use an xml parser
		    BufferedReader br = new BufferedReader(new InputStreamReader(configTemplate));
		    StringBuilder sb = new StringBuilder();
		    String line = null;
		    while ((line = br.readLine()) != null) {
		    	sb.append(line);
		    }
		    br.close();
		    String templateString = sb.toString();
		    String configString = templateString.replaceAll("NODE_ID", nodeId);
		    InputStream config = new ByteArrayInputStream(configString.getBytes("UTF8"));

		    // Build the repository
		    RepositoryConfig rc = RepositoryConfig.create(config, repoHome);
		    
		    logger.info("Creating a new JCR instance with cluster id=" + rc.getClusterConfig().getId());

		    repo = RepositoryImpl.create(rc);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public void closeRepository() {
		((JackrabbitRepository)repo).shutdown();
	}
	
	public Repository getRepository() {
		return repo;
	}

}
