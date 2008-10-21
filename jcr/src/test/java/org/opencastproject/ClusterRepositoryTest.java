package org.opencastproject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ClusterRepositoryTest {
	private static final Logger logger = LoggerFactory.getLogger(ClusterRepositoryTest.class);
	
	Repository repo;
	ClassPathXmlApplicationContext appContext;
	
	@Before
	public void setUp() throws Exception {
		appContext = new ClassPathXmlApplicationContext("classpath:/cluster-test.xml");
		repo = (Repository) appContext.getBean("jcrRepository");
	}

	public void tearDown() throws Exception {
		appContext.destroy();
	}

	@Test
	public void testTransientRepositoryExists() throws Exception {
		assertNotNull(repo);
	}
	
	
}
