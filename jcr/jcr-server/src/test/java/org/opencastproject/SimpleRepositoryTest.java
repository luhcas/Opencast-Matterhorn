package org.opencastproject;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.TransientRepository;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRepositoryTest {
	private static final Logger logger = LoggerFactory.getLogger(SimpleRepositoryTest.class);
	
	TransientRepository repo;
	
	@Before
	public void setUp() throws Exception {
		repo = new TransientRepository("./src/test/resources/simple-repository.xml", "./target/simple_repo/");
	}

	public void tearDown() throws Exception {
		repo.shutdown();
	}

	@Test
	public void testTransientRepositoryExists() throws Exception {
		assertNotNull(repo);
	}
	
	@Test
	public void testLogin() throws Exception {
		Session session = repo.login();
        try {
            String user = session.getUserID();
            String name = repo.getDescriptor(Repository.REP_NAME_DESC);
            logger.info("Logged in as " + user + " to a " + name + " repository.");
        } finally {
            session.logout();
        }
	}

	@Test
	public void testStoreAndRetrieveContent() throws Exception {
		Session session1 = repo.login(new SimpleCredentials("username", "password".toCharArray()));
		Session session2 = repo.login(new SimpleCredentials("username", "password".toCharArray()));

		try {
            // Store content under session 1
            Node root1 = session1.getRootNode();
            Node hello1 = root1.addNode("hello");
            Node world1 = hello1.addNode("world");
            world1.setProperty("message", "Hello, World!");
            root1.save();
            session1.save();

            // Retrieve content under session2
            Node root2 = session2.getRootNode();
            Node hello2 = root2.getNode("hello");
            Node world2 = hello2.getNode("world");
            logger.info(world2.getPath());
            logger.info(world2.getProperty("message").getString());

            // Remove content
            hello2.remove();
            root2.save();
            session2.save();
            assertFalse(root2.hasNode("hello"));
            assertFalse(root2.hasNode("hello/world"));
        } finally {
            session1.logout();
            session2.logout();
        }
	}
	
}
